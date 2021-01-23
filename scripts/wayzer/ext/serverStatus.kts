package wayzer.ext

command("status", "Get server information") {
    aliases = listOf("服务器状态")
    body {
        reply(
            """
            | [green]Server Status[]
            | [green]map: [ [yellow]{map.id} [green]][yellow]{map.name}[green] mode: [yellow]{map.mode} [green]wave count [yellow]{state.wave}
            | [green]{fps} FPS, {heapUse} MB used[]
            | [green]TotalUnits: {state.allUnit} Players: {state.playerSize}
            | [yellow]Total banned: {state.allBan}
            """.trimMargin().with()
        )
    }
}