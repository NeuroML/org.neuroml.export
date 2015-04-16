package org.neuroml.export.geppetto;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lemsml.export.base.AXMLWriter;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.support.SupportLevelInfo;
import org.neuroml.export.utils.Format;
import org.neuroml.export.utils.Utils;
import org.neuroml.export.utils.support.ModelFeature;
import org.neuroml.model.util.NeuroMLException;

/**
 *
 * @author padraig
 */
public class GeppettoWriter extends AXMLWriter
{

    private final File inputFile;

    public GeppettoWriter(Lems lems, File outputFolder, String outputFileName, File inputFile) throws ModelFeatureSupportException, LEMSException, NeuroMLException
    {
        super(lems, Format.GEPPETTO, outputFolder, outputFileName);
        this.inputFile = inputFile;
    }

    @Override
    public void setSupportedFeatures()
    {
        sli.addSupportInfo(format, ModelFeature.ALL, SupportLevelInfo.Level.HIGH);
    }

    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    public String getMainScript() throws ContentError
    {

        StringBuilder main = new StringBuilder();
        main.append("<?xml version='1.0' encoding='UTF-8'?>\n");

        String[] attrs = new String[]
        {
            "xmlns=http://www.openworm.org/simulationSchema", "xmlns:xsi=http://www.w3.org/2001/XMLSchema-instance",
            "xsi:schemaLocation=http://www.openworm.org/simulationSchema https://raw.githubusercontent.com/openworm/org.geppetto.core/master/src/main/resources/schema/simulation/simulationSchema.xsd"
        };

        startElement(main, "simulation", attrs);

        startElement(main, "entity");

        Target target = lems.getTarget();

        Component simCpt = target.getComponent();
        E.info("simCpt: " + simCpt);
        String simId = simCpt.getID();
        startEndTextElement(main, "id", simId);

        startElement(main, "aspect");

        startEndTextElement(main, "id", "electrical");

        startElement(main, "simulator");
        startEndTextElement(main, "simulatorId", "jLemsSimulator");
        endElement(main, "simulator");

        startElement(main, "model");
        startEndTextElement(main, "modelInterpreterId", "lemsModelInterpreter");
        startEndTextElement(main, "modelURL", "file:///" + this.inputFile.getAbsolutePath());
        endElement(main, "model");

        endElement(main, "aspect");
        endElement(main, "entity");

        File geppettoScript = new File(getOutputFolder(), this.inputFile.getName().replace(".xml", ".js"));

        StringBuilder gScript = new StringBuilder();

        ArrayList<String> watchVars = new ArrayList<String>();

        gScript.append("Simulation.addWatchLists([{name:\"variables\", variablePaths:[ ");

        StringBuilder gScriptPlots = new StringBuilder();

        int dispIndex = 1;
        for (Component dispComp : simCpt.getAllChildren())
        {
            if (dispComp.getTypeName().equals("Display"))
            {

                gScriptPlots.append("\nG.addWidget(GEPPETTO.Widgets.PLOT);\n");
                String plot = "Plot" + dispIndex;
                gScriptPlots.append(plot + ".setOptions({yaxis:{min:-.08,max:-.04},xaxis:{min:0,max:400,show:false}});\n");
                gScriptPlots.append(plot + ".setSize(400, 600);\n");
                gScriptPlots.append(plot + ".setPosition(" + (dispIndex * 200) + "," + (dispIndex * 200) + ");\n");
                gScriptPlots.append(plot + ".setName(\"" + dispComp.getStringValue("title") + "\");\n");

                dispIndex++;
                for (Component lineComp : dispComp.getAllChildren())
                {
                    if (lineComp.getTypeName().equals("Line"))
                    {

                        String ref = lineComp.getStringValue("quantity");
                        String gepRef = simId + ".electrical.SimulationTree." + ref.replaceAll("/", ".");
                        gScriptPlots.append(plot + ".plotData(" + gepRef + ");\n");
                        watchVars.add(gepRef);
                    }
                }
            }
        }

        for (int ii = 0; ii < watchVars.size(); ii++)
        {
            if (ii > 0)
            {
                gScript.append(", ");
            }
            gScript.append("\"" + watchVars.get(ii) + "\"");
        }
        gScript.append(" ]}]);\n\n");

        gScript.append(gScriptPlots.toString());

        gScript.append("\nSimulation.startWatch();\n");
        //gScript.append("Simulation.start();\n");

        try
        {
            FileUtil.writeStringToFile(gScript.toString(), geppettoScript);

        } catch (IOException ex)
        {
            throw new ContentError("Error saving Geppetto script", ex);
        }

        startEndTextElement(main, "script", "file:///" + geppettoScript.getAbsolutePath());

        endElement(main, "simulation");

        return main.toString();
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

        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ContentError e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return outputFiles;
    }

    public static void main(String[] args) throws Exception
    {
        File exampleFile = new File("/home/padraig/NeuroML2/LEMSexamples/LEMS_NML2_Ex5_DetCell.xml");
        exampleFile = new File("/home/padraig/NeuroML2/LEMSexamples/LEMS_NML2_Ex9_FN.xml");
        Lems lems = Utils.readLemsNeuroMLFile(exampleFile).getLems();

        GeppettoWriter gw = new GeppettoWriter(lems, exampleFile.getParentFile(), exampleFile.getName().replaceAll("xml", "geppetto.xml"), exampleFile);

        List<File> outputFiles = gw.convert();
        for (File outputFile : outputFiles)
        {
            System.out.println("Checking: " + outputFile.getCanonicalPath());
            if (outputFile.getName().indexOf("geppetto") > 0)
            {
                System.out.println("\nTry running this file locally with Geppetto using:\n\n    "
                    + "http://localhost:8080/org.geppetto.frontend/?sim=file://" + outputFile + "\n");
            }
        }

    }

}
