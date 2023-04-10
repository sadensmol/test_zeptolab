import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ApplicationTest {
    private var server: Server = Server()

    @BeforeAll
    fun setUp() {
        server.start()
    }

    @Test
    fun `should disconnect`() = runBlocking{
        val cl = Client()
        cl.start()
        assertTrue(cl.isConnected())
        cl.write("/disconnect")
        Thread.sleep(1000)
        assertFalse(cl.isConnected())
        cl.stop()
    }

    @Test
    fun `should login`() = runBlocking{
        val cl = Client()
        cl.start()
        val userName = UUID.randomUUID().toString()
        assertFalse(server.hasUser(userName))
        cl.write("/login $userName password")
        Thread.sleep(1000)
        assertTrue(server.hasUser(userName))
        cl.stop()
    }

    @AfterAll
    fun tearDown() {
        server.stop()
    }

}

