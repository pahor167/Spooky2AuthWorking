using System;
using System.Globalization;
using System.Linq;
using Avalonia;
using Avalonia.Controls;
using Avalonia.Media;

namespace Spooky2.Views.Controls;

public class SpectralGraph : Control
{
    public static readonly StyledProperty<double[]?> FrequenciesProperty =
        AvaloniaProperty.Register<SpectralGraph, double[]?>(nameof(Frequencies));

    public static readonly StyledProperty<double[]?> AmplitudesProperty =
        AvaloniaProperty.Register<SpectralGraph, double[]?>(nameof(Amplitudes));

    public static readonly StyledProperty<double> MaxFrequencyProperty =
        AvaloniaProperty.Register<SpectralGraph, double>(nameof(MaxFrequency), 200_000);

    public static readonly StyledProperty<string> TitleProperty =
        AvaloniaProperty.Register<SpectralGraph, string>(nameof(Title), "Spectral Analysis");

    public double[]? Frequencies
    {
        get => GetValue(FrequenciesProperty);
        set => SetValue(FrequenciesProperty, value);
    }

    public double[]? Amplitudes
    {
        get => GetValue(AmplitudesProperty);
        set => SetValue(AmplitudesProperty, value);
    }

    public double MaxFrequency
    {
        get => GetValue(MaxFrequencyProperty);
        set => SetValue(MaxFrequencyProperty, value);
    }

    public string Title
    {
        get => GetValue(TitleProperty);
        set => SetValue(TitleProperty, value);
    }

    static SpectralGraph()
    {
        AffectsRender<SpectralGraph>(FrequenciesProperty, AmplitudesProperty, MaxFrequencyProperty, TitleProperty);
    }

    public override void Render(DrawingContext context)
    {
        base.Render(context);
        var bounds = Bounds;

        // Background
        context.FillRectangle(new SolidColorBrush(Color.FromRgb(0xD4, 0xD4, 0xD4)), bounds);

        const double marginLeft = 60;
        const double marginRight = 20;
        const double marginTop = 30;
        const double marginBottom = 40;

        double plotWidth = bounds.Width - marginLeft - marginRight;
        double plotHeight = bounds.Height - marginTop - marginBottom;

        if (plotWidth <= 0 || plotHeight <= 0)
            return;

        // Plot area border
        var borderPen = new Pen(new SolidColorBrush(Colors.Gray), 1);
        context.DrawRectangle(
            null,
            borderPen,
            new Rect(marginLeft, marginTop, plotWidth, plotHeight));

        // Title
        var typeface = new Typeface("Arial");
        var titleText = new FormattedText(
            Title,
            CultureInfo.CurrentCulture,
            FlowDirection.LeftToRight,
            typeface,
            14,
            new SolidColorBrush(Colors.Black));
        context.DrawText(titleText, new Point(marginLeft + (plotWidth - titleText.Width) / 2, 5));

        // Grid lines
        var gridPen = new Pen(new SolidColorBrush(Color.FromRgb(0xC0, 0xC0, 0xC0)), 0.5);
        for (int i = 1; i <= 4; i++)
        {
            double y = marginTop + plotHeight * i / 5;
            context.DrawLine(gridPen, new Point(marginLeft, y), new Point(marginLeft + plotWidth, y));
        }

        for (int i = 1; i <= 4; i++)
        {
            double x = marginLeft + plotWidth * i / 5;
            context.DrawLine(gridPen, new Point(x, marginTop), new Point(x, marginTop + plotHeight));
        }

        // Axis labels
        var smallTypeface = new Typeface("Arial");
        double maxFreq = MaxFrequency > 0 ? MaxFrequency : 200_000;

        // X axis labels
        for (int i = 0; i <= 5; i++)
        {
            double freq = maxFreq * i / 5;
            string label = freq >= 1000 ? $"{freq / 1000:F0}k" : $"{freq:F0}";
            var labelText = new FormattedText(
                label,
                CultureInfo.CurrentCulture,
                FlowDirection.LeftToRight,
                smallTypeface,
                10,
                new SolidColorBrush(Colors.Black));
            double x = marginLeft + plotWidth * i / 5 - labelText.Width / 2;
            context.DrawText(labelText, new Point(x, marginTop + plotHeight + 5));
        }

        // X axis title
        var xAxisTitle = new FormattedText(
            "Frequency (Hz)",
            CultureInfo.CurrentCulture,
            FlowDirection.LeftToRight,
            smallTypeface,
            11,
            new SolidColorBrush(Colors.Black));
        context.DrawText(xAxisTitle, new Point(
            marginLeft + (plotWidth - xAxisTitle.Width) / 2,
            bounds.Height - 15));

        var frequencies = Frequencies;
        var amplitudes = Amplitudes;

        if (frequencies is null || amplitudes is null || frequencies.Length == 0 || amplitudes.Length == 0)
        {
            // Empty state
            var emptyText = new FormattedText(
                "No data",
                CultureInfo.CurrentCulture,
                FlowDirection.LeftToRight,
                typeface,
                14,
                new SolidColorBrush(Colors.Gray));
            context.DrawText(emptyText, new Point(
                marginLeft + (plotWidth - emptyText.Width) / 2,
                marginTop + (plotHeight - emptyText.Height) / 2));
            return;
        }

        int count = Math.Min(frequencies.Length, amplitudes.Length);
        double maxAmp = amplitudes.Take(count).DefaultIfEmpty(1).Max();
        if (maxAmp <= 0) maxAmp = 1;

        // Y axis labels
        for (int i = 0; i <= 5; i++)
        {
            double val = maxAmp * (5 - i) / 5;
            var labelText = new FormattedText(
                val.ToString("F1"),
                CultureInfo.CurrentCulture,
                FlowDirection.LeftToRight,
                smallTypeface,
                10,
                new SolidColorBrush(Colors.Black));
            double y = marginTop + plotHeight * i / 5 - labelText.Height / 2;
            context.DrawText(labelText, new Point(marginLeft - labelText.Width - 5, y));
        }

        // Draw bars
        double barWidth = Math.Max(plotWidth / count * 0.7, 1);
        var barBrush = new SolidColorBrush(Color.FromRgb(0x33, 0x66, 0x99));

        for (int i = 0; i < count; i++)
        {
            double freq = frequencies[i];
            double amp = amplitudes[i];

            double x = marginLeft + (freq / maxFreq) * plotWidth - barWidth / 2;
            double barHeight = (amp / maxAmp) * plotHeight;
            double y = marginTop + plotHeight - barHeight;

            if (x >= marginLeft && x + barWidth <= marginLeft + plotWidth)
            {
                context.FillRectangle(barBrush, new Rect(x, y, barWidth, barHeight));
            }
        }
    }
}
