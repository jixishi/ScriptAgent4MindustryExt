package main

import arc.graphics.Colors

command("showColor", "Show all colors", {}) {
    reply(Colors.getColors().joinToString("[],") { "[#${it.value}]${it.key}" }.with())
}