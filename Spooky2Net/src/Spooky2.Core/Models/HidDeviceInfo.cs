namespace Spooky2.Core.Models;

public sealed record HidDeviceInfo
{
    public int DeviceIndex { get; init; }
    public int VendorId { get; init; }
    public int ProductId { get; init; }
    public string DevicePath { get; init; } = string.Empty;
    public string ProductName { get; init; } = string.Empty;
}
