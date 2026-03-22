using System.IO.Ports;
using Spooky2.Core.Interfaces;

namespace Spooky2.Services.Communication;

/// <summary>
/// Production implementation wrapping System.IO.Ports.SerialPort.
/// </summary>
public sealed class SerialPortFactory : ISerialPortFactory
{
    public ISerialPortConnection Open(string portName, int baudRate, int readTimeoutMs = 2000, int writeTimeoutMs = 2000)
    {
        var port = new SerialPort
        {
            PortName = portName,
            BaudRate = baudRate,
            DataBits = 8,
            Parity = Parity.None,
            StopBits = StopBits.One,
            ReadTimeout = readTimeoutMs,
            WriteTimeout = writeTimeoutMs,
            Handshake = Handshake.None,
            DtrEnable = false,
            RtsEnable = true
        };
        port.Open();
        return new SerialPortConnection(port);
    }

    public List<string> GetAvailablePorts()
    {
        return SerialPort.GetPortNames().ToList();
    }

    private sealed class SerialPortConnection : ISerialPortConnection
    {
        private readonly SerialPort _port;

        public SerialPortConnection(SerialPort port) => _port = port;
        public bool IsOpen => _port.IsOpen;
        public int BytesAvailable => _port.BytesToRead;
        public void Write(string data) => _port.Write(data);
        public string ReadLine() => _port.ReadLine();
        public string ReadExisting() => _port.ReadExisting();
        public void Dispose() => _port.Dispose();
    }
}
