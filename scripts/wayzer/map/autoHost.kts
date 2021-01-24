@file:Import("@wayzer/services/MapService.kt", sourceFile = true)
package wayzer.map

import mindustry.game.EventType
import wayzer.services.MapService
import java.time.Duration

name = "AutoHost"

val mapService by ServiceRegistry.nullable<MapService>()
val autoHostTime by config.key(Duration.ofSeconds(6)!!, "AutoHost latency, too short may be the server is not ready")

listen<EventType.ServerLoadEvent> {
    ContentHelper.logToConsole("Auto Host after ${autoHostTime.seconds} seconds")
    launch {
        delay(autoHostTime.toMillis())
        if (net.server()) {//Already host
            ContentHelper.logToConsole("[AutoHost]Already host, pass!")
            return@launch
        }
        mapService ?: ContentHelper.logToConsole("[AutoHost][red]Can't find MapService, pass!")
        mapService?.loadMap()
    }
}