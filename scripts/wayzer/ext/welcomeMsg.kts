package wayzer.ext

import mindustry.game.EventType

val type by config.key(MsgType.InfoMessage, "发送方式")
val template by config.key("""
Welcome {player.name}[white]to the [green]beta[white]
 --- Current map:{map.name}[][white]-
 ---[yellow]Please use the /help page lookup command[white]---
 ---[pink]◈Common commands are as follows ◈[white]---
 --- vote for map/vote map map id --- [white]
 --- all map lookup/maps all page number ---[white]
 --- personalinfo/info--[white]
 --- skipwave/vote skipwave wave--[white]
 ---read archive/vote rollback archive id--[white]
---observer-mode/spectate ---[white]	 
---request-for-support/gather---[white]
---Go to support go---[white]
==[pink]admin command[white]==
---generate units/generate units teams number--[white] 
 [red]pvp map has 15 minutes protection time!
 No messing up violators banned!
[white]
===[pink]Number of banned players:{state.allBan}===[white]
""".trimIndent(), "欢迎信息模板")

listen<EventType.PlayerJoin> {
    it.player.sendMessage(template.with(),type)
}