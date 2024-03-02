import smartaccess.item.Page
import smartaccess.jpa.access.JpaAccess
import rain.classloader.AppClassloader


interface TestAccess : JpaAccess<String, String> {

    fun findByName(name: String): String
    fun findByNameAndAge(name: String, age: Int): String
    fun findByNameAndAgeAndSex(name: String, age: Int, sex: String, page: Page): List<String>

}

fun main() {
    val classloader = AppClassloader(TestAccess::class.java.classLoader)

//    val clazz = classloader.define(
//        "TestAccessImpl",
//        JpaAccessMaker(
//            JpaAccessBase::class.java,
//            TestAccess::class.java,
//            String::class.java,
//            String::class.java
//        ).also { File("TestAccessImpl.class").writeBytes(it) }
//    )
//    println(clazz)
}