package com.spooky2.huntkill.di

import javax.inject.Qualifier

/** Marks the no-hardware demo (FakeTransport) binding — the default runtime path. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Demo

/** Marks the real USB (UsbCdcSerialTransport) binding — available but not default. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Usb
