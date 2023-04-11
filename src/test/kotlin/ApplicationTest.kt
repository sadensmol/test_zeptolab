import domain.repository.InMemoryChannelRepository
import domain.repository.InMemoryUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * Since lots of code has some common setup
 * it's better to refactor it to something like scenario-based
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ApplicationTest {

    private val usersRepository = InMemoryUserRepository()
    private val channelsRepository = InMemoryChannelRepository()
    private val chatService = ChatService(userRepository = usersRepository, channelRepository = channelsRepository)

    private var server: Server = Server(chatService)

    @BeforeAll
    fun setUp() {
        server.start()
    }

    @AfterAll
    fun tearDown() {
        server.stop()
    }

    @Test
    fun `should disconnect`() = runBlocking {
        val cl = Client()
        cl.start()
        assertTrue(cl.isConnected())
        cl.write("/disconnect")
        Thread.sleep(1000)
        assertFalse(cl.isConnected())
        cl.stop()
    }

    @Test
    fun `should login`() = runBlocking {
        val cl = Client()
        cl.start()
        val userName = UUID.randomUUID().toString()
        assertFalse(server.hasUser(userName))
        cl.write("/login $userName password")
        Thread.sleep(1000)
        assertTrue(server.hasUser(userName))
        cl.stop()
    }

    private fun loginAndJoin(cl: Client, userName: String, channelName: String) {
        cl.start()
        cl.write("/login $userName password")
        Thread.sleep(1000)
        assertTrue(server.hasUser(userName))
        cl.write("/join $channelName")
    }

    @Test
    fun `should login and join channel and leave channel`() = runBlocking {
        val cl = Client()
        val userName = UUID.randomUUID().toString()
        val channelName = UUID.randomUUID().toString()
        assertFalse(server.hasUser(userName))

        loginAndJoin(cl, userName, channelName )
        Thread.sleep(1000)
        assertTrue(server.hasUserInChannel(userName, channelName))
        cl.write("/leave")
        Thread.sleep(1000)
        cl.stop()
    }


    @Test
    fun `should join same channel same user multiple times`() = runBlocking {
        val chName="testchname"
        val cl = Client()
        loginAndJoin(cl, "testu1", chName)
        val cl2 = Client()
        loginAndJoin(cl2, "testu1", chName)
        val cl3 = Client()
        loginAndJoin(cl3, "testu1", chName)
        val cl4 = Client()
        loginAndJoin(cl4, "testu1", chName)

        Thread.sleep(1000)



    }



    @Test
    fun `should join channel only if channel has less than 10 clients`() = runBlocking {
        val clients = mutableListOf<Client>()
        val userName = UUID.randomUUID().toString()
        val channelName = UUID.randomUUID().toString()

        (0 until 20).map {
            async(Dispatchers.IO) {
                val cl = Client()
                clients.add(cl)
                loginAndJoin(cl, userName + it, channelName)
            }
        }.awaitAll()

        Thread.sleep(1000)

        var numberInChannels = 0

        for ((i, cl) in clients.withIndex()) {
            if (server.hasUserInChannel(userName + i, channelName )) numberInChannels++
            cl.stop()
        }

        assertEquals(10, numberInChannels)
    }

    @Test
    fun `should return list of channels`() = runBlocking {
        val cl = Client()
        loginAndJoin(cl, "testu1", "testch1")
        val cl2 = Client()
        loginAndJoin(cl2, "testu2", "testch2")
        val cl3 = Client()
        loginAndJoin(cl3, "testu3", "testch3")

        Thread.sleep(1000)
        cl.write("/list")
    }

    @Test
    fun `should return deduplicated list of users for specific channel`() = runBlocking {
        val chName="testchname"
        val cl = Client()
        loginAndJoin(cl, "testu1", chName)
        val cl2 = Client()
        loginAndJoin(cl2, "testu2", chName)
        val cl3 = Client()
        loginAndJoin(cl3, "testu3", chName)
        val cl4 = Client()
        loginAndJoin(cl4, "testu3", chName)
        val cl5 = Client()
        loginAndJoin(cl5, "testu5", "testchnameadditional")

        Thread.sleep(1000)
        cl.write("/users")

    }

    @Test
    fun `should send message from one user to another`() = runBlocking {
//        val cl1 = Client()
//        cl1.start()
//        val userName1 = UUID.randomUUID().toString()
//        cl1.write("/login $userName1 password")
//        Thread.sleep(1000)
//        val channelName = UUID.randomUUID().toString()
//        cl1.write("/join $channelName")
//        Thread.sleep(1000)
//        assertTrue(server.hasUserInChannel(userName, channelName))
//
//        cl.write("test message 12345!!!")
//        Thread.sleep(1000)
//
//        cl.stop()
    }



}

