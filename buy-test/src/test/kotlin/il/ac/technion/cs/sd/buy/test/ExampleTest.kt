package il.ac.technion.cs.sd.buy.test


import kotlinx.coroutines.test.*
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

import java.io.FileNotFoundException

import com.google.inject.Guice
import com.google.inject.Injector
import dev.misfitlabs.kotlinguice4.getInstance


import il.ac.technion.cs.sd.buy.app.BuyProductReader
import il.ac.technion.cs.sd.buy.app.BuyProductInitializer
import il.ac.technion.cs.sd.buy.app.BuyProductModule
import il.ac.technion.cs.sd.buy.external.LineStorageModule

class ExampleTest {

    private suspend fun setupAndGetInjector(fileName: String): Injector {
        val fileContents: String =
            javaClass.getResource(fileName)?.readText() ?:
            throw FileNotFoundException("Could not open file $fileName")

        val injector = Guice.createInjector(BuyProductModule(), LineStorageModule())
        val buyProductInitializer = injector.getInstance<BuyProductInitializer>()
        if (fileName.endsWith("xml"))
            buyProductInitializer.setupXml(fileContents)
        else {
            assert(fileName.endsWith("json"))
            buyProductInitializer.setupJson(fileContents)
        }
        return injector
    }

    @Test
    fun `test small xml`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()

        launch(Dispatchers.Default) {
            assertEquals(listOf(5, 10, -1), reader.getHistoryOfOrder("1"))
        }
    }

    @Test
    fun `test small json`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.json")
        val reader = injector.getInstance<BuyProductReader>()

        launch(Dispatchers.Default) {
            assertEquals(2 * 10000 + 5 * 100 + 100 * 1,
                reader.getTotalAmountSpentByUser("1"))
        }
    }

    @Test
    fun `test small json 2`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small_2.json")
        val reader = injector.getInstance<BuyProductReader>()

        launch(Dispatchers.Default) {
            assertTrue(reader.isValidOrderId("foo1234"))
            assertTrue(reader.isModifiedOrder("foo1234"))
            assertTrue(reader.isCanceledOrder("foo1234"))
        }
    }
}