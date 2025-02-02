package il.ac.technion.cs.sd.buy.external
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext


class FakeLineStorage : SuspendLineStorage {
    private val linesList = mutableListOf<String>()

    override suspend fun appendLine(line: String) {
        linesList.add(line)
    }

    /** Returns the line at index lineNumber (0-indexed) */
    override suspend fun read(lineNumber: Int): String {
        if (lineNumber < linesList.size && lineNumber >= 0)
        {
            withContext(Dispatchers.Default)
            {
                delay(linesList[lineNumber].length.toLong())
            }
            return linesList[lineNumber]
        } else
        {
            return ""
        }
    }

    /** Returns the total number of lines in the file */
    override suspend fun numberOfLines(): Int {
        withContext(Dispatchers.Default)
        {
            delay(100)
        }
        return linesList.size
    }
}