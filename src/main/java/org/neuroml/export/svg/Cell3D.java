package org.neuroml.export.svg;

import org.neuroml.model.Cell;
import org.neuroml.model.Point3DWithDiam;
import org.neuroml.model.Segment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Cell3D
{
    public ArrayList<Line3D> Lines;

    public Cell3D()
    {
        Lines = new ArrayList<Line3D>(100);
    }

    public Cell3D(Cell cell)
    {
        Lines = extractLines(cell);
    }

    public Cell2D TopView()
    {
        return DefaultView();
    }

    public Cell2D SideView()
    {
        return Rotate(90, 0).DefaultView();
    }

    public Cell2D FrontView()
    {
        return Rotate(0, 90).DefaultView();
    }

    public Cell2D PerspectiveView(double degreesAroundZ, double degreesAroundY)
    {
        return Rotate(degreesAroundZ, degreesAroundY).DefaultView();
    }




    private Cell2D DefaultView()
    {
        return new Cell2D(this);
    }

    private Cell3D Rotate(double degreesAroundZ, double degreesAroundY)
    {
        Matrix3D rotationM = new Matrix3D();

        rotationM.rotateZ(degreesAroundZ * Math.PI / 180.0);
        rotationM.rotateY(degreesAroundY * Math.PI / 180.0);

        Cell3D result = new Cell3D();

        for(Line3D line : Lines)
        {
            Line3D newLine = new Line3D();

            newLine.Distal = new Vector3D(line.Distal.transform(rotationM));
            newLine.Proximal = new Vector3D(line.Proximal.transform(rotationM));
            newLine.Diameter = line.Diameter;
            newLine.SegmentName = line.SegmentName;

            result.Lines.add(newLine);
        }

        return result;
    }

    private ArrayList<Line3D> extractLines(Cell cell)
    {
        ArrayList<Line3D> result = new ArrayList<Line3D>();

        if (cell.getMorphology() == null)
        {
            return result;
        }

        List<Segment> segments = cell.getMorphology().getSegment();
        HashMap<Integer, Segment> segmentMap = new HashMap<Integer, Segment>();

        for(Segment segment : segments)
        {
            //Get both ends of the segment
            Point3DWithDiam distal = segment.getDistal();
            Point3DWithDiam proximal = segment.getProximal();

            //Parent distal node is the default proximal node
            if(proximal == null)
            {
                Segment parent = segmentMap.get(segment.getParent().getSegment());
                proximal = parent.getDistal();
            }

            //Parse the end coordinates into matrix transformable vectors
            Line3D line = new Line3D();
            line.Distal = new Vector3D(distal.getX(), distal.getY(), distal.getZ());
            line.Proximal = new Vector3D(proximal.getX(), proximal.getY(), proximal.getZ());
            line.Diameter = (int) Math.round(0.49 + ((proximal.getDiameter() + distal.getDiameter()) / 2));
            line.SegmentName = segment.getName();

            result.add(line);
            segmentMap.put(segment.getId(), segment);
        }

        return result;
    }
}
