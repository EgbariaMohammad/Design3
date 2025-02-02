package il.ac.technion.cs.sd.buy.app

import dev.misfitlabs.kotlinguice4.KotlinModule
import il.ac.technion.cs.sd.buy.lib.IStorageLibraryFactory
import il.ac.technion.cs.sd.buy.lib.StorageLibraryFactory

class BuyProductModule : KotlinModule() {
    override fun configure() {
        bind<IParser<DataElement>>().to<UnifiedParser>()
        bind<IStorageLibraryFactory>().to<StorageLibraryFactory>()

        bind<BuyProductInitializer>().to<BuyProductImpl>().asEagerSingleton()
        bind<BuyProductReader>().to<BuyProductImpl>().asEagerSingleton()
    }
}
