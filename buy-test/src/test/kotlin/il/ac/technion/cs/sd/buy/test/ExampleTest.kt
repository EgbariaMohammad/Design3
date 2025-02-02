package il.ac.technion.cs.sd.buy.test


import kotlinx.coroutines.test.*
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Test

import java.io.FileNotFoundException

import com.google.inject.Guice
import com.google.inject.Injector
import dev.misfitlabs.kotlinguice4.getInstance


import il.ac.technion.cs.sd.buy.app.BuyProductReader
import il.ac.technion.cs.sd.buy.app.BuyProductInitializer
import il.ac.technion.cs.sd.buy.app.BuyProductModule
import il.ac.technion.cs.sd.buy.external.LineStorageModule
import org.junit.jupiter.api.Assertions.*

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

    @Test
    fun `test isValidOrderId - non-existing-product order`() = runTest(timeout = 30.seconds)
    {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()

        launch(Dispatchers.Default) {
            assertFalse(reader.isValidOrderId("non-existing-product-order"))
            assertFalse(reader.isModifiedOrder("non-existing-product-order"))
            assertFalse(reader.isCanceledOrder("non-existing-product-order"))
        }
    }

    @Test
    fun `test isValidOrderId - modified then cancelled non-existing-product order should be false for all`() = runTest(timeout = 30.seconds)
    {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()

        launch(Dispatchers.Default) {
            assertFalse(reader.isValidOrderId("modified-then-cancelled-non-existing-order"))
            assertFalse(reader.isModifiedOrder("modified-then-cancelled-non-existing-order"))
            assertFalse(reader.isCanceledOrder("modified-then-cancelled-non-existing-order"))
        }
    }

    @Test
    fun `test isValidOrderId - cancelled then modified non-existing-product order should be false for all`() = runTest(timeout = 30.seconds)
    {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()

        launch(Dispatchers.Default) {
            assertFalse(reader.isValidOrderId("cancelled-then-modified-non-existing-order"))
            assertFalse(reader.isModifiedOrder("cancelled-then-modified-non-existing-order"))
            assertFalse(reader.isCanceledOrder("cancelled-then-modified-non-existing-order"))
        }
    }

    @Test
    fun `test isValidOrderId - normal cancelled order should be valid and not modified`() = runTest(timeout = 30.seconds)
    {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()

        launch(Dispatchers.Default) {
            assertTrue(reader.isValidOrderId("cancelled-order"))
            assertFalse(reader.isModifiedOrder("cancelled-order"))
            assertTrue(reader.isCanceledOrder("cancelled-order"))
        }
    }

    @Test
    fun `test isValidOrderId - modified-then-cancelled-order should be true for all`() = runTest(timeout = 30.seconds)
    {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()

        launch(Dispatchers.Default) {
            assertTrue(reader.isValidOrderId("modified-then-cancelled-order"))
            assertTrue(reader.isModifiedOrder("modified-then-cancelled-order"))
            assertTrue(reader.isCanceledOrder("modified-then-cancelled-order"))
        }
    }

    @Test
    fun `test isValidOrderId - invalid-override-valid-order should be false for all`() = runTest(timeout = 30.seconds)
    {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()

        launch(Dispatchers.Default) {
            assertFalse(reader.isValidOrderId("invalid-override-valid-order"))
            assertFalse(reader.isModifiedOrder("invalid-override-valid-order"))
            assertFalse(reader.isCanceledOrder("invalid-override-valid-order"))
        }
    }

    @Test
    fun `test isValidOrderId - valid-override-invalid-order should be true for valid`() = runTest(timeout = 30.seconds)
    {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()

        launch(Dispatchers.Default) {
            assertTrue(reader.isValidOrderId("valid-override-invalid-order"))
            assertFalse(reader.isModifiedOrder("valid-override-invalid-order"))
            assertFalse(reader.isCanceledOrder("valid-override-invalid-order"))
        }
    }
    @Test
    fun `test getNumberOfProductOrdered - normal-number-of-products`() = runTest(timeout = 30.seconds)
    {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()

        launch(Dispatchers.Default) {
            assertEquals(reader.getNumberOfProductOrdered("normal-number-of-products"),5000)
        }
    }

    @Test
    fun `test getNumberOfProductOrdered - modified-number-of-products`() = runTest(timeout = 30.seconds)
    {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()

        launch(Dispatchers.Default) {
            assertEquals(reader.getNumberOfProductOrdered("modified-number-of-products"),10)
        }
    }
    @Test
    fun `test getNumberOfProductOrdered - cancelled-number-of-products`() = runTest(timeout = 30.seconds)
    {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()

        launch(Dispatchers.Default) {
            assertEquals(reader.getNumberOfProductOrdered("cancelled-number-of-products"),-5000)
        }
    }
    @Test
    fun `test getNumberOfProductOrdered - modified-then-cancelled-number-of-products`() = runTest(timeout = 30.seconds)
    {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()

        launch(Dispatchers.Default) {
            assertEquals(reader.getNumberOfProductOrdered("modified-then-cancelled-number-of-products"),-10)
        }
    }
    @Test
    fun `test getNumberOfProductOrdered - override-should-null-number-of-products`() = runTest(timeout = 30.seconds)
    {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()

        launch(Dispatchers.Default) {
            assertNull(reader.getNumberOfProductOrdered("override-should-null-number-of-products"))
        }
    }
    @Test
    fun `test getHistoryOfOrder - normal-history-order`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedHistory = listOf(5000, 5001, 5002, 5003)

        launch(Dispatchers.Default) {
            assertEquals(expectedHistory, reader.getHistoryOfOrder("normal-history-order"))
        }
    }
    @Test
    fun `test getHistoryOfOrder - cancelled-history-order`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedHistory = listOf(5000, 5002, -1)

        launch(Dispatchers.Default) {
            assertEquals(expectedHistory, reader.getHistoryOfOrder("cancelled-history-order"))
        }
    }
    @Test
    fun `test getHistoryOfOrder - cancelled-modify-alternate-history-order`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedHistory = listOf(5000, 5002, 5002, 5002, -1)

        launch(Dispatchers.Default) {
            assertEquals(expectedHistory, reader.getHistoryOfOrder("cancelled-modify-alternate-history-order"))
        }
    }
    @Test
    fun `test getHistoryOfOrder - cancelled-cancelled-history-order`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedHistory = listOf(5000, 5002, -1)

        launch(Dispatchers.Default) {
            assertEquals(expectedHistory, reader.getHistoryOfOrder("cancelled-cancelled-history-order"))
        }
    }
    @Test
    fun `test getHistoryOfOrder - invalid-history-order`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedHistory = emptyList<Int>()

        launch(Dispatchers.Default) {
            assertEquals(expectedHistory, reader.getHistoryOfOrder("invalid-history-order"))
        }
    }

    @Test
    fun `test getOrderIdsForUser - normal user with multiple orders`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedOrders = listOf("order1", "order2", "order3")

        launch(Dispatchers.Default) {
            assertEquals(expectedOrders, reader.getOrderIdsForUser("normal-user"))
        }
    }

    @Test
    fun `test getOrderIdsForUser - user with cancelled orders`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedOrders = listOf("order4", "order5")  // Should include cancelled orders

        launch(Dispatchers.Default) {
            assertEquals(expectedOrders, reader.getOrderIdsForUser("cancelled-orders-user"))
        }
    }

    @Test
    fun `test getOrderIdsForUser - user with some invalid product orders`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedOrders = listOf("order7")  // Should only include order with valid product

        launch(Dispatchers.Default) {
            assertEquals(expectedOrders, reader.getOrderIdsForUser("invalid-products-user"))
        }
    }

    @Test
    fun `test getOrderIdsForUser - non-existent user`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedOrders = emptyList<String>()

        launch(Dispatchers.Default) {
            assertEquals(expectedOrders, reader.getOrderIdsForUser("non-existent-user"))
        }
    }

    @Test
    fun `test getOrderIdsForUser - user with no orders`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedOrders = emptyList<String>()

        launch(Dispatchers.Default) {
            assertEquals(expectedOrders, reader.getOrderIdsForUser("no-orders-user"))
        }
    }

    @Test
    fun `test getTotalAmountSpentByUser - normal user with multiple purchases`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedTotal = (1000L * 2) + (10L * 5)  // 2 expensive products + 5 cheap products

        launch(Dispatchers.Default) {
            assertEquals(expectedTotal, reader.getTotalAmountSpentByUser("getTotalAmountSpentByUser-spending-user"))
        }
    }

    @Test
    fun `test getTotalAmountSpentByUser - user with cancelled orders`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedTotal = (10L * 5)  // Only the non-cancelled order should count

        launch(Dispatchers.Default) {
            assertEquals(expectedTotal, reader.getTotalAmountSpentByUser("getTotalAmountSpentByUser-cancelled-spending-user"))
        }
    }

    @Test
    fun `test getTotalAmountSpentByUser - user with modified orders`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedTotal = 1000L * 3  // Should use the modified amount (3) not original (2)

        launch(Dispatchers.Default) {
            assertEquals(expectedTotal, reader.getTotalAmountSpentByUser("getTotalAmountSpentByUser-modified-spending-user"))
        }
    }

    @Test
    fun `test getTotalAmountSpentByUser - user with invalid product orders`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedTotal = 0L  // Orders with invalid products should be ignored

        launch(Dispatchers.Default) {
            assertEquals(expectedTotal, reader.getTotalAmountSpentByUser("getTotalAmountSpentByUser-invalid-product-spending-user"))
        }
    }

    @Test
    fun `test getTotalAmountSpentByUser - non-existent user`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedTotal = 0L

        launch(Dispatchers.Default) {
            assertEquals(expectedTotal, reader.getTotalAmountSpentByUser("getTotalAmountSpentByUser-non-existent-user"))
        }
    }

    @Test
    fun `test getUsersThatPurchased - normal product with multiple users`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedUsers = listOf(
            "getUsersThatPurchased-user1",
            "getUsersThatPurchased-user2",
            "getUsersThatPurchased-mixed-user"  // included because has valid order
        ).sorted()

        launch(Dispatchers.Default) {
            assertEquals(expectedUsers, reader.getUsersThatPurchased("getUsersThatPurchased-normal-product"))
        }
    }

    @Test
    fun `test getUsersThatPurchased - user with only cancelled order should not appear`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()

        launch(Dispatchers.Default) {
            assertFalse(reader.getUsersThatPurchased("getUsersThatPurchased-normal-product")
                .contains("getUsersThatPurchased-cancelled-user"))
        }
    }

    @Test
    fun `test getUsersThatPurchased - non-existent product`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedUsers = emptyList<String>()

        launch(Dispatchers.Default) {
            assertEquals(expectedUsers, reader.getUsersThatPurchased("getUsersThatPurchased-non-existent-product"))
        }
    }
}