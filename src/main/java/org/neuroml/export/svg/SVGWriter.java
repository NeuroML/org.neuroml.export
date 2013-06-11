package org.neuroml.export.svg;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;


import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.base.XMLWriter;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.Point3DWithDiam;
import org.neuroml.model.Segment;

import org.neuroml.model.Cell;
import org.neuroml.model.util.NeuroMLConverter;

public class SVGWriter extends XMLWriter {

	String originalFilename;
	
	enum Orientation {xy, yz, xz};

    private String SVG_NAMESPACE = "http://www.w3.org/2000/svg";
    private String SVG_VERSION = "1.1";

    int axisWidth = 1;
	private String styleXaxis = "stroke:rgb(0,255,0);stroke-width:"+axisWidth;
	private String styleYaxis = "stroke:rgb(255,255,0);stroke-width:"+axisWidth;
	private String styleZaxis = "stroke:rgb(255,0,0);stroke-width:"+axisWidth;

    public SVGWriter(NeuroMLDocument nmlDocument, String originalFilename) {
        super(nmlDocument, "SVG");
        this.originalFilename = originalFilename;
    }

	@Override
	public String getMainScript() throws ContentError {

        StringBuilder main = new StringBuilder();
        main.append("<?xml version='1.0' encoding='UTF-8'?>\n");


        startElement(main, "svg", "xmlns="+SVG_NAMESPACE, "version="+SVG_VERSION);
        int spacer = 20;
        
        for (Cell cell: nmlDocument.getCell()) {
            analyseCell(cell);
        	cellToLines(main, cell, spacer, spacer, Orientation.xy);
        	cellToLines(main, cell, spacer+spacer+(int)(maxX-minX), spacer, Orientation.xz);
        	cellToLines(main, cell, spacer, spacer+spacer+(int)(maxY-minY), Orientation.yz);
        }
        endElement(main, "svg");
        //System.out.println(main);
        return main.toString();
	}

	double minX = Double.MAX_VALUE;
	double maxX = Double.MIN_VALUE;
	double minY = Double.MAX_VALUE;
	double maxY = Double.MIN_VALUE;
	double minZ = Double.MAX_VALUE;
	double maxZ = Double.MIN_VALUE;
	
	private void analyseCell(Cell cell){
		if (cell.getMorphology() != null) {
    		Hashtable<Integer, Segment> segs = new Hashtable<Integer, Segment>();
			for(Segment segment: cell.getMorphology().getSegment()) {
				Point3DWithDiam dist = segment.getDistal();
				
				minX = Math.min(minX, dist.getX());
				maxX = Math.max(maxX, dist.getX());
				minY = Math.min(minY, dist.getY());
				maxY = Math.max(maxY, dist.getY());
				minZ = Math.min(minZ, dist.getZ());
				maxZ = Math.max(maxZ, dist.getZ());
				
				Point3DWithDiam prox = segment.getProximal();
				if (prox!=null) {
					minX = Math.min(minX, prox.getX());
					maxX = Math.max(maxX, prox.getX());
					minY = Math.min(minY, prox.getY());
					maxY = Math.max(maxY, prox.getY());
					minZ = Math.min(minZ, prox.getZ());
					maxZ = Math.max(maxZ, prox.getZ());
				}
			}
		}
	}
	
	void cellToLines(StringBuilder main, Cell cell, int xTotOffset, int yTotOffset, Orientation or) {
		
		
		if (cell.getMorphology() != null) {
    		Hashtable<Integer, Segment> segs = new Hashtable<Integer, Segment>();
			for(Segment segment: cell.getMorphology().getSegment()) {
				int segId = Integer.parseInt(segment.getId());
				// <line x1="0" y1="0" x2="200" y2="200" style="stroke:rgb(255,0,0);stroke-width:2"/>
				
				Point3DWithDiam prox = segment.getProximal();
				Point3DWithDiam dist = segment.getDistal();
				if (prox==null) {
					Segment parent = segs.get(new Integer(segment.getParent().getSegment().intValue()));
					prox = parent.getDistal();
				}
				int avgDiam = (int)Math.round(0.49+ ((prox.getDiameter()+dist.getDiameter())/2) );
				String style = "stroke:rgb(100,100,100);stroke-width:"+avgDiam;

				double x1 = 0;
				double y1 = 0;
				double x2 = 0;
				double y2 = 0;
				String borderStyle = "fill:none;stroke:black;stroke-width:1;";

				if (or.equals(Orientation.xy)) {
					x1 = prox.getX()-minX;
					y1 = prox.getY()-maxY;
					x2 = dist.getX()-minX;
					y2 = dist.getY()-maxY;

					//addLine(main, xOffset, yOffset, xOffset+100, yOffset, styleXaxis);
					//addLine(main, xOffset, yOffset, xOffset, yOffset+100, styleYaxis);

					addRect(main, xTotOffset, yTotOffset, 0, 0, (maxX-minX), (maxY-minY), borderStyle);
					
				} else if (or.equals(Orientation.xz)) {
					x1 = prox.getX()-minX;
					y1 = prox.getZ()-maxZ;
					x2 = dist.getX()-minX;
					y2 = dist.getZ()-maxZ;

					//addLine(main, xOffset, yOffset, xOffset+100, yOffset, styleXaxis);
					//addLine(main, xOffset, yOffset, xOffset, yOffset+100, styleZaxis);

					addRect(main, xTotOffset, yTotOffset, 0, 0, (maxX-minX), (maxZ-minZ), borderStyle);
					
				} else if (or.equals(Orientation.yz)) {
					x1 = prox.getY()-minY;
					y1 = prox.getZ()-maxZ;
					x2 = dist.getY()-minY;
					y2 = dist.getZ()-maxZ;

					//addLine(main, xOffset, yOffset, xOffset+100, yOffset, styleYaxis);
					//addLine(main, xOffset, yOffset, xOffset, yOffset+100, styleZaxis);
					addRect(main, xTotOffset, yTotOffset, 0, 0, (maxY-minY), (maxZ-minZ), borderStyle);
				}
				
				addLine(main, (x1+xTotOffset), ((-y1)+yTotOffset), (x2+xTotOffset), ((-y2)+yTotOffset), style);
				
				
				segs.put(segId, segment);
			}
    	}

		//main.append("<circle cx=\""+xTotOffset+"\" cy=\""+yTotOffset+"\" r=\"4\" stroke=\"black\" stroke-width=\"1\" fill=\"red\"/>");
		//main.append("<circle cx=\""+(minX+xTotOffset)+"\" cy=\""+(minY+yTotOffset)+"\" r=\"4\" stroke=\"black\" stroke-width=\"1\" fill=\"yellow\"/>");
	}

	private void addLine(StringBuilder main, double x1, double y1, double x2, double y2, String style) {
		startEndElement(main,
                "line", 
		        "x1="+x1, 
		        "y1="+y1, 
		        "x2="+x2, 
		        "y2="+y2, 
		        "style="+style);
	}
	private void addRect(StringBuilder main, double xOffset, double yOffset, double x, double y, double width, double height, String style) {
		//<rect x="50" y="20" width="150" height="150" style="fill:blue;stroke:pink;stroke-width:5;fill-opacity:0.1;stroke-opacity:0.9" />
		startEndElement(main,
                "rect", 
		        "x="+(x+xOffset), 
		        "y="+(y+yOffset), 
		        "width="+width, 
		        "height="+height, 
		        "style="+style);
	}
	
	public static void main(String[] args) throws Exception {

		String fileName = "../neuroConstruct/osb/cerebellum/cerebellar_nucleus_cell/CerebellarNucleusNeuron/NeuroML2/DCN.nml";
		fileName = "../neuroConstruct/osb/hippocampus/CA1_pyramidal_neuron/CA1PyramidalCell/neuroConstruct/generatedNeuroML2/CA1.nml";
		fileName = "../neuroConstruct/osb/cerebral_cortex/networks/Thalamocortical/neuroConstruct/generatedNeuroML2/SupAxAx.nml";
		//fileName = "../CATMAIDShowcase/NeuroML2/catmaid_skeleton.nml";
		fileName = "../CATMAIDShowcase/NeuroML2/catmaid_skeleton_x10.nml";
		NeuroMLConverter nmlc = new NeuroMLConverter();
    	NeuroMLDocument nmlDocument = nmlc.loadNeuroML(new File(fileName));

    	SVGWriter svgw = new SVGWriter(nmlDocument, fileName);
        String svg = svgw.getMainScript();

        System.out.println(svg);

        File svgFile = new File(fileName.replaceAll("nml", "svg"));
        System.out.println("Writing file to: "+svgFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(svg, svgFile);
        
	}
	

}
