package wayzer.map

import mindustry.core.GameState
import mindustry.io.SaveIO
import java.util.*
import java.util.concurrent.TimeUnit

name = "Automatic Save"
val autoSaveRange = 100 until 106
command("slots", "List automatically saved archives") {
    body {
        val list = autoSaveRange.map { it to SaveIO.fileFor(it) }.filter { it.second.exists() }.map { (id, file) ->
            "[red]{id}[]: [yellow]Save on {date:hh:mm}".with("id" to id, "date" to file.lastModified().let(::Date))
        }
        reply(
            """
            |[green]===[white] Auto-archive [green]===
            |{list:${"\n"}}
            |[green]===[white] {range} [green]===
        """.trimMargin().with("range" to autoSaveRange, "list" to list)
        )
    }
}

val nextSaveTime: Date
    get() {//Every 10 minutes
        val t = Calendar.getInstance()
        t.set(Calendar.SECOND, 0)
        val mNow = t.get(Calendar.MINUTE)
        t.add(Calendar.MINUTE, (mNow + 10) / 10 * 10 - mNow)
        return t.time
    }

onEnable {
    launch {
        while (true) {
            val nextTime = nextSaveTime.time
            delay(nextTime - System.currentTimeMillis())
            if (state.`is`(GameState.State.playing)) {
                val minute = ((nextTime / TimeUnit.MINUTES.toMillis(1)) % 60).toInt() //Get the minute
                Core.app.post {
                    val id = autoSaveRange.first + minute / 10
                    SaveIO.save(SaveIO.fileFor(id))
                    broadcast("[green]Auto-archive complete (once every 10 minutes), archive number [red]{id}".with("id" to id))
                }
            }
        }
    }
}