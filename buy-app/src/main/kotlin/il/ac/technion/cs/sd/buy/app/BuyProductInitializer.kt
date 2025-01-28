package il.ac.technion.cs.sd.buy.app

interface BuyProductInitializer {
    /** Saves the XML data persistently, so that it could be queried using BuyProductReader */
    suspend fun setupXml(xmlData: String)
    /** Saves the JSON data persistently, so that it could be queried using BuyProductReader */
    suspend fun setupJson(jsonData: String)
}