@file:Suppress("unused")

package coreMindustry.lib

import arc.struct.Seq
import arc.util.CommandHandler
import cf.wayzer.script_agent.Config
import cf.wayzer.script_agent.getContextModule
import cf.wayzer.script_agent.listenTo
import cf.wayzer.script_agent.util.DSLBuilder
import coreLibrary.lib.*
import coreLibrary.lib.event.PermissionRequestEvent
import coreMindustry.lib.util.sendMenuPhone
import mindustry.gen.Player

object RootCommands : Commands() {
    var overwrite = true
    override fun getSubCommands(context: CommandContext?): Map<String, CommandInfo> {
        if (!overwrite || context == null) return super.getSubCommands(context)
        val origin =
            (if (context.player != null) Config.clientCommands else Config.serverCommands).let { originHandler ->
                originHandler.commandList.associate {
                    it.text.toLowerCase() to CommandInfo(null, it.text, it.description) {
                        usage = it.paramText
                        body {
                            prefix = prefix.removePrefix("* ")
                            (if (originHandler is MyCommandHandler) originHandler.origin else originHandler).handleMessage(
                                prefix + arg.joinToString(" "),
                                player
                            )
                        }
                    }
                }
        }
        return origin.filterValues { if (context.player != null) it.type.client() else it.type.server() } + subCommands.filterValues { if (context.player != null) it.type.client() else it.type.server() }
    }

    override fun addSub(name: String, command: CommandInfo, isAliases: Boolean) {
        if (overwrite) {
            if (command.type.server())
                Config.serverCommands.removeCommand(name)
            if (command.type.client())
                Config.clientCommands.removeCommand(name)
            return super.addSub(name, command, isAliases)
        }
        super.addSub(name, command, isAliases)
        fun CommandHandler.register() {
            removeCommand(name)
            register(name, "[arg...]", command.description) { arg, player: Player? ->
                command(CommandContext().apply {
                    reply = { reply(it, MsgType.Message) }
                    this.player = player
                    prefix = "/$name"
                    this.arg = arg.getOrNull(0)?.split(' ') ?: emptyList()
                })
            }
        }
        if (command.type.server())
            Config.serverCommands.register()
        if (command.type.client())
            Config.clientCommands.register()
    }

    override fun removeSub(name: String) {
        if (overwrite) return super.removeSub(name)
        subCommands[name]?.let {
            if (it.type.server())
                Config.serverCommands.removeCommand(name)
            if (it.type.client())
                Config.clientCommands.removeCommand(name)
        }
        super.removeSub(name)
    }

    fun tabComplete(player: Player?, args: List<String>): List<String> {
        var result: List<String> = emptyList()
        try {
            onComplete(CommandContext().apply {
                this.player = player
                reply = {}
                replyTabComplete = { result = it;CommandInfo.Return() }
                arg = args
            })
        } catch (e: CommandInfo.Return) {
        }
        return result
    }

    override fun onHelp(context: CommandContext, explicit: Boolean) {
        if (!explicit) return context.reply("[red]Invalid command, please use /help to query".with())
        assert(overwrite)
        val showDetail = context.arg.firstOrNull() == "-v"
        val page = context.arg.lastOrNull()?.toIntOrNull()

        context.sendMenuPhone("help", getSubCommands(context).values.toSet().filter {
            (it.permission.isBlank() || context.hasPermission(it.permission))
        }, page, 10) {
            context.helpInfo(it, showDetail)
        }
    }

    init {
        if (overwrite) arrayOf(Config.clientCommands, Config.serverCommands).forEach {
            it.removeCommand("help")
        }
        RootCommands::class.java.getContextModule()!!.apply {
            listenTo<PermissionRequestEvent> {
                if (context.player?.admin == true)
                    result = true
            }
        }
    }
}

// TODO 覆盖原版接口
class MyCommandHandler(private var prefix: String, val origin: CommandHandler) : CommandHandler("") {
    override fun setPrefix(prefix: String) {
        this.prefix = prefix
    }

    override fun <T : Any?> register(text: String, params: String, description: String, runner: CommandRunner<T>): Command {
        return origin.register(text, params, description, runner)
    }

    override fun <T : Any?> register(text: String, description: String, runner: CommandRunner<T>): Command = register(text, "", description, runner)
    override fun removeCommand(text: String) {
        return origin.removeCommand(text)
    }

    override fun getCommandList(): Seq<Command> {
        return origin.commandList
    }

    override fun handleMessage(raw: String?, params: Any?): CommandResponse {
        fun myTrim(text: String) = buildString {
            var start = 0
            var end = text.length - 1
            while (start < text.length && text[start] == ' ') start++
            while (end >= 0 && text[end] == ' ') end--
            var lastBlank = false
            for (i in start..end) {
                val nowBlank = text[i] == ' '
                if (!lastBlank || !nowBlank)
                    append(text[i])
                lastBlank = nowBlank
            }
        }

        val message = raw?.let(::myTrim)
        if (message?.startsWith(prefix) != true || message.isBlank()) return CommandResponse(ResponseType.noCommand, null, null)
        RootCommands.invoke(CommandContext().apply {
            player = params as? Player
            reply = { reply(it, MsgType.Message) }
            prefix = this@MyCommandHandler.prefix.let { if (it.isEmpty()) "* " else it }
            this.arg = message.removePrefix(prefix).split(' ')
        })
        return CommandResponse(ResponseType.valid, null, message)
    }
}

enum class CommandType {
    Client, Server, Both;

    fun client() = this == Client || this == Both
    fun server() = this == Server || this == Both
}

var CommandInfo.type by DSLBuilder.dataKeyWithDefault { CommandType.Both }

/**
 * null for console or other
 */
var CommandContext.player by DSLBuilder.dataKey<Player>()
fun CommandContext.reply(text: PlaceHoldString, type: MsgType = MsgType.Message, time: Float = 10f) {
    if (player == null) {
        println(ColorApi.handle("$text[RESET]", ColorApi::consoleColorHandler))
    } else {
        player.sendMessage("{msg}".with("msg" to text, "player" to player!!), type, time)
    }
}