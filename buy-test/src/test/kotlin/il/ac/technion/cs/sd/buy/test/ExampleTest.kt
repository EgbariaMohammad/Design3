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

    @Test
    fun `test getOrderIdsThatPurchased - order before product definition`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedOrders = listOf("getOrderIdsThatPurchased-early-order").sorted()

        launch(Dispatchers.Default) {
            assertEquals(expectedOrders, reader.getOrderIdsThatPurchased("getOrderIdsThatPurchased-late-product"))
        }
    }

    @Test
    fun `test getOrderIdsThatPurchased - multiple orders from same user`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedOrders = listOf(
            "getOrderIdsThatPurchased-order1",
            "getOrderIdsThatPurchased-order2",
            "getOrderIdsThatPurchased-order3",  // included because cancelled orders count
            "getOrderIdsThatPurchased-order4",   // included because cancelled orders count
            "getOrderIdsThatPurchased-repeat-order1",
            "getOrderIdsThatPurchased-repeat-order2"
        ).sorted()

        launch(Dispatchers.Default) {
            assertEquals(expectedOrders, reader.getOrderIdsThatPurchased("getOrderIdsThatPurchased-normal-product"))
        }
    }

    @Test
    fun `test getOrderIdsThatPurchased - product redefinition`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedOrders = listOf("getOrderIdsThatPurchased-redef-order").sorted()

        launch(Dispatchers.Default) {
            assertEquals(expectedOrders, reader.getOrderIdsThatPurchased("getOrderIdsThatPurchased-redef-product"))
        }
    }

    @Test
    fun `test getOrderIdsThatPurchased - normal product with multiple orders`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedOrders = listOf(
            "getOrderIdsThatPurchased-order1",
            "getOrderIdsThatPurchased-order2",
            "getOrderIdsThatPurchased-order3",  // included because cancelled orders count
            "getOrderIdsThatPurchased-order4",   // included because cancelled orders count
            "getOrderIdsThatPurchased-repeat-order1",
            "getOrderIdsThatPurchased-repeat-order2"
        ).sorted()

        launch(Dispatchers.Default) {
            assertEquals(expectedOrders, reader.getOrderIdsThatPurchased("getOrderIdsThatPurchased-normal-product"))
        }
    }

    @Test
    fun `test getOrderIdsThatPurchased - non-existent product`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedOrders = emptyList<String>()

        launch(Dispatchers.Default) {
            assertEquals(expectedOrders, reader.getOrderIdsThatPurchased("getOrderIdsThatPurchased-non-existent-product"))
        }
    }


    @Test
    fun `test getAverageNumberOfItemsPurchased - normal case with multiple orders`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedAverage = (10.0 + 20.0) / 2  // Average of two orders: 15.0

        launch(Dispatchers.Default) {
            assertEquals(expectedAverage, reader.getAverageNumberOfItemsPurchased("getAverageNumberOfItemsPurchased-normal-product"))
        }
    }

    @Test
    fun `test getAverageNumberOfItemsPurchased - product with only cancelled orders`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()

        launch(Dispatchers.Default) {
            assertNull(reader.getAverageNumberOfItemsPurchased("getAverageNumberOfItemsPurchased-cancelled-only-product"))
        }
    }

    @Test
    fun `test getAverageNumberOfItemsPurchased - mix of cancelled and valid orders`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedAverage = 40.0  // Only the non-cancelled order should count

        launch(Dispatchers.Default) {
            assertEquals(expectedAverage, reader.getAverageNumberOfItemsPurchased("getAverageNumberOfItemsPurchased-mixed-product"))
        }
    }

    @Test
    fun `test getAverageNumberOfItemsPurchased - modified orders`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedAverage = 20.0  // Should use modified amount

        launch(Dispatchers.Default) {
            assertEquals(expectedAverage, reader.getAverageNumberOfItemsPurchased("getAverageNumberOfItemsPurchased-modified-product"))
        }
    }

    @Test
    fun `test getAverageNumberOfItemsPurchased - non-existent product`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()

        launch(Dispatchers.Default) {
            assertNull(reader.getAverageNumberOfItemsPurchased("getAverageNumberOfItemsPurchased-non-existent-product"))
        }
    }

    @Test
    fun `test getAverageNumberOfItemsPurchased - single order`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedAverage = 50.0  // Single order amount

        launch(Dispatchers.Default) {
            assertEquals(expectedAverage, reader.getAverageNumberOfItemsPurchased("getAverageNumberOfItemsPurchased-single-product"))
        }
    }
    @Test
    fun `test getAverageNumberOfItemsPurchased - multiple modifications`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedAverage = (30.0 + 50.0) / 2  // Average of last values: (30 + 50)/2 = 40.0

        launch(Dispatchers.Default) {
            assertEquals(expectedAverage,
                reader.getAverageNumberOfItemsPurchased("getAverageNumberOfItemsPurchased-multi-modified-product"))
        }
    }

    @Test
    fun `test getCancelRatioForUser - normal mix of orders`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedRatio = 0.5  // 1 cancelled out of 2 total

        launch(Dispatchers.Default) {
            assertEquals(expectedRatio, reader.getCancelRatioForUser("getCancelRatioForUser-normal-user"))
        }
    }

    @Test
    fun `test getCancelRatioForUser - all orders cancelled`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedRatio = 1.0  // All orders cancelled

        launch(Dispatchers.Default) {
            assertEquals(expectedRatio, reader.getCancelRatioForUser("getCancelRatioForUser-all-cancelled-user"))
        }
    }

    @Test
    fun `test getCancelRatioForUser - modifications then cancellations`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedRatio = 0.5  // 1 cancelled out of 2 total

        launch(Dispatchers.Default) {
            assertEquals(expectedRatio, reader.getCancelRatioForUser("getCancelRatioForUser-modify-cancel-user"))
        }
    }

    @Test
    fun `test getCancelRatioForUser - multiple cancel attempts`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedRatio = 1.0  // Order is cancelled regardless of multiple attempts

        launch(Dispatchers.Default) {
            assertEquals(expectedRatio, reader.getCancelRatioForUser("getCancelRatioForUser-multiple-cancels-user"))
        }
    }

    @Test
    fun `test getCancelRatioForUser - cancel then modify`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedRatio = 0.0  // Order should still be considered modified

        launch(Dispatchers.Default) {
            assertEquals(expectedRatio, reader.getCancelRatioForUser("getCancelRatioForUser-cancel-modify-user"))
        }
    }

    @Test
    fun `test getCancelRatioForUser - invalid product orders`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedRatio = 1.0  // Only valid order is cancelled

        launch(Dispatchers.Default) {
            assertEquals(expectedRatio, reader.getCancelRatioForUser("getCancelRatioForUser-invalid-product-user"))
        }
    }

    @Test
    fun `test getCancelRatioForUser - single non-cancelled order`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedRatio = 0.0  // No cancelled orders

        launch(Dispatchers.Default) {
            assertEquals(expectedRatio, reader.getCancelRatioForUser("getCancelRatioForUser-single-order-user"))
        }
    }

    @Test
    fun `test getCancelRatioForUser - non-existent user`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()

        launch(Dispatchers.Default) {
            assertNull(reader.getCancelRatioForUser("getCancelRatioForUser-non-existent-user"))
        }
    }

    @Test
    fun `test getModifyRatioForUser - normal mix of orders`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedRatio = 0.5  // 1 modified out of 2 total

        launch(Dispatchers.Default) {
            assertEquals(expectedRatio, reader.getModifyRatioForUser("getModifyRatioForUser-normal-user"))
        }
    }

    @Test
    fun `test getModifyRatioForUser - all orders modified`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedRatio = 1.0  // All orders modified

        launch(Dispatchers.Default) {
            assertEquals(expectedRatio, reader.getModifyRatioForUser("getModifyRatioForUser-all-modified-user"))
        }
    }

    @Test
    fun `test getModifyRatioForUser - modified then cancelled`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedRatio = 0.5  // 1 modified (and cancelled) out of 2 total

        launch(Dispatchers.Default) {
            assertEquals(expectedRatio, reader.getModifyRatioForUser("getModifyRatioForUser-modify-cancel-user"))
        }
    }

    @Test
    fun `test getModifyRatioForUser - multiple modifications`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedRatio = 1.0  // Order is modified regardless of number of modifications

        launch(Dispatchers.Default) {
            assertEquals(expectedRatio, reader.getModifyRatioForUser("getModifyRatioForUser-multiple-mods-user"))
        }
    }

    @Test
    fun `test getModifyRatioForUser - cancelled then modified`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedRatio = 1.0  // Cancelled order, modifications after cancel do count

        launch(Dispatchers.Default) {
            assertEquals(expectedRatio, reader.getModifyRatioForUser("getModifyRatioForUser-cancel-modify-user"))
        }
    }

    @Test
    fun `test getModifyRatioForUser - invalid product orders`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedRatio = 1.0  // Only valid order is modified

        launch(Dispatchers.Default) {
            assertEquals(expectedRatio, reader.getModifyRatioForUser("getModifyRatioForUser-invalid-product-user"))
        }
    }

    @Test
    fun `test getModifyRatioForUser - single unmodified order`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expectedRatio = 0.0  // No modified orders

        launch(Dispatchers.Default) {
            assertEquals(expectedRatio, reader.getModifyRatioForUser("getModifyRatioForUser-single-order-user"))
        }
    }

    @Test
    fun `test getModifyRatioForUser - non-existent user`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()

        launch(Dispatchers.Default) {
            assertNull(reader.getModifyRatioForUser("getModifyRatioForUser-non-existent-user"))
        }
    }

    @Test
    fun `test getAllItemsPurchased - normal user with multiple products`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expected = mapOf(
            "getAllItemsPurchased-productA" to 10L,
            "getAllItemsPurchased-productB" to 20L
        )

        launch(Dispatchers.Default) {
            assertEquals(expected, reader.getAllItemsPurchased("getAllItemsPurchased-normal-user"))
        }
    }

    @Test
    fun `test getAllItemsPurchased - multiple orders of same product`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expected = mapOf(
            "getAllItemsPurchased-productA" to 30L  // 10 + 20
        )

        launch(Dispatchers.Default) {
            assertEquals(expected, reader.getAllItemsPurchased("getAllItemsPurchased-repeat-product-user"))
        }
    }

    @Test
    fun `test getAllItemsPurchased - modified orders`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expected = mapOf(
            "getAllItemsPurchased-productA" to 15L,  // Modified from 10 to 15
            "getAllItemsPurchased-productB" to 25L   // Modified from 20 to 25
        )

        launch(Dispatchers.Default) {
            assertEquals(expected, reader.getAllItemsPurchased("getAllItemsPurchased-modified-user"))
        }
    }

    @Test
    fun `test getAllItemsPurchased - cancelled orders`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expected = mapOf(
            "getAllItemsPurchased-productB" to 20L  // Only non-cancelled order
        )

        launch(Dispatchers.Default) {
            assertEquals(expected, reader.getAllItemsPurchased("getAllItemsPurchased-cancelled-user"))
        }
    }

    @Test
    fun `test getAllItemsPurchased - modified then cancelled`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expected = emptyMap<String, Long>()  // All orders cancelled

        launch(Dispatchers.Default) {
            assertEquals(expected, reader.getAllItemsPurchased("getAllItemsPurchased-mod-cancel-user"))
        }
    }

    @Test
    fun `test getAllItemsPurchased - invalid product orders`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expected = mapOf(
            "getAllItemsPurchased-productA" to 20L  // Only valid product order
        )

        launch(Dispatchers.Default) {
            assertEquals(expected, reader.getAllItemsPurchased("getAllItemsPurchased-invalid-product-user"))
        }
    }

    @Test
    fun `test getAllItemsPurchased - same product mix cancelled and valid`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expected = mapOf(
            "getAllItemsPurchased-productA" to 20L  // Only non-cancelled order
        )

        launch(Dispatchers.Default) {
            assertEquals(expected, reader.getAllItemsPurchased("getAllItemsPurchased-mixed-user"))
        }
    }

    @Test
    fun `test getAllItemsPurchased - non-existent user`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expected = emptyMap<String, Long>()

        launch(Dispatchers.Default) {
            assertEquals(expected, reader.getAllItemsPurchased("getAllItemsPurchased-non-existent-user"))
        }
    }

    @Test
    fun `test getItemsPurchasedByUsers - multiple users normal orders`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expected = mapOf(
            "getItemsPurchasedByUsers-user1" to 10L,
            "getItemsPurchasedByUsers-user2" to 20L
        )

        launch(Dispatchers.Default) {
            assertEquals(expected,
                reader.getItemsPurchasedByUsers("getItemsPurchasedByUsers-multiple-users-normal-orders-test-product"))
        }
    }

    @Test
    fun `test getItemsPurchasedByUsers - same user multiple orders`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expected = mapOf(
            "getItemsPurchasedByUsers-repeat-user" to 30L  // 10 + 20
        )

        launch(Dispatchers.Default) {
            assertEquals(expected,
                reader.getItemsPurchasedByUsers("getItemsPurchasedByUsers-same-user-multiple-orders-test-product"))
        }
    }

    @Test
    fun `test getItemsPurchasedByUsers - modified orders`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expected = mapOf(
            "getItemsPurchasedByUsers-modified-user" to 15L  // Modified from 10 to 15
        )

        launch(Dispatchers.Default) {
            assertEquals(expected,
                reader.getItemsPurchasedByUsers("getItemsPurchasedByUsers-modified-orders-test-product"))
        }
    }

    @Test
    fun `test getItemsPurchasedByUsers - cancelled orders`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expected = emptyMap<String, Long>()  // Only cancelled order

        launch(Dispatchers.Default) {
            assertEquals(expected,
                reader.getItemsPurchasedByUsers("getItemsPurchasedByUsers-cancelled-orders-test-product"))
        }
    }

    @Test
    fun `test getItemsPurchasedByUsers - mixed cancelled and valid`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expected = mapOf(
            "getItemsPurchasedByUsers-mixed-user" to 20L  // Only count non-cancelled order
        )

        launch(Dispatchers.Default) {
            assertEquals(expected,
                reader.getItemsPurchasedByUsers("getItemsPurchasedByUsers-mixed-orders-test-product"))
        }
    }

    @Test
    fun `test getItemsPurchasedByUsers - modified then cancelled`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expected = emptyMap<String, Long>()  // Modified but then cancelled

        launch(Dispatchers.Default) {
            assertEquals(expected,
                reader.getItemsPurchasedByUsers("getItemsPurchasedByUsers-modified-then-cancelled-test-product"))
        }
    }

    @Test
    fun `test getItemsPurchasedByUsers - different product orders`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expected = emptyMap<String, Long>()  // Order for different product

        launch(Dispatchers.Default) {
            assertEquals(expected,
                reader.getItemsPurchasedByUsers("getItemsPurchasedByUsers-different-product-test-product"))
        }
    }

    @Test
    fun `test getItemsPurchasedByUsers - non-existent product`() = runTest(timeout = 30.seconds) {
        val injector = setupAndGetInjector("small.xml")
        val reader = injector.getInstance<BuyProductReader>()
        val expected = emptyMap<String, Long>()

        launch(Dispatchers.Default) {
            assertEquals(expected,
                reader.getItemsPurchasedByUsers("getItemsPurchasedByUsers-non-existent-test-product"))
        }
    }
}