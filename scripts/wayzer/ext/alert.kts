package wayzer.ext

import java.time.Duration

val type by config.key(MsgType.InfoMessage, "Sending method")
val time by config.key(Duration.ofMinutes(10)!!,"Announcement interval")
val list by config.key(emptyList<String>(),"Announcement list, support color and variables")

var i = 0
fun broadcast(){
    if(list.isEmpty())return
    i %= list.size
    broadcast(list[i].with(),type,15f)
    i++
}

onEnable{
    launch(Dispatchers.game) {
        while (true) {
            delay(time.toMillis())
            broadcast()
        }
    }
}