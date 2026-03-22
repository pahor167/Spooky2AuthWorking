namespace Spooky2.Core.Interfaces;

public interface IDialogService
{
    Task ShowDialogAsync<TViewModel>(TViewModel viewModel) where TViewModel : class;
    Task ShowDialogAsync(string dialogName);
}
