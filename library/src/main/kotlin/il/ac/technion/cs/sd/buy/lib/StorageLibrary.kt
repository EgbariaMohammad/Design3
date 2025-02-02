package il.ac.technion.cs.sd.buy.lib

import com.google.inject.Inject
import il.ac.technion.cs.sd.buy.external.SuspendLineStorage

// In library layer (StorageLibrary.kt)
class StorageLibrary @Inject constructor(private val lineStorage: SuspendLineStorage) : IStorageLibrary {
    private var storage = lineStorage

    /**
     * Initialize storage with records where each record is key,value1,value2,...
     * Records will be sorted by key, and all entries with the same key will be merged into one row
     */
    override suspend fun initializeFromRecords(records: List<String>) {
        // Group by key and merge values for same key
        val mergedRecords = records
            .groupBy { it.substringBefore(",") }
            .map { (key, rows) ->
                val values = rows.flatMap { row ->
                    val afterComma = row.substringAfter(",", "")  // Return empty string if no comma
                    if (afterComma.isEmpty()) listOf() else afterComma.split(",")
                }
                if (values.isEmpty()) key else key + "," + values.joinToString(",")
            }
            .sorted()

        // Write merged and sorted records
        mergedRecords.forEach { record ->
            storage.appendLine(record)
        }
    }

    /**
     * Find the row containing the given key using binary search
     * Returns row index if found, -1 if not found
     */
    private suspend fun binarySearch(key: String): Int {
        var left = 0
        var right = storage.numberOfLines() - 1

        while (left <= right) {
            val mid = (left + right) / 2
            val line = storage.read(mid)
            val comparison = key.compareTo(line.substringBefore(","))

            when {
                comparison == 0 -> return mid
                comparison < 0 -> right = mid - 1
                else -> left = mid + 1
            }
        }
        return -1
    }

    /**
     * Get the list of values associated with a key
     * Returns null if key not found
     */
    override suspend fun getValues(key: String): List<String>? {
        val index = binarySearch(key)
        if (index == -1) return null

        return storage.read(index)
            .substringAfter(",", "")  // Returns empty string if no comma found
            .let { if (it.isEmpty()) emptyList() else it.split(",") }
    }
}