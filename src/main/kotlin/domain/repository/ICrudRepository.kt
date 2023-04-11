package domain.repository

import kotlinx.coroutines.flow.Flow

interface ICrudRepository<T, ID> {
    fun save(value: T)
    fun getAll(): Flow<T>
    fun findById(id: ID): T?
    fun deleteById(id: ID)

}