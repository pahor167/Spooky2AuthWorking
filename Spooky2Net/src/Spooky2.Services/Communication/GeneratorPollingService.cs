using Spooky2.Core.Constants;
using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;

namespace Spooky2.Services.Communication;

/// <summary>
/// Continuous polling service ported from VB6 Timer1_Timer.
/// Iterates over active generators, reads their status, and raises events
/// for UI updates. Includes reentrance protection and a 60-second
/// watchdog timer for connection health monitoring.
/// </summary>
public sealed class GeneratorPollingService : IDisposable
{
    private readonly IGeneratorService _generatorService;
    private readonly IErrorLoggingService _errorLoggingService;
    private readonly System.Timers.Timer _timer;
    private readonly object _reentryGuard = new();
    private volatile bool _isPolling;

    /// <summary>Watchdog timeout: 60 seconds (matches VB6 original).</summary>
    private const int WatchdogTimeoutMs = 60_000;

    /// <summary>Poll interval: 100 ms (typical VB6 timer tick).</summary>
    private const int PollIntervalMs = 100;

    /// <summary>
    /// Tracks the last successful communication time per generator
    /// for watchdog timeout detection.
    /// </summary>
    private readonly System.Collections.Concurrent.ConcurrentDictionary<int, DateTime> _lastResponseTimes = new();

    /// <summary>
    /// Cached list of known generators from the last discovery.
    /// Polling reads status from these — it does NOT re-discover every tick.
    /// </summary>
    private List<GeneratorState> _knownGenerators = new();
    private readonly object _generatorsLock = new();

    public GeneratorPollingService(
        IGeneratorService generatorService,
        IErrorLoggingService errorLoggingService)
    {
        _generatorService = generatorService ?? throw new ArgumentNullException(nameof(generatorService));
        _errorLoggingService = errorLoggingService ?? throw new ArgumentNullException(nameof(errorLoggingService));

        _timer = new System.Timers.Timer(PollIntervalMs);
        _timer.Elapsed += OnTimerElapsed;
        _timer.AutoReset = true;
    }

    /// <summary>Raised when a generator's status has been read successfully.</summary>
    public event EventHandler<GeneratorStatusUpdateEventArgs>? StatusUpdated;

    /// <summary>Raised when the watchdog detects a generator that has not responded within the timeout.</summary>
    public event EventHandler<GeneratorWatchdogEventArgs>? WatchdogTimeout;

    /// <summary>Raised when an unexpected error occurs during polling.</summary>
    public event EventHandler<string>? ErrorOccurred;

    public void Start() => _timer.Start();
    public void Stop() => _timer.Stop();

    /// <summary>
    /// Performs initial generator discovery and caches the results.
    /// Call this once before Start(), or call RefreshGenerators() to re-discover later.
    /// </summary>
    public async Task DiscoverGeneratorsAsync()
    {
        var generators = await _generatorService.FindGenerators().ConfigureAwait(false);
        lock (_generatorsLock)
        {
            _knownGenerators = generators;
        }
    }

    /// <summary>
    /// Forces a re-discovery of generators. Use when the user explicitly
    /// requests a rescan or when all generators become unreachable.
    /// </summary>
    public async Task RefreshGeneratorsAsync()
    {
        await DiscoverGeneratorsAsync().ConfigureAwait(false);
    }

    private async void OnTimerElapsed(object? sender, System.Timers.ElapsedEventArgs e)
    {
        // Reentrance protection (same pattern as VB6 eax+0000055Ch guard)
        if (_isPolling) return;
        lock (_reentryGuard)
        {
            if (_isPolling) return;
            _isPolling = true;
        }

        try
        {
            List<GeneratorState> generators;
            lock (_generatorsLock)
            {
                generators = _knownGenerators;
            }

            if (generators.Count == 0)
            {
                return;
            }

            foreach (var generator in generators)
            {
                await PollGenerator(generator).ConfigureAwait(false);
            }
        }
        catch (Exception ex)
        {
            var message = $"Error in Timer1_Timer: {ex.Message}";
            ErrorOccurred?.Invoke(this, message);

            await _errorLoggingService
                .WriteError("GeneratorPollingService.OnTimerElapsed", ex.GetType().Name, ex.Message)
                .ConfigureAwait(false);
        }
        finally
        {
            _isPolling = false;
        }
    }

    private async Task PollGenerator(GeneratorState generator)
    {
        try
        {
            var status = await _generatorService.ReadStatus(generator.Id).ConfigureAwait(false);

            // Record successful communication for watchdog
            _lastResponseTimes[generator.Id] = DateTime.UtcNow;

            StatusUpdated?.Invoke(this, new GeneratorStatusUpdateEventArgs
            {
                GeneratorId = status.Id,
                CurrentFrequency = status.CurrentFrequency,
                CurrentAmplitude = 0, // Amplitude is not tracked in GeneratorState; placeholder for future extension
                Status = status.Status.ToString(),
                Elapsed = status.ElapsedTime
            });
        }
        catch (Exception)
        {
            // Communication failed — check watchdog
            CheckWatchdog(generator.Id);
        }
    }

    private void CheckWatchdog(int generatorId)
    {
        if (!_lastResponseTimes.TryGetValue(generatorId, out var lastResponse))
        {
            // Never responded — treat as timed out immediately
            RaiseWatchdogTimeout(generatorId, TimeSpan.Zero);
            return;
        }

        var elapsed = DateTime.UtcNow - lastResponse;
        if (elapsed.TotalMilliseconds >= WatchdogTimeoutMs)
        {
            RaiseWatchdogTimeout(generatorId, elapsed);
        }
    }

    private void RaiseWatchdogTimeout(int generatorId, TimeSpan silenceDuration)
    {
        WatchdogTimeout?.Invoke(this, new GeneratorWatchdogEventArgs
        {
            GeneratorId = generatorId,
            SilenceDuration = silenceDuration
        });
    }

    public void Dispose()
    {
        _timer.Stop();
        _timer.Dispose();
    }
}

public sealed class GeneratorStatusUpdateEventArgs : EventArgs
{
    public int GeneratorId { get; init; }
    public double CurrentFrequency { get; init; }
    public double CurrentAmplitude { get; init; }
    public string Status { get; init; } = "";
    public TimeSpan Elapsed { get; init; }
}

public sealed class GeneratorWatchdogEventArgs : EventArgs
{
    public int GeneratorId { get; init; }
    public TimeSpan SilenceDuration { get; init; }
}
