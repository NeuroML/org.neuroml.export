package org.neuroml.export.svg;

import org.neuroml.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import org.neuroml.model.util.CellUtils;

public class Network3D
{
    public ArrayList<Line3D> lines;
    public String comment;
    

    public Network3D(String comment)
    {
        this.comment = comment;
        lines = new ArrayList<Line3D>(100);
    }

    public Network3D(Cell cell)
    {
        this.comment = "Cell: "+cell.getId();
        lines = extractLines(cell);
    }
    
    public void addCell(Cell cell, float offsetX, float offsetY, float offsetZ)
    {
        lines.addAll(extractLines(cell, offsetX, offsetY, offsetZ));
    }

    public void addLine(int id, Vector3D a, Vector3D b, float diameter, String color) {
        Line3D line = new Line3D();
        line.proximal = a;
        line.distal = b;
        line.diameter = diameter;
        
        line.id = id;
        line.color = color;
        lines.add(line);
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
    

    private Network3D rotate(double degreesAroundZ, double degreesAroundY)
    {
        Matrix3D rotationM = new Matrix3D();

        rotationM.rotateZ(degreesAroundZ * Math.PI / 180.0);
        rotationM.rotateY(degreesAroundY * Math.PI / 180.0);

        Network3D result = new Network3D("Rot z: "+degreesAroundZ+", rot y: "+degreesAroundY+" of: "+comment);

        for(Line3D line : lines)
        {
            Line3D newLine = new Line3D();

            newLine.distal = new Vector3D(line.distal.transform(rotationM));
            newLine.proximal = new Vector3D(line.proximal.transform(rotationM));
            newLine.diameter = line.diameter;
            newLine.id = line.id;
            newLine.color = line.color;

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


        //LinkedHashMap<SegmentGroup, ArrayList<Integer>> segGrpsVsSegIds = CellUtils.getSegmentGroupsVsSegIds(cell);
        
        ArrayList<Integer> somaSegIds = CellUtils.getSegmentIdsInGroup(cell, "soma_group");
        ArrayList<Integer> dendSegIds = CellUtils.getSegmentIdsInGroup(cell, "dendrite_group");
        ArrayList<Integer> axonSegIds = CellUtils.getSegmentIdsInGroup(cell, "axon_group");
        ArrayList<Integer> apicalDendSegIds = CellUtils.getSegmentIdsInGroup(cell, "apical_dends");
      
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
            //Gray is default
            String color = "rgb(100,100,100)";
            
            if(somaSegIds.contains(segment.getId()))
            {
                color = "red";
            }
            else if(dendSegIds.contains(segment.getId()))
            {
                color = "grey";
            }
            else if(axonSegIds.contains(segment.getId()))
            {
                color = "blue";
            }
            else if(apicalDendSegIds.contains(segment.getId()))
            {
                color = "black";
            }

            //Parse the end coordinates into matrix transformable vectors
            Line3D line = new Line3D();
            line.distal = new Vector3D(distal.getX()+offsetX, distal.getY()+offsetY, distal.getZ()+offsetZ);
            line.proximal = new Vector3D(proximal.getX()+offsetX, proximal.getY()+offsetY, proximal.getZ()+offsetZ);
            line.diameter = (int) Math.round(0.49 + ((proximal.getDiameter() + distal.getDiameter()) / 2));

            line.id = segment.getId();
            line.color = color;


            result.add(line);
            segmentMap.put(segment.getId(), segment);
        }

        return result;
    }
}
