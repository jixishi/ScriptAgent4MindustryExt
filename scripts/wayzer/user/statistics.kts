package wayzer.user

import cf.wayzer.placehold.DynamicVar
import mindustry.game.EventType
import mindustry.game.Gamemode
import mindustry.game.Team
import mindustry.gen.Groups
import mindustry.world.Block
import mindustry.world.blocks.distribution.Conveyor
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.Serializable
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.math.ceil
import kotlin.math.min

data class StatisticsData(
        var playedTime: Int = 0,
        var idleTime: Int = 0,
        var buildScore: Float = 0f,
        var breakBlock: Int = 0,
        var lastActive: Long = 0,
        @Transient var pvpTeam: Team = Team.sharded
) : Serializable {
    val win get() = state.rules.pvp && pvpTeam == teamWin

    //Weighted Score
    val score get() = playedTime - 0.8 * idleTime + 0.6 * buildScore + if (win) 1200 * (1 - idleTime / playedTime) else 0

    //Settlement experience calculation
    val exp get() = min(ceil(score * 15 / 3600).toInt(), 40)//3600 points are capped at 15,40

    companion object {
        lateinit var teamWin: Team
    }
}

val Block.buildScore: Float
    get() {
        //If there is a better construction points rule, please modify it here
        return buildCost / 60f //Construction time (in seconds)
    }
val Player.isIdle get() = (unit().vel.isZero(1e-9F) || (unit().onSolid() && tileOn()?.block() is Conveyor)) && !unit().isBuilding && !shooting() && textFadeTime < 0
val Player.active: Boolean
    get() {//Whether to hang for more than 10 seconds
        if (!isIdle) data.lastActive = System.currentTimeMillis()
        return System.currentTimeMillis() - data.lastActive < 10_000
    }

fun active(p: Player) = p.active
export(::active)

@Savable
val statisticsData = mutableMapOf<String, StatisticsData>()
customLoad(::statisticsData) { statisticsData += it }
val Player.data get() = statisticsData.getOrPut(uuid()) { StatisticsData() }
registerVarForType<StatisticsData>().apply {
    registerChild("playedTime", "Online time in this bureau", DynamicVar.obj { Duration.ofSeconds(it.playedTime.toLong()) })
    registerChild("idleTime", "Online time in this bureau", DynamicVar.obj { Duration.ofSeconds(it.idleTime.toLong()) })
    registerChild("buildScore", "Construction Credits") { _, obj, p ->
        if (!p.isNullOrBlank()) p.format(obj.buildScore)
        else obj.buildScore
    }
    registerChild("breakBlock", "Number of destroyed squares", DynamicVar.obj { it.breakBlock })
}
registerVarForType<Player>().apply {
    registerChild("statistics", "Game Statistics", DynamicVar.obj { it.data })
}
onDisable {
    PlaceHoldString.bindTypes.remove(StatisticsData::class.java)//Local class to prevent leakage
}

listen<EventType.ResetEvent> {
    statisticsData.clear()
}
listen<EventType.PlayerJoin> {
    it.player.data.pvpTeam = it.player.team()
}

onEnable {
    launch {
        while (true) {
            delay(1000)
            Groups.player.forEach {
                it.data.playedTime++
                if (!it.active)
                    it.data.idleTime++
            }
        }
    }
}
listen<EventType.BlockBuildEndEvent> {
    it.unit.player?.data?.apply {
        if (it.breaking)
            breakBlock++
        else
            buildScore += it.tile.block().buildScore
    }
}

listen<EventType.GameOverEvent> { event ->
    onGameOver(event.winner)
}

fun onGameOver(winner: Team) {
    val gameTime by PlaceHold.reference<Duration>("state.gameTime")
    if (state.rules.infiniteResources || state.rules.editor) {
        return broadcast(
            """
            [yellow]Game duration: {gameTime:minutes}
            [yellow]Sandbox or editor mode, no contribution
        """.trimIndent().with("gameTime" to gameTime)
        )
    }

    StatisticsData.teamWin = if (state.rules.mode() != Gamemode.survival) winner else Team.sharded
    var totalTime = 0
    val sortedData = statisticsData.filterValues { it.playedTime > 60 }
            .mapKeys { netServer.admins.getInfo(it.key) }
            .toList()
            .sortedByDescending { it.second.score }
    val list = sortedData.map { (player, data) ->
        totalTime += data.playedTime - data.idleTime
        "\n>------<\n[white]{pvpState}{player.name}[white](\n{statistics.playedTime:Minutes}\n{statistics.idleTime:Minutes}\n{statistics.buildScore:%.1f})".with(
            "player" to player, "statistics" to data, "pvpState" to if (data.win) "[green][win][]" else ""
        )
    }
    broadcast("""
        [yellow]Game duration: {gameTime:min}
        [yellow]Total contribution time: {totalTime:min}
        [yellow]Contribution ranking (time/hangout/building): {list}
    """.trimIndent().with("gameTime" to gameTime, "totalTime" to Duration.ofSeconds(totalTime.toLong()), "list" to list))

    if (sortedData.isNotEmpty() && depends("wayzer/user/expReward") != null && gameTime > Duration.ofMinutes(15)) {
        val updateExp = depends("wayzer/user/level")?.import<PlayerProfile.(Int) -> List<Player>>("updateExp")
        if (updateExp != null) {
            @OptIn(CacheEntity.NeedTransaction::class)
            transaction {
                val map = mutableMapOf<PlayerProfile, StatisticsData>()
                sortedData.groupBy { PlayerData.find(it.first)?.profile }.forEach { (key, value) ->
                    if (key == null || value.isEmpty()) return@forEach
                    map[key] = value.maxByOrNull { it.second.score }!!.second
                }
                map.forEach { (profile, data) ->
                    profile.updateExp(data.exp).forEach {
                        it.sendMessage("[green]experience +${data.exp}")
                    }
                    profile.save()
                }
            }
        }
    }
    statisticsData.clear()
}
export(::onGameOver)//Need in Dispatchers.game