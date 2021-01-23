package wayzer.user

import cf.wayzer.placehold.DynamicVar
import cf.wayzer.placehold.PlaceHoldApi.with
import mindustry.game.EventType
import mindustry.gen.Groups
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

val customWelcome by config.key(true, "是否开启自定义进服信息(中文)")

fun getIcon(level: Int): Char {
    if (level <= 0) return (63611).toChar()
    return (63663 - min(level, 12)).toChar()
    //0级为电池,1-12级为铜墙到合金墙
}

fun level(exp: Int) = floor(sqrt(max(exp, 0).toDouble()) / 10).toInt()
fun expByLevel(level: Int) = level * level * 100

registerVarForType<PlayerProfile>().apply {
    registerChild("level", "当前等级", DynamicVar.obj { level(it.totalExp) })
    registerChild("levelIcon", "当前等级图标", DynamicVar.obj { getIcon(level(it.totalExp)) })
    registerChild("nextLevel", "下一级的要求经验值", DynamicVar.obj { expByLevel(level(it.totalExp) + 1) })
}

/**
 * @return 所有在线用户
 */
fun updateExp(p: PlayerProfile, dot: Int): List<Player> {
    val players = Groups.player.filter { PlayerData.getOrNull(it.uuid())?.profile == p }
    p.apply {
        totalExp += dot
        if (level(totalExp) != level(totalExp - dot)) {
            players.forEach {
                it.sendMessage("[gold] Congratulations on your successful upgrade to {level}".with("level" to level(totalExp)))
                it.name = it.name.replaceFirst(Regex("<.>"), "<${getIcon(level(totalExp))}>")
            }
        }
    }
    return players
}
export(::updateExp)

listen<EventType.PlayerConnect> {
    Core.app.post {
        it.player.apply {
            name = "[white]<${getIcon(level(PlayerData.getOrNull(uuid())?.profile?.totalExp ?: 0))}>[#$color]$name"
        }
    }
}

listen<EventType.PlayerJoin> {
    if (!customWelcome) return@listen
    it.player.sendMessage("[cyan][+]{player.name} [gold] joined the server".with("player" to it.player))
    broadcast("[cyan][+]{player.name} [goldenrod] joined the server".with("player" to it.player))
}
