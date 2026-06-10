package com.spooky2.huntkill.core.model

/**
 * Generator families distinguished during discovery (see Spooky2Net GeneratorService).
 *  - [Xm]: probed at 57600 baud, simple ping/handshake auth.
 *  - [GeneratorX]: probed at 115200 baud, challenge-response auth (the Hunt & Kill target).
 */
enum class GeneratorType {
    Xm,
    GeneratorX,
    Unknown,
}
