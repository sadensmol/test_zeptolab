import domain.repository.InMemoryChannelRepository
import domain.repository.InMemoryUserRepository


fun main() {

    val usersRepository = InMemoryUserRepository()
    val channelsRepository = InMemoryChannelRepository()
    val chatService = ChatService(userRepository = usersRepository, channelRepository = channelsRepository)

    Server(chatService).start()
}