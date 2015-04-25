package org.neuroml.export.svg;

import java.util.ArrayList;

public class Cell2D implements Comparable<Cell2D>
{
    public ArrayList<Line2D> Lines;
    public String comment;

    public double xMin;
    public double xMax;
    public double yMin;
    public double yMax;

    public Cell2D(Cell3D source, String comment)
    {
        this.comment = comment;
        Lines = new ArrayList<Line2D>(source.Lines.size());

        flatten3DCell(source);

        getRange();
    }

    public double width()
    {
        return xMax - xMin;
    }

    public double height()
    {
        return yMax - yMin;
    }

    public double area()
    {
        return width()*height();
    }

    //Make all coordinates positive and translate them by the offsets
    public ArrayList<Line2D> getLinesForSVG(int offsetX, int offsetY)
    {
        ArrayList<Line2D> result = new ArrayList<Line2D>(Lines.size());

        for(Line2D line : Lines)
        {
            Line2D newLine = line.copy();

            newLine.x1 = line.x1 + offsetX - xMin;
            newLine.x2 = line.x2 + offsetX - xMin;

            newLine.y1 = line.y1 + offsetY - yMin;
            newLine.y2 = line.y2 + offsetY - yMin;

            result.add(newLine);
        }

        return result;
    }

    private void getRange()
    {
        //Assume the first point has the min/max
        if(Lines.size() > 0)
        {
            Line2D first = Lines.get(0);

            xMin = xMax = first.x1;
            yMin = yMax = first.y1;
        }

        for(Line2D line : Lines)
        {
            //Minimums
            if(line.x1 < xMin)
                xMin = line.x1;

            if(line.x2 < xMin)
                xMin = line.x2;

            if(line.y1 < yMin)
                yMin = line.y1;

            if(line.y2 < yMin)
                yMin = line.y2;

            //Maximums
            if(line.x1 > xMax)
                xMax = line.x1;

            if(line.x2 > xMax)
                xMax = line.x2;

            if(line.y1 > yMax)
                yMax = line.y1;

            if(line.y2 > yMax)
                yMax = line.y2;
        }
    }

    private void flatten3DCell(Cell3D source)
    {
        for(Line3D line3D : source.Lines)
        {
            Line2D line2D = new Line2D();

            //Flatten by taking only X-Y coordinates
            line2D.x1 = line3D.proximal.getX();
            line2D.y1 = line3D.proximal.getY();

            line2D.x2 = line3D.distal.getX();
            line2D.y2 = line3D.distal.getY();


            line2D.segmentName = line3D.segmentName;
            line2D.diameter = line3D.diameter;

            Lines.add(line2D);
        }
    }

    @Override
    public int compareTo(Cell2D o)
    {
        return (int)Math.round(o.height() - height());
    }
}
