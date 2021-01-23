package wayzer.ext

import arc.math.geom.Geometry
import mindustry.game.EventType
import mindustry.game.Gamemode
import mindustry.gen.Groups
import mindustry.gen.Unit
import mindustry.world.blocks.storage.CoreBlock
import java.time.Duration
import kotlin.math.ceil

val time by config.key(900, "pvp protection time (unit seconds, less than or equal to 0 off)")

listen<EventType.WorldLoadEvent> {
    launch(Dispatchers.game) {
        delay(3_000)
        var leftTime = state.rules.tags.getInt("@pvpProtect", time)
        if (state.rules.mode() != Gamemode.pvp || time <= 0) return@launch
        broadcast("[yellow] PVP protection time, forbidden to attack in other bases (duration {time:minutes})".with("time" to Duration.ofSeconds(leftTime.toLong())), quite = true)
        suspend fun checkAttack(time: Int) = repeat(time) {
            delay(1000)
            Groups.unit.forEach {
                fun Unit.nearest(): CoreBlock.CoreBuild? {
                    return Geometry.findClosest(it.x, it.y, state.teams.present
                            .filterNot { t -> t.team == it.team }.flatMap { t -> t.cores })
                }
                if (it.nearest()?.within(it, state.rules.enemyCoreBuildRadius) == true) {
                    it.player?.sendMessage("[red]PVP protection time, forbidden to attack in other bases".with())
                    it.kill()
                }
            }
        }
        while (leftTime > 0) {
            checkAttack(60)
            leftTime -= 60
            broadcast("[yellow]PVP protection time left {time} minutes".with("time" to ceil(leftTime / 60f)), quite = true)
            if (leftTime < 60) {
                checkAttack(leftTime)
                broadcast("[yellow] PVP protection time is over, let's go all out!".with())
                return@launch
            }
        }
    }
}

listen<EventType.ResetEvent> {
    coroutineContext[Job]?.cancelChildren()
}