using System;
using System.Collections.Generic;
using System.Globalization;
using Avalonia;
using Avalonia.Controls;
using Avalonia.Media;

namespace Spooky2.Views.Controls;

public class BiofeedbackGraphControl : Control
{
    public static readonly StyledProperty<IReadOnlyList<double>?> ReadingsProperty =
        AvaloniaProperty.Register<BiofeedbackGraphControl, IReadOnlyList<double>?>(nameof(Readings));

    public static readonly StyledProperty<double> RunningAverageProperty =
        AvaloniaProperty.Register<BiofeedbackGraphControl, double>(nameof(RunningAverage));

    public static readonly StyledProperty<double> MinValueProperty =
        AvaloniaProperty.Register<BiofeedbackGraphControl, double>(nameof(MinValue));

    public static readonly StyledProperty<double> MaxValueProperty =
        AvaloniaProperty.Register<BiofeedbackGraphControl, double>(nameof(MaxValue), 100);

    public static readonly StyledProperty<string> FrequencyRangeTextProperty =
        AvaloniaProperty.Register<BiofeedbackGraphControl, string>(nameof(FrequencyRangeText), "41000 - 1800000");

    public IReadOnlyList<double>? Readings
    {
        get => GetValue(ReadingsProperty);
        set => SetValue(ReadingsProperty, value);
    }

    public double RunningAverage
    {
        get => GetValue(RunningAverageProperty);
        set => SetValue(RunningAverageProperty, value);
    }

    public double MinValue
    {
        get => GetValue(MinValueProperty);
        set => SetValue(MinValueProperty, value);
    }

    public double MaxValue
    {
        get => GetValue(MaxValueProperty);
        set => SetValue(MaxValueProperty, value);
    }

    public string FrequencyRangeText
    {
        get => GetValue(FrequencyRangeTextProperty);
        set => SetValue(FrequencyRangeTextProperty, value);
    }

    static BiofeedbackGraphControl()
    {
        AffectsRender<BiofeedbackGraphControl>(
            ReadingsProperty, RunningAverageProperty,
            MinValueProperty, MaxValueProperty, FrequencyRangeTextProperty);
    }

    public override void Render(DrawingContext context)
    {
        var bounds = Bounds;
        var w = bounds.Width;
        var h = bounds.Height;

        if (w < 10 || h < 10) return;

        // Background (pink/salmon like original Spooky2)
        var backgroundBrush = new SolidColorBrush(Color.Parse("#E8B0B0"));
        context.FillRectangle(backgroundBrush, new Rect(0, 0, w, h));

        // Border
        var borderPen = new Pen(Brushes.Gray, 1);
        context.DrawRectangle(borderPen, new Rect(0, 0, w, h));

        var readings = Readings;
        var minVal = MinValue;
        var maxVal = MaxValue;
        var range = maxVal - minVal;
        if (range <= 0) range = 1;

        // Margins for labels
        const double marginLeft = 50;
        const double marginRight = 10;
        const double marginTop = 18;
        const double marginBottom = 18;
        double graphW = w - marginLeft - marginRight;
        double graphH = h - marginTop - marginBottom;

        if (graphW <= 0 || graphH <= 0) return;

        // Graph area border
        var graphBorderPen = new Pen(Brushes.DimGray, 1);
        context.DrawRectangle(graphBorderPen,
            new Rect(marginLeft, marginTop, graphW, graphH));

        // Running average line (magenta)
        if (range > 0)
        {
            var raY = marginTop + graphH - ((RunningAverage - minVal) / range * graphH);
            raY = Math.Clamp(raY, marginTop, marginTop + graphH);
            var raPen = new Pen(Brushes.Magenta, 1);
            context.DrawLine(raPen, new Point(marginLeft, raY), new Point(marginLeft + graphW, raY));
        }

        // Data line (black)
        if (readings is { Count: > 1 })
        {
            var dataPen = new Pen(Brushes.Black, 1);
            var pointCount = readings.Count;
            var xStep = graphW / Math.Max(1, pointCount - 1);

            for (int i = 1; i < pointCount; i++)
            {
                var x1 = marginLeft + (i - 1) * xStep;
                var y1 = marginTop + graphH - ((readings[i - 1] - minVal) / range * graphH);
                var x2 = marginLeft + i * xStep;
                var y2 = marginTop + graphH - ((readings[i] - minVal) / range * graphH);

                y1 = Math.Clamp(y1, marginTop, marginTop + graphH);
                y2 = Math.Clamp(y2, marginTop, marginTop + graphH);

                context.DrawLine(dataPen, new Point(x1, y1), new Point(x2, y2));
            }
        }

        // Labels
        var typeface = new Typeface("Arial", FontStyle.Normal, FontWeight.Normal);
        var labelBrush = Brushes.Black;

        // Max value (top-left)
        var maxText = new FormattedText(maxVal.ToString("F2", CultureInfo.InvariantCulture),
            CultureInfo.InvariantCulture, FlowDirection.LeftToRight, typeface, 10, labelBrush);
        context.DrawText(maxText, new Point(2, 2));

        // Min value (bottom-left)
        var minText = new FormattedText(minVal.ToString("F2", CultureInfo.InvariantCulture),
            CultureInfo.InvariantCulture, FlowDirection.LeftToRight, typeface, 10, labelBrush);
        context.DrawText(minText, new Point(2, h - 16));

        // Frequency range (bottom-right)
        var freqLabel = FrequencyRangeText ?? "";
        var freqText = new FormattedText(freqLabel,
            CultureInfo.InvariantCulture, FlowDirection.LeftToRight, typeface, 10, labelBrush);
        context.DrawText(freqText, new Point(w - freqText.Width - 4, h - 16));

        // RA label (left of RA line, in magenta)
        var raLabelText = new FormattedText(RunningAverage.ToString("F1", CultureInfo.InvariantCulture),
            CultureInfo.InvariantCulture, FlowDirection.LeftToRight, typeface, 9, Brushes.Magenta);
        var raLabelY = marginTop + graphH - ((RunningAverage - minVal) / range * graphH) - 6;
        raLabelY = Math.Clamp(raLabelY, marginTop, marginTop + graphH - 12);
        context.DrawText(raLabelText, new Point(2, raLabelY));
    }
}
