package com.spooky2.huntkill.di

import android.content.Context
import android.hardware.usb.UsbManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * App-wide singleton providers that Hilt cannot construct by `@Inject` alone.
 *
 * The runtime connects to a real generator over USB only; the
 * [UsbConnectionManager][com.spooky2.huntkill.data.UsbConnectionManager] builds the
 * [GeneratorSession][com.spooky2.huntkill.data.GeneratorSession] directly, so the only
 * framework dependency needed here is the system [UsbManager].
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideUsbManager(@ApplicationContext context: Context): UsbManager =
        context.getSystemService(Context.USB_SERVICE) as UsbManager
}
