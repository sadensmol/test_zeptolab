package domain.repository

import domain.Channel
import domain.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

class InMemoryUserRepository : ICrudRepository<User, String> {
    private val users = mutableListOf<User>()
    override fun save(value: User) {
        synchronized(this) {
            users.add(value)
        }
    }
    override fun getAll(): Flow<User> {
        return users.asFlow()
    }

    override fun deleteById(id: String) {
        synchronized(this) {
            users.firstOrNull { it.name == id }?.let { users.remove(it) }
        }
    }

    override fun findById(id: String): User? {
        return synchronized(this) {
            users.firstOrNull { it.name == id }
        }
    }
}