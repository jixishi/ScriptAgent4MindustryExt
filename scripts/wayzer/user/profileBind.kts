package wayzer.user

import cf.wayzer.placehold.PlaceHoldApi.with
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.random.Random

export(::generate)// Generate binding code
export(::check)// Detection of binding code

class ExpireMutableMap<K, V> {
    class ExpireItem<V>(val time: Long, val v: V) : Comparable<ExpireItem<V>> {
        override fun compareTo(other: ExpireItem<V>): Int {
            return compareValuesBy(this, other) { it.time }
        }
    }

    private val map = mutableMapOf<K, V>()
    private val expireQueue = PriorityQueue<ExpireItem<K>>()
    fun add(expireTime: Long, key: K, value: V): Boolean {
        if (key in this) return false
        map[key] = value
        return expireQueue.add(ExpireItem(System.currentTimeMillis() + expireTime, key))
    }

    fun checkOut() {
        while (true) {
            val item = expireQueue.peek() ?: break
            if (item.time >= System.currentTimeMillis()) break
            expireQueue.poll()
            map.remove(item.v)
        }
    }

    operator fun get(key: K): V? {
        checkOut()
        return map[key]
    }

    fun removeValue(v: V) {
        map.entries.removeIf { it.value == v }
    }

    private operator fun contains(key: K): Boolean = get(key) != null
}

val expireTime: Duration by config.key(Duration.ofMinutes(10), "Random binding code expiration time")
val map = ExpireMutableMap<Int, Long>()

fun generate(qq: Long): Int {
    map.removeValue(qq)
    var code: Int
    do {
        code = Random.nextInt(1000000)
    } while (!map.add(expireTime.toMillis(), code, qq))
    return code
}

fun check(code: Int): Long? {
    return map[code]
}

onEnable {
    launch {
        while (true) {
            delay(60_000)
            map.checkOut()
        }
    }
}

command("genCode", "Administration command: Generate random binding codes for users") {
    usage = "<qq>"
    permission = "wayzer.user.genCode"
    body {
        val qq = arg.firstOrNull()?.toLongOrNull() ?: returnReply("[red]Please enter the correct qq number".with())
        reply("[green]Binding code{code},expiration date:{expireTime}".with("code" to generate(qq), "expireTime" to expireTime))
    }
}

command("bind", "Binding users") {
    usage = "<Six digit code>";this.type = CommandType.Client
    body {
        val qq = arg.firstOrNull()?.toIntOrNull()?.let(::check)
            ?: returnReply("[red]Please enter the correct 6-digit binding code, if not, you can find the robot in the group to get".with())
        PlayerData[player!!.uuid()].apply {
            if (profile != null)
                returnReply("[red]You have bound users, if you need to unbind, please contact the administrator".with())
            @Suppress("EXPERIMENTAL_API_USAGE")
            transaction {
                profile = PlayerProfile.getOrCreate(qq, true).apply {
                    lastTime = Instant.now()
                }
                save()
            }
            val finishAchievement =
                depends("wayzer/user/achievement")?.import<(PlayerProfile, String, Int, Boolean) -> Unit>("finishAchievement")
            finishAchievement?.invoke(profile!!, "Binding account", 100, false)
        }
        reply("[green]Bind account[yellow]$qq[green] successfully.".with())
    }
}
