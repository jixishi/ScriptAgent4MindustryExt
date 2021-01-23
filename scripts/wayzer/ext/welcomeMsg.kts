package wayzer.ext

import mindustry.game.EventType

val type by config.key(MsgType.InfoMessage, "Sending method")
val template by config.key("""
Welcome {player.name}[white]to the [green]server[white]

""".trimIndent(), "Welcome Message Template")

listen<EventType.PlayerJoin> {
    it.player.sendMessage(template.with(),type)
}