namespace Spooky2.Core.Constants;

public static class GeneratorConstants
{
    /// <summary>Microchip Technology USB Vendor ID used by Spooky2 generators.</summary>
    public const int VendorId = 0x04D8; // Microchip Technology Inc.

    /// <summary>Spooky2 generator USB Product ID.</summary>
    public const int ProductId = 0x0032; // Spooky2 HID device

    /// <summary>Maximum number of simultaneously connected generators supported.</summary>
    public const int MaxGenerators = 128;
}
