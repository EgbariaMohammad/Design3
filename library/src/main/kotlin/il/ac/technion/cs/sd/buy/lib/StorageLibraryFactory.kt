package il.ac.technion.cs.sd.buy.lib

import com.google.inject.Inject
import il.ac.technion.cs.sd.buy.external.SuspendLineStorageFactory

class StorageLibraryFactory @Inject constructor(
    private val lineStorageFactory: SuspendLineStorageFactory
) : IStorageLibraryFactory {
    private var fileCounter = 0

    override suspend fun createStorage(records: List<String>): IStorageLibrary {
        val fileName = "storage_${fileCounter++}"
        val lineStorage = lineStorageFactory.open(fileName)
        return StorageLibrary(lineStorage).apply {
            initializeFromRecords(records)
        }
    }
}