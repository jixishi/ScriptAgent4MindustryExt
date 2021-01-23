package wayzer.ext

import arc.Net
import arc.util.Log
import arc.util.async.Threads
import arc.util.serialization.Jval
import mindustry.core.Version
import mindustry.game.EventType
import mindustry.gen.Groups
import mindustry.io.SaveIO
import mindustry.net.BeControl
import java.io.File
import java.net.URL
import kotlin.system.exitProcess

name = "自动更新"

val enableUpdate by config.key(true, "是否开启自动更新")

var updateCallback: (() -> Unit)? = null

onEnable {
    launch {
        while (updateCallback == null) {
            if (enableUpdate)
                Core.net.httpGet("https://api.github.com/repos/Anuken/Mindustry/releases", { res ->
                    if (res.status == Net.HttpStatus.OK) {
                        val json = Jval.read(res.resultAsString).asArray().first()
                        val newBuild = json.getString("tag_name", "").removePrefix("v").split(".")[0].toInt()
                        if (newBuild > Version.build) {
                            val asset = json.get("assets").asArray().find {
                                it.getString("name", "").startsWith("server-release")
                            }
                            val url = asset.getString("browser_download_url", "")
                            update(newBuild, "https://gh.api.99988866.xyz/$url")
                        }
                    }
                }, { it.printStackTrace() })
            delay(5 * 60_000)//延时5分钟
        }
    }
}

fun update(version: Int, url: String) {
    launch(Dispatchers.IO) {
        Log.info("Found a new version available $version being downloaded from $url")
        val con = URL(url).openConnection()
        val dest = File(BeControl::class.java.protectionDomain.codeSource.location.toURI().path)
        val tmp = dest.resolveSibling("server-be-$version.jar")
        val size = con.getInputStream().use { input ->
            tmp.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        Log.info("New version $version Download complete: ${size / 1024 / 1024}MB")
        updateCallback = {
            Groups.player.forEach {
                it.kick("[yellow] Server reboot to update to new version $version")
            }
            Threads.sleep(32L)
            Log.info("&lcVersion downloaded, exiting. Note that if you are not using a auto-restart script, the server will not restart automatically.")
            tmp.renameTo(dest)
            exitProcess(2)
        }
        Core.app.post {
            broadcast("[yellow] server new version {version} download completed, will automatically restart the update after this game".with("version" to version))
            if (Groups.player.isEmpty) {
                Log.info("&lcSaving...")
                @Suppress("SpellCheckingInspection")
                SaveIO.save(saveDirectory.child("autosavebe.$saveExtension"))
                Log.info("&lcAutoSaved.")
                updateCallback!!()
            }
        }
    }
}

listen<EventType.WorldLoadEvent> {
    updateCallback?.invoke()
}