package coreLibrary.lib

import cf.wayzer.script_agent.IModuleScript
import cf.wayzer.script_agent.util.DSLBuilder
import coreLibrary.lib.DataBaseApi.registeredTable
import coreLibrary.lib.util.Provider
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

object DataBaseApi {
    val IModuleScript.registeredTable by DSLBuilder.dataKeyWithDefault { mutableSetOf<Table>() }
    val db = Provider<Database>()
}

/**
 * Registration form for the module
 * It does not necessarily run immediately when registered
 * Will wait for [DataBaseApi.db] to be initialized and then register it uniformly
 */
fun IModuleScript.registerTable(vararg t: Table) {
    registeredTable.addAll(t)
    DataBaseApi.db.listenWithAutoCancel(this) {
        transaction(it) {
            SchemaUtils.createMissingTablesAndColumns(*t)
        }
    }
}