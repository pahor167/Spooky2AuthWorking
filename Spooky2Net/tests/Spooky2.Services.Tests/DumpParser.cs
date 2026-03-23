using System.Text.RegularExpressions;

namespace Spooky2.Services.Tests;

/// <summary>
/// Parses serial port monitor dump files captured from Spooky2 GeneratorX communication.
/// The dump format consists of timestamped hex blocks with ASCII preview columns.
/// </summary>
public static class DumpParser
{
    public record DumpEntry(string Direction, string Port, string Timestamp, string Text);

    private static readonly Regex HeaderPattern = new(
        @"\[(?<ts>.*?)\] (?<dir>Written|Read) data \((?<port>\w+)\)",
        RegexOptions.Compiled);

    private static readonly Regex HexLinePattern = new(
        @"^\s+(?<hex>[0-9a-f ]{2,48})",
        RegexOptions.Compiled);

    /// <summary>
    /// Parse a serial port monitor dump file into a list of entries.
    /// Each entry contains the direction (Written/Read), port, timestamp, and decoded text.
    /// </summary>
    public static List<DumpEntry> Parse(string filePath)
    {
        var entries = new List<DumpEntry>();
        string? currentDirection = null;
        string? currentPort = null;
        string? currentTimestamp = null;
        var currentHexParts = new List<string>();

        foreach (var line in File.ReadLines(filePath))
        {
            var headerMatch = HeaderPattern.Match(line);
            if (headerMatch.Success)
            {
                if (currentDirection is not null && currentHexParts.Count > 0)
                {
                    var text = DecodeHexParts(currentHexParts);
                    entries.Add(new DumpEntry(currentDirection, currentPort!, currentTimestamp!, text));
                }

                currentDirection = headerMatch.Groups["dir"].Value;
                currentPort = headerMatch.Groups["port"].Value;
                currentTimestamp = headerMatch.Groups["ts"].Value;
                currentHexParts.Clear();
            }
            else
            {
                var hexMatch = HexLinePattern.Match(line);
                if (hexMatch.Success)
                {
                    currentHexParts.Add(hexMatch.Groups["hex"].Value.Trim());
                }
            }
        }

        // Flush final entry
        if (currentDirection is not null && currentHexParts.Count > 0)
        {
            var text = DecodeHexParts(currentHexParts);
            entries.Add(new DumpEntry(currentDirection, currentPort!, currentTimestamp!, text));
        }

        return entries;
    }

    /// <summary>
    /// Extract only TX (Written) commands from the dump file.
    /// Returns the decoded text with CRLF stripped.
    /// </summary>
    public static List<string> ExtractTxCommands(string filePath)
    {
        return Parse(filePath)
            .Where(e => e.Direction == "Written")
            .Select(e => e.Text)
            .ToList();
    }

    /// <summary>
    /// Extract TX/RX pairs from the dump file.
    /// Each pair is a (Written command, Read response).
    /// </summary>
    public static List<(string Tx, string Rx)> ExtractPairs(string filePath)
    {
        var entries = Parse(filePath);
        var pairs = new List<(string Tx, string Rx)>();

        for (int i = 0; i < entries.Count - 1; i++)
        {
            if (entries[i].Direction == "Written" && entries[i + 1].Direction == "Read")
            {
                pairs.Add((entries[i].Text, entries[i + 1].Text));
            }
        }

        return pairs;
    }

    /// <summary>
    /// Filter out authentication commands (:r90, :w92, :r91, etc.) from a command list.
    /// </summary>
    public static List<string> FilterAuthCommands(List<string> commands)
    {
        return commands
            .Where(c => !c.StartsWith(":r90") && !c.StartsWith(":w92") &&
                        !c.StartsWith(":r91") && !c.StartsWith(":a00"))
            .ToList();
    }

    private static string DecodeHexParts(List<string> hexParts)
    {
        var allHex = string.Join(" ", hexParts);
        var hexBytes = allHex.Split(' ', StringSplitOptions.RemoveEmptyEntries);
        var bytes = new byte[hexBytes.Length];

        for (int i = 0; i < hexBytes.Length; i++)
        {
            bytes[i] = Convert.ToByte(hexBytes[i], 16);
        }

        return System.Text.Encoding.ASCII.GetString(bytes)
            .TrimEnd('\r', '\n');
    }
}
