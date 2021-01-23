package coreLibrary

import coreLibrary.lib.ConfigBuilder.Companion.configs
import coreLibrary.lib.PlaceHold.registeredVars

val thisRef = this
onEnable {
    Commands.controlCommand.run {
        addSub(CommandInfo(thisRef, "info", "Get specific information about a script") {
            usage = "<module[/script]>"
            permission = "scriptAgent.info"
            onComplete {
                onComplete(0) {
                    (arg[0].split('/')[0].let(ScriptManager::getScript)?.let { it as IModuleScript }?.children
                        ?: ScriptManager.loadedInitScripts.values).map { it.id }
                }
            }
            body {
                if (arg.isEmpty()) replyUsage()
                val script = ScriptManager.getScript(arg[0]) ?: returnReply("[red]Script not found, please make sure it is loaded successfully and entered correctly".with())

                val configs = script.configs.map {
                    "[purple]{key} [blue]{desc}\n".with("key" to it.path, "desc" to (it.desc.firstOrNull() ?: ""))
                }
                val registeredVars = script.registeredVars.map {
                    "[purple]{key} [blue]{desc}\n".with("key" to it.key, "desc" to it.value)
                }

                returnReply(
                    """
                [yellow]==== [light_yellow]{name} info[yellow] ====
                [cyan] Configuration entry:
                {configs}
                [cyan] supplied variables:
                {registeredVars}
                [cyan]Registered directives: not implemented yet
            """.trimIndent().with("name" to script.clsName, "configs" to configs, "registeredVars" to registeredVars)
                )
            }
        })
        onDisable { removeAll(thisRef) }
    }
}