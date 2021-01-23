package main
//WayZer 版权所有(请勿删除版权注解)
import mindustry.gen.Call

name = "Cross-Service Teleportation"

val servers by config.key(mapOf<String, String>(), "Server Transfer List", "Format: {Name: \"Intro;Address\"} (; as separator)")

data class Info(val name: String, val desc: String, val address: String, val port: Int)

val infos: Map<String, Info>
    get() = servers.mapValues { (k, v) ->
        val sp1 = v.split(";")
        assert(sp1.size == 2) { "Format error: $v" }
        val sp2 = sp1[1].split(":")
        val port = sp2.getOrNull(1)?.toIntOrNull() ?: port
        Info(k, sp1[0], sp2[0], port)
    }


command("go", "Transfer to other servers") {
    usage = "[name, listed as empty]"
    type = CommandType.Client
    aliases = listOf("前往")
    body {
        val info = arg.firstOrNull()
            ?.let { infos[it] ?: returnReply("[red]Wrong server name".with()) }
            ?: let {
                val list = infos.values.map { "[gold]{name}:[tan]{desc}".with("name" to it.name, "desc" to it.desc) }
                returnReply("[violet] Available servers: \n{list:\n}".with("list" to list))
            }
        Call.connect(player!!.con, info.address, info.port)
        broadcast(
            "[cyan][-][salmon]{player.name}[salmon] transferred to {name} server (/go {name})".with(
                "player" to player!!,
                "name" to info.name
            )
        )
    }
}