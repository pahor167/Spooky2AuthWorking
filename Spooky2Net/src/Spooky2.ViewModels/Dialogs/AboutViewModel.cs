using CommunityToolkit.Mvvm.ComponentModel;

namespace Spooky2.ViewModels.Dialogs;

public partial class AboutViewModel : ObservableObject
{
    [ObservableProperty]
    private string _version = "2.00";

    [ObservableProperty]
    private string _company = "Cancer Clinic (NZ) Ltd";
}
