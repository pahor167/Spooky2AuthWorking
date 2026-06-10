package com.spooky2.huntkill.core.model

/**
 * Spooky2 text-protocol command type.
 *
 * Commands are colon-prefixed strings: `:Xnn=value,value` where X is the
 * single-letter prefix below.
 *
 * Verbatim port of the C# reference `Spooky2.Core.Models.CommandType`.
 */
enum class CommandType {
    /** Action/address commands (`:a`). Used for init, ping, handshake. */
    Action,

    /** Read/query commands (`:r`). Used to query device state and info. */
    Read,

    /** Write/set commands (`:w`). Used to set parameters and control outputs. */
    Write,
}
