package com.spooky2.huntkill.di

import android.content.Context
import android.hardware.usb.UsbManager
import com.spooky2.huntkill.data.DemoDumpLoader
import com.spooky2.huntkill.data.DemoTransportFactory
import com.spooky2.huntkill.data.GeneratorSessionFactory
import com.spooky2.huntkill.data.TransportFactory
import com.spooky2.huntkill.log.LogBus
import com.spooky2.huntkill.log.LoggingSerialTransport
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the DEFAULT no-hardware demo wiring: the bundled dump is parsed once, a
 * [DemoTransportFactory] yields a fresh [FakeTransport][com.spooky2.huntkill.transport.fake.FakeTransport]
 * per connection, and a [GeneratorSessionFactory] builds the
 * [GeneratorClient][com.spooky2.huntkill.transport.GeneratorClient] +
 * [ScanEngine][com.spooky2.huntkill.core.scan.ScanEngine] on top.
 *
 * Structured so a real USB path can swap in later: bind a `UsbTransportFactory`
 * under the [Usb] qualifier and inject the [GeneratorSessionFactory] with that
 * factory + `connectViaProbe = true`. The app defaults to [Demo].
 */
@Module
@InstallIn(SingletonComponent::class)
object DemoModule {

    @Provides
    @Singleton
    fun provideDemoData(@ApplicationContext context: Context): DemoDumpLoader.DemoData =
        DemoDumpLoader.loadDemoData(context)

    @Provides
    @Singleton
    fun provideUsbManager(@ApplicationContext context: Context): UsbManager =
        context.getSystemService(Context.USB_SERVICE) as UsbManager

    @Provides
    @Singleton
    @Demo
    fun provideDemoTransportFactory(
        demoData: DemoDumpLoader.DemoData,
        logBus: LogBus,
    ): TransportFactory {
        val inner = DemoTransportFactory(demoData)
        // Wrap each produced transport with logging so demo TX/RX is captured too.
        return TransportFactory { LoggingSerialTransport(inner.create(), logBus) }
    }

    /**
     * The default session factory used by ViewModels. Bound to the [Demo] transport
     * factory and the recorded handshake fixture so the GeneratorX challenge-response
     * is exercised against the fake.
     */
    @Provides
    @Singleton
    fun provideGeneratorSessionFactory(
        @Demo transportFactory: TransportFactory,
        demoData: DemoDumpLoader.DemoData,
    ): GeneratorSessionFactory =
        GeneratorSessionFactory(
            transportFactory = transportFactory,
            handshake = demoData.handshake,
            connectViaProbe = false,
        )
}
