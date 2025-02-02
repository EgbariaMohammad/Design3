package il.ac.technion.cs.sd.buy.external

class FakeLineStorageFactory : SuspendLineStorageFactory {
    private val openedFiles = mutableSetOf<String>() // Track opened files
    private val storageMap = mutableMapOf<String, SuspendLineStorage>()

    override suspend fun open(fileName: String): SuspendLineStorage {
        // Check if file was already opened
        if (fileName in openedFiles) {
            throw IllegalStateException("File $fileName was already opened")
        }

        // Create new fake storage, add to map and track as opened
        return FakeLineStorage().also {
            storageMap[fileName] = it
            openedFiles.add(fileName)
        }
    }

    // For testing purposes
    fun getStorage(fileName: String): SuspendLineStorage? = storageMap[fileName]
}