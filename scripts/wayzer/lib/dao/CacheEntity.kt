package wayzer.lib.dao

/**
 * @author WayZer
 * Self-made database Entity class, support in non-transaction modified entity properties, and finally saved by save (not dependent on exposed-dao.jar)
 * will be moved to coreLib in the future
 */
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import kotlin.reflect.KProperty

open class CacheEntity<ID : Comparable<ID>>(private val table: IdTable<ID>) {
    @RequiresOptIn("need in Transaction{}",level = RequiresOptIn.Level.WARNING)
    @OptIn(NeedTransaction::class)
    annotation class NeedTransaction
    private val changed = mutableMapOf<Column<*>, Any?>()
    private var resultRow: ResultRow? = null
    var id: EntityID<ID> by table.id
        private set

    /**
     * @return false can't find
     */
    @NeedTransaction
    fun load(where: SqlExpressionBuilder.() -> Op<Boolean>): Boolean {
        val resultRow = table.select(where).firstOrNull()
        return resultRow != null && load(resultRow)
    }

    open fun load(resultRow: ResultRow): Boolean {
        this.resultRow = resultRow
        changed.clear()
        return true
    }

    /**
     * @return false can't find
     */
    @NeedTransaction
    fun loadById(id: ID): Boolean = load {
        table.id eq EntityID(id, table)
    }

    @NeedTransaction
    fun reload() = loadById(id.value)

    @NeedTransaction
    open fun save(id: ID? = null) {
        fun <T> UpdateBuilder<*>.set(k: Column<T>, v: Any?) {
            @Suppress("UNCHECKED_CAST")
            set(k, v as T)
        }
        if (resultRow==null) {
            loadById(table.insertAndGetId {
                if (id != null) it[this.id] = EntityID(id, table)
                changed.forEach { (k, v) -> it.set(k, v) }
            }.value)
        } else if (changed.isNotEmpty()) {
            table.update({ table.id eq this@CacheEntity.id }) {
                if (id != null) it[this.id] = EntityID(id, table)
                changed.forEach { (k, v) -> it.set(k, v) }
            }
            reload()
        }
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> Column<T>.getValue(obj: CacheEntity<*>, desc: KProperty<*>): T {
        if (this in changed) return changed[this] as T
        if (resultRow==null)
            return defaultValueFun?.invoke() ?: error("not init,load first")
        return resultRow!![this]
    }

    operator fun <T> Column<T>.setValue(obj: CacheEntity<*>, desc: KProperty<*>, v: T) {
        if (resultRow?.get(this) == v)changed.remove(this)
        else changed[this] = v
    }
    open class EntityClass<ID : Comparable<ID>,T:CacheEntity<ID>>(val factory:()->T){
        private val cacheData = mutableMapOf<ID,T>()
        val allCached get() = cacheData.values as Collection<T>
        protected fun addCache(t:T):T{
            if(t.id.value in cacheData) error("$t already in cache")
            cacheData[t.id.value] = t
            return t
        }
        open fun removeCache(id:ID):T? = cacheData.remove(id)?.apply {
            if(changed.isNotEmpty()) error("Entity CHANGE don't save $this")
        }
        fun getOrNull(id:ID) = cacheData[id]
        @NeedTransaction
        fun getOrFind(id:ID,cache:Boolean):T? = cacheData.getOrElse(id){
            factory().apply {
                if(!loadById(id))return null
                if(cache) cacheData[id] = this
            }
        }
        @NeedTransaction
        fun findOrCreate(id:ID,cache:Boolean,default: T.() -> Unit) = cacheData.getOrElse(id){
            factory().apply {
                if(!loadById(id)){
                    default()
                }
                if(cache) cacheData[id] = this
            }
        }
    }
}