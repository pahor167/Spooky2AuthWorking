using System;
using System.Globalization;
using Avalonia;
using Avalonia.Controls;
using Avalonia.Media;
using Spooky2.Core.Models;

namespace Spooky2.Views.Controls;

public class WaveformDisplay : Control
{
    public static readonly StyledProperty<WaveformType> WaveformKindProperty =
        AvaloniaProperty.Register<WaveformDisplay, WaveformType>(nameof(WaveformKind), WaveformType.Sine);

    public static readonly StyledProperty<double> FrequencyProperty =
        AvaloniaProperty.Register<WaveformDisplay, double>(nameof(Frequency), 1.0);

    public static readonly StyledProperty<double> AmplitudeProperty =
        AvaloniaProperty.Register<WaveformDisplay, double>(nameof(Amplitude), 1.0);

    public static readonly StyledProperty<double> PhaseProperty =
        AvaloniaProperty.Register<WaveformDisplay, double>(nameof(Phase), 0.0);

    public WaveformType WaveformKind
    {
        get => GetValue(WaveformKindProperty);
        set => SetValue(WaveformKindProperty, value);
    }

    public double Frequency
    {
        get => GetValue(FrequencyProperty);
        set => SetValue(FrequencyProperty, value);
    }

    public double Amplitude
    {
        get => GetValue(AmplitudeProperty);
        set => SetValue(AmplitudeProperty, value);
    }

    public double Phase
    {
        get => GetValue(PhaseProperty);
        set => SetValue(PhaseProperty, value);
    }

    static WaveformDisplay()
    {
        AffectsRender<WaveformDisplay>(WaveformKindProperty, FrequencyProperty, AmplitudeProperty, PhaseProperty);
    }

    public override void Render(DrawingContext context)
    {
        base.Render(context);
        var bounds = Bounds;

        // Background
        context.FillRectangle(new SolidColorBrush(Color.FromRgb(0xD4, 0xD4, 0xD4)), bounds);

        // Draw grid lines
        var gridPen = new Pen(new SolidColorBrush(Color.FromRgb(0xB0, 0xB0, 0xB0)), 0.5);
        double midY = bounds.Height / 2;
        context.DrawLine(gridPen, new Point(0, midY), new Point(bounds.Width, midY));
        for (int i = 1; i <= 4; i++)
        {
            double x = bounds.Width * i / 4;
            context.DrawLine(gridPen, new Point(x, 0), new Point(x, bounds.Height));
        }

        // Draw waveform
        var pen = new Pen(new SolidColorBrush(Colors.DarkBlue), 2);
        var geometry = new StreamGeometry();
        using (var ctx = geometry.Open())
        {
            int points = Math.Max((int)bounds.Width, 1);
            double phaseRad = Phase * Math.PI / 180.0;
            double amplitudeNormalized = Math.Clamp(Amplitude / 100.0, 0, 1);

            for (int i = 0; i <= points; i++)
            {
                double t = (double)i / points;
                double sample = ComputeSample(WaveformKind, t, phaseRad);
                double y = midY - sample * amplitudeNormalized * (midY - 10);

                if (i == 0)
                    ctx.BeginFigure(new Point(i, y), false);
                else
                    ctx.LineTo(new Point(i, y));
            }
        }

        context.DrawGeometry(null, pen, geometry);

        // Draw label
        var typeface = new Typeface("Arial");
        var text = new FormattedText(
            WaveformKind.ToString(),
            CultureInfo.CurrentCulture,
            FlowDirection.LeftToRight,
            typeface,
            12,
            new SolidColorBrush(Colors.Black));
        context.DrawText(text, new Point(5, 5));
    }

    private static double ComputeSample(WaveformType waveform, double t, double phaseRad)
    {
        return waveform switch
        {
            WaveformType.Sine => Math.Sin(2 * Math.PI * t + phaseRad),
            WaveformType.Square => t < 0.5 ? 1.0 : -1.0,
            WaveformType.Sawtooth => 2 * t - 1,
            WaveformType.InverseSawtooth => 1 - 2 * t,
            WaveformType.Triangle => t < 0.5 ? 4 * t - 1 : 3 - 4 * t,
            WaveformType.Damped => Math.Sin(2 * Math.PI * t + phaseRad) * Math.Exp(-3 * t),
            WaveformType.DampedSquare => (t < 0.5 ? 1.0 : -1.0) * Math.Exp(-3 * t),
            WaveformType.HBomb => Math.Sin(2 * Math.PI * t) * Math.Sin(Math.PI * t),
            _ => Math.Sin(2 * Math.PI * t + phaseRad)
        };
    }
}
