package wayzer.map

import mindustry.content.Blocks
import mindustry.game.EventType
import mindustry.gen.Call
import mindustry.gen.Groups

val enable get() = state.rules.tags.getBool("@limitAir")

onEnable {
    launch {
        while (true) {
            delay(3_000)
            if (net.server() && enable) {
                Groups.unit.forEach {
                    if (it.type().flying && state.teams.closestEnemyCore(it.x, it.y, it.team)?.within(it, state.rules.enemyCoreBuildRadius) == true) {
                        it.player?.sendMessage("[red]This map restricts the air force and prohibits entry into enemy airspace".with())
                        it.kill()
                    }
                }
            }
        }
    }
}

listen<EventType.BlockBuildEndEvent> {
    if (it.tile.block() == Blocks.airFactory && !it.breaking) {
        if (enable)
            Call.label("[yellow] This map is restricted to the air force, forbidden to enter the enemy airspace", 60f, it.tile.getX(), it.tile.getY())
    }
}

listen<EventType.PlayerJoin> {
    if (enable)
        it.player.sendMessage("[yellow] This map is restricted to the air force, forbidden to enter the enemy airspace")
}