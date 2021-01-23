package wayzer.ext

import arc.math.geom.Vec2
import mindustry.gen.Call
import java.time.Duration
import java.time.Instant

var lastPos: Vec2 = Vec2.ZERO
var lastTime: Instant = Instant.MIN

command("gather", "Send a collection request") {
    usage = "[可选说明]"
    type = CommandType.Client
    aliases = listOf("集合")
    body {
        if (Duration.between(lastTime, Instant.now()) < Duration.ofSeconds(30)) {
            returnReply("[red]Someone just started a request, please wait 30s and try again".with())
        }
        val message = "[white]\"${arg.firstOrNull() ?: ""}[white]\""
        lastPos = player!!.unit().run { Vec2(lastX, lastY) }
        lastTime = Instant.now()
        broadcast(
            "[yellow][set][cyan]{player.name}[white] initiate set([red]{x},{y}[white]){message},input \"[gold]go[white]\" to"
                .with(
                    "player" to player!!,
                    "x" to lastPos.x.toInt() / 8,
                    "y" to lastPos.y.toInt() / 8,
                    "message" to message
                ), quite = true
        )
    }
}

listen<EventType.PlayerChatEvent> {
    if (it.message.equals("go", true) && lastPos != Vec2.ZERO) {
        it.player.unit().set(lastPos.x, lastPos.y)
        it.player.set(lastPos.x, lastPos.y)
        Call.setPosition(it.player.con, lastPos.x, lastPos.y)
    }
}

listen<EventType.ResetEvent> {
    lastPos = Vec2.ZERO
}