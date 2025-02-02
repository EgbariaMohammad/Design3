package il.ac.technion.cs.sd.buy.app

import com.google.inject.Inject
import com.google.inject.Singleton
import il.ac.technion.cs.sd.buy.lib.IStorageLibrary
import il.ac.technion.cs.sd.buy.lib.IStorageLibraryFactory
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking

@Singleton
class BuyProductImpl @Inject constructor(
    private val storageLibraryFactory: IStorageLibraryFactory,
    private val unifiedParser: UnifiedParser
) : BuyProductInitializer, BuyProductReader {

    private lateinit var orderStorage: IStorageLibrary
    private lateinit var productStorage: IStorageLibrary
    private lateinit var userStorage: IStorageLibrary

    override suspend fun setupXml(xmlData: String) {
        setupFromParsedData(unifiedParser.parse(xmlData))
    }

    override suspend fun setupJson(jsonData: String) {
        setupFromParsedData(unifiedParser.parse(jsonData))
    }

    private suspend fun setupFromParsedData(elements: List<DataElement>) {
        val storageFormat = elements.toStorageFormat()
        orderStorage = storageLibraryFactory.createStorage(storageFormat.orderRecords)
        productStorage = storageLibraryFactory.createStorage(storageFormat.productRecords)
        userStorage = storageLibraryFactory.createStorage(storageFormat.userRecords)

    }

    override suspend fun isValidOrderId(orderId: String): Boolean {
        return orderStorage.getValues(orderId) != null
    }

    override suspend fun isCanceledOrder(orderId: String): Boolean {
        val values = orderStorage.getValues(orderId) ?: return false
        return values.drop(2).last().toInt() == -1
    }

    override suspend fun isModifiedOrder(orderId: String): Boolean {
        val values = orderStorage.getValues(orderId) ?: return false
        val amounts = values.drop(2).map { it.toInt() }
        val nonCancelAmounts = amounts.filter { it != -1 }
        return nonCancelAmounts.size > 1
    }

    override suspend fun getNumberOfProductOrdered(orderId: String): Int? {
        val values = orderStorage.getValues(orderId) ?: return null
        val amounts = values.drop(2).map { it.toInt() }
        val lastNonCancelAmount = amounts.findLast { it != -1 } ?: return null
        return if (amounts.last() == -1) -lastNonCancelAmount else lastNonCancelAmount
    }

    override suspend fun getHistoryOfOrder(orderId: String): List<Int> {
        val values = orderStorage.getValues(orderId) ?: return emptyList()
        val amounts = values.drop(2).map { it.toInt() }

        return if (amounts.last() == -1) {
            amounts.filter { it != -1 } + listOf(-1)
        } else {
            amounts.filter { it != -1 }
        }
    }

    override suspend fun getOrderIdsForUser(userId: String): List<String> {
        val values = userStorage.getValues(userId) ?: return emptyList()
        return values.chunked(2) { it[0] }.sorted()
    }

    override suspend fun getTotalAmountSpentByUser(userId: String): Long {
        val values = userStorage.getValues(userId) ?: return 0
        var total = 0L
        val orders = values.chunked(2)
            orders.forEach { (orderId, amount) ->
                if (amount.toInt() > 0) {  // Don't count canceled orders
                    // Get product price from order details
                    orderStorage.getValues(orderId)?.let { orderValues ->
                        val productId = orderValues[1]
                        productStorage.getValues(productId)?.let { productValues ->
                            val price = productValues[0].toLong()
                            total += price * amount.toLong()
                        }
                    }
            }
        }

        return total
    }

    override suspend fun getUsersThatPurchased(productId: String): List<String> {
        val values = productStorage.getValues(productId) ?: return emptyList()
        val userSet = mutableSetOf<String>()

        values.drop(1).chunked(2).forEach { (orderId, amount) ->
            if (amount.toInt() > 0) {  // Only include non-canceled orders
                orderStorage.getValues(orderId)?.let { orderValues ->
                    userSet.add(orderValues[0])  // Add userId
                }
            } else
            {
                orderStorage.getValues(orderId)?.let { orderValues ->
                    userSet.remove(orderValues[0])  // Add userId
                }
            }
        }
        return userSet.sorted()
    }

    override suspend fun getOrderIdsThatPurchased(productId: String): List<String> {
        val values = productStorage.getValues(productId) ?: return emptyList()
        return values.drop(2).chunked(2) { it[0] }.sorted()
    }

    override suspend fun getTotalNumberOfItemsPurchased(productId: String): Long? {
        val values = productStorage.getValues(productId) ?: return null
        return values.drop(2)
            .chunked(2)
            .sumOf { (_, amount) ->
                val amt = amount.toInt()
                if (amt > 0) amt.toLong() else 0L
            }
    }

    override suspend fun getAverageNumberOfItemsPurchased(productId: String): Double? {
        val values = productStorage.getValues(productId) ?: return null
        val amounts = values.drop(2)
            .chunked(2)
            .map { it[1].toInt() }
            .filter { it > 0 }

        if (amounts.isEmpty()) return null
        return amounts.average()
    }

    override suspend fun getCancelRatioForUser(userId: String): Double? {
        val values = userStorage.getValues(userId) ?: return null
        val orders = values.chunked(2)
        if (orders.isEmpty()) return null

        val totalOrders = orders.size
        val canceledOrders = orders.count { (_, amount) -> amount.toInt() < 0 }

        return canceledOrders.toDouble() / totalOrders
    }

    override suspend fun getModifyRatioForUser(userId: String): Double? {
        val values = userStorage.getValues(userId) ?: return null
        val orders = values.chunked(2).map { it[0] }  // Get orderIds
        if (orders.isEmpty()) return null

        val totalOrders = orders.size
        var modifiedOrders = 0

        orders.forEach { orderId ->
            orderStorage.getValues(orderId)?.let { orderValues ->
                if (orderValues.drop(3).size > 1) {  // Has more than one amount -> modified
                    modifiedOrders++
                }
            }
        }

        return modifiedOrders.toDouble() / totalOrders
    }

    override suspend fun getAllItemsPurchased(userId: String): Map<String, Long> {
        val values = userStorage.getValues(userId) ?: return emptyMap()
        val result = mutableMapOf<String, Long>()

        values.chunked(2).forEach { (orderId, amount) ->
            if (amount.toInt() > 0) {  // Don't count canceled orders
                orderStorage.getValues(orderId)?.let { orderValues ->
                    val productId = orderValues[2]
                    val currentAmount = result.getOrDefault(productId, 0L)
                    result[productId] = currentAmount + amount.toLong()
                }
            }
        }

        return result
    }

    override suspend fun getItemsPurchasedByUsers(productId: String): Map<String, Long> {
        val values = productStorage.getValues(productId) ?: return emptyMap()
        val result = mutableMapOf<String, Long>()

        values.drop(2).chunked(2).forEach { (orderId, amount) ->
            val amountNum = amount.toInt()
            if (amountNum > 0) {  // Don't count canceled orders
                orderStorage.getValues(orderId)?.let { orderValues ->
                    val userId = orderValues[1]
                    val currentAmount = result.getOrDefault(userId, 0L)
                    result[userId] = currentAmount + amountNum
                }
            }
        }

        return result
    }
}