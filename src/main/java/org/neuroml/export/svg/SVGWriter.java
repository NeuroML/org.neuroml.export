package org.neuroml.export.svg;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
//import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.imageio.ImageIO;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.base.ANeuroMLXMLWriter;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.svg.RectanglePacker.Rectangle;
import org.neuroml.export.utils.Format;
import org.neuroml.export.utils.Utils;
import org.neuroml.export.utils.support.ModelFeature;
import org.neuroml.export.utils.support.SupportLevelInfo;
import org.neuroml.model.Annotation;
import org.neuroml.model.Cell;
import org.neuroml.model.Instance;
import org.neuroml.model.Location;
import org.neuroml.model.Member;
import org.neuroml.model.Morphology;
import org.neuroml.model.Network;
import org.neuroml.model.Property;
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

    private Graphics2D graphics2d = null;
    BufferedImage bufferedImg = null;

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

    private Rectangle expand(Rectangle r1, Rectangle r2)
    {
        return new Rectangle(Math.min(r1.x,r2.x), Math.min(r1.y,r2.y),Math.max(r1.width,r2.width),Math.max(r1.height,r2.height));
    }

    public String getMainScript() throws GenerationException
    {
        StringBuilder core = new StringBuilder();

        Rectangle bounds = render(core, false);
        
        StringBuilder result = new StringBuilder();
        //Add header
        result.append("<?xml version='1.0' encoding='UTF-8'?>\n");
        //addComment(result, "Total bounds: "+bounds.toString());
        startElement(result, "svg", "xmlns=" + SVG_NAMESPACE, 
                                    "version=" + SVG_VERSION, 
                                    "width="+bounds.width, 
                                    "height="+bounds.height,
                                    "viewBox=0 0 "+bounds.width+" "+bounds.height);

        result.append(core.toString());
        
        endElement(result, "svg");

        return result.toString();
    }
    
    public List<Cell> getAllBasedOnCell(NeuroMLDocument nmlDocument) {
        List<Cell> cells = nmlDocument.getCell();
        cells.addAll(nmlDocument.getCell2CaPools());
        return cells;
    }

    public Rectangle render(StringBuilder result, boolean png) {

        if (nmlDocument.getNetwork().isEmpty()) 
        {
            Rectangle r = new Rectangle(0,0,0,0);
            for(Cell cell : getAllBasedOnCell(nmlDocument))
            {
                //Extract 3d vectors from morphology
                Network3D cell3D = new Network3D(cell);
                r = expand(r,renderCells(result, cell3D, png));
            }
            return r;
        }
        else 
        {
            Network3D net3D = new Network3D("View of network");
            for (Network net: nmlDocument.getNetwork()) 
            {
                for (Population pop: net.getPopulation()) 
                {
                    String comp = pop.getComponent();
                    Cell cell = null;
                    for(Cell nextCell : getAllBasedOnCell(nmlDocument))
                    {
                        if (nextCell.getId().equals(comp))
                            cell = nextCell;
                    }
                    if (cell==null) 
                    {
                        for (Property p: pop.getProperty())
                        {
                            if (p.getTag().equals("radius"))
                            {
                                float r = Float.parseFloat(p.getValue());
                                E.info("Using a \"cell\" for population "+pop.getId()+" with radius "+r);
                                cell = getDummySingleCompCell("CellFor_"+comp, r);
                            }
                        }
                        if (cell==null) // still..
                        {
                            E.warning("Cell: "+comp+" not found for population: "+pop.getId()+" in network "+net.getId()+"\n"
                                    +"Using dummy cell with radius "+RADIUS_DUMMY_CELL);
                            cell = getDummySingleCompCell("DummyCellFor_"+comp, RADIUS_DUMMY_CELL);
                        }
                    }
                    String defColor = null;
                    //System.out.println("----" + pop.getProperty());
                    for (Property p: pop.getProperty())
                    {
                        if (p.getTag().equals("color"))
                        {
                            String[] w = p.getValue().split(" ");
                            defColor = "rgb("+(int)Math.floor(Float.parseFloat(w[0])*255) 
                                         +","+(int)Math.floor(Float.parseFloat(w[1])*255)
                                         +","+(int)Math.floor(Float.parseFloat(w[2])*255) +")";
                        }
                    }
                    if (pop.getInstance().isEmpty())
                    {
                        for (int i= 0; i<pop.getSize(); i++) {
                            net3D.addCell(cell, 0, 0, 0, defColor);
                        }
                    } 
                    else 
                    {
                        for (Instance instance: pop.getInstance())
                        {
                            Location loc = instance.getLocation();
                            net3D.addCell(cell, loc.getX(), loc.getY(), loc.getZ(), defColor);
                        }
                    }

                }
            }
            return renderCells(result, net3D, png);
        }
    }

    public void convertToPng(File pngFile) {

        // One quick run to get bounds...
        Rectangle bounds = render(new StringBuilder(), false);
        
        bufferedImg = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_RGB);

        graphics2d = bufferedImg.createGraphics();
        RenderingHints rh = new RenderingHints(
             RenderingHints.KEY_ANTIALIASING,
             RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2d.setRenderingHints(rh);
        rh = new RenderingHints(
             RenderingHints.KEY_TEXT_ANTIALIASING,
             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics2d.setRenderingHints(rh);

        graphics2d.setColor(Color.white);
        graphics2d.fillRect(0,0, bounds.width, bounds.height);

        render(null, true);

        try {
            ImageIO.write(bufferedImg, "png", pngFile);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

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

    private Rectangle renderCells(StringBuilder result, Network3D net3D, boolean png) 
    {
        ArrayList<Cell2D> views = new ArrayList<Cell2D>(4);
        double maxX = -1;
        double maxY = -1;

        //Project 2D views from different perspectives
        net3D.addBoundingBox();
        float offset = perspectiveMargin/2;

        views.add(net3D.perspectiveView(-10, -20));

        net3D.removeAllAxesIndicators();
        float scalebar = net3D.addAxes(offset);
        views.add(net3D.topView());
        views.add(net3D.sideView());
        views.add(net3D.frontView());

        //Pack views to minimize occupied area
        RectanglePacker<Cell2D> packer = packViews(views);
        boolean legendAdded = false;
        for(Cell2D cellView : views)
        {
            //Find where the view will be drawn
            RectanglePacker.Rectangle location = packer.findRectangle(cellView);

            String comment = cellView.comment + "\n" + location;
            //Draw border around perspective
            if (!png) 
            {
                addRect(result, location.x, location.y, 0, 0, location.width, location.height, borderStyle, comment);
            }
            else
            {
                graphics2d.draw3DRect(location.x, location.y, location.width, location.height, false);
            }

            if (!cellView.hasBoundingBox() && !legendAdded)
            {
                int size = 20;
                if (scalebar<50)
                    size = 8;
                String info = scalebar+" \u03BCm";
                if (!png) 
                {
                    addText(result, location.x+5+perspectiveMargin, location.y+location.height-5-perspectiveMargin, size, info, "black");
                }
                else 
                {
                    graphics2d.setColor(Color.BLACK);
                    graphics2d.drawString(info, location.x+5+perspectiveMargin, location.y+location.height-5-perspectiveMargin);
                }
                legendAdded = true;
            }

            //Translate coordinates for the view location
            ArrayList<Line2D> lines = cellView.getLinesForSVG(location.x+20, location.y+20);
            for (Line2D line: lines) 
            {
                maxX = line.getMaxX(maxX);
                maxY = line.getMaxY(maxY);
            }
            //Write SVG for each line
            renderLines(result, lines, png);
        }
        return new Rectangle(0,0,(int)maxX,(int)maxY);
    }

    private Color getColor(String color) {
        if (color.equals("red")) return Color.RED;
        else if (color.equals("grey")) return Color.GRAY;
        else if (color.equals("gray")) return Color.GRAY;
        else if (color.equals("blue")) return Color.BLUE;
        else if (color.equals("black")) return Color.BLACK;
        else if (color.equals("yellow")) return Color.YELLOW;
        else if (color.equals("green")) return Color.GREEN.darker();
        else if (color.startsWith("rgb("))
        {
            String[] rgb = color.substring(4, color.length()-1).split(",");
            //System.out.println(color+" -> ("+rgb[0]+","+rgb[1]+","+rgb[2]+")");
            return new Color(Integer.parseInt(rgb[0]),Integer.parseInt(rgb[1]),Integer.parseInt(rgb[2]));
            
        }
        else return Color.ORANGE;
    }

    private void renderLines(StringBuilder result, 
                             ArrayList<Line2D> lines,
                             boolean png)
    {
        for(Line2D line : lines)
        {
            if (line.x1 == line.x2 && line.y1 == line.y2) 
            {
                if (!png) 
                {
                    addCircle(result, line.x1, line.y1, line.diameter, line.color);
                }
                else 
                {
                    graphics2d.setColor(getColor(line.color));
                    graphics2d.fillOval((int)(line.x1-line.diameter/2), (int)(line.y1-line.diameter/2), (int)line.diameter, (int)line.diameter);
                }
            } 
            else 
            {
                String style = "stroke:"+line.color+";stroke-width:" + line.diameter;

                if (!png) 
                {
                    addLine(result, line.x1, line.y1, line.x2, line.y2, style);
                }
                else 
                {
                    graphics2d.setColor(getColor(line.color));
                    //graphics2d.drawLine((int)line.x1, (int)line.y1, (int)line.x2, (int)line.y2);
                    double theta = Math.atan2(line.y2-line.y1, line.x2-line.x1);
                    double dx = Math.sin(theta) * line.diameter/2;
                    double dy = Math.cos(theta) * line.diameter/2;
                    
                    int[] xpoints = new int[]{(int)(line.x1-dx),(int)(line.x2-dx),(int)(line.x2+dx),(int)(line.x1+dx)};
                    int[] ypoints = new int[]{(int)(line.y1+dy),(int)(line.y2+dy),(int)(line.y2-dy),(int)(line.y1-dy)};
                    /* Better?
                    Path2D.Double path = new Path2D.Double(perspectiveMargin, perspectiveMargin);
                    for(int i=0;i<4;i++) {
                        path.moveTo(xpoints[i], ypoints[i]);
                    }
                    */
                    
                    Polygon p = new Polygon(xpoints, ypoints, 4);
                    graphics2d.fill(p);
                }
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

    private void addText(StringBuilder main, double x, double y, int size, String text, String color)
    {
        startElement(main, "text", "x=" + x, "y=" + y, "fill=" + color, "font-size="+size+"px");
        main.append(text);
        endElement(main, "text");
    }

    public static void main(String[] args) throws Exception
    {

        //String fileName = 
        ArrayList<String> fileNames = new ArrayList<String>();
        fileNames.add("../git/ca1/NeuroML2/network/PINGNet_0_1.net.nml");
        //fileNames.add("../neuroConstruct/osb/cerebral_cortex/networks/PotjansDiesmann2014/NeuroML2/MicrocircuitNoInput.2percent.net.nml");
        /*
        fileNames.add("src/test/resources/examples/L5PC.cell.nml");
        fileNames.add("src/test/resources/examples/L23PyrRS.nml");
        fileNames.add("src/test/resources/examples/TwoCell.net.nml");
        fileNames.add("src/test/resources/examples/MediumNet.net.nml");
        fileNames.add("src/test/resources/examples/pyr_4_sym.cell.nml");
        fileNames.add("../neuroConstruct/osb/cerebral_cortex/networks/ACnet2/neuroConstruct/generatedNeuroML2/MediumNet.net.nml");
        fileNames.add("../neuroConstruct/osb/cerebellum/networks/VervaekeEtAl-GolgiCellNetwork/NeuroML2/Golgi_040408_C1.cell.nml");
        fileNames.add("../neuroConstruct/osb/cerebellum/networks/Cerebellum3DDemo/NeuroML2/CerebellarCortex.net.nml");*/
        fileNames.add("../neuroConstruct/osb/cerebral_cortex/networks/MejiasEtAl2016/NeuroML2/Interareal_3.net.nml");
        fileNames.add("../neuroConstruct/osb/cerebral_cortex/networks/MejiasEtAl2016/NeuroML2/Interareal_30.net.nml");
        
        //fileNames.add("../git/WeilerEtAl08-LaminarCortex/NeuroML2/CortexDemo.net.nml");
        //fileNames.add("../git/OlfactoryBulbMitralCell/neuroConstruct/generatedNeuroML2/Cell1.cell.nml");/**/

        NeuroMLConverter nmlc = new NeuroMLConverter();

        for (String fileName: fileNames) {
            File inputFile = new File(fileName);

            boolean inclIncludes = fileName.indexOf("net.nml") >0;

            NeuroMLDocument nmlDocument = nmlc.loadNeuroML(inputFile, inclIncludes, false);

            SVGWriter svgw = new SVGWriter(nmlDocument, inputFile.getParentFile(), inputFile.getName());

            //Color different segment groups differently
            svgw.useColor = true;

            String svg = svgw.getMainScript();

            File svgFile = new File(fileName.replaceAll("nml", "svg"));
            System.out.println("Writing file to: " + svgFile.getAbsolutePath());

            FileUtil.writeStringToFile(svg, svgFile);

            File pngFile = new File(fileName.replaceAll("nml", "png"));

            svgw.convertToPng(pngFile);
            System.out.println("Writing file to: " + pngFile.getAbsolutePath());

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
