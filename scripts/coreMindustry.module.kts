@file:DependsModule("coreLibrary")

import arc.Core
import coreMindustry.lib.*
import mindustry.Vars

name = "Mindustry Core Script Module"

addLibraryByClass("mindustry.Vars")
addDefaultImport("arc.Core")
addDefaultImport("mindustry.gen.Player")
addDefaultImport("mindustry.game.EventType")
addDefaultImport("mindustry.Vars.*")
addDefaultImport("coreMindustry.lib.*")
addDefaultImport("coreMindustry.lib.compatibilities.*")
generateHelper()

fun updateOriginCommandHandler(client: arc.util.CommandHandler, server: arc.util.CommandHandler) {
    Vars.netServer?.apply {
        javaClass.getDeclaredField("clientCommands").let {
            it.isAccessible = true
            it.set(this, client)
        }
    }
    Core.app.listeners.find { it.javaClass.simpleName == "ServerControl" }?.let {
        it.javaClass.getDeclaredField("handler").apply {
            isAccessible = true
            set(it, server)
        }
    }
}

Listener//ensure init

onEnable {
    Vars.dataDirectory.child("scriptsConfig.conf").file().takeIf { it.exists() }?.apply {
        println("Detect old configuration files and migrate them automatically")
        copyTo(Config.dataDirectory.resolve("config.conf"), true)
        ConfigBuilder.reloadFile()
        delete()
    }
    Vars.dataDirectory.child("scriptAgent.db").file().takeIf { it.exists() }?.let {
        println("Old data storage files are detected and discarded, please remove them manually $it")
    }
    Commands.rootProvider.set(RootCommands)
    updateOriginCommandHandler(
        MyCommandHandler("/", Config.clientCommands),
        MyCommandHandler("", Config.serverCommands)
    )
}

onDisable {
    updateOriginCommandHandler(Config.clientCommands, Config.serverCommands)
}