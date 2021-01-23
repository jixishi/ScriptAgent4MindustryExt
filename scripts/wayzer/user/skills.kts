package wayzer.user

import mindustry.game.EventType
import mindustry.game.Gamemode
import mindustry.gen.Firec
import mindustry.gen.Groups
import java.time.Duration

@Savable
var used = mutableMapOf<String, Long>()

listen<EventType.ResetEvent> {
    used.clear()
}

/**
 * @param coolDown in ms; <0 ->一局冷却
 */
fun skill(name: String, desc: String, allowPvp: Boolean, coolDown: Int?, vararg aliases: String, body: CommandContext.() -> Unit) {
    command(name, desc) {
        permission = "wayzer.user.skills.$name"
        this.aliases = aliases.toList()
        type = CommandType.Client
        body {
            if (state.rules.mode() !in arrayOf(Gamemode.survival, Gamemode.attack) && (!allowPvp || state.rules.pvp))
                returnReply("[red]Current mode disabled".with())
            if (player!!.dead())
                returnReply("[red]You are dead".with())
            if (coolDown != null) {
                val id = PlayerData[player!!.uuid()].profile?.id?.value ?: 0
                val key = "${name}@$id"
                if (key in used) {
                    if (coolDown < 0)
                        returnReply("[red]This skill is used once per limitation".with())
                    else if (used[key]!! >= System.currentTimeMillis())
                        returnReply("[red]Skill on cooldown, {time:seconds} left".with("time" to Duration.ofMillis(used[key]!! - System.currentTimeMillis())))
                }
                used[key] = System.currentTimeMillis() + coolDown
            }
            body()
        }
    }
}

fun Player.broadcastSkill(skill: String) = broadcast("[yellow][skill][green]{player.name}[white] used the [green]{skill}[white] skill.".with("player" to this, "skill" to skill), quite = true)

//skill("draug", "Skill: Summon mining machine, once per limit, PVP disabled", false, -1, "矿机") {
//    if (state.rules.bannedBlocks.contains(Blocks.airFactory))
//        return@skill reply("[red]This map mining machine has been banned, forbidden to summon".with())
//    UnitTypes.draug.create(player!!.team).apply {
//        set(player!!.x, player!!.y)
//        add()
//    }
//    player!!.broadcastSkill("Summon Mining Machine")
//}

skill("noFire", "Skills: Fire extinguisher, radius 10 frames, cooling 120s", false, 120_000, "灭火") {
    Groups.sync.filterIsInstance<Firec>().forEach {
        if (it.dst(player!!) <= 80)
            it.remove()
    }
    player!!.broadcastSkill("Fire Fighting")
}