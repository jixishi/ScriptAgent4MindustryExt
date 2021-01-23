//1. different database driver Maven, according to the choice of comments
@file:MavenDepends("com.h2database:h2:1.4.200", single = false)
//@file:MavenDepends("org.postgresql:postgresql:42.2.15", single = false)
@file:Suppress("unused")

package coreLibrary

import org.jetbrains.exposed.sql.Database
import java.sql.Connection
import java.sql.DriverManager

//2. modify the items to be configured in the corresponding type (address, user name, password)
fun h2(): () -> Connection {
    sourceFile.parentFile.listFiles{_,n->n.startsWith("h2DB.db")}?.takeIf { it.isNotEmpty() }?.forEach {
        println("Detect old database files and automatically migrate them to the new directory")
        val new = Config.dataDirectory.resolve(it.name)
        if(new.exists()){
            println("The target file $new exists, do not overwrite it, please handle it yourself")
        }else {
            it.copyTo(new)
            it.delete()
        }
    }
    val file = Config.dataDirectory.resolve("h2DB.db")
    Class.forName("org.h2.Driver")
    return {DriverManager.getConnection("jdbc:h2:${file.absolutePath}")}
}

fun postgre(): () -> Connection {
    Class.forName("org.postgresql.Driver")
    //To use, please change the connection method and account password here
    return {DriverManager.getConnection("jdbc:postgresql://localhost:5432/mindustry","mindustry", "")}
}

onEnable {
    //3. Please re-comment here
    DataBaseApi.db.set(Database.connect(h2()))
//    DataBaseApi.db.set(Database.connect(postgre()))
}