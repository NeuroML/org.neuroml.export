package org.lemsml.export.sedml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lemsml.export.base.AXMLWriter;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.logging.MinimalMessageHandler;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Format;
import org.neuroml.export.utils.LEMSQuantityPath;
import org.neuroml.export.utils.Utils;
import org.neuroml.export.utils.support.ModelFeature;
import org.neuroml.model.util.NeuroMLException;
import org.neuroml.export.utils.support.SupportLevelInfo;
import org.neuroml.export.sbml.SBMLWriter;

public class SEDMLWriter extends AXMLWriter
{

    public static final int SEDML_LEVEL = 1;
    public static final int SEDML_VERSION = 3;
    public static final String PREF_SEDML_SCHEMA = "https://raw.githubusercontent.com/SED-ML/sed-ml/master/schema/level"+SEDML_LEVEL+"/version"+SEDML_VERSION+"/sed-ml-L"+SEDML_LEVEL+"-V"+SEDML_VERSION+".xsd";

    public static final String GLOBAL_TIME_SBML = "t";
    public static final String GLOBAL_TIME_SBML_MATHML = "<csymbol encoding=\"text\" definitionURL=\"http://www.sbml.org/sbml/symbols/time\"> time </csymbol>";

    private String inputFileName = "";
    private Format modelFormat;

    private final String DISPLAY_PREFIX = "DISPLAY__";
    private final String OUTPUT_PREFIX = "OUTPUT__";

    public SEDMLWriter(Lems lems, File outputFolder, String outputFileName, String inputFileName, Format modelFormat) throws ModelFeatureSupportException, NeuroMLException, LEMSException
    {
        super(lems, Format.SEDML, outputFolder, outputFileName);
        this.inputFileName = inputFileName;
        this.modelFormat = modelFormat;
    }

    public SEDMLWriter(Lems lems) throws ModelFeatureSupportException, NeuroMLException, LEMSException
    {
        super(lems, Format.SEDML);
    }

    public SEDMLWriter(Lems lems, File outputFolder, String outputFileName, String inputFileName) throws ModelFeatureSupportException, NeuroMLException, LEMSException
    {
        this(lems, outputFolder, outputFileName, inputFileName, Format.NEUROML2);
    }

    public void setSupportedFeatures()
    {
        sli.addSupportInfo(format, ModelFeature.ALL, SupportLevelInfo.Level.HIGH);
    }

    public String getMainScript() throws ContentError
    {

        StringBuilder main = new StringBuilder();
        main.append("<?xml version='1.0' encoding='UTF-8'?>\n");
        String[] attrs = new String[] { "xmlns=http://sed-ml.org/sed-ml/level"+SEDML_LEVEL+"/version"+SEDML_VERSION, "level="+SEDML_LEVEL, "version="+SEDML_VERSION+"", "xmlns:xsi=http://www.w3.org/2001/XMLSchema-instance",
                "xsi:schemaLocation=http://sed-ml.org/sed-ml/level"+SEDML_LEVEL+"/version"+SEDML_VERSION+"   " + PREF_SEDML_SCHEMA };

        startElement(main, "sedML", attrs);
        startElement(main, "notes");
        startElement(main, "p", "xmlns=http://www.w3.org/1999/xhtml");
        main.append("\n" + format + " export for:\n" + lems.textSummary(false, false) + "\n");
        endElement(main, "p");
        endElement(main, "notes");

        Target target = lems.getTarget();

        Component simCpt = target.getComponent();

        String simId = simCpt.getID();

        String targetId = simCpt.getStringValue("target");

        Component tgtNet = lems.getComponent(targetId);
        addComment(main, "Adding simulation " + simCpt + " of network: " + tgtNet.summary() + "", true);

        String netId = tgtNet.getID();

        startElement(main, "listOfSimulations");
        main.append("\n");
        int numPts = (int) Math.ceil(simCpt.getParamValue("length").getDoubleValue() / simCpt.getParamValue("step").getDoubleValue());
        startElement(main, "uniformTimeCourse", "id = " + simId, "initialTime=0", "outputStartTime=0", "outputEndTime=" + simCpt.getParamValue("length").getDoubleValue(), "numberOfPoints=" + numPts);

        startEndElement(main, "algorithm", "kisaoID=KISAO:0000019");

        endElement(main, "uniformTimeCourse");
        main.append("\n");
        endElement(main, "listOfSimulations");

        main.append("\n");

        startElement(main, "listOfModels");

        if(modelFormat == Format.NEUROML2)
        {
            startEndElement(main, "model", "id=" + netId, "language=urn:sedml:language:lems", "source=" + inputFileName);
        }
        else if(modelFormat == Format.SBML)
        {
            startEndElement(main, "model", "id=" + netId, "language=urn:sedml:language:sbml", "source=" + inputFileName.replaceAll(".xml", ".sbml"));
        }
        else if(modelFormat == Format.CELLML)
        {
            startEndElement(main, "model", "id=" + netId, "language=urn:sedml:language:cellml", "source=" + inputFileName.replaceAll(".xml", ".cellml"));
        }

        endElement(main, "listOfModels");
        main.append("\n");

        startElement(main, "listOfTasks");

        // <task simulationReference="Sim_45" id="RUN_Sim_45" modelReference="Ex1_Simple"/>
        String taskId = simId + "_" + netId;
        startEndElement(main, "task", "id=" + taskId, "simulationReference=" + simId, "modelReference=" + netId);

        endElement(main, "listOfTasks");
        main.append("\n");

        startElement(main, "listOfDataGenerators");
        /*
         * <dataGenerator id="time" name="time"> <listOfVariables> <variable id="var_time_0" taskReference="task1" symbol="urn:sedml:symbol:time" /> </listOfVariables> <math
         * xmlns="http://www.w3.org/1998/Math/MathML"> <ci> var_time_0 </ci> </math> </dataGenerator>
         */

        startElement(main, "dataGenerator", "id=time", "name=time");
        startElement(main, "listOfVariables");
        startEndElement(main, "variable", "id=var_time_0", "taskReference=" + taskId, "symbol=urn:sedml:symbol:time");
        endElement(main, "listOfVariables");

        startElement(main, "math", "xmlns=http://www.w3.org/1998/Math/MathML");
        addTextElement(main, "ci", " var_time_0 ");
        endElement(main, "math");
        endElement(main, "dataGenerator");

        for(Component dispOrOutputComp : simCpt.getAllChildren())
        {
            if(dispOrOutputComp.getTypeName().equals("Display") || dispOrOutputComp.getTypeName().equals("OutputFile"))
            {
                String id = dispOrOutputComp.getID().replace(" ","_");

                for(Component lineOrColumnComp : dispOrOutputComp.getAllChildren())
                {
                    if(lineOrColumnComp.getTypeName().equals("Line") || lineOrColumnComp.getTypeName().equals("OutputColumn"))
                    {
                        String quantity = lineOrColumnComp.getStringValue("quantity");
                        LEMSQuantityPath lqp = new LEMSQuantityPath(quantity);
                        String pop = lqp.getPopulation();
                        String num = lqp.getPopulationIndex() + "";
                        String segid = lqp.getSegmentId()==0 ? "" : ("_"+lqp.getSegmentId());
                        String var = lqp.getVariable();

                        String prefix = DISPLAY_PREFIX;
                        if (lineOrColumnComp.getTypeName().equals("OutputColumn"))
                        {
                            prefix = OUTPUT_PREFIX;
                        }

                        String genId = prefix + id + "_" + lineOrColumnComp.getID().replace(" ","_");
                        String varFull = genId + "_" +  pop + "_" + num+segid + "_" + var;

                        startElement(main, "dataGenerator", "id=" + genId, "name=" + genId);
                        startElement(main, "listOfVariables");
                        String targ = "???";

                        if(modelFormat == Format.NEUROML2)
                        {
                            targ = quantity;
                        }
                        else if(modelFormat == Format.SBML)
                        {
                            targ = "/sbml:sbml/sbml:model/sbml:listOfParameters/sbml:parameter[@id='"+var+"']";
                        }
                        else if(modelFormat == Format.CELLML)
                        {
                            targ = quantity;
                        }

                        startEndElement(main, "variable", "id=" + varFull, "taskReference=" + taskId, "target=" + targ);
                        endElement(main, "listOfVariables");

                        startElement(main, "math", "xmlns=http://www.w3.org/1998/Math/MathML");
                        addTextElement(main, "ci", varFull);
                        endElement(main, "math");
                        endElement(main, "dataGenerator");

                    }
                }
            }
        }

        endElement(main, "listOfDataGenerators");
        main.append("\n");

        startElement(main, "listOfOutputs");

        for(Component dispOrOutputComp : simCpt.getAllChildren())
        {
            if(dispOrOutputComp.getTypeName().equals("OutputFile"))
            {
                String reportName = dispOrOutputComp.getStringValue("fileName").replace(".dat","");
                String reportId = reportName.replaceAll("[\\W]", "_");
                String ofId = dispOrOutputComp.getID().replace(" ","_");
                
                startElement(main, "report", "id=" + reportId);
                startElement(main, "listOfDataSets");
                        
                startEndElement(main, "dataSet", "id=time", "name=time", "dataReference=time", "label=time");

                for(Component ocComp : dispOrOutputComp.getAllChildren())
                {
                    if(ocComp.getTypeName().equals("OutputColumn"))
                    {
                        // <dataSet id="d1" name="time" dataReference="time"/>
                        String ocid = reportId + "_" + ocComp.getID().replace(" ","_");
                        
                        String genId = OUTPUT_PREFIX + ofId + "_" + ocid;
                        startEndElement(main, "dataSet", "id=" + ocid, "name=" + genId, "dataReference=" + genId, "label=" + genId);
                    }
                }
                endElement(main, "listOfDataSets");

                endElement(main, "report");
            }
            if(dispOrOutputComp.getTypeName().equals("Display"))
            {
                String dispId = dispOrOutputComp.getID().replace(" ","_");

                startElement(main, "plot2D", "id=" + dispId);
                startElement(main, "listOfCurves");

                for(Component lineComp : dispOrOutputComp.getAllChildren())
                {
                    if(lineComp.getTypeName().equals("Line"))
                    {
                        // trace=StateMonitor(hhpop,'v',record=[0])
                        // //String ref = lineComp.getStringValue("quantity");
                        // //String pop = ref.split("/")[0].split("\\[")[0];
                        // //String num = ref.split("\\[")[1].split("\\]")[0];
                        // //String var = ref.split("/")[1];

                        String lcid = lineComp.getID().replace(" ","_");

                        String genId = DISPLAY_PREFIX + dispId + "_" + lcid;
                        // String varFull = pop+"_"+num+"_"+var;
                        // <curve id="curve_0" logX="false" logY="false" xDataReference="time" yDataReference="v_1" />
                        startEndElement(main, "curve", "id=curve_" + lcid, "logX=false", "logY=false", "xDataReference=time", "yDataReference=" + genId);

                    }
                }
                endElement(main, "listOfCurves");

                endElement(main, "plot2D");
            }
        }

        endElement(main, "listOfOutputs");
        main.append("\n");

        endElement(main, "sedML");
        // System.out.println(main);
        return main.toString();
    }

    @Override
    public List<File> convert() throws IOException, GenerationException
    {
        List<File> outputFiles = new ArrayList<File>();

        try
        {
            String code = this.getMainScript();

            File outputFile = new File(this.getOutputFolder(), this.getOutputFileName());
            FileUtil.writeStringToFile(code, outputFile);
            outputFiles.add(outputFile);

        }
        catch(ContentError e)
        {
            throw new GenerationException("Issue when converting files", e);
        }

        return outputFiles;
    }



    public static void main(String[] args) throws Exception, ModelFeatureSupportException
    {

        MinimalMessageHandler.setVeryMinimal(true);
        E.setDebug(false);

        ArrayList<File> lemsFiles = new ArrayList<File>();


        //lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/ACnet2/neuroConstruct/generatedNeuroML2/LEMS_TwoCell.xml"));
        //lemsFiles.add(new File("../OpenCortex/examples/LEMS_ACNet.xml"));

        //lemsFiles.add(new File("../OpenCortex/examples/LEMS_SpikingNet.xml"));
        //lemsFiles.add(new File("../OpenCortex/examples/LEMS_SimpleNet.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/showcase/SBMLShowcase/NeuroML2/LEMS_NML2_Ex9_FN.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/generic/HindmarshRose1984/NeuroML2/LEMS_Regular_HindmarshRose.xml"));


        SEDMLWriter nw;
        for(File lemsFile : lemsFiles)
        {
            Lems lems = Utils.readLemsNeuroMLFile(lemsFile.getAbsoluteFile()).getLems();

            SBMLWriter sbmlw = new SBMLWriter(lems, lemsFile.getParentFile(), lemsFile.getName().replaceAll(".xml", ".sbml"));
            for(File genFile : sbmlw.convert())
            {
                System.out.println("Generated SBML: " + genFile.getAbsolutePath());
            }

            nw = new SEDMLWriter(lems, lemsFile.getParentFile(), lemsFile.getName().replaceAll(".xml", ".sedml"), lemsFile.getName(), Format.SBML);
            List<File> ff = nw.convert();
            for(File f : ff)
            {
                System.out.println("Generated SED-ML: " + f.getCanonicalPath());
            }

        }


    }
}
