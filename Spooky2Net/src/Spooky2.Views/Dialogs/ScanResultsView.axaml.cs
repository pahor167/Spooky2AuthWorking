using System;
using System.Collections.Specialized;
using Avalonia.Controls;
using Spooky2.Core.Models;
using Spooky2.ViewModels.Dialogs;

namespace Spooky2.Views.Dialogs;

public partial class ScanResultsView : Window
{
    public ScanResultsView()
    {
        InitializeComponent();

        // When the VM updates SelectedResults (e.g. Select All / Clear),
        // push those changes into the ListBox UI selection.
        DataContextChanged += (_, _) => SubscribeToViewModelSelection();
    }

    private ScanResultsViewModel? _currentVm;

    private void SubscribeToViewModelSelection()
    {
        // Unsubscribe from previous VM if any.
        if (_currentVm is not null)
        {
            _currentVm.SelectedResults.CollectionChanged -= OnVmSelectionChanged;
        }

        _currentVm = DataContext as ScanResultsViewModel;
        if (_currentVm is not null)
        {
            _currentVm.SelectedResults.CollectionChanged += OnVmSelectionChanged;
        }
    }

    private bool _syncingSelection;

    /// <summary>
    /// ListBox UI -> ViewModel: push the current ListBox selection into the VM.
    /// </summary>
    private void OnHitsSelectionChanged(object? sender, SelectionChangedEventArgs e)
    {
        if (_syncingSelection) return;
        if (DataContext is not ScanResultsViewModel vm || sender is not ListBox lb) return;

        _syncingSelection = true;
        try
        {
            vm.SelectedResults.Clear();
            foreach (var item in lb.SelectedItems!)
            {
                if (item is ScanResult sr)
                {
                    vm.SelectedResults.Add(sr);
                }
            }
        }
        finally
        {
            _syncingSelection = false;
        }
    }

    /// <summary>
    /// ViewModel -> ListBox UI: when VM programmatically changes SelectedResults
    /// (e.g. SelectAll, ClearSelection), reflect it in the ListBox.
    /// </summary>
    private void OnVmSelectionChanged(object? sender, NotifyCollectionChangedEventArgs e)
    {
        if (_syncingSelection) return;

        _syncingSelection = true;
        try
        {
            var lb = HitsListBox;
            switch (e.Action)
            {
                case NotifyCollectionChangedAction.Reset:
                    lb.SelectedItems!.Clear();
                    break;
                case NotifyCollectionChangedAction.Add when e.NewItems is not null:
                    foreach (var item in e.NewItems)
                    {
                        lb.SelectedItems!.Add(item);
                    }
                    break;
                case NotifyCollectionChangedAction.Remove when e.OldItems is not null:
                    foreach (var item in e.OldItems)
                    {
                        lb.SelectedItems!.Remove(item);
                    }
                    break;
                default:
                    // Full replace: rebuild from VM collection.
                    lb.SelectedItems!.Clear();
                    if (_currentVm is not null)
                    {
                        foreach (var item in _currentVm.SelectedResults)
                        {
                            lb.SelectedItems!.Add(item);
                        }
                    }
                    break;
            }
        }
        finally
        {
            _syncingSelection = false;
        }
    }
}
