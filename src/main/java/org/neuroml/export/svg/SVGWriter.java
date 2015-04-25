package org.neuroml.export.svg;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.util.NeuroMLConverter;
import org.neuroml.model.util.NeuroMLException;

public class SVGWriter extends ANeuroMLXMLWriter
{
    public boolean useColor = true;

    private final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";
    private final String SVG_VERSION = "1.1";
    private int perspectiveMargin = 20;
    private final String borderStyle = "fill:none;stroke:black;stroke-width:1;";

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

        for(Cell cell : nmlDocument.getCell())
        {
            //Extract 3d vectors from morphology
            Cell3D cell3D = new Cell3D(cell);

            ArrayList<Cell2D> views = new ArrayList<Cell2D>(4);

            //Project 2D views from different perspectives
            views.add(cell3D.topView());
            views.add(cell3D.sideView());
            views.add(cell3D.frontView());
            views.add(cell3D.perspectiveView(180+45, 45));

            //Pack views to minimize occupied area
            RectanglePacker<Cell2D> packer = packViews(views);

            for(Cell2D cellView : views)
            {
                //Find where the view will be drawn
                RectanglePacker.Rectangle location = packer.findRectangle(cellView);

                //Draw border around perspective
                addRect(result, location.x, location.y, 0, 0, location.width, location.height, borderStyle, cellView.comment);
                
                //Translate coordinates for the view location
                ArrayList<Line2D> lines = cellView.getLinesForSVG(location.x, location.y);

                //Write SVG for each line
                renderLines(result, lines, cell3D.somaSegIds, cell3D.dendSegIds, cell3D.axonSegIds, cell3D.apicalDendSegIds);
            }
        }

        endElement(result, "svg");

        return result.toString();
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

            String style = "stroke:"+color+";stroke-width:" + line.diameter;

            addLine(result, line.x1, line.y1, line.x2, line.y2, style);
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

    private void addRect(StringBuilder main, double xOffset, double yOffset, double x, double y, double width, double height, String style, String comment)
    {
        // <rect x="50" y="20" width="150" height="150" style="fill:blue;stroke:pink;stroke-width:5;fill-opacity:0.1;stroke-opacity:0.9" />
        addComment(main, comment);
        startEndElement(main, "rect", "x=" + (x + xOffset), "y=" + (y + yOffset), "width=" + width, "height=" + height, "style=" + style);
    }

    public static void main(String[] args) throws Exception
    {

        //String fileName = 
        ArrayList<String> fileNames = new ArrayList<String>();
        fileNames.add("src/test/resources/examples/L5PC.cell.nml");
        fileNames.add("src/test/resources/examples/L23PyrRS.nml");
        fileNames.add("src/test/resources/examples/ShapedCell.cell.nml");
        
        NeuroMLConverter nmlc = new NeuroMLConverter();

        for (String fileName: fileNames) {
            File inputFile = new File(fileName);

            NeuroMLDocument nmlDocument = nmlc.loadNeuroML(inputFile);

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
