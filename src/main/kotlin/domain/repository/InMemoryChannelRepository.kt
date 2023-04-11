package domain.repository

import domain.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

class InMemoryChannelRepository : ICrudRepository<Channel, String> {
    private val channels = mutableListOf<Channel>()
    override fun save(value: Channel) {
        synchronized(this) {
            channels.add(value)
        }
    }
    override fun getAll(): Flow<Channel> {
        return channels.asFlow()
    }
    override fun deleteById(id: String) {
        synchronized(this) {
            channels.firstOrNull { it.name == id }?.let { channels.remove(it) }
        }
    }

    override fun findById(id: String): Channel? {
        return synchronized(this) {
            channels.firstOrNull { it.name == id }
        }
    }

}