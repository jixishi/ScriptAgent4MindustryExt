//WayZer All Rights Reserved (Please do not remove the copyright notice)
package coreMindustry

import arc.util.Time
import cf.wayzer.placehold.DynamicVar
import mindustry.core.Version
import mindustry.game.EventType
import mindustry.game.Team
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.gen.Unit
import mindustry.maps.Map
import mindustry.net.Administration
import java.time.Duration
import java.time.Instant
import java.util.*

name = "Basics: Global Variables"

//SystemVars
registerVar("fps", "Server fps", DynamicVar.v {
    (60f / Time.delta).toInt()
})
registerVar("heapUse", "Memory usage (MB)", DynamicVar.v {
    Core.app.javaHeap / 1024 / 1024  //MB
})
//GameVars
registerVar("map", "The current map in the game", DynamicVar.v {
    state.map
})
registerVarForType<Map>().apply {
    registerChild("name", "Map Name", DynamicVar.obj { it.name() })
    registerChild("desc", "Map Introduction", DynamicVar.obj { it.description() })
    registerChild("author", "Map Author", DynamicVar.obj { it.author() })
    registerChild("width", "Width", DynamicVar.obj { it.width })
    registerChild("height", "Height", DynamicVar.obj { it.height })
    registerChild("size", "Area is:width x height", DynamicVar.obj { "${it.width}x${it.height}" })
    registerChild("fileName", "Map file name (without extension)", DynamicVar.obj { it.file?.nameWithoutExtension() })
}
registerVar("state.allUnit", "Total unit number", DynamicVar.v { Groups.unit.size() })
registerVar("state.allBan", "Total number of bans", DynamicVar.v { netServer.admins.banned.size })
registerVar("state.playerSize", "Current number of players", DynamicVar.v { Groups.player.size() })
registerVar("state.wave", "Current wave number", DynamicVar.v { state.wave })
registerVar("state.enemies", "Current enemy number", DynamicVar.v { state.enemies })
registerVar("state.gameMode", "Map game mode", DynamicVar.v { state.rules.mode() })
registerVar("state.startTime", "Game start time", DynamicVar.v { startTime })
registerVar("state.gameTime", "Duration of the start of the game", DynamicVar.v { Duration.between(startTime, Instant.now()) })
registerVar("game.version", "Current Game Versions", DynamicVar.v { Version.build })

//PlayerVars
registerVarForType<Player>().apply {
    registerChild("name", "Name", DynamicVar.obj { it.name })
    registerChild("uuid", "uuid", DynamicVar.obj { it.uuid() })
    registerChild("ip", "ip", DynamicVar.obj { it.con?.address })
    registerChild("team", "Current Team", DynamicVar.obj { it.team() })
    registerChild("unit", "Get PlayerUnit", DynamicVar.obj { it.unit() })
    registerChild("info", "PlayerInfo", DynamicVar.obj { netServer.admins.getInfoOptional(it.uuid()) })
}
registerVarForType<Administration.PlayerInfo>().apply {
    registerChild("name", "Name", DynamicVar.obj { it.lastName })
    registerChild("uuid", "uuid", DynamicVar.obj { it.id })
    registerChild("lastIP", "Last loginIP", DynamicVar.obj { it.lastIP })
    registerChild("lastBan", "Time of last ban", DynamicVar.obj { it.lastKicked.let(::Date) })
}

registerVar("team", "Current Player Team", DynamicVar.v { getVar("player.team") })
registerVarForType<Team>().apply {
    registerChild("name", "Team Name", DynamicVar.obj { it.name })
    registerChild("color", "Team Color", DynamicVar.obj { "[#${it.color}]" })
    registerChild(
        "colorizeName",
        "彩色队伍名",
        DynamicVar.obj { typeResolve(it, "color")?.toString() + typeResolve(it, "name")?.toString() })
}

//Unit
registerVarForType<Unit>().apply {
    registerChild("x", "x", DynamicVar.obj { it.tileX() })
    registerChild("y", "y", DynamicVar.obj { it.tileY() })
    registerChild("health", "Current blood level", DynamicVar.obj { it.health })
    registerChild("maxHealth", "Maximum Blood", DynamicVar.obj { it.maxHealth })
    registerChild("shield", "Shield Value", DynamicVar.obj { it.shield })
    registerChild("ammo", "Ammo", DynamicVar.obj { if (state.rules.unitAmmo) it.shield else typeResolve(it, "maxAmmo") })
    registerChild("maxAmmo", "Ammo capacity", DynamicVar.obj { it.type.ammoCapacity })
}

var startTime = Instant.now()!!
listen<EventType.WorldLoadEvent> {
    startTime = Instant.now()
}