package wayzer.ext.reGrief

import arc.graphics.Color
import cf.wayzer.placehold.PlaceHoldApi.with
import mindustry.content.Blocks
import mindustry.content.Fx
import mindustry.gen.Call
import mindustry.type.Item
import mindustry.world.Block
import mindustry.world.blocks.storage.CoreBlock
import java.time.Instant
import java.util.*

sealed class Log(val uid: String, val time: Instant) {
    class Place(uid: String, time: Instant, val type: Block) : Log(uid, time)
    class Break(uid: String, time: Instant) : Log(uid, time)
    class Config(uid: String, time: Instant, val value: String) : Log(uid, time)
    class Deposit(uid: String, time: Instant, val item: Item, val amount: Int) : Log(uid, time)
}

val historyLimit by config.key(10, "Longest diary entry in a single cell")
lateinit var logs: Array<Array<List<Log>>>

//初始化
fun initData() {
    logs = Array(world.width()) {
        Array(world.height()) {
            emptyList()
        }
    }
}
onEnable {
    if (net.server())
        initData()
}
listen<EventType.WorldLoadEvent> {
    initData()
}

//记录
fun log(x: Int, y: Int, log: Log) {
    if (historyLimit <= 0) return
    if (logs[x][y].isEmpty()) logs[x][y] = LinkedList(listOf(log))
    else with(logs[x][y] as LinkedList) {
        while (size >= historyLimit)
            remove()
        add(log)
    }
}
listen<EventType.BlockBuildEndEvent> {
    val player = it.unit.player ?: return@listen
    if (it.breaking)
        log(it.tile.x.toInt(), it.tile.y.toInt(), Log.Break(player.uuid(), Instant.now()))
    else
        log(it.tile.x.toInt(), it.tile.y.toInt(), Log.Place(player.uuid(), Instant.now(), it.tile.block()))
}
listen<EventType.ConfigEvent> {
    val player = it.player ?: return@listen
    log(it.tile.tileX(), it.tile.tileY(), Log.Config(player.uuid(), Instant.now(), it.value.toString()))
}
listen<EventType.DepositEvent> {
    val player = it.player ?: return@listen
    log(it.tile.tileX(), it.tile.tileY(), Log.Deposit(player.uuid(), Instant.now(), it.item, it.amount))
}

fun Player.showLog(xf: Float, yf: Float) {
    val x = xf.toInt() / 8
    val y = yf.toInt() / 8
    if (x < 0 || x >= world.width()) return
    if (y < 0 || y >= world.height()) return
    val logs = logs[x][y]
    if (logs.isEmpty()) Call.label(con, "[yellow] position($x,$y) no record", 5f, xf, yf)
    else {
        val list = logs.map { log ->
            "[red]{time:HH:mm:ss}[]-[yellow]{info.name}[yellow]({info.shortID})[white]{desc}".with(
                "time" to Date.from(log.time), "info" to netServer.admins.getInfo(log.uid), "desc" to when (log) {
                    is Log.Place -> "Placed the square ${log.type.name}"
                    is Log.Break -> "Removed the cube"
                    is Log.Config -> "Changed property: ${log.value}"
                    is Log.Deposit -> "Dropped ${log.amount} and ${log.item.name} into it"
                }
            )
        }
        Call.label(
            con,
            "====[gold]操作记录($x,$y)[]====\n{list:\n}".with("list" to list, "receiver" to this).toString(),
            15f,
            xf,
            yf
        )
    }
}

//查询
val enabledPlayer = mutableSetOf<String>()
command("history", "Switching query mode") {
    permission = "wayzer.ext.history"
    usage = "[core(query core)]"
    aliases = listOf("历史")
    body {
        if (arg.getOrElse(0) { "" }.contains("core")) returnReply(
            "[green]Core damage perimeter:\n{list:\n}".with("list" to lastCoreLog)
        )
        if (player == null) returnReply("[red]Console can only query core damage records".with())
        if (player!!.uuid() in enabledPlayer) {
            enabledPlayer.remove(player!!.uuid())
            reply("[green] Close query mode".with())
        } else {
            enabledPlayer.add(player!!.uuid())
            reply("[green] Open query mode, click on the square to query history".with())
        }
    }
}

listen<EventType.TapEvent> {
    val p = it.player
    if (p.uuid() !in enabledPlayer) return@listen
    Call.effect(p.con, Fx.placeBlock, it.tile.worldx(), it.tile.worldy(), 0.5f, Color.green)
    p.showLog(it.tile.worldx(), it.tile.worldy())
}

// Automatic retention of suspicious behavior that damages the core
var lastCoreLog = emptyList<PlaceHoldString>()
var lastTime = 0L
val dangerBlock = arrayOf(
    Blocks.thoriumReactor,
    Blocks.liquidTank, Blocks.liquidRouter, Blocks.bridgeConduit, Blocks.phaseConduit,
    Blocks.conduit, Blocks.platedConduit, Blocks.pulseConduit
)
listen<EventType.BlockDestroyEvent> { event ->
    if (event.tile.block() is CoreBlock) {
        if (System.currentTimeMillis() - lastTime > 5000) { //Prevent cores from exploding in succession, record only the first blown core
            val list = mutableListOf<PlaceHoldString>()
            for (x in event.tile.x.let { it - 10..it + 10 })
                for (y in event.tile.y.let { it - 10..it + 10 })
                    logs.getOrNull(x)?.getOrNull(y)?.lastOrNull { it is Log.Place }?.let { log ->
                        if (log is Log.Place && log.type in dangerBlock)
                            list.add(
                                "[red]{time:HH:mm:ss}[]-[yellow]{info.name}[yellow]({info.shortID})[white]{desc}".with(
                                    "time" to Date.from(log.time), "info" to netServer.admins.getInfo(log.uid),
                                    "desc" to "placed {type} at a distance from the core ({x},{})".with(
                                        "x" to (x - event.tile.x), "y" to (y - event.tile.y), "type" to log.type.name
                                    )
                                )
                            )
                    }
            lastCoreLog = list
        }
        lastTime = System.currentTimeMillis()
    }
}

