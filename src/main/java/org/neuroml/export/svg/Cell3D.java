package org.neuroml.export.svg;

import org.neuroml.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import org.neuroml.model.util.CellUtils;

public class Cell3D
{
    public ArrayList<Line3D> lines;
    public String comment;
    public ArrayList<Integer> somaSegIds = new ArrayList<Integer>();
    public ArrayList<Integer> dendSegIds = new ArrayList<Integer>();
    public ArrayList<Integer> axonSegIds = new ArrayList<Integer>();
    public ArrayList<Integer> apicalDendSegIds = new ArrayList<Integer>();
    

    public Cell3D(String comment)
    {
        this.comment = comment;
        lines = new ArrayList<Line3D>(100);
    }

    public Cell3D(Cell cell)
    {
        this.comment = "Cell: "+cell.getId();
        lines = extractLines(cell);
    }
    
    public void addCell(Cell cell, float offsetX, float offsetY, float offsetZ)
    {
        lines.addAll(extractLines(cell, offsetX, offsetY, offsetZ));
    }

    public Cell2D topView()
    {
        return rotate(0,90).rotate(90,0).rotate(0,90).defaultView();
    }

    public Cell2D sideView()
    {
        return rotate(0, 90).defaultView();
    }

    public Cell2D frontView()
    {
        return rotate(0, 0).defaultView();
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

        Cell3D result = new Cell3D("Rot z: "+degreesAroundZ+", rot y: "+degreesAroundY+" of: "+comment);

        for(Line3D line : lines)
        {
            Line3D newLine = new Line3D();

            newLine.distal = new Vector3D(line.distal.transform(rotationM));
            newLine.proximal = new Vector3D(line.proximal.transform(rotationM));
            newLine.diameter = line.diameter;
            newLine.segmentId = line.segmentId;

            result.lines.add(newLine);
        }

        return result;
    }

    
    private ArrayList<Line3D> extractLines(Cell cell)
    {
        return extractLines(cell, 0, 0, 0);
    }
    
    private ArrayList<Line3D> extractLines(Cell cell, float offsetX, float offsetY, float offsetZ)
    {
        ArrayList<Line3D> result = new ArrayList<Line3D>();

        if (cell.getMorphology() == null)
        {
            return result;
        }

        Morphology morphology = cell.getMorphology();
        List<Segment> segments = morphology.getSegment();
        HashMap<Integer, Segment> segmentMap = new HashMap<Integer, Segment>();


        LinkedHashMap<SegmentGroup, ArrayList<Integer>> segGrpsVsSegIds = CellUtils.getSegmentGroupsVsSegIds(cell);
        
        try{
            somaSegIds = CellUtils.getSegmentIdsInGroup(cell, "soma_group");
            dendSegIds = CellUtils.getSegmentIdsInGroup(cell, "dendrite_group");
            axonSegIds = CellUtils.getSegmentIdsInGroup(cell, "axon_group");
            apicalDendSegIds = CellUtils.getSegmentIdsInGroup(cell, "apical_dends");
        }
        catch (Exception e) {

        }

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
            line.distal = new Vector3D(distal.getX()+offsetX, distal.getY()+offsetY, distal.getZ()+offsetZ);
            line.proximal = new Vector3D(proximal.getX()+offsetX, proximal.getY()+offsetY, proximal.getZ()+offsetZ);
            line.diameter = (int) Math.round(0.49 + ((proximal.getDiameter() + distal.getDiameter()) / 2));

            line.segmentId = segment.getId();


            result.add(line);
            segmentMap.put(segment.getId(), segment);
        }

        return result;
    }
}
