@file:Import("@wayzer/services/VoteService.kt", sourceFile = true)
@file:Import("@wayzer/services/MapService.kt", sourceFile = true)

package wayzer.ext

import arc.files.Fi
import arc.util.Time
import cf.wayzer.script_agent.util.ServiceRegistry
import mindustry.game.Team
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.io.MapIO
import mindustry.io.SaveIO
import wayzer.services.MapService
import wayzer.services.VoteService
import java.io.InputStream
import java.net.URL
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.min
import kotlin.random.Random

val voteService by ServiceRegistry<VoteService>()
val mapService by ServiceRegistry<MapService>()

val enableWebMap by config.key(false, "Whether to allow web maps", "来自mdt.wayzer.top")

class NetFi(private val url: URL, file: String) : Fi(file) {
    override fun read(): InputStream {
        return url.openStream()
    }
}

fun VoteService.register() {
    addSubVote("Vote for a new picture", "<地图ID> [Network permutation type parameter]", "map", "换图") {
        if (arg.isEmpty())
            returnReply("[red]Please enter the map serial number".with())
        val maps = mapService.maps
        launch(Dispatchers.game) {
            val map = when {
                Regex("[0-9a-z]{32}.*").matches(arg[0]) -> {
                    if (!enableWebMap) return@launch reply("[red]This service does not open the network map support".with())
                    val mode = arg.getOrElse(1) { "Q" }
                    reply("[green] Loading network map in".with())
                    try {
                        withContext(Dispatchers.IO) {
                            @Suppress("BlockingMethodInNonBlockingContext")
                            MapIO.createMap(
                                NetFi(
                                    URL("https://mdt.wayzer.top/api/maps/${arg[0]}/download.msav"),
                                    mode + "download.msav"
                                ), true
                            )
                        }
                    } catch (e: Exception) {
                        reply("[red]Network map failed to load, please try again later".with())
                        throw e
                    }
                }
                arg[0].toIntOrNull() in 1..maps.size -> {
                    maps[arg[0].toInt() - 1]
                }
                else -> return@launch reply("[red]Error parameters".with())
            }
            start(
                player!!,
                "changeMap({nextMap.id}: [yellow]{nextMap.name}[yellow])".with("nextMap" to map),
                supportSingle = true
            ) {
                if (!SaveIO.isSaveValid(map.file))
                    return@start broadcast("[red]Failed to change map,Map[yellow]{nextMap.name}[green](id: {nextMap.id})[red] is corrupted".with("nextMap" to map))
                depends("wayzer/user/statistics")?.import<(Team) -> Unit>("onGameOver")?.invoke(Team.derelict)
                mapService.loadMap(map)
                Core.app.post { // Push back and make sure the map loads successfully
                    broadcast("[green] The change of map is successful, current map [yellow]{map.name}[green](id: {map.id})".with())
                }
            }
        }
    }
    addSubVote("Surrender or end the game and settle", "", "gameOver", "投降", "结算") {
        if (state.rules.pvp) {
            val team = player!!.team()
            if (!state.teams.isActive(team) || state.teams.get(team)!!.cores.isEmpty)
                returnReply("[red]The team has lost, no need to surrender".with())

            canVote = canVote.let { default -> { default(it) && it.team() == team } }
            requireNum = { allCanVote().size }
            start(player!!, "Surrender ({team.colorizeName}[yellow] team|needs full team agreement)".with("player" to player!!, "team" to team)) {
                state.teams.get(team).cores.forEach { Time.run(Random.nextFloat() * 60 * 3, it::kill) }
            }
        }
        start(player!!, "投降".with(), supportSingle = true) {
            state.teams.get(player!!.team()).cores.forEach { Time.run(Random.nextFloat() * 60 * 3, it::kill) }
        }
    }
    addSubVote("Fast wave (default 10 waves, maximum 50)", "[Wave]", "skipWave", "跳波") {
        val lastResetTime by PlaceHold.reference<Instant>("state.startTime")
        val t = min(arg.firstOrNull()?.toIntOrNull() ?: 10, 50)
        start(player!!, "Jumping wave ({t} wave)".with("t" to t), supportSingle = true) {
            launch {
                val startTime = Instant.now()
                var waitTime = 3
                repeat(t) {
                    while (state.enemies > 300) {//Extended waiting time
                        if (waitTime > 60) return@launch //Waiting timeout
                        delay(waitTime * 1000L)
                        waitTime *= 2
                    }
                    if (lastResetTime > startTime) return@launch //Have change map
                    Core.app.post { logic.runWave() }
                    delay(waitTime * 1000L)
                }
            }
        }
    }
    addSubVote("Rollback to an archive (use /slots to view)", "<SaveID>", "rollback", "load", "回档") {
        if (arg.firstOrNull()?.toIntOrNull() == null)
            returnReply("[red]Please enter the correct archive number".with())
        val map = mapService.getSlot(arg[0].toInt())
            ?: returnReply("[red]The archive does not exist or the archive is corrupted".with())
        start(player!!, "回档".with(), supportSingle = true) {
            depends("wayzer/user/statistics")?.import<(Team) -> Unit>("onGameOver")?.invoke(Team.derelict)
            mapService.loadSave(map)
            broadcast("[green]回档成功".with(), quite = true)
        }
    }
    addSubVote("Kick out someone for 15 minutes", "<Player Name>", "kick", "踢出") {
        val target = Groups.player.find { it.name == arg.joinToString(" ") }
            ?: returnReply("[red]Please enter the correct player name, or go to the list and click on vote".with())
        val adminBan = depends("wayzer/admin")?.import<(Player, String) -> Unit>("ban")
        if (hasPermission("wayzer.vote.ban") && adminBan != null) {
            return@addSubVote adminBan(player!!, target.uuid())
        }
        start(player!!, "Kicker(kick out [red]{target.name}[yellow])".with("target" to target)) {
            target.info.lastKicked = Time.millis() + (15 * 60 * 1000) //Kick for 15 Minutes
            target.con?.kick("[yellow] You were voted out for 15 minutes")
            val secureLog = depends("wayzer/admin")?.import<(String, String) -> Unit>("secureLog") ?: return@start
            secureLog(
                "Kick",
                "${target.name}(${target.uuid()},${target.con.address}) is kicked By ${player!!.name}(${player!!.uuid()})"
            )
        }
    }
    addSubVote("Clean up our team's building records", "", "clear", "清理", "清理记录") {
        val team = player!!.team()

        canVote = canVote.let { default -> { default(it) && it.team() == team } }
        requireNum = { ceil(allCanVote().size * 2.0 / 5).toInt() }
        start(player!!, "Clear building records ({team.colorizeName}[yellow] team|needs 2/5 consent)".with("team" to team)) {
            team.data().blocks.clear()
        }
    }
    addSubVote("Customized Voting", "<Content>", "text", "文本", "t") {
        if (arg.isEmpty()) returnReply("[red]Please enter your vote".with())
        start(player!!, "custom([green]{text}[yellow])".with("text" to arg.joinToString(" "))) {}
    }
}

onEnable {
    voteService.register()
}