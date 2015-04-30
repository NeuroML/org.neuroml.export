package org.neuroml.export.svg;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.base.ANeuroMLXMLWriter;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Format;
import org.neuroml.export.utils.Utils;
import org.neuroml.export.utils.support.ModelFeature;
import org.neuroml.export.utils.support.SupportLevelInfo;
import org.neuroml.model.Cell;
import org.neuroml.model.Instance;
import org.neuroml.model.Location;
import org.neuroml.model.Member;
import org.neuroml.model.Morphology;
import org.neuroml.model.Network;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.Point3DWithDiam;
import org.neuroml.model.Population;
import org.neuroml.model.Segment;
import org.neuroml.model.SegmentGroup;
import org.neuroml.model.util.NeuroMLConverter;
import org.neuroml.model.util.NeuroMLException;

public class SVGWriter extends ANeuroMLXMLWriter
{
    public boolean useColor = true;

    private final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";
    private final String SVG_VERSION = "1.1";
    private final int perspectiveMargin = 20;
    
    private final String borderStyle = "fill:none;stroke:black;stroke-width:1;";
    
    private final float RADIUS_DUMMY_CELL = 5; // um

    public SVGWriter(NeuroMLDocument nmlDocument, File outputFolder, String outputFileName) throws ModelFeatureSupportException, LEMSException, NeuroMLException
    {
        super(Utils.convertNeuroMLToSim(nmlDocument).getLems(), nmlDocument, Format.SVG, outputFolder, outputFileName);
    }

    @Override
    public void setSupportedFeatures()
    {
        sli.addSupportInfo(format, ModelFeature.ABSTRACT_CELL_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(format, ModelFeature.COND_BASED_CELL_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.SINGLE_COMP_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.NETWORK_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(format, ModelFeature.MULTI_POPULATION_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_INPUTS_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(format, ModelFeature.MULTICOMPARTMENTAL_CELL_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(format, ModelFeature.HH_CHANNEL_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.KS_CHANNEL_MODEL, SupportLevelInfo.Level.LOW);
    }

    public String getMainScript() throws GenerationException
    {
        StringBuilder result = new StringBuilder();

        //Add header
        result.append("<?xml version='1.0' encoding='UTF-8'?>\n");
        startElement(result, "svg", "xmlns=" + SVG_NAMESPACE, "version=" + SVG_VERSION);

        if (nmlDocument.getNetwork().isEmpty()) 
        {
            for(Cell cell : nmlDocument.getCell())
            {
                //Extract 3d vectors from morphology
                Cell3D cell3D = new Cell3D(cell);

                renderCells(cell3D, result);

            }
        }
        else 
        {
            Cell3D net3D = new Cell3D("View of network");
            for (Network net: nmlDocument.getNetwork()) 
            {
                for (Population pop: net.getPopulation()) 
                {
                    String comp = pop.getComponent();
                    Cell cell = null;
                    for(Cell nextCell : nmlDocument.getCell())
                    {
                        if (nextCell.getId().equals(comp))
                            cell = nextCell;
                    }
                    if (cell==null) 
                    {
                        E.warning("Cell: "+comp+" not found for population: "+pop.getId()+" in network "+net.getId());
                        E.warning("Using dummy cell with radius "+RADIUS_DUMMY_CELL);
                        cell = getDummySingleCompCell("DummyCellFor_"+comp, RADIUS_DUMMY_CELL);
                    }
                    for (Instance instance: pop.getInstance())
                    {
                        Location loc = instance.getLocation();
                        net3D.addCell(cell, loc.getX(), loc.getY(), loc.getZ());
                    }
                    
                }
            }
            renderCells(net3D, result);
        }

        endElement(result, "svg");

        return result.toString();
    }
    
    private Cell getDummySingleCompCell(String id, float radius)
    {
        Cell cell = new Cell();
        cell.setId(id);
        Morphology morph = new Morphology();
        cell.setMorphology(morph);
        
        Segment soma = new Segment();
        soma.setId(0);
        soma.setName("soma");
        Point3DWithDiam p = (new Point3DWithDiam());
        p.setX(0);
        p.setY(0);
        p.setZ(0);
        p.setDiameter(radius*2);
        soma.setDistal(p);
        soma.setProximal(p);
        
        morph.getSegment().add(soma);
        SegmentGroup sg = new SegmentGroup();
        sg.setId("soma_group");
        sg.setNeuroLexId("sao864921383");
        Member m = new Member();
        m.setSegment(0);
        sg.getMember().add(m);
        
        morph.getSegmentGroup().add(sg);
        
        return cell;
    }
    
    private void renderCells(Cell3D cell3D, StringBuilder result) 
    {
        ArrayList<Cell2D> views = new ArrayList<Cell2D>(4);

        //Project 2D views from different perspectives
        views.add(cell3D.topView());
        views.add(cell3D.sideView());
        views.add(cell3D.frontView());
        views.add(cell3D.perspectiveView(-10, -20));

        //Pack views to minimize occupied area
        RectanglePacker<Cell2D> packer = packViews(views);

        for(Cell2D cellView : views)
        {
            //Find where the view will be drawn
            RectanglePacker.Rectangle location = packer.findRectangle(cellView);

            String comment = cellView.comment + "\n" + location;
            //Draw border around perspective
            addRect(result, location.x, location.y, 0, 0, location.width, location.height, borderStyle, comment);

            //addText(result, location.x+5, location.y+location.height-5, cellView.comment, "black");

            //Translate coordinates for the view location
            ArrayList<Line2D> lines = cellView.getLinesForSVG(location.x+20, location.y+20);

            //Write SVG for each line
            renderLines(result, lines, cell3D.somaSegIds, cell3D.dendSegIds, cell3D.axonSegIds, cell3D.apicalDendSegIds);
        }
    }

    private void renderLines(StringBuilder result, 
                             ArrayList<Line2D> lines,
                             ArrayList<Integer> somaSegIds,
                             ArrayList<Integer> dendSegIds,
                             ArrayList<Integer> axonSegIds,
                             ArrayList<Integer> apicalDendSegIds)
    {
        for(Line2D line : lines)
        {
            //Gray is default
            String color = "rgb(100,100,100)";

            if(useColor)
            {
                if(somaSegIds.contains(line.segmentId))
                {
                    color = "red";
                }
                else if(dendSegIds.contains(line.segmentId))
                {
                    color = "green";
                }
                else if(axonSegIds.contains(line.segmentId))
                {
                    color = "blue";
                }
                else if(apicalDendSegIds.contains(line.segmentId))
                {
                    color = "black";
                }
            }

            if (line.x1 == line.x2 && line.y1 == line.y2) 
            {
                addCircle(result, line.x1, line.y1, line.diameter, color);
            } 
            else 
            {
                String style = "stroke:"+color+";stroke-width:" + line.diameter;

                addLine(result, line.x1, line.y1, line.x2, line.y2, style);
            }
        }
    }

    private RectanglePacker<Cell2D> packViews(ArrayList<Cell2D> views)
    {
        //Sort by descending height
        Collections.sort(views);

        //Find max width/height
        double maxHeight = views.get(0).height();
        double maxWidth = views.get(0).width();
        for(Cell2D view : views)
        {
            if(view.height() > maxHeight)
                maxHeight = view.height();

            if(view.width() > maxWidth)
                maxWidth = view.width();
        }

        int scale = 1;
        boolean doesNotFit;
        RectanglePacker<Cell2D> packer;

        do
        {
            doesNotFit = false;

            //Define the region in which to pack
            packer = new RectanglePacker<Cell2D>
            (
                    (int)(maxWidth * scale),
                    (int)(maxHeight * scale),
                    perspectiveMargin //margin
            );

            //Place the views, starting with largest into packer, and have it recursively try packing
            //If one does not fit into the current size, increase the region by scaling dimensions
            for(Cell2D cellView : views)
            {
                if(packer.insert((int)cellView.width(), (int)cellView.height(), cellView) == null)
                {
                    doesNotFit = true;
                    break;
                }
            }

            scale++;
        }
        while (doesNotFit);

        return packer;
    }

    private void addLine(StringBuilder main, double x1, double y1, double x2, double y2, String style)
    {
        startEndElement(main, "line", "x1=" + x1, "y1=" + y1, "x2=" + x2, "y2=" + y2, "style=" + style);
    }
    
    private void addCircle(StringBuilder main, double x, double y, double diameter, String color)
    {
        // <circle cx="50" cy="50" r="40" stroke="black" stroke-width="3" fill="red" />
        startEndElement(main, "circle", "cx=" + x, "cy=" + y, "r=" + diameter/2, "stroke-width=0", "fill="+color);
    }

    private void addRect(StringBuilder main, double xOffset, double yOffset, double x, double y, double width, double height, String style, String comment)
    {
        // <rect x="50" y="20" width="150" height="150" style="fill:blue;stroke:pink;stroke-width:5;fill-opacity:0.1;stroke-opacity:0.9" />
        addComment(main, comment);
        startEndElement(main, "rect", "x=" + (x + xOffset), "y=" + (y + yOffset), "width=" + width, "height=" + height, "style=" + style);
    }

    private void addText(StringBuilder main, double x, double y, String text, String color)
    {
        startElement(main, "text", "x=" + x, "y=" + y, "fill=" + color);
        main.append(text);
        endElement(main, "text");
    }

    public static void main(String[] args) throws Exception
    {

        //String fileName = 
        ArrayList<String> fileNames = new ArrayList<String>();
        fileNames.add("src/test/resources/examples/L5PC.cell.nml");
        fileNames.add("src/test/resources/examples/L23PyrRS.nml");
        fileNames.add("src/test/resources/examples/ShapedCell.cell.nml");
        fileNames.add("src/test/resources/examples/TwoCell.net.nml");
        //fileNames.add("src/test/resources/examples/MediumNet.net.nml");
        fileNames.add("../neuroConstruct/osb/cerebral_cortex/networks/ACnet2/neuroConstruct/generatedNeuroML2/MediumNet.net.nml");
        fileNames.add("../git/WeilerEtAl08-LaminarCortex/NeuroML2/CortexDemo.net.nml");
        
        NeuroMLConverter nmlc = new NeuroMLConverter();

        for (String fileName: fileNames) {
            File inputFile = new File(fileName);

            boolean inclIncludes = fileName.indexOf("net.nml") >0;
            
            NeuroMLDocument nmlDocument = nmlc.loadNeuroML(inputFile, inclIncludes);

            SVGWriter svgw = new SVGWriter(nmlDocument, inputFile.getParentFile(), inputFile.getName());

            //Color different segment groups differently
            svgw.useColor = true;

            String svg = svgw.getMainScript();

            File svgFile = new File(fileName.replaceAll("nml", "svg"));
            System.out.println("Writing file to: " + svgFile.getAbsolutePath());

            FileUtil.writeStringToFile(svg, svgFile);
        }

    }

    @Override
    public List<File> convert()
    {
        List<File> outputFiles = new ArrayList<File>();

        try
        {
            String code = this.getMainScript();

            File outputFile = new File(this.getOutputFolder(), this.getOutputFileName());
            FileUtil.writeStringToFile(code, outputFile);
            outputFiles.add(outputFile);
        }
        catch(GenerationException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch(IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return outputFiles;
    }

}
