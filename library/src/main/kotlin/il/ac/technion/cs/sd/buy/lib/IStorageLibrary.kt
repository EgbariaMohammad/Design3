package il.ac.technion.cs.sd.buy.lib

interface IStorageLibrary {
    suspend fun initializeFromRecords(records: List<String>)
    suspend fun getValues(key: String): List<String>?
}