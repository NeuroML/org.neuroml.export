package org.neuroml.export.svg;

public class Line2D extends LineND
{
    public double x1;
    public double y1;

    public double x2;
    public double y2;

    public Line2D Copy()
    {
        Line2D result = new Line2D();

        result.Diameter = Diameter;
        result.SegmentName = SegmentName;

        result.x1 = x1;
        result.x2 = x2;
        result.y1 = y1;
        result.y2 = y2;

        return result;
    }
}
