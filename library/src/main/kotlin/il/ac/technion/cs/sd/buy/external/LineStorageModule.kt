package il.ac.technion.cs.sd.buy.external

import dev.misfitlabs.kotlinguice4.KotlinModule

public final class LineStorageModule public constructor() : KotlinModule() {
    override fun configure(): kotlin.Unit {
        bind<SuspendLineStorageFactory>().to<FakeLineStorageFactory>()
    }
}