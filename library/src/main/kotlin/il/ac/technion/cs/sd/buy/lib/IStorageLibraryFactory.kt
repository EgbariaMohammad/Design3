package il.ac.technion.cs.sd.buy.lib

interface IStorageLibraryFactory {
    suspend fun createStorage(records: List<String>): IStorageLibrary
}

