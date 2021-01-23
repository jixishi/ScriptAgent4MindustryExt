package wayzer.user

import org.jetbrains.exposed.sql.transactions.transaction

val template by config.key(
    """
    | [#DEA82A] {player.name} [#DEA82A]Personal Information[]
    | [#2B60DE] =======================================[]
    | [green]username[]:{player.name}
    | [green]Represents a 3-digit ID[white]:{player.shortID}
    | [green]earliest time to enter the service[]:{player.ext.firstJoin:YYYY-MM-dd}
    | {profileInfo}
    | [#2B60DE]=======================================[]
""".trimMargin(), "个人信息模板"
)
val profileTemplate by config.key(
    """
    | [green]Current bound account[]:{profile.id}
    | [green]Total online time[]:{profile.onlineTime:minutes}
    | [green]Current level[]:{profile.levelIcon}{profile.level}
    | [green]Current experience (required for next level)[]:{profile.totalExp}({profile.nextLevel})
    | [green]registrationTime[]:{profile.registerTime:YYYY-MM-dd}
""".trimMargin(), "统一账号信息介绍"
)

command("info", "Get current personal information") {
    type = CommandType.Client
    aliases = listOf("个人信息")
    body {
        val profile = PlayerData[player!!.uuid()].profile
        val profileInfo = profile?.let {
            profileTemplate.with("profile" to it)
        } ?: """
        [yellow]The current account is not bound, please private chat group robot "binding" to bind
        [yellow]You can get experience and use more functions only after binding successfully.
    """.trimIndent()
        reply(template.with("player" to player!!, "profileInfo" to profileInfo), MsgType.InfoMessage)
    }
}

command("mInfo", "Obtain user information") {
    usage = "[uid]"
    permission = "wayzer.info.other"
    body {
        if (arg.isEmpty()) returnReply("[red]请输入玩家uuid".with())
        val player = netServer.admins.getInfo(arg[0]) ?: returnReply("[red]Player not found".with())

        @Suppress("EXPERIMENTAL_API_USAGE")
        val data = transaction { PlayerData.find(player) }
            ?: returnReply("[red]Player not found".with())
        val profileInfo = data.profile?.let {
            profileTemplate.with("profile" to it)
        } ?: """
        [yellow] The player is not bound to the account
    """.trimIndent()
        reply(template.with("player" to player, "player.ext" to data, "profileInfo" to profileInfo))
    }
}