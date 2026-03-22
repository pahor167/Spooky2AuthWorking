namespace Spooky2.Core.Interfaces;

public interface IMicroGenService
{
    Task<List<string>> FindSerialPorts();
    Task SendToLowPower(string port, List<double> frequencies, int dwellSeconds);
    Task SendToHighPower(string port, List<double> frequencies, int dwellSeconds);
    Task SendToZapper(string port, List<double> frequencies, int dwellSeconds);
    Task SendToBloodPurifier(string port, List<double> frequencies, int dwellSeconds);
    Task Stop(string port);
}
