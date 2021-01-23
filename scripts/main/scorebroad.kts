package main
//WayZer 版权所有(请勿删除版权注解)
import arc.util.Align
import mindustry.gen.Call
import mindustry.gen.Groups
import java.time.Duration

name = "扩展功能: 积分榜"
//建议只修改下面一段,其他地方代码请勿乱动
val msg = """
    [magenta]Welcome [goldenrod]{player.name}[magenta] to BE6 server
    [violet]Current map is: [orange]{map.name}
    [violet]Server FPS: [orange]{fps}
    [royal]Enter /broad to switch the display on/off
""".trimIndent()

val disabled = mutableSetOf<String>()

command("broad", "Switching scoreboard display") {
    this.type = CommandType.Client
    body {
        if (!disabled.remove(player!!.uuid()))
            disabled.add(player!!.uuid())
        reply("[green]Switching successful".with())
    }
}

onEnable {
    launch {
        while (true) {
            withContext(Dispatchers.game) {
                Groups.player.forEach {
                    if (disabled.contains(it.uuid())) return@forEach
                    if (it.con?.mobile == true) {
                        Call.infoPopup(
                            it.con,
                            msg.with("player" to it, "receiver" to it).toString(),
                            2.013f,
                            Align.topLeft,
                            210,
                            0,
                            0,
                            0
                        )
                    } else
                        Call.infoPopup(
                            it.con,
                            msg.with("player" to it, "receiver" to it).toString(),
                            2.013f,
                            Align.topLeft,
                            155,
                            0,
                            0,
                            0
                        )
                }
            }
            delay(Duration.ofSeconds(2).toMillis())
        }
    }
}