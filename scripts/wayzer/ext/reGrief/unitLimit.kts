package wayzer.ext.reGrief

import arc.util.Interval
import mindustry.game.EventType
import mindustry.gen.Groups

val unitToWarn by config.key(200, "Number of units to start warning")
val unitToKill by config.key(230, "The maximum number of units prohibits the creation of new")

val interval = Interval(1)
listen<EventType.UnitCreateEvent> { e ->
    if (e.unit.team == state.rules.waveTeam) return@listen
    fun alert(text: PlaceHoldString) {
        if (interval[0, 2 * 60f]) {//2s cooldown
            broadcast(text, MsgType.InfoToast, 4f, true, Groups.player.filter { it.team() == e.unit.team })
        }
    }

    val count = Groups.unit.count { it.team == e.unit.team }
    when {
        count >= unitToKill -> {
            alert("[red]Warning: Building too many units, may cause server lag, have disabled generation".with("count" to count))
            e.unit.kill()
        }
        count >= unitToWarn -> {
            alert("[yellow] Warning: Building too many units, may cause server lag, current: {count}".with("count" to count))
        }
    }
}