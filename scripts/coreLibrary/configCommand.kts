package coreLibrary

import cf.wayzer.placehold.PlaceHoldApi.with

val thisRef = this
onEnable {
    Commands.controlCommand.run {
        addSub(CommandInfo(thisRef, "config", "View or modify configuration") {
            usage = "[help/arg...]"
            permission = "scriptAgent.config"
            onComplete {
                onComplete(0) { listOf("help", "reload") + ConfigBuilder.all.keys }
                onComplete(1) { listOf("set", "write", "reset") }
            }
            body {
                if (arg.isEmpty() || arg[0].equals("help", true))
                    returnReply("""
                        [yellow]available operations
                        [purple]config reload [light_purple]reload configuration file
                        [purple]config <config item> [light_purple]View configuration item description and current value
                        [purple]config <configuration> set <value> [light_purple]set configuration value
                        [purple]config <config> write [light_purple]Write default values to the configuration file
                        [purple]config <config> reset [light_purple] restore defaults (remove defaults from config file)
                    """.trimIndent().with())
                if (arg[0].equals("reload", true)) {
                    ConfigBuilder.reloadFile()
                    returnReply("[green]reload success".with())
                }
                val config = arg.firstOrNull()?.let { ConfigBuilder.all[it] } ?: returnReply("[red]Configuration items not found".with())
                when (arg.getOrNull(1)?.toLowerCase()) {
                    null -> {
                        returnReply("""
                        [yellow]==== [light_yellow] Configuration entry: {name}[yellow] ====
                        {desc}
                        [cyan] Current value: [yellow]{value}
                        [yellow]Use /sa config help to see the available actions
                    """.trimIndent().with("name" to config.path, "desc" to config.desc.map { "[purple]$it\n" },
                                "value" to config.getString()))
                    }
                    "reset" -> {
                        config.reset()
                        returnReply("[green] Reset successful, current:[yellow]{value}".with("value" to config.getString()))
                    }
                    "write" -> {
                        config.writeDefault()
                        reply("[green]Write file successfully".with())
                    }
                    "set" -> {
                        if (arg.size <= 2) returnReply("[red]Please enter the value".with())
                        val value = arg.subList(2, arg.size).joinToString(" ")
                        returnReply("[green] set successful, current:[yellow]{value}".with("value" to config.setString(value)))
                    }
                    else -> {
                        returnReply("[red]Unknown operation, please check help help".with())
                    }
                }
            }
        })
        onDisable { removeAll(thisRef) }
    }
}