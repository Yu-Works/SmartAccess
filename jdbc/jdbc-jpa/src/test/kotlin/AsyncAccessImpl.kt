import smartaccess.jdbc.access.JDBCPageAble
import smartaccess.jpa.access.JpaAsyncAccessBase
import smartaccess.jpa.db.JpaContext

class AsyncAccessImpl(
    context: JpaContext,
    pageable: JDBCPageAble?,
    _modelType: Class<User>,
    _primaryKeyType: Class<Int>
) : JpaAsyncAccessBase<User, Int>(context, pageable, _modelType, _primaryKeyType) {

    suspend fun findByUsernameAndPassword(username: String, password: String): User {
        val query = jpaQuery("findByUsernameAndPassword")
        query.setParameter(0, username)
        query.setParameter(1, password)
        return query.singleResult as User
    }

}