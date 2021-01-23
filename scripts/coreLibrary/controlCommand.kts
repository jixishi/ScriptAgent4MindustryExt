package coreLibrary

import cf.wayzer.script_agent.events.ModuleDisableEvent
import cf.wayzer.script_agent.events.ScriptDisableEvent
import kotlinx.coroutines.launch

val thisRef = this
onEnable {
    Commands.controlCommand.run {
        addSub(CommandInfo(thisRef, "list", "List all modules or all scripts within a module") {
            usage = "[module]"
            permission = "scriptAgent.control.list"
            onComplete {
                onComplete(0) {
                    ScriptManager.loadedInitScripts.values.map { it.id }
                }
            }
            body {
                if (arg.isEmpty()) {
                    val list = ScriptManager.loadedInitScripts.values.map {
                        val enable = if (it.enabled) "purple" else "reset"
                        "[{enable}]{name} [blue]{desc}".with(
                            "enable" to enable,
                            "name" to it.id.padEnd(20),
                            "desc" to it.name
                        )
                    }
                    return@body reply(
                        """
                    [yellow]==== [light_yellow]loaded modules[yellow] ====
                    {list:${"\n"}}
                """.trimIndent().with("list" to list)
                    )
                }
                val module = arg[0].let(ScriptManager::getScript)?.let { it as? IModuleScript }
                    ?: return@body reply("[red]Module not found".with())
                val list = module.children.map {
                    val enable = if (it.enabled) "purple" else "reset"
                    "[{enable}]{name} [blue]{desc}".with(
                        "enable" to enable,
                        "name" to it.id.padEnd(30),
                        "desc" to it.name
                    )
                }
                reply(
                    """
                [yellow]==== [light_yellow]{module} script [yellow] ====
                {list:${"\n"}}
            """.trimIndent().with("module" to module.name, "list" to list)
                )
            }
        })
        addSub(CommandInfo(thisRef, "reload", "Reload a script or module") {
            usage = "<module[/script]>"
            permission = "scriptAgent.control.reload"
            onComplete {
                onComplete(0) {
                    (arg[0].split('/')[0].let(ScriptManager::getScript)?.let { it as IModuleScript }?.children
                        ?: ScriptManager.loadedInitScripts.values).map { it.id }
                }
            }
            body {
                if (arg.isEmpty()) replyUsage()
                GlobalScope.launch {
                    reply("[yellow] Asynchronous processing in progress".with())
                    val success: Boolean = when (val script = arg.getOrNull(0)?.let(ScriptManager::getScript)) {
                        is IModuleScript -> ScriptManager.loadModule(
                            script.sourceFile,
                            force = true,
                            enable = true
                        ) != null
                        is ISubScript -> ScriptManager.loadContent(
                            script.module, script.sourceFile,
                            force = true,
                            enable = true
                        ) != null
                        else -> return@launch reply("[red]No module or script found".with())
                    }
                    reply((if (success) "[green]reload success" else "[red]Loading failed").with())
                }
            }
        })
        addSub(CommandInfo(thisRef, "disable", "Close a script or module") {
            usage = "<module[/script]>"
            permission = "scriptAgent.control.disable"
            onComplete {
                onComplete(0) {
                    (arg[0].split('/')[0].let(ScriptManager::getScript)?.let { it as IModuleScript }?.children
                        ?: ScriptManager.loadedInitScripts.values).map { it.id }
                }
            }
            body {
                if (arg.isEmpty()) replyUsage()
                GlobalScope.launch {
                    reply("[yellow] Asynchronous processing in progress".with())
                    when (val script = arg.getOrNull(0)?.let(ScriptManager::getScript)) {
                        is IModuleScript -> ModuleDisableEvent(script).emit()
                        is ISubScript -> ScriptDisableEvent(script).emit()
                        else -> return@launch reply("[red]No module or script found".with())
                    }
                    reply("[green] Close script successful".with())
                }
            }
        })
        addSub(CommandInfo(thisRef, "loadScript", "Load a new script or module") {
            usage = "<filePath>"
            aliases = listOf("load")
            permission = "scriptAgent.control.load"
            body {
                val file = arg.getOrNull(0)?.let(Config.rootDir::resolve)
                    ?: return@body reply("[red]No corresponding file found".with())
                GlobalScope.launch {
                    reply("[yellow] Asynchronous processing in progress".with())
                    val success: Boolean = when {
                        file.name.endsWith(Config.moduleDefineSuffix) -> ScriptManager.loadModule(
                            file,
                            enable = true
                        ) != null
                        file.name.endsWith(Config.contentScriptSuffix) -> {
                            val module = ScriptManager.getScript(arg[0].split('/')[0])
                            if (module !is IModuleScript) return@launch reply("[red]Module not found, please make sure the module has been loaded first".with())
                            ScriptManager.loadContent(module, file, enable = true) != null
                        }
                        else -> return@launch reply("[red]Unsupported file format".with())
                    }
                    reply((if (success) "[green]Loading script successfully" else "[red]Loading failed, check backend for details").with())
                }
            }
        })
        onDisable { removeAll(thisRef) }
    }
}