package wayzer.ext

import cf.wayzer.placehold.TemplateHandler
import cf.wayzer.placehold.TemplateHandlerKey
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.*

name = "国际化多语言"

var default by config.key("default.user", "默认语言")
var console by config.key("default.console", "控制台语言(不发给玩家的语句)")

val tempLang = mutableMapOf<String, String>()//uuid -> lang

var Player.lang
    get() = PlayerData[uuid()].profile?.lang ?: tempLang[uuid()] ?: default
    set(v) {
        if (lang == v) return
        PlayerData[uuid()].profile?.apply {
            @OptIn(CacheEntity.NeedTransaction::class)
            transaction {
                lang = v
                save()
            }
        } ?: let {
            tempLang[uuid()] = v
            sendMessage("[yellow] The language setting will be reset after you quit the game.".with())
        }
    }

listen<EventType.PlayerLeave> {
    tempLang.remove(it.player.uuid())
}


//===main===
class Lang(private val lang: String) : Properties() {
    private val file: File get() = dir.resolve("$lang.properties")

    init {
        if (file.exists()) file.reader().use(this::load)
    }

    private fun save() {
        file.parentFile.mkdirs()
        file.writer().use {
            it.write(header)
            store(it, null)
        }
    }

    fun trans(origin: String): String = getProperty(origin.hashCode().toString()) ?: let {
        put("HASH" + origin.hashCode().toString(), origin)
        save()
        origin
    }

    companion object {
        val dir = Config.dataDirectory.resolve("lang")
        val header = """
                |# Auto generated(automatically generated file)
                |# backup before modify(Note backup before modify)
                |# Auto generated
            """.trimMargin()
    }
}

private val cache = mutableMapOf<String, Lang>()
fun getLang(lang: String) = cache.getOrPut(lang) { Lang(lang) }

registerVar(TemplateHandlerKey, "多语言处理", TemplateHandler.new {
    when (val p = getVar("receiver")) {
        null -> getLang(console).trans(it)//console
        is Player -> getLang(p.lang).trans(it)
        else -> it
    }
})

//===commands===
val commands = Commands()
commands += CommandInfo(this, "reload", "Reload language files") {
    permission = "wayzer.lang.reload"
    body {
        cache.clear()
        reply("[green]Cache is refreshed".with())
    }
}
commands += CommandInfo(this, "setDefault", "Set the player's default language") {
    permission = "wayzer.lang.setDefault"
    body {
        arg.getOrNull(0)?.let { default = it }
        reply("[green] Player default language has been set to {v}".with("v" to default))
    }
}
commands += CommandInfo(this, "set", "Set the currently used language") {
    permission = "wayzer.lang.setDefault"
    body {
        val suffix = if (player == null) ".console" else ".user"
        if (arg.isEmpty())
            returnReply("[yellow] Available Languages: {list}".with(
                "list" to Lang.dir.listFiles { it -> it.nameWithoutExtension.endsWith(suffix) }.orEmpty().map {
                    it.nameWithoutExtension.removeSuffix(suffix)
                }
            ))
        else {
            if (player == null) {//console
                console = arg[0]
                returnReply("[green]控制台语言已设为 {v}".with("v" to console))
            } else {
                player!!.lang = arg[0]
                returnReply("[green] Your language is set to {v}".with("v" to player!!.lang))
            }
        }
    }
}
command("lang", "Set language") {
    body(commands)
    onComplete(commands)
}
