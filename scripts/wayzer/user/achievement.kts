package wayzer.user

import org.jetbrains.exposed.sql.transactions.transaction
import wayzer.lib.dao.Achievement

fun finishAchievement(profile: PlayerProfile, name: String, exp: Int, broadcast: Boolean = false) {
    val updateExp = depends("wayzer/user/level")?.import<PlayerProfile.(Int) -> List<Player>>("updateExp")
    if (updateExp == null) {
        println("[Error] Rating system is not available")
        return
    }
    @OptIn(CacheEntity.NeedTransaction::class)
    transaction {
        if (!Achievement.newWithCheck(profile.id, name, exp)) return@transaction
        val players = profile.updateExp(exp)
        profile.save()
        if (broadcast) {
            broadcast("[gold][achievement] Congratulations to {player.name}[gold] for completing achievement [scarlet]{name},[gold] for gaining [violet]{exp}[gold] experience".with(
                    "player" to (players.firstOrNull() ?: ""), "name" to name, "exp" to exp
            ))
        } else {
            players.forEach {
                it.sendMessage("[gold][achievement]Congratulations on completing achievement [scarlet]{name},[gold]Gain [violet]{exp}[gold] experience".with(
                        "name" to name, "exp" to exp
                ))
            }
        }
    }
}
export(::finishAchievement)

command("achieve", "Management Instructions: Add Achievement") {
    this.usage = "<qq> <name> <exp>"
    this.type = CommandType.Server
    permission = "wayzer.user.achieve"
    body {
        if (arg.size < 3) replyUsage()
        val profile = arg[0].toLongOrNull()?.let {
            transaction {
                @OptIn(CacheEntity.NeedTransaction::class)
                PlayerProfile.getOrFindByQQ(it, false)
            }
        } ?: returnReply("[red]The user could not be found".with())
        val name = arg[1]
        val exp = arg[2].toIntOrNull() ?: returnReply("[red]Please enter the correct number".with())
        finishAchievement(profile, name, exp, false)
        reply("[green]Added successfully".with())
    }
}