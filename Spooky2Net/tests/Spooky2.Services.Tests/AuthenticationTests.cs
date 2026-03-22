using Spooky2.Services.Communication;
using Xunit;

namespace Spooky2.Services.Tests;

public class AuthenticationTests
{
    /// <summary>
    /// Verify ComputeAuthToken against all 8 captured handshakes from real hardware.
    /// Algorithm extracted from Spooky.exe binary (Proc_0_354).
    /// Formula: token[i] = (R[posB]*R[posC] + R[posA]*R[chalDigit]) % 9 + 1
    /// </summary>
    [Theory]
    [InlineData("271543986", "941378256", "883542462")]
    [InlineData("128743956", "518926473", "778914182")]
    [InlineData("953471268", "516273489", "757466413")]
    [InlineData("568732914", "325891674", "885362331")]
    [InlineData("132795684", "479235681", "499857653")]
    [InlineData("532697841", "637518249", "263339911")]
    [InlineData("431697852", "947832165", "567898698")]
    [InlineData("571892436", "642195837", "721863165")]
    public void ComputeAuthToken_MatchesCapturedHandshake(
        string challenge, string deviceResponse, string expectedToken)
    {
        var token = GeneratorAuthentication.ComputeAuthToken(challenge, deviceResponse);
        Assert.Equal(expectedToken, token);
    }

    /// <summary>
    /// Verify ComputeEcho against all 8 captured handshakes from real hardware.
    /// Algorithm extracted from Spooky.exe binary (Proc_0_353).
    /// Formula: echo[i] = (C[posB]*C[posC] + C[posA]*C[respDigit]) % 9 + 1
    /// </summary>
    [Theory]
    [InlineData("271543986", "941378256", "726911191")]
    [InlineData("128743956", "518926473", "622425247")]
    [InlineData("953471268", "516273489", "569931448")]
    [InlineData("568732914", "325891674", "648244366")]
    [InlineData("132795684", "479235681", "371718423")]
    [InlineData("532697841", "637518249", "225113832")]
    [InlineData("431697852", "947832165", "273821994")]
    [InlineData("571892436", "642195837", "752745965")]
    public void ComputeEcho_MatchesCapturedHandshake(
        string challenge, string deviceResponse, string expectedEcho)
    {
        var echo = GeneratorAuthentication.ComputeEcho(challenge, deviceResponse);
        Assert.Equal(expectedEcho, echo);
    }

    [Fact]
    public void GenerateChallenge_IsPermutationOf1To9()
    {
        var challenge = GeneratorAuthentication.GenerateChallenge();
        Assert.Equal(9, challenge.Length);
        Assert.Equal("123456789", new string(challenge.OrderBy(c => c).ToArray()));
    }

    [Fact]
    public void GenerateChallenge_ProducesDifferentValues()
    {
        var challenges = Enumerable.Range(0, 20)
            .Select(_ => GeneratorAuthentication.GenerateChallenge())
            .ToHashSet();
        Assert.True(challenges.Count >= 2);
    }

    /// <summary>
    /// Auth token digits must all be 1-9 (from % 9 + 1).
    /// </summary>
    [Fact]
    public void ComputeAuthToken_AllDigitsAre1To9()
    {
        var token = GeneratorAuthentication.ComputeAuthToken("123456789", "192837465");
        Assert.Equal(9, token.Length);
        Assert.All(token, c => Assert.InRange(c, '1', '9'));
    }

    [Fact]
    public void ComputeEcho_AllDigitsAre1To9()
    {
        var echo = GeneratorAuthentication.ComputeEcho("123456789", "192837465");
        Assert.Equal(9, echo.Length);
        Assert.All(echo, c => Assert.InRange(c, '1', '9'));
    }
}
