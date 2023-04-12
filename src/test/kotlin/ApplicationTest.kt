import domain.repository.InMemoryChannelRepository
import domain.repository.InMemoryUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
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
internal class ApplicationTest {

    private val usersRepository = InMemoryUserRepository()
    private val channelsRepository = InMemoryChannelRepository()
    private val chatService = ChatService(userRepository = usersRepository, channelRepository = channelsRepository)

    private var server: Server = Server(chatService)

    @BeforeEach
    fun setUp() {
        server.start()
    }

    @AfterEach
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
        assertEquals(
            """connected!connected!
            |disconnected!""".trimMargin(), cl.getRespCache()
        )
        assertFalse(cl.isConnected())
        cl.stop()
    }

    @Test
    fun `should login`() = runBlocking {
        val cl = Client()
        cl.start()
        val uName = UUID.randomUUID().toString()
        cl.write("/login $uName password")
        Thread.sleep(1000)
        cl.stop()

        assertEquals(
            """connected!connected!
            |user $uName logged in!
            |disconnected!""".trimMargin(), cl.getRespCache()
        )
    }
    @Test
    fun `should return syntax error on login`() = runBlocking {
        val cl = Client()
        cl.start()
        val uName = UUID.randomUUID().toString()
        cl.write("/login $uName")
        Thread.sleep(1000)
        cl.stop()

        assertEquals(
            """connected!connected!
            |error: use /login <user_name> <password>
            |disconnected!""".trimMargin(), cl.getRespCache()
        )
    }

    private fun loginAndJoin(cl: Client, userName: String, channelName: String) {
        cl.start()
        cl.write("/login $userName password")
        Thread.sleep(1000)
        cl.write("/join $channelName")
    }

    @Test
    fun `should login and join channel and leave channel`() = runBlocking {
        val cl = Client()
        val uName = UUID.randomUUID().toString()
        val chName = UUID.randomUUID().toString()
        loginAndJoin(cl, uName, chName)
        Thread.sleep(1000)
        cl.write("/leave")
        Thread.sleep(1000)
        cl.stop()

        assertEquals(
            """connected!connected!
            |user $uName logged in!
            |joined $chName channel!
            |exited channel!
            |disconnected!""".trimMargin(), cl.getRespCache()
        )
    }

    @Test
    fun `should join same channel same user multiple times`() = runBlocking {
        val uName = "testu1"
        val chName = "testchname"
        val cl = Client()
        loginAndJoin(cl, uName, chName)
        Thread.sleep(1000)
        cl.stop()
        assertEquals(
            """connected!connected!
            |user $uName logged in!
            |joined $chName channel!
            |disconnected!""".trimMargin(), cl.getRespCache()
        )


        val cl2 = Client()
        cl2.start()
        cl2.write("/login $uName password")
        Thread.sleep(1000)
        cl2.stop()
        assertEquals(
            """connected!connected!
            |welcome again, $uName!
            |joined $chName channel!
            |disconnected!""".trimMargin(), cl2.getRespCache()
        )

        val cl3 = Client()
        cl3.start()
        cl3.write("/login $uName password")
        Thread.sleep(1000)
        cl3.stop()
        assertEquals(
            """connected!connected!
            |welcome again, $uName!
            |joined $chName channel!
            |disconnected!""".trimMargin(), cl3.getRespCache()
        )
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

        clients.forEach { cl ->
            if (cl.getRespCache().contains("joined $channelName channel!")) numberInChannels++
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
        cl2.stop()
        val cl3 = Client()
        loginAndJoin(cl3, "testu3", "testch3")
        cl3.stop()

        Thread.sleep(1000)
        cl.write("/list")
        Thread.sleep(1000)
        cl.stop()

        assertEquals(
            """connected!connected!
            |user testu1 logged in!
            |joined testch1 channel!
            |available channels:
            |testch1
            |testch2
            |testch3
            |disconnected!""".trimMargin(), cl.getRespCache()
        )
    }

    @Test
    fun `should return deduplicated list of users for specific channel`() = runBlocking {
        val chName = "testchname"
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

        Thread.sleep(1000)

        assertEquals(
            """connected!connected!
            |user testu1 logged in!
            |joined testchname channel!
            |available users:
            |testu1
            |testu2
            |testu3
            |""".trimMargin(), cl.getRespCache()
        )

        Thread.sleep(1000)

        cl.stop()
        cl2.stop()
        cl3.stop()
        cl4.stop()
        cl5.stop()
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

