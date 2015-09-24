package org.neuroml.export.vertex;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.lemsml.export.dlems.DLemsKeywords;
import org.lemsml.export.dlems.DLemsWriter;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.logging.MinimalMessageHandler;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.base.ANeuroMLBaseWriter;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Format;
import org.neuroml.export.utils.Utils;
import org.neuroml.export.utils.VelocityUtils;
import org.neuroml.export.utils.support.ModelFeature;
import org.neuroml.export.utils.support.SupportLevelInfo;
import org.neuroml.model.util.NeuroMLException;

@SuppressWarnings("StringConcatenationInsideStringBufferAppend")

public class VertexWriter extends ANeuroMLBaseWriter
{

    String comm = "%";
    String commPre = "%{";
    String commPost = "%}";

    private final List<File> outputFiles = new ArrayList<File>();
    private final DLemsWriter dlemsw;

    private String mainDlemsFile = null;

    /*
    public VertexWriter(Lems lems) throws ModelFeatureSupportException, LEMSException, NeuroMLException
    {
        super(lems, Format.VERTEX);
        dlemsw = new DLemsWriter(lems, null, false);
        dlemsw.setPopulationMode(true);
        initializeWriter();
    }*/

    public VertexWriter(Lems lems, File outputFolder, String outputFileName) throws ModelFeatureSupportException, NeuroMLException, LEMSException
    {
        super(lems, Format.VERTEX, outputFolder, outputFileName);
        mainDlemsFile = outputFileName+"_main.json";
        dlemsw = new DLemsWriter(lems, outputFolder, mainDlemsFile, null, false);
        dlemsw.setPopulationMode(true);
        initializeWriter();
    }

    private void initializeWriter()
    {
        MinimalMessageHandler.setVeryMinimal(true);
        E.setDebug(false);
    }

    @Override
    public void setSupportedFeatures()
    {
        sli.addSupportInfo(format, ModelFeature.ABSTRACT_CELL_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(format, ModelFeature.COND_BASED_CELL_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.SINGLE_COMP_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.NETWORK_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.MULTI_POPULATION_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_INPUTS_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.MULTICOMPARTMENTAL_CELL_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(format, ModelFeature.HH_CHANNEL_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.KS_CHANNEL_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(format, ModelFeature.MULTI_CELL_MODEL, SupportLevelInfo.Level.LOW);
    }

    @Override
    protected void addComment(StringBuilder sb, String comment)
    {

        if(!comment.contains("\n")) sb.append(comm + comment + "\n");
        else sb.append(commPre + "\n" + comment + "\n" + commPost + "\n");
    }

    public String getMainScript() throws GenerationException, IOException
    {
        StringBuilder mainRunScript = new StringBuilder();

        addComment(mainRunScript, format + " simulator compliant export for:\n\n" + lems.textSummary(false, false));



        try
        {
            List<File> files = dlemsw.convert();

            for (File file: files) {


                E.info("Writing " + format + " files to: " + this.getOutputFolder());
                if (file.getName().equals(mainDlemsFile)) {

                    VelocityUtils.initializeVelocity();
                    VelocityContext context = new VelocityContext();
                    VelocityEngine ve = VelocityUtils.getVelocityEngine();
                    String dlems = FileUtil.readStringFromFile(file);
                    DLemsWriter.putIntoVelocityContext(dlems, context);

                    addComment(mainRunScript, "Using the following distilled version of the LEMS model description for the script below:\n\n"+dlems);

                    StringWriter sw1 = new StringWriter();
                    boolean generationStatus = ve.evaluate(context, sw1, "LOG", VelocityUtils.getTemplateAsReader(VelocityUtils.vertexRunTemplateFile));
                    mainRunScript.append(sw1);
                }
                else {

                    StringBuilder cellScript = new StringBuilder();
                    addComment(cellScript, format + " simulator compliant export for:\n\n" + lems.textSummary(false, false));
                    E.info(" ====  Handling DLEMS file: " + file.getAbsolutePath());

                    VelocityUtils.initializeVelocity();
                    VelocityContext context = new VelocityContext();
                    VelocityEngine ve = VelocityUtils.getVelocityEngine();
                    String dlems = FileUtil.readStringFromFile(file);
                    DLemsWriter.putIntoVelocityContext(dlems, context);

                    StringWriter sw2 = new StringWriter();
                    boolean generationStatus2 = ve.evaluate(context, sw2, "LOG", VelocityUtils.getTemplateAsReader(VelocityUtils.vertexCellTemplateFile));

                    addComment(cellScript, "Using the following distilled version of the LEMS model description for the script below:\n\n"+dlems);
                    cellScript.append(sw2);

                    String name = (String) context.internalGet(DLemsKeywords.NAME.get());
                    E.info("Name: "+name);

                    File cellScriptFile = new File(this.getOutputFolder(),"PointNeuronModel_" + name.toLowerCase() + ".m");

                    FileUtil.writeStringToFile(cellScript.toString(), cellScriptFile);
                    outputFiles.add(cellScriptFile);

                }

            }

        }
        catch(IOException e1)
        {
            throw new GenerationException("Problem converting LEMS to dLEMS", e1);
        }
        catch(VelocityException e)
        {
            throw new GenerationException("Problem using Velocity template", e);
        }

        return mainRunScript.toString();

    }

    @Override
    public List<File> convert() throws GenerationException, IOException
    {
        String code = this.getMainScript();

        File outputFile = new File(this.getOutputFolder(), this.getOutputFileName());
        FileUtil.writeStringToFile(code, outputFile);
        outputFiles.add(outputFile);

        return this.outputFiles;
    }


    public static void main(String[] args) throws Exception
    {

        ArrayList<File> lemsFiles = new ArrayList<File>();

        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex9_FN.xml"));
        lemsFiles.add(new File("../git/VERTEXShowcase/test_LEMS/Adex2pop_1comp_test2.xml"));


        for(File lemsFile : lemsFiles) {

            Lems lems = Utils.readLemsNeuroMLFile(lemsFile).getLems();
            System.out.println("Loaded: " + lemsFile.getAbsolutePath());

            VertexWriter nw = new VertexWriter(lems, lemsFile.getParentFile(), lemsFile.getName().replaceAll(".xml", "_run.m"));
            List<File> files = nw.convert(); 
            for (File f: files) {
                System.out.println("Have created: "+f.getAbsolutePath());
            }
        }
    }

}