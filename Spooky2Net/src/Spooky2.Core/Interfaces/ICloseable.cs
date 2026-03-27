namespace Spooky2.Core.Interfaces;

public interface ICloseable
{
    Action? CloseAction { get; set; }
}
