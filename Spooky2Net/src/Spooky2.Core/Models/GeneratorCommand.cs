namespace Spooky2.Core.Models;

/// <summary>
/// Represents a command type in the Spooky2 text-based protocol.
/// Commands are colon-prefixed strings: :Xnn=value,value
/// </summary>
public enum CommandType
{
    /// <summary>Action/address commands (:a). Used for init, ping, handshake.</summary>
    Action,

    /// <summary>Read/query commands (:r). Used to query device state and info.</summary>
    Read,

    /// <summary>Write/set commands (:w). Used to set parameters and control outputs.</summary>
    Write
}

/// <summary>
/// Model representing a single Spooky2 generator command.
/// The protocol uses text-based commands over USB HID in the format :Xnn=value,value
/// terminated with CRLF. Serial settings: 57600,n,8,1 (type 1) or 115200,n,8,1 (type 2/3).
/// </summary>
public sealed record GeneratorCommand
{
    /// <summary>The command type (Action, Read, or Write).</summary>
    public required CommandType Type { get; init; }

    /// <summary>The register identifier, e.g. "00", "11", "24", "68".</summary>
    public required string Register { get; init; }

    /// <summary>The value payload (may be empty for commands without parameters).</summary>
    public string Value { get; init; } = string.Empty;

    /// <summary>
    /// The expected response prefix for validation ("ok", "err", or empty if any response is accepted).
    /// </summary>
    public string ExpectedResponse { get; init; } = string.Empty;

    /// <summary>Builds the full command string ready to send (without CRLF terminator).</summary>
    public string ToCommandString()
    {
        var prefix = Type switch
        {
            CommandType.Action => "a",
            CommandType.Read => "r",
            CommandType.Write => "w",
            _ => throw new ArgumentOutOfRangeException(nameof(Type))
        };

        return string.IsNullOrEmpty(Value)
            ? $":{prefix}{Register}"
            : $":{prefix}{Register}={Value}";
    }
}
