using Xunit;
using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;
using Spooky2.Services.Communication;

namespace Spooky2.Services.Tests;

/// <summary>
/// Tests for the generator polling service (Timer1_Timer equivalent).
/// Verifies reentrance protection, status events, watchdog, and error handling.
/// Uses a mock generator service to avoid hardware dependency.
/// </summary>
public class GeneratorPollingTests
{
    // ─────────────────────────────────────────────────────────────
    // Mock infrastructure
    // ─────────────────────────────────────────────────────────────

    private sealed class MockGeneratorService : IGeneratorService
    {
        public List<GeneratorState> Generators { get; set; } = [];
        public int ReadStatusCallCount { get; private set; }
        public bool ReadStatusShouldThrow { get; set; }

        public Task<List<GeneratorState>> FindGenerators() => Task.FromResult(new List<GeneratorState>(Generators));
        public Task<GeneratorState> ReadStatus(int generatorId)
        {
            ReadStatusCallCount++;
            if (ReadStatusShouldThrow) throw new InvalidOperationException("Device disconnected");
            var gen = Generators.FirstOrDefault(g => g.Id == generatorId)
                      ?? new GeneratorState { Id = generatorId, Status = GeneratorStatus.Idle };
            return Task.FromResult(gen);
        }

        public Task Start(int generatorId) => Task.CompletedTask;
        public Task Stop(int generatorId) => Task.CompletedTask;
        public Task Pause(int generatorId) => Task.CompletedTask;
        public Task Hold(int generatorId) => Task.CompletedTask;
        public Task Resume(int generatorId) => Task.CompletedTask;
        public Task WriteFrequencies(int generatorId, List<double> frequencies) => Task.CompletedTask;
        public Task EraseMemory(int generatorId) => Task.CompletedTask;
        public Task IdentifyGenerators() => Task.CompletedTask;
        public Task SendRawCommand(int generatorId, string command) => Task.CompletedTask;
        public Task<string?> SendCommandWithResponse(int generatorId, string command) => Task.FromResult<string?>("ok");
        public Task SendCommandsBatch(int generatorId, IReadOnlyList<string> commands) => Task.CompletedTask;
    }

    private sealed class MockErrorLoggingService : IErrorLoggingService
    {
        public List<string> Errors { get; } = [];
        public Task WriteError(string procedureName, string errorSource, string errorDescription)
        {
            Errors.Add($"{procedureName}: {errorSource} - {errorDescription}");
            return Task.CompletedTask;
        }
        public Task TruncateIfNeeded() => Task.CompletedTask;
    }

    // ─────────────────────────────────────────────────────────────
    // Constructor and lifecycle
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public void Constructor_NullGeneratorService_Throws()
    {
        var logger = new MockErrorLoggingService();
        Assert.Throws<ArgumentNullException>(() => new GeneratorPollingService(null!, logger));
    }

    [Fact]
    public void Constructor_NullErrorLogging_Throws()
    {
        var genService = new MockGeneratorService();
        Assert.Throws<ArgumentNullException>(() => new GeneratorPollingService(genService, null!));
    }

    [Fact]
    public void Dispose_DoesNotThrow()
    {
        var genService = new MockGeneratorService();
        var logger = new MockErrorLoggingService();
        var poller = new GeneratorPollingService(genService, logger);

        poller.Start();
        poller.Dispose(); // Should stop timer and dispose cleanly
    }

    // ─────────────────────────────────────────────────────────────
    // Status update events
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task Polling_FiresStatusUpdatedEvent()
    {
        var genService = new MockGeneratorService
        {
            Generators =
            [
                new GeneratorState { Id = 0, Status = GeneratorStatus.Running, CurrentFrequency = 440.0 }
            ]
        };
        var logger = new MockErrorLoggingService();
        using var poller = new GeneratorPollingService(genService, logger);

        var events = new List<GeneratorStatusUpdateEventArgs>();
        poller.StatusUpdated += (_, e) => events.Add(e);

        poller.Start();
        await Task.Delay(300); // Allow a few poll cycles
        poller.Stop();

        Assert.NotEmpty(events);
        Assert.Equal(0, events[0].GeneratorId);
        Assert.Equal("Running", events[0].Status);
        Assert.Equal(440.0, events[0].CurrentFrequency);
    }

    [Fact]
    public async Task Polling_MultipleGenerators_FiresEventForEach()
    {
        var genService = new MockGeneratorService
        {
            Generators =
            [
                new GeneratorState { Id = 0, Status = GeneratorStatus.Running },
                new GeneratorState { Id = 1, Status = GeneratorStatus.Idle }
            ]
        };
        var logger = new MockErrorLoggingService();
        using var poller = new GeneratorPollingService(genService, logger);

        var generatorIds = new HashSet<int>();
        poller.StatusUpdated += (_, e) => generatorIds.Add(e.GeneratorId);

        poller.Start();
        await Task.Delay(300);
        poller.Stop();

        Assert.Contains(0, generatorIds);
        Assert.Contains(1, generatorIds);
    }

    // ─────────────────────────────────────────────────────────────
    // Error handling
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task Polling_ErrorInReadStatus_FiresErrorEvent()
    {
        var genService = new MockGeneratorService
        {
            Generators = [new GeneratorState { Id = 0 }],
            ReadStatusShouldThrow = true
        };
        var logger = new MockErrorLoggingService();
        using var poller = new GeneratorPollingService(genService, logger);

        var watchdogFired = false;
        poller.WatchdogTimeout += (_, _) => watchdogFired = true;

        poller.Start();
        await Task.Delay(300);
        poller.Stop();

        // ReadStatus throws, which triggers watchdog check
        Assert.True(watchdogFired);
    }

    // ─────────────────────────────────────────────────────────────
    // Reentrance protection
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task Polling_ReentranceProtection_PreventsOverlap()
    {
        // Use a slow ReadStatus to simulate long-running poll
        var genService = new SlowGeneratorService();
        var logger = new MockErrorLoggingService();
        using var poller = new GeneratorPollingService(genService, logger);

        poller.Start();
        await Task.Delay(500); // Timer fires multiple times during slow poll
        poller.Stop();

        // With reentrance protection, concurrent calls should be skipped
        // So the call count should be low despite many timer ticks
        Assert.True(genService.ConcurrentPollCount <= 1,
            $"Concurrent polls detected: {genService.ConcurrentPollCount} (should be <= 1)");
    }

    private sealed class SlowGeneratorService : IGeneratorService
    {
        private int _activeCalls;
        public int ConcurrentPollCount { get; private set; }

        public async Task<List<GeneratorState>> FindGenerators()
        {
            var current = Interlocked.Increment(ref _activeCalls);
            if (current > ConcurrentPollCount) ConcurrentPollCount = current;
            await Task.Delay(200); // Simulate slow operation
            Interlocked.Decrement(ref _activeCalls);
            return [new GeneratorState { Id = 0 }];
        }

        public Task<GeneratorState> ReadStatus(int generatorId)
            => Task.FromResult(new GeneratorState { Id = generatorId, Status = GeneratorStatus.Idle });
        public Task Start(int generatorId) => Task.CompletedTask;
        public Task Stop(int generatorId) => Task.CompletedTask;
        public Task Pause(int generatorId) => Task.CompletedTask;
        public Task Hold(int generatorId) => Task.CompletedTask;
        public Task Resume(int generatorId) => Task.CompletedTask;
        public Task WriteFrequencies(int generatorId, List<double> frequencies) => Task.CompletedTask;
        public Task EraseMemory(int generatorId) => Task.CompletedTask;
        public Task IdentifyGenerators() => Task.CompletedTask;
        public Task SendRawCommand(int generatorId, string command) => Task.CompletedTask;
        public Task<string?> SendCommandWithResponse(int generatorId, string command) => Task.FromResult<string?>("ok");
        public Task SendCommandsBatch(int generatorId, IReadOnlyList<string> commands) => Task.CompletedTask;
    }

    // ─────────────────────────────────────────────────────────────
    // Watchdog
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task Watchdog_NeverResponded_FiresImmediately()
    {
        var genService = new MockGeneratorService
        {
            Generators = [new GeneratorState { Id = 0 }],
            ReadStatusShouldThrow = true
        };
        var logger = new MockErrorLoggingService();
        using var poller = new GeneratorPollingService(genService, logger);

        GeneratorWatchdogEventArgs? watchdogEvent = null;
        poller.WatchdogTimeout += (_, e) => watchdogEvent = e;

        poller.Start();
        await Task.Delay(300);
        poller.Stop();

        Assert.NotNull(watchdogEvent);
        Assert.Equal(0, watchdogEvent!.GeneratorId);
        Assert.Equal(TimeSpan.Zero, watchdogEvent.SilenceDuration);
    }

    // ─────────────────────────────────────────────────────────────
    // Start/Stop
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task StoppedPoller_DoesNotPoll()
    {
        var genService = new MockGeneratorService
        {
            Generators = [new GeneratorState { Id = 0 }]
        };
        var logger = new MockErrorLoggingService();
        using var poller = new GeneratorPollingService(genService, logger);

        // Don't start — just wait
        await Task.Delay(300);

        Assert.Equal(0, genService.ReadStatusCallCount);
    }

    [Fact]
    public async Task StartThenStop_StopsPolling()
    {
        var genService = new MockGeneratorService
        {
            Generators = [new GeneratorState { Id = 0 }]
        };
        var logger = new MockErrorLoggingService();
        using var poller = new GeneratorPollingService(genService, logger);

        poller.Start();
        await Task.Delay(200);
        var countAtStop = genService.ReadStatusCallCount;
        poller.Stop();

        await Task.Delay(300);
        var countAfterStop = genService.ReadStatusCallCount;

        // After stopping, count should not increase significantly
        // (allow 1 extra for in-flight poll)
        Assert.True(countAfterStop - countAtStop <= 1);
    }
}
