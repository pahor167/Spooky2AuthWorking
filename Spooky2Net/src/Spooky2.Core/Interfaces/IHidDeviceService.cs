using Spooky2.Core.Models;

namespace Spooky2.Core.Interfaces;

public interface IHidDeviceService
{
    Task<List<HidDeviceInfo>> FindDevices(int vendorId, int productId);
    Task<bool> OpenDevice(int deviceIndex);
    Task<byte[]> ReadDevice(int deviceIndex, int length);
    Task WriteDevice(int deviceIndex, byte[] data);
    void CloseDevice(int deviceIndex);
    void CloseAllDevices();
}
