package coreLibrary

import cf.wayzer.placehold.PlaceHoldApi.with
import java.nio.file.*

var watcher: WatchService? = null

fun enableWatch() {
    if (watcher != null) return//Enabled
    watcher = FileSystems.getDefault().newWatchService()
    Config.rootDir.walkTopDown().onEnter { it.name != "cache" && it.name != "lib" && it.name != "res" }
            .filter { it.isDirectory }.forEach {
                it.toPath().register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY)
            }
    launch(Dispatchers.IO) {
        while (true) {
            val key = try {
                watcher?.take() ?: return@launch
            } catch (e: ClosedWatchServiceException) {
                return@launch
            }
            key.pollEvents().forEach { event ->
                if (event.count() != 1) return@forEach
                val file = (key.watchable() as Path).resolve(event.context() as? Path ?: return@forEach)
                when {
                    file.toString().endsWith(Config.moduleDefineSuffix) ->{ //Handling module reloading
                        println("Template file updates: ${event.kind().name()} ${Config.getIdByFile(file.toFile())}")
                        delay(1000)
                        ScriptManager.loadModule(file.toFile(), force = true, enable = true)
                    }
                    file.toString().endsWith(Config.contentScriptSuffix) -> { //Handling subscript overloads
                        println("Script file update: ${event.kind().name()} ${Config.getIdByFile(file.toFile())}")
                        delay(1000)
                        val module = Config.findModuleBySource(file.toFile())?.let {
                            ScriptManager.getScript(it) as? IModuleScript
                        } ?: return@forEach println("[WARN]Can't get Module by $file")
                        ScriptManager.loadContent(module, file.toFile(), force = true, enable = true)
                    }
                    file.toFile().isDirectory -> {//Add subdirectories to Watch
                        file.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY)
                    }
                }
            }
            if (!key.reset()) return@launch
        }
    }
}

onEnable{
    Commands.controlCommand += CommandInfo(this, "hotReload", "Switch script automatic thermal reload") {
        permission = "scriptAgent.control.hotReload"
        body {
            if (watcher == null) {
                enableWatch()
                reply("[green] script automatic hot reload monitoring start".with())
            } else {
                watcher?.close()
                watcher = null
                reply("[yellow] Script automatic hot reload monitoring off".with())
            }
        }
    }
    onDisable { Commands.controlCommand.removeAll(this) }
}

onDisable {
    watcher?.close()
}