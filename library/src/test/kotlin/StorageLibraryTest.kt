package il.ac.technion.cs.sd.buy.test

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions

import il.ac.technion.cs.sd.buy.external.FakeLineStorageFactory
import il.ac.technion.cs.sd.buy.lib.IStorageLibrary
import il.ac.technion.cs.sd.buy.lib.StorageLibraryFactory

public class StorageLibraryTest {

    private fun getStorageLibrary(records: List<String>): IStorageLibrary {
        return StorageLibraryFactory(FakeLineStorageFactory()).createStorage(records)
    }

    @Test
    fun `read non existing line`() {
        val storageLibrary = getStorageLibrary(listOf("baraa,book1,8,book2,10"))
        val line = storageLibrary.getValues("samer")
        Assertions.assertNull(line)
    }

    @Test
    fun `read only existing line`() {
        val storageLibrary = getStorageLibrary(listOf("baraa,book1,8,book2,10"))
        val line = storageLibrary.getValues("baraa")
        Assertions.assertEquals(listOf("book1","8","book2","10"), line)
    }

    @Test
    fun `read number of lines`() {
        val list = mutableListOf("baraa,book1,8,book2,10")
        list.add("ayman,book1,2,book2,5")
        val storageLibrary = getStorageLibrary(list)

        val lines = storageLibrary.getValues("baraa")
        Assertions.assertEquals(listOf("book1","8","book2","10"), lines)
    }

}