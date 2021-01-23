@file:Import("@wayzer/services/MapService.kt", sourceFile = true)

package wayzer

import arc.files.Fi
import arc.util.Log
import cf.wayzer.placehold.DynamicVar
import coreMindustry.lib.util.sendMenuPhone
import mindustry.Vars
import mindustry.game.Gamemode
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.io.SaveIO
import mindustry.maps.Map
import wayzer.services.MapService
import java.time.Duration

name = "基础: 地图控制与管理"

val configEnableInternMaps by config.key(false, "是否开启原版内置地图")
val mapsDistinguishMode by config.key(true, "是否在/maps区分不同模式的地图")
val configTempSaveSlot by config.key(111, "临时缓存的存档格位")
val mapsPrePage by config.key(10, "/maps每页显示数")

@Suppress("PropertyName")
val MapManager = MapManager()

inner class MapManager : MapService {
    override val maps: Array<Map>
        get() {
            Vars.maps.reload()
            if (!configEnableInternMaps && Vars.maps.customMaps().isEmpty) {
                Log.warn("服务器未安装自定义地图,使用自带地图")
                return Vars.maps.all().toArray(Map::class.java)
            }
            return if (configEnableInternMaps) Vars.maps.all().toArray(Map::class.java) else Vars.maps.customMaps()
                .toArray(Map::class.java)
        }

    override fun loadMap(map: Map, mode: Gamemode) {
        resetAndLoad {
            world.loadMap(map, map.applyRules(mode))
            state.rules = state.map.applyRules(mode).apply {
                Regex("\\[(@[a-zA-Z0-9]+)(=[0-9a-z]+)?]").findAll(map.description()).forEach {
                    val value = it.groupValues[2].takeIf(String::isNotEmpty) ?: "true"
                    tags.put(it.groupValues[1], value.removePrefix("="))
                }
            }
            logic.play()
        }
    }

    override fun loadSave(file: Fi) {
        resetAndLoad {
            SaveIO.load(file)
            logic.play()
        }
    }

    private var _nextMap: Map? = null

    override fun nextMap(map: Map?, mode: Gamemode): Map {
        _nextMap?.let {
            _nextMap = null
            return it
        }
        val maps = maps.toMutableList()
        maps.shuffle()
        val ret = maps.filter { bestMode(it) == mode }.firstOrNull { it.file != map?.file } ?: maps[0]
        if (!SaveIO.isSaveValid(ret.file)) {
            ContentHelper.logToConsole("[yellow]invalid map ${ret.file.nameWithoutExtension()}, auto change")
            return nextMap(map, mode)
        }
        return ret
    }

    override fun setNextMap(map: Map) {
        _nextMap = map
    }

    override fun bestMode(map: Map): Gamemode {
        return when (map.file.name()[0]) {
            'A' -> Gamemode.attack
            'P' -> Gamemode.pvp
            'S' -> Gamemode.survival
            'C' -> Gamemode.sandbox
            'E' -> Gamemode.editor
            else -> Gamemode.survival
        }
    }

    private fun resetAndLoad(callBack: () -> Unit) {
        Core.app.post {
            if (!net.server()) netServer.openServer()
            val players = Groups.player.toList()
            players.forEach { it.clearUnit() }
            callBack()
            Call.worldDataBegin()
            players.forEach {
                if (it.con == null) return@forEach
                it.reset()
                it.team(netServer.assignTeam(it, players))
                netServer.sendWorldData(it)
            }
        }
    }

    override fun getSlot(id: Int): Fi? {
        val file = SaveIO.fileFor(id)
        if (!SaveIO.isSaveValid(file)) return null
        val voteFile = SaveIO.fileFor(configTempSaveSlot)
        if (voteFile.exists()) voteFile.delete()
        file.copyTo(voteFile)
        return voteFile
    }
}
provide<MapService>(MapManager)

PlaceHold.registerForType<Map>(this).apply {
    registerChild("id", "id in /maps", DynamicVar.obj { obj ->
        MapManager.maps.indexOfFirst { it.file == obj.file } + 1
    })
    registerChild("mode", "Map Setting Mode", DynamicVar.obj { obj ->
        MapManager.bestMode(obj).name
    })
}

command("maps", "List server map") {
    usage = "[page/pvp/attack/all] [page]"
    aliases = listOf("地图")
    body {
        val mode: Gamemode? = arg.getOrNull(0).let {
            when {
                "pvp".equals(it, true) -> Gamemode.pvp
                "attack".equals(it, true) -> Gamemode.attack
                "all".equals(it, true) -> null
                else -> Gamemode.survival.takeIf { mapsDistinguishMode }
            }
        }
        if (mapsDistinguishMode) reply("By default [yellow] only shows all survival maps, enter [green]/maps pvp[yellow] to show pvp maps, [green]/maps attack[yellow] to show siege maps [green]/maps all[yellow] to show all".with())
        val page = arg.lastOrNull()?.toIntOrNull()
        var maps = MapManager.maps.mapIndexed { index, map -> (index + 1) to map }
        maps = if (arg.getOrNull(0) == "new")
            maps.sortedByDescending { it.second.file.lastModified() }
        else
            maps.filter { mode == null || MapManager.bestMode(it.second) == mode }
        sendMenuPhone("Server Map [#00bbff] Rotterdam: [red]机[pink]械[purple]师", maps, page, mapsPrePage) { (id, map) ->
            "[pink]{id}[green]({map.width},{map.height})[]:[yellow]{map.fileName}[] | [#00bbff]{map.name}"
                .with("id" to "%2d".format(id), "map" to map)
        }
    }
}
onEnable {
    //hack to stop origin gameOver logic
    val control = Core.app.listeners.find { it.javaClass.simpleName == "ServerControl" }
    val field = control.javaClass.getDeclaredField("inExtraRound")
    field.apply {
        isAccessible = true
        setBoolean(control, true)
    }
}

val waitingTime by config.key(Duration.ofSeconds(10)!!, "游戏结束换图的等待时间")
val gameOverMsgType by config.key(MsgType.InfoMessage, "游戏结束消息是显示方式")
listen<EventType.GameOverEvent> { event ->
    ContentHelper.logToConsole(
        if (state.rules.pvp) "&lcGame over! Team &ly${event.winner.name}&lc is victorious with &ly${Groups.player.size()}&lc players online on map &ly${state.map.name()}&lc."
        else "&lcGame over! Reached wave &ly${state.wave}&lc with &ly${Groups.player.size()}&lc players online on map &ly${state.map.name()}&lc."
    )
    val map = MapManager.nextMap(state.map)
    val winnerMsg: Any = if (state.rules.pvp) "[YELLOW] {team.colorizeName} Team win!".with("team" to event.winner) else ""
    val msg = """
                | [SCARLET] Game over! []"
                | {winnerMsg}
                | The next map is: [accent]{nextMap.name}[] By: [accent]{nextMap.author}[]
                | The next game will start in {waitTime} seconds
            """.trimMargin().with("nextMap" to map, "winnerMsg" to winnerMsg, "waitTime" to waitingTime.seconds)
    broadcast(msg, gameOverMsgType, quite = true)
    ContentHelper.logToConsole("Next Map is ${map.name()}")
    launch {
        val now = state.map
        delay(waitingTime.toMillis())
        if (state.map != now) return@launch//已经通过其他方式换图
        MapManager.loadMap(map)
    }
}
command("host", "Management Instructions: Change Map") {
    usage = "[mapId] [mode]"
    permission = "wayzer.maps.host"
    body {
        val map = if (arg.isEmpty()) MapManager.nextMap(state.map) else
            arg[0].toIntOrNull()?.let { MapManager.maps.getOrNull(it - 1) }
                ?: returnReply("[red]Please enter the correct map ID".with())
        val mode = arg.getOrNull(1)?.let { name ->
            Gamemode.values().find { it.name == name } ?: returnReply("[red]Please enter the correct mode".with())
        } ?: MapManager.bestMode(map)
        MapManager.loadMap(map, mode)
        broadcast("[green]强制换图为{map.name},模式{map.mode}".with("map" to map, "map.mode" to mode.name))
    }
}
command("load", "Manage commands: Load archive") {
    usage = "<slot>"
    permission = "wayzer.maps.load"
    body {
        val file = arg[0].let { saveDirectory.child("$it.$saveExtension") }
        if (!file.exists() || !SaveIO.isSaveValid(file))
            returnReply("[red]Archive does not exist or is corrupted".with())
        MapManager.loadSave(file)
        broadcast("[green] Force load archive {slot}".with("slot" to arg[0]))
    }
}