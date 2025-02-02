package il.ac.technion.cs.sd.buy.app

import com.gitlab.mvysny.konsumexml.konsumeXml
import com.gitlab.mvysny.konsumexml.Konsumer
import com.gitlab.mvysny.konsumexml.Names
import com.google.inject.Inject
import kotlinx.serialization.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

class KonsumeXmlParser{
    fun parse(input: String): List<DataElement> {
        val elements = mutableListOf<DataElement>()
        input.konsumeXml().child("Root")
        {
            this.children(Names.of("Product","ModifyOrder","CancelOrder", "Order"))
            {
                when (this.name.toString()) {
                    "Product" -> elements.add(parseProduct(this))
                    "Order" -> elements.add(parseOrder(this))
                    "ModifyOrder" -> elements.add(parseModifyOrder(this))
                    "CancelOrder" -> elements.add(parseCancelOrder(this))
                    else -> elements.isEmpty()
                }
            }
        }
        return elements
    }

    private fun parseProduct(node: Konsumer): DataElement.Product {
        val id = node.childText("id")
        val price = node.childText("price").toInt()
        return DataElement.Product(id = id, price = price)
    }

    private fun parseOrder(node: Konsumer): DataElement.Order {
        val userId = node.childText("user-id")
        val orderId = node.childText("order-id")
        val productId = node.childText("product-id")
        val amount = node.childText("amount").toInt()
        return DataElement.Order(
            orderId = orderId,
            userId = userId,
            productId = productId,
            amount = amount
        )
    }

    private fun parseModifyOrder(node: Konsumer): DataElement.ModifyOrder {
        val orderId = node.childText("order-id")
        val amount = node.childText("new-amount").toInt()
        return DataElement.ModifyOrder(
            orderId = orderId,
            amount = amount
        )
    }

    private fun parseCancelOrder(node: Konsumer): DataElement.CancelOrder {
        val orderId = node.childText("order-id")
        return DataElement.CancelOrder(orderId = orderId)
    }
}

@Serializable
sealed class DataElement {
    @Serializable
    data class Product(
        @SerialName("type")
        val type: String = "product",
        val id: String,
        val price: Int
    ) : DataElement()

    @Serializable
    data class Order(
        @SerialName("type")
        val type: String = "order",
        @SerialName("order-id")
        val orderId: String,
        @SerialName("user-id")
        val userId: String,
        @SerialName("product-id")
        val productId: String,
        val amount: Int
    ) : DataElement()

    @Serializable
    data class ModifyOrder(
        @SerialName("type")
        val type: String = "modify-order",
        @SerialName("order-id")
        val orderId: String,
        val amount: Int
    ) : DataElement()

    @Serializable
    data class CancelOrder(
        @SerialName("type")
        val type: String = "cancel-order",
        @SerialName("order-id")
        val orderId: String
    ) : DataElement()
}

class JsonParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(input: String): List<DataElement> {
        val jsonData = Json.parseToJsonElement(input).jsonArray
        return jsonData.map { element ->
            val jsonObject = element.jsonObject
            when (jsonObject["type"]?.jsonPrimitive?.content) {
                "product" -> DataElement.Product(
                    id = jsonObject["id"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing id"),
                    price = jsonObject["price"]?.jsonPrimitive?.int ?: throw IllegalArgumentException("Missing or invalid price")
                )
                "order" -> DataElement.Order(
                    orderId = jsonObject["order-id"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing order-id"),
                    userId = jsonObject["user-id"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing user-id"),
                    productId = jsonObject["product-id"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing product-id"),
                    amount = jsonObject["amount"]?.jsonPrimitive?.int ?: throw IllegalArgumentException("Missing or invalid amount")
                )
                "modify-order" -> DataElement.ModifyOrder(
                    orderId = jsonObject["order-id"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing order-id"),
                    amount = jsonObject["amount"]?.jsonPrimitive?.int ?: throw IllegalArgumentException("Missing or invalid amount")
                )
                "cancel-order" -> DataElement.CancelOrder(
                    orderId = jsonObject["order-id"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing order-id")
                )
                else -> throw IllegalArgumentException("Unknown type: ${jsonObject["type"]}")
            }
        }
    }
}

class UnifiedParser @Inject constructor() : IParser<DataElement>  {
    private val xmlParser = KonsumeXmlParser()
    private val jsonParser = JsonParser()

    override fun parse(input: String): List<DataElement> {
        val trimmed = input.trim()
        return when {
            trimmed.startsWith("<?xml") || trimmed.startsWith("<Root>") -> xmlParser.parse(input)
            trimmed.startsWith("[") && trimmed.endsWith("]") -> jsonParser.parse(input)
            else -> throw IllegalArgumentException("Input format not recognized")
        }
    }
}

data class StorageFormat(
    val orderRecords: List<String>,
    val productRecords: List<String>,
    val userRecords: List<String>
)

fun List<DataElement>.toStorageFormat(): StorageFormat {
    val orderHistory = mutableMapOf<String, MutableList<Int>>()  // orderId -> list of amounts
    val orderDetails = mutableMapOf<String, Pair<String, String>>()  // orderId -> (userId, productId)
    val productPrices = mutableMapOf<String, Int>()  // productId -> price

    forEach { element ->
        if (element is DataElement.Product) {
            productPrices[element.id] = element.price
        }
    }

    // Then process orders, only keeping those with valid products
    forEach { element ->
        when (element) {
            is DataElement.Order -> {
                if (productPrices.containsKey(element.productId)) {  // Only process if product exists
                    orderHistory[element.orderId] = mutableListOf(element.amount)
                    orderDetails[element.orderId] = Pair(element.userId, element.productId)
                } else
                {
                    orderHistory.remove(element.orderId)
                    orderDetails.remove(element.orderId)
                }
            }
            is DataElement.ModifyOrder -> {
                // Only modify if the order exists (which means it had a valid product)
                orderHistory[element.orderId]?.add(element.amount)
            }
            is DataElement.CancelOrder -> {
                // Only cancel if the order exists (which means it had a valid product)
                orderHistory[element.orderId]?.add(-1)
            }
            is DataElement.Product -> { /* Already handled */ }
        }
    }

    // Generate order records
    val orderRecords = orderDetails.map { (orderId, details) ->
        val (userId, productId) = details
        val amounts = orderHistory[orderId] ?: listOf()
        "$orderId,$userId,$productId,${amounts.joinToString(",")}"
    }

    // Updated product records to include price
    val productRecords = orderDetails
        .entries
        .groupBy { it.value.second } // group by productId
        .map { (productId, orders) ->
            buildString {
                append(productId)
                append(",${productPrices[productId]}")  // Add price after productId
                orders.mapNotNull { (orderId, _) ->
                    orderHistory[orderId]?.let { amounts ->
                        ",$orderId,${amounts.last()}"
                    }
                }.forEach { append(it) }
            }
        }

    // Generate user records (grouping by userId)
    val userRecords = orderDetails
        .entries
        .groupBy { it.value.first } // group by userId
        .map { (userId, orders) ->
            buildString {
                append(userId)
                orders.mapNotNull { (orderId, _) ->
                    orderHistory[orderId]?.let { amounts ->
                        ",$orderId,${amounts.last()}"
                    }
                }.forEach { append(it) }
            }
        }

    return StorageFormat(orderRecords, productRecords, userRecords)
}