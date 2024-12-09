import kotlinx.coroutines.runBlocking
import rain.classloader.SpawnClassLoader
import smartaccess.access.AccessMaker
import smartaccess.jpa.access.*
import smartaccess.jpa.db.JpaContext
import java.io.File
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.functions

interface AsyncAccess : JpaAsyncAccess<User, Int> {

//    suspend fun findById(): User
    suspend fun findByUserAndName(user: String, name: String): User

}

fun main() {
    val p = AccessMaker(
        JpaAsyncAccessBase::class.java,
        AsyncAccess::class.java,
        User::class.java,
        Int::class.java,
        JpaAsyncAccessMaker
    )
//    val bytes = AccessMaker.createSuspendMethodContextClass(type, method, 0)
    File("test_output.class").writeBytes(p.first)
    p.second.forEachIndexed { i, it ->
        File("test_output_$i.class").writeBytes(it)
    }
    val spawn = SpawnClassLoader(Thread.currentThread().contextClassLoader)
//    spawn.define("AsyncAccess\$Impl\$findById\$0", p.second.first())
    spawn.define("AsyncAccess\$Impl\$findByUserAndName\$0", p.second.first())
    val accessClass = spawn.define("AsyncAccess\$Impl", p.first)
    val access = accessClass.constructors.first().newInstance(JpaContext(hashMapOf()), null, User::class.java, Int::class.java)
    runBlocking {
        (access as AsyncAccess).findByUserAndName("","")
    }
}