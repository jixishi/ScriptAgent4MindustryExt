@file:DependsModule("coreLibrary")
@file:MavenDepends("net.mamoe:mirai-core-jvm:2.0-RC", single = false)

import arc.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.utils.*

addDefaultImport("mirai.lib.*")
addLibraryByClass("net.mamoe.mirai.Bot")
addDefaultImport("net.mamoe.mirai.Bot")
addDefaultImport("net.mamoe.mirai.event.*")
addDefaultImport("net.mamoe.mirai.event.events.*")
addDefaultImport("net.mamoe.mirai.message.*")
addDefaultImport("net.mamoe.mirai.message.data.*")
addDefaultImport("net.mamoe.mirai.contact.*")
generateHelper()

val enable by config.key(false, "Whether to start the robot (set the account password before opening)")
val qq by config.key(13456789L, "Robot qq number")
val password by config.key("123456", "Robot qq password")
val qqProtocol by config.key(BotConfiguration.MiraiProtocol.ANDROID_PAD, "QQ login type, different types can login at the same time", "Available values: ANDROID_PHONE ANDROID_PAD ANDROID_WATCH")

val channel = Channel<String>()

onEnable {
    if (!enable) {
        println("Robot is not open, please modify the configuration file first")
        return@onEnable
    }
    MiraiLogger.setDefaultLoggerCreator {
        SimpleLogger { priority, msg, throwable ->
            when (priority) {
                SimpleLogger.LogPriority.WARNING -> {
                    Log.warn("[$it]$msg", throwable)
                }
                SimpleLogger.LogPriority.ERROR -> {
                    Log.err("[$it]$msg", throwable)
                }
                SimpleLogger.LogPriority.INFO -> {
                    if (it?.startsWith("Bot") == true)
                        Log.info("[$it]$msg", throwable)
                }
                else -> {
                    // ignore
                }
            }
        }
    }
    val bot = BotFactory.newBot(qq, password) {
        protocol = qqProtocol
        fileBasedDeviceInfo(Config.dataDirectory.resolve("miraiDeviceInfo.json").absolutePath)
        parentCoroutineContext = coroutineContext
        loginSolver = StandardCharImageLoginSolver(channel::receive)
    }
    launch {
        bot.login()
    }
}

Commands.controlCommand.let {
    it += CommandInfo(this, "mirai", "Redirect input to mirai") {
        usage = "[args...]"
        permission = "mirai.input"
        body {
            channel.sendBlocking(arg.joinToString(" "))
        }
    }
}