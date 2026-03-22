namespace Spooky2.Core.Constants;

public static class AppConstants
{
    /// <summary>Root directory for frequency database files.</summary>
    public const string DataDir = "Data";

    /// <summary>Directory for biofeedback scan result data.</summary>
    public const string ScanDataDir = "ScanData";

    /// <summary>Directory for custom waveform definition files.</summary>
    public const string WaveformsDir = "Waveforms";

    /// <summary>Directory for audio files used in treatments.</summary>
    public const string AudioDir = "Audio";

    /// <summary>Directory for system notification sounds.</summary>
    public const string SystemSoundsDir = "SystemSounds";

    /// <summary>Root directory for preset collections (factory and user).</summary>
    public const string PresetCollectionsDir = "Preset Collections";

    /// <summary>Directory for user-created presets.</summary>
    public const string UserPresetsDir = "Preset Collections/User";

    /// <summary>Directory for biofeedback scan configuration and results.</summary>
    public const string BiofeedbackDir = "Biofeedback";

    /// <summary>Default dwell time per frequency in seconds (3 minutes).</summary>
    public const double DefaultDwell = 180.0;

    /// <summary>Maximum error log file size in bytes (10 MB).</summary>
    public const int MaxErrorLogSize = 10_485_760;
}
