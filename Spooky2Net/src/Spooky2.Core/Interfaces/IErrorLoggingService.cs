namespace Spooky2.Core.Interfaces;

public interface IErrorLoggingService
{
    Task WriteError(string procedureName, string errorSource, string errorDescription);
    Task TruncateIfNeeded();
}
