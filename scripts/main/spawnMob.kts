package main
//WayZer 版权所有(请勿删除版权注解)
import mindustry.ctype.ContentType
import mindustry.game.Team
import mindustry.type.UnitType

name = "Extension: Summoning units"

command("spawn", "Summoning units") {
    usage = "[Type ID=listed] [Number=1] [Team ID, default is sharded]"
    permission = id.replace("/", ".")
    aliases = listOf("召唤")
    body {
        val list = content.getBy<UnitType>(ContentType.unit)
        val type = arg.getOrNull(0)?.toIntOrNull()?.let { list.items.getOrNull(it) } ?: returnReply(
            "[red]Please enter the type ID: {list}"
                .with("list" to list.mapIndexed { i, type -> "[yellow]$i[green]($type)" }.joinToString())
        )
        
        val num = arg.getOrNull(1)?.toIntOrNull() ?: 1
        repeat(num) {
            type.create(team).apply {
                if (player != null) set(player!!.unit().x, player!!.unit().y)
                else team.data().core()?.let {
                    set(it.x, it.y)
                }
                add()
            }
        }
        val team = arg.getOrNull(2)?.let { s ->
            s.toIntOrNull()?.let { Team.all.getOrNull(it) } ?: returnReply(
                "[red]Please enter team ID: {list}"
                    .with("list" to Team.baseTeams.mapIndexed { i, type -> "[yellow]$i[green]($type)" }.joinToString())
            )
        } ?: Team.sharded
        
        reply("[green] successfully generated {num} for {team} only {type}".with("team" to team, "num" to num, "type" to type.name))
    }
}