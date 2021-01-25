package cf.wayzer

import arc.ApplicationListener
import arc.Core
import arc.util.ColorCodes
import arc.util.CommandHandler
import arc.util.Log
import cf.wayzer.ConfigExt.clientCommands
import cf.wayzer.ConfigExt.serverCommands
import cf.wayzer.script_agent.Config
import cf.wayzer.script_agent.ScriptAgent
import cf.wayzer.script_agent.ScriptManager
import mindustry.Vars
import mindustry.plugin.Plugin

class ScriptAgent4Mindustry: Plugin() {
    init {
        if(System.getProperty("java.util.logging.SimpleFormatter.format")==null)
            System.setProperty("java.util.logging.SimpleFormatter.format","[%1\$tF | %1\$tT | %4\$s] [%3\$s] %5\$s%6\$s%n")
        ScriptAgent.load()
    }
    override fun registerClientCommands(handler: CommandHandler) {
        Config.clientCommands = handler
    }

    override fun registerServerCommands(handler: CommandHandler) {
        Config.serverCommands = handler
    }

    override fun init() {
        val dir = Vars.dataDirectory.child("scripts").file()
        ScriptManager.loadDir(dir)
        Core.app.addListener(object : ApplicationListener {
            override fun pause() {
                ScriptManager.disableAll()
            }
        })
        Log.info("&y===========================")
        Log.info("&lm&fb     ScriptAgent          ")
        Log.info("&b           By &cWayZer    ")
        Log.info("&bPlugin Chinese Official Website: http://git.io/SA4Mindustry")
        Log.info("&bPlugin English Official Website: https://git.io/JtZ6t")
        Log.info("&bQQ交流群: 1033116078")
        if (dir.listFiles()?.isEmpty() != false)
            Log.warn("The script is not found under config/scripts, please download and install the script package to make use of the plugin's functions.")
        Log.info("&y===========================")
    }
}
