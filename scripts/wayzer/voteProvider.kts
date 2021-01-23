@file:Depends("wayzer/user/statistics", "玩家活跃判定", soft = true)
@file:Import("@wayzer/services/VoteService.kt", sourceFile = true)

package wayzer

import cf.wayzer.placehold.PlaceHoldContext
import cf.wayzer.script_agent.util.ServiceRegistry
import coreMindustry.lib.util.sendMenuPhone
import mindustry.game.EventType
import mindustry.gen.Call
import mindustry.gen.Groups
import wayzer.services.VoteService
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil
import kotlin.math.max

name = "投票服务"

val voteTime by config.key(Duration.ofSeconds(60)!!, "Voting time")

inner class VoteCommands : Commands() {
    override fun invoke(context: CommandContext) {
        if (VoteHandler.voting.get()) return context.reply("[red]Voting is underway".with())
        super.invoke(context)
        if (VoteHandler.voting.get()) {//success
            Call.sendMessage(
                context.prefix + context.arg.joinToString(" "),
                mindustry.core.NetClient.colorizeName(context.player!!.id, context.player!!.name),
                context.player!!
            )
        }
    }

    override fun onHelp(context: CommandContext, explicit: Boolean) {
        if (!explicit) context.reply("[red]Wrong vote type, please check if the input is correct".with())
        context.sendMenuPhone("Available Voting Types", subCommands.values.toSet().filter {
            it.permission.isBlank() || context.hasPermission(it.permission)
        }, 1, 100) {
            context.helpInfo(it, false)
        }
    }
}

//instead of object because object can't be inner
@Suppress("PropertyName")
val VoteHandler = VoteHandler()

inner class VoteHandler : VoteService {
    override val voteCommands: Commands = VoteCommands()

    //private set
    val voting = AtomicBoolean(false)
    private val voted: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private var lastAction = 0L //The last time a player quit or voted successfully, used to process single player votes

    lateinit var voteDesc: PlaceHoldContext
    override lateinit var requireNum: () -> Int
    override lateinit var canVote: (Player) -> Boolean

    init {
        reset()
    }

    override fun start(player: Player, voteDesc: PlaceHoldContext, supportSingle: Boolean, onSuccess: () -> Unit) {
        if (voting.get()) return
        voting.set(true)
        this.voteDesc = voteDesc
        GlobalScope.launch(Dispatchers.game) {
            try {
                if (supportSingle && allCanVote().run { count(canVote) == 0 || singleOrNull() == player }) {
                    if (System.currentTimeMillis() - lastAction > 60_000) {
                        broadcast("[yellow] Single Quick Vote {type} Success".with("type" to voteDesc))
                        lastAction = System.currentTimeMillis()
                        Core.app.post(onSuccess)
                        return@launch
                    } else
                        broadcast("[red]Less than 1 minute after the last player left or the last successful vote, quick vote failed".with())
                }
                broadcast(
                    "[yellow]{player.name}[yellow] initiate {type}[yellow] vote, total {require} people, enter y or 1 to agree"
                        .with("player" to player, "require" to requireNum(), "type" to voteDesc)
                )
                repeat(voteTime.seconds.toInt()) {
                    delay(1000L)
                    if (voted.size >= requireNum()) {//Early end
                        broadcast(
                            "[yellow]{type}[yellow]Voting is over, voting is successful. [green]{voted}/{all}[yellow], reached [red]{require}[yellow] people"
                                .with(
                                    "type" to voteDesc,
                                    "voted" to voted.size,
                                    "require" to requireNum(),
                                    "all" to allCanVote().count(canVote)
                                )
                        )
                        Core.app.post(onSuccess)
                        return@launch
                    }
                }
                //TimeOut
                broadcast(
                    "[yellow]{type}[yellow]Voting is over, voting failed. [green]{voted}/{all}[yellow], not reached [red]{require}[yellow] people"
                        .with(
                            "type" to voteDesc,
                            "voted" to voted.size,
                            "require" to requireNum(),
                            "all" to allCanVote().count(canVote)
                        )
                )
            } finally {
                reset()
            }
        }
    }

    override fun allCanVote() = Groups.player.filter(canVote)

    private fun reset() {
        requireNum = { max(ceil(allCanVote().size * 2.0 / 3).toInt(), 2) }
        canVote = {
            val active = depends("wayzer/user/statistics")?.import<(Player) -> Boolean>("active") ?: { true }
            !it.dead() && active(it)
        }
        voted.clear()
        voting.set(false)
    }

    fun onVote(p: Player) {
        if (!voting.get()) return
        if (p.uuid() in voted) return p.sendMessage("[red]You have voted".with())
        if (!canVote(p)) return p.sendMessage("[red]You can't vote on this".with())
        voted.add(p.uuid())
        broadcast("[green]Voting success, still need {left} people to vote".with("left" to (requireNum() - voted.size)), quite = true)
    }

    fun onLeave(p: Player) {
        lastAction = System.currentTimeMillis()
        voted.remove(p.uuid())
    }

    override fun ISubScript.addSubVote(
        desc: String,
        usage: String,
        vararg aliases: String,
        body: CommandContext.() -> Unit
    ) {
        voteCommands += CommandInfo(this, aliases.first(), desc) {
            this.usage = usage
            this.aliases = aliases.toList()
            body(body)
        }
        voteCommands.autoRemove(this)
    }
}

provide<VoteService>(VoteHandler)

command("vote", "Initiate a poll") {
    type = CommandType.Client
    aliases = listOf("投票")
    body(VoteHandler.voteCommands)
}
command("votekick", "(Abstain) Vote Kicker") {
    this.usage = "<player...>";this.type = CommandType.Client
    body {
        //Redirect
        arg = listOf("kick", *arg.toTypedArray())
        VoteHandler.voteCommands.invoke(this)
    }
}

listen<EventType.PlayerChatEvent> { e ->
    e.player.textFadeTime = 0f //Prevent the judgment of hanging for not talking
    if (e.message.equals("y", true) || e.message == "1") VoteHandler.onVote(e.player)
}

listen<EventType.PlayerJoin> {
    if (!VoteHandler.voting.get()) return@listen
    it.player.sendMessage("[yellow] currently voting on {type}[yellow], enter y or 1 to agree".with("type" to VoteHandler.voteDesc))
}

listen<EventType.PlayerLeave> {
    VoteHandler.onLeave(it.player)
}