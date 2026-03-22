using HidSharp;
using Microsoft.Extensions.Logging;
using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;

namespace Spooky2.Services.Communication;

/// <summary>
/// HID device communication using HidSharp.
/// Protocol-specific packet formats and command structures are marked as TODO
/// and must be ported from the VB6 USB communication layer.
/// </summary>
public sealed class HidDeviceService : IHidDeviceService, IDisposable
{
    private readonly ILogger<HidDeviceService> _logger;
    private readonly Dictionary<int, HidStream> _openStreams = new();
    private readonly Dictionary<int, HidDevice> _openDevices = new();

    public HidDeviceService(ILogger<HidDeviceService> logger)
    {
        _logger = logger;
    }

    public Task<List<HidDeviceInfo>> FindDevices(int vendorId, int productId)
    {
        var localDevices = DeviceList.Local.GetHidDevices();
        _logger.LogDebug("HidSharp found {Total} HID device(s) total on system", localDevices.Count());

        var matched = new List<HidDeviceInfo>();
        int index = 0;

        foreach (var device in localDevices)
        {
            _logger.LogDebug("  HID: VID=0x{Vid:X4} PID=0x{Pid:X4} Path={Path}",
                device.VendorID, device.ProductID, device.DevicePath);

            if (device.VendorID == vendorId && device.ProductID == productId)
            {
                var name = "(unknown)";
                try { name = device.GetProductName() ?? "(unknown)"; } catch { }

                _logger.LogInformation("  ** MATCH: {Name} at {Path}", name, device.DevicePath);

                matched.Add(new HidDeviceInfo
                {
                    DeviceIndex = index,
                    VendorId = device.VendorID,
                    ProductId = device.ProductID,
                    DevicePath = device.DevicePath,
                    ProductName = name
                });
                index++;
            }
        }

        _logger.LogInformation("Matched {Count} Spooky2 device(s)", matched.Count);
        return Task.FromResult(matched);
    }

    public Task<bool> OpenDevice(int deviceIndex)
    {
        if (_openStreams.ContainsKey(deviceIndex))
            return Task.FromResult(true);

        var devices = DeviceList.Local.GetHidDevices().ToArray();
        if (deviceIndex < 0 || deviceIndex >= devices.Length)
            return Task.FromResult(false);

        var device = devices[deviceIndex];
        if (!device.TryOpen(out var stream))
            return Task.FromResult(false);

        _openDevices[deviceIndex] = device;
        _openStreams[deviceIndex] = stream;
        return Task.FromResult(true);
    }

    public async Task<byte[]> ReadDevice(int deviceIndex, int length)
    {
        if (!_openStreams.TryGetValue(deviceIndex, out var stream))
            throw new InvalidOperationException($"Device {deviceIndex} is not open.");

        // HidSharp handles HID report framing (report ID prefix, padding) automatically.
        // The caller receives raw payload bytes; protocol-level parsing (ASCII commands + CRLF)
        // is handled by GeneratorProtocol.
        var buffer = new byte[length];
        int bytesRead = await stream.ReadAsync(buffer, 0, length);
        if (bytesRead < length)
        {
            Array.Resize(ref buffer, bytesRead);
        }
        return buffer;
    }

    public async Task WriteDevice(int deviceIndex, byte[] data)
    {
        if (!_openStreams.TryGetValue(deviceIndex, out var stream))
            throw new InvalidOperationException($"Device {deviceIndex} is not open.");

        // HidSharp handles HID report packaging (report ID, padding to report size) automatically.
        // The caller provides raw payload bytes; protocol-level command construction (ASCII + CRLF)
        // is handled by GeneratorProtocol.
        await stream.WriteAsync(data, 0, data.Length);
    }

    public void CloseDevice(int deviceIndex)
    {
        if (_openStreams.TryGetValue(deviceIndex, out var stream))
        {
            stream.Dispose();
            _openStreams.Remove(deviceIndex);
            _openDevices.Remove(deviceIndex);
        }
    }

    public void CloseAllDevices()
    {
        foreach (var stream in _openStreams.Values)
        {
            stream.Dispose();
        }
        _openStreams.Clear();
        _openDevices.Clear();
    }

    public void Dispose()
    {
        CloseAllDevices();
    }
}
