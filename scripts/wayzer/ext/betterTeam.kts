package wayzer.ext

import mindustry.core.NetServer
import mindustry.game.Team
import mindustry.gen.Groups

name = "A better team"

val enableTeamLock by config.key(true, "PVP mode team lock, single game can not change the team")
val spectateTeam = Team.all[255]!!
val backup = netServer.assigner!!
onDisable {
    netServer.assigner = backup
}

val teams = mutableMapOf<String, Team>()
onEnable {
    netServer.assigner = object : NetServer.TeamAssigner {
        override fun assign(player: Player, p1: MutableIterable<Player>): Team {
            if (!enableTeamLock) return backup.assign(player, p1)
            if (!state.rules.pvp) return state.rules.defaultTeam
            if (teams[player.uuid()]?.active() == false)
                teams.remove(player.uuid())
            return teams.getOrPut(player.uuid()) {
                //not use old,because it may assign to team without core
                val teams = state.teams.active.filter { it.hasCore() }
                teams.shuffled()
                teams.minByOrNull { p1.count { p -> p.team() == it.team && player != p } }!!.team
            }
        }
    }
}
listen<EventType.ResetEvent> {
    teams.clear()
}

fun changeTeam(p: Player, team: Team) {
    teams[p.uuid()] = team
    p.clearUnit()
    p.team(team)
    p.clearUnit()
}

export(::changeTeam)

command("ob", "Switch to observer") {
    type = CommandType.Client
    permission = "wayzer.ext.observer"
    body {
        if (player!!.team() == spectateTeam) {
            val team = netServer.assignTeam(player!!)
            changeTeam(player!!, team)
            broadcast(
                "[yellow]player[green]{player.name}[yellow]reincarnate to {team.colorizeName}"
                    .with("player" to player!!, "team" to team), quite = true
            )
        } else {
            changeTeam(player!!, spectateTeam)
            broadcast("[yellow]player[green]{player.name}[yellow]choose to be an observer".with("player" to player!!), quite = true)
            player!!.sendMessage("[green] Enter the command again to reincarnate")
        }
    }
}

command("team", "Management commands: Modify your own or others' teams (PVP mode)") {
    usage = "[Team, do not fill in the list] [Player 3 ids, default themselves]"
    permission = "wayzer.ext.team.change"
    body {
        if (!state.rules.pvp) returnReply("[red]Available in PVP mode only".with())
        val team = arg.getOrNull(0)?.toIntOrNull()?.let { Team.get(it) } ?: let {
            val teams = Team.baseTeams
                .mapIndexed { i, t -> "{id}({team.colorizeName}[])".with("id" to i, "team" to t) }
            returnReply("[yellow]Available teams: []{list}".with("list" to teams))
        }
        val player = arg.getOrNull(1)?.let {
            Groups.player.find { p -> p.uuid().startsWith(it) }
                ?: returnReply("[red]Can't find the player, please use /list to look up the correct 3 digit id".with())
        } ?: player ?: returnReply("[red]Please enter the player ID".with())
        changeTeam(player, team)
        broadcast("[green]admin changed {player.name}[green] to {team.colorizeName}".with("player" to player, "team" to team))
    }
}