package wayzer

import cf.wayzer.placehold.DynamicVar
import mindustry.game.EventType
import mindustry.gen.Groups
import mindustry.net.Administration
import mindustry.net.Packets
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.util.*

name = "基础: 玩家数据"


registerVarForType<Player>().apply {
    registerChild("shortID", "uuid 3-bit prefix, can be shown to other players", DynamicVar.obj { it.uuid().substring(0, 3) })
    registerChild("ext", "Module extension data", DynamicVar.obj { PlayerData.getOrNull(it.uuid()) })
    registerChild("profile", "Unified account information (may not exist)", DynamicVar.obj { PlayerData.getOrNull(it.uuid())?.profile })
}

registerVarForType<Administration.PlayerInfo>().apply {
    registerChild("shortID", "uuid 3-bit prefix, can be shown to other players", DynamicVar.obj { it.id.substring(0, 3) })
    registerChild("ext", "Module extension data", DynamicVar.obj { PlayerData.getOrNull(it.id) })
    registerChild("profile", "Uniform account information (may not exist)", DynamicVar.obj { PlayerData.getOrNull(it.id)?.profile })
}

registerVarForType<PlayerData>().apply {
    registerChild("name", "Name", DynamicVar.obj { it.lastName })
    registerChild("uuid", "uuid", DynamicVar.obj { it.id.value })
    registerChild("firstJoin", "First time in the service", DynamicVar.obj { Date.from(it.firstTime) })
    registerChild("lastJoin", "Last Online", DynamicVar.obj { Date.from(it.lastTime) })
    registerChild("profile", "Uniform account information (may not exist)", DynamicVar.obj { it.profile })
}

registerVarForType<PlayerProfile>().apply {
    registerChild("id", "Bound account ID(qq)", DynamicVar.obj { it.qq })
    registerChild("totalExp", "Total Experience", DynamicVar.obj { it.totalExp })
    registerChild("onlineTime", "Total Online Time", DynamicVar.obj { Duration.ofSeconds(it.totalTime.toLong()) })
    registerChild("registerTime", "Registration Time", DynamicVar.obj { Date.from(it.registerTime) })
    registerChild("lastTime", "Account last login time", DynamicVar.obj { Date.from(it.lastTime) })
}

listen<EventType.PlayerConnect> {
    val p = it.player
    if (Groups.player.any { pp -> pp.uuid() == p.uuid() }) return@listen p.con.kick(Packets.KickReason.idInUse)
    @Suppress("EXPERIMENTAL_API_USAGE")
    transaction {
        PlayerData.findOrCreate(p)
    }
}

listen<EventType.PlayerLeave> { event ->
    @Suppress("EXPERIMENTAL_API_USAGE")
    transaction {
        PlayerData.getOrNull(event.player.uuid())?.apply {
            save()
            if (Groups.player.none { it != event.player && it.uuid() == event.player.uuid() })
                PlayerData.removeCache(event.player.uuid())
        }
    }
}