package org.neuroml.export.svg;

import org.neuroml.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Cell3D
{
    public ArrayList<Line3D> Lines;
    public String comment;
    

    public Cell3D(String comment)
    {
        this.comment = comment;
        Lines = new ArrayList<Line3D>(100);
    }

    public Cell3D(Cell cell)
    {
        this.comment = "3D lines of cell: "+cell.getId();
        Lines = extractLines(cell);
    }

    public Cell2D topView()
    {
        return rotate(180,0).defaultView();
    }

    public Cell2D sideView()
    {
        return rotate(180, 90).defaultView();
    }

    public Cell2D frontView()
    {
        return rotate(270, 90).defaultView();
    }

    public Cell2D perspectiveView(double degreesAroundZ, double degreesAroundY)
    {
        return rotate(degreesAroundZ, degreesAroundY).defaultView();
    }




    private Cell2D defaultView()
    {
        return new Cell2D(this, this.comment);
    }

    private Cell3D rotate(double degreesAroundZ, double degreesAroundY)
    {
        Matrix3D rotationM = new Matrix3D();

        rotationM.rotateZ(degreesAroundZ * Math.PI / 180.0);
        rotationM.rotateY(degreesAroundY * Math.PI / 180.0);

        Cell3D result = new Cell3D("Rotation z: "+degreesAroundZ+", rotation y: "+degreesAroundY+" of: "+comment);

        for(Line3D line : Lines)
        {
            Line3D newLine = new Line3D();

            newLine.distal = new Vector3D(line.distal.transform(rotationM));
            newLine.proximal = new Vector3D(line.proximal.transform(rotationM));
            newLine.diameter = line.diameter;
            newLine.segmentName = line.segmentName;

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

        Morphology morphology = cell.getMorphology();
        List<Segment> segments = morphology.getSegment();
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
            line.distal = new Vector3D(distal.getX(), distal.getY(), distal.getZ());
            line.proximal = new Vector3D(proximal.getX(), proximal.getY(), proximal.getZ());
            line.diameter = (int) Math.round(0.49 + ((proximal.getDiameter() + distal.getDiameter()) / 2));

            //Get the group name of the segment
            for(SegmentGroup group : morphology.getSegmentGroup())
            {
                for(Member member : group.getMember())
                {
                    if(member.getSegment().equals(segment.getId()))
                        line.segmentName = group.getId();
                }
            }

            result.add(line);
            segmentMap.put(segment.getId(), segment);
        }

        return result;
    }
}
