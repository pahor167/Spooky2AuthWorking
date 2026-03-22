namespace Spooky2.Core.Interfaces;

/// <summary>
/// Abstraction over serial port operations to enable testing without hardware.
/// Production implementation wraps System.IO.Ports.SerialPort.
/// Test implementation captures commands for verification.
/// </summary>
public interface ISerialPortConnection : IDisposable
{
    void Write(string data);
    string ReadLine();
    /// <summary>
    /// Reads all currently available bytes from the input buffer without blocking.
    /// Equivalent to VB6 SComm32x.Input property.
    /// </summary>
    string ReadExisting();
    /// <summary>Number of bytes waiting in the input buffer.</summary>
    int BytesAvailable { get; }
    bool IsOpen { get; }
}

/// <summary>
/// Factory for creating serial port connections.
/// Decouples MicroGenService from System.IO.Ports.SerialPort for testability.
/// </summary>
public interface ISerialPortFactory
{
    ISerialPortConnection Open(string portName, int baudRate, int readTimeoutMs = 2000, int writeTimeoutMs = 2000);
    List<string> GetAvailablePorts();
}
