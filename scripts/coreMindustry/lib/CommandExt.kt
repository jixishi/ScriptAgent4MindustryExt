package coreMindustry.lib

import cf.wayzer.script_agent.ISubScript
import coreLibrary.lib.CommandContext
import coreLibrary.lib.CommandHandler
import coreLibrary.lib.CommandInfo
import coreLibrary.lib.event.PermissionRequestEvent
import mindustry.gen.Player

fun ISubScript.command(name: String, description: String, init: CommandInfo.() -> Unit) {
    RootCommands += CommandInfo(this, name, description, init)
}

@Suppress("unused")
fun Player.hasPermission(permission: String): Boolean {
    return CommandContext().also {
        it.player = this
    }.hasPermission(permission)
}

@Deprecated("use new command api", ReplaceWith("command(name,description){init\nbody(handler)}"))
fun ISubScript.command(name: String, description: String, init: CommandInfo.() -> Unit, handler: CommandHandler) {
    RootCommands += CommandInfo(this, name, description) {
        init()
        body(handler)
    }
}

//常见拼写错误，但不报错
@Suppress("unused")
@Deprecated("Please check if the variables are used correctly, Vars.player is null", ReplaceWith("error(\"This variable is not allowed in the server\")"), DeprecationLevel.ERROR)
val ISubScript.player: Player?
    get() = error("This variable is not allowed in the server")