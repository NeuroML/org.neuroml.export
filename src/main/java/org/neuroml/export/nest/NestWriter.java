package org.neuroml.export.nest;

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
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.base.ANeuroMLBaseWriter;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.neuron.NRNUtils;
import org.neuroml.export.utils.Format;
import org.neuroml.export.utils.Utils;
import org.neuroml.export.utils.VelocityUtils;
import org.neuroml.export.utils.support.ModelFeature;
import org.neuroml.export.utils.support.SupportLevelInfo;
import org.neuroml.model.util.NeuroMLElements;
import org.neuroml.model.util.NeuroMLException;

@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public class NestWriter extends ANeuroMLBaseWriter
{

    String comm = "#";
    String commPre = "'''";
    String commPost = "'''";

    private final List<File> outputFiles = new ArrayList<File>();
    private final DLemsWriter dlemsw;

    public NestWriter(Lems lems) throws ModelFeatureSupportException, LEMSException, NeuroMLException
    {
        super(lems, Format.NEST);
        dlemsw = new DLemsWriter(lems, null, false);
        dlemsw.setPopulationMode(true);
        initializeWriter();
    }

    public NestWriter(Lems lems, File outputFolder, String outputFileName) throws ModelFeatureSupportException, NeuroMLException, LEMSException
    {
        super(lems, Format.NEST, outputFolder, outputFileName);
        dlemsw = new DLemsWriter(lems, outputFolder, null, null, false);
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
        sli.addSupportInfo(format, ModelFeature.ABSTRACT_CELL_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.COND_BASED_CELL_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(format, ModelFeature.SINGLE_COMP_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.NETWORK_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.MULTI_POPULATION_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_INPUTS_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(format, ModelFeature.MULTICOMPARTMENTAL_CELL_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(format, ModelFeature.HH_CHANNEL_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(format, ModelFeature.KS_CHANNEL_MODEL, SupportLevelInfo.Level.NONE);
    }

    @Override
    protected void addComment(StringBuilder sb, String comment)
    {

        if(!comment.contains("\n")) sb.append(comm + comment + "\n");
        else sb.append(commPre + "\n" + comment + "\n" + commPost + "\n");
    }
    
    protected void addCommentNestML(StringBuilder sb, String comment)
    {

        sb.append("/*" + "\n" + comment + "\n" + "*/" + "\n");
    }

    public String getMainScript() throws GenerationException, IOException
    {
        String mainDlemsFile = getOutputFileName()+"_main.json";
        dlemsw.setOutputFileName(mainDlemsFile);
        NRNUtils nrnUtils = new NRNUtils();
        dlemsw.setUnitConverter(nrnUtils);
        dlemsw.setOnlyFlattenIfNecessary(true);
        
        StringBuilder mainRunScript = new StringBuilder();
        StringBuilder cellScript = new StringBuilder();

        addComment(mainRunScript, format + " simulator compliant export for:\n\n" + lems.textSummary(false, false) + "\n\n" + Utils.getHeaderComment(format) + "\n");

        VelocityUtils.initializeVelocity();

        try
        {
            
            List<File> files = dlemsw.convert();
            
            for (File file: files) {
                    
                E.info("\n>>> Processing DLEMS file: " + file.getAbsolutePath());
                    
                String dlems = FileUtil.readStringFromFile(file);
                VelocityContext context = new VelocityContext();
                DLemsWriter.putIntoVelocityContext(dlems, context);

                VelocityEngine ve = VelocityUtils.getVelocityEngine();
                StringWriter sw1 = new StringWriter();

                if (file.getName().equals(mainDlemsFile)) {
                    ve.evaluate(context, sw1, "LOG", VelocityUtils.getTemplateAsReader(VelocityUtils.nestRunTemplateFile));
                    mainRunScript.append(sw1);
                }
                else 
                {
                    StringBuilder script = new StringBuilder();
        
                    String name = (String) context.internalGet(DLemsKeywords.NAME.get());
                    Component comp = lems.components.getByID(name);
                    addCommentNestML(script, format + " simulator compliant export for:\n\n" + comp.details("") + "\n\n" + Utils.getHeaderComment(format) + "\n");
                    E.info("Component LEMS: " + comp.summary());
                    String suffix = ".nestml";
                    String template = VelocityUtils.nestCellTemplateFile;
                    
                    ve.evaluate(context, sw1, "LOG", VelocityUtils.getTemplateAsReader(template));
                    script.append(sw1);

                    E.info("Writing " + format + " file to: " + file.getAbsolutePath());
                    File scriptFile = new File(this.getOutputFolder(), name + suffix);
                    FileUtil.writeStringToFile(script.toString(), scriptFile);
                    outputFiles.add(scriptFile);
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
        catch(LEMSException e)
        {
            throw new GenerationException("Problem generating the files", e);
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
        lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/IzhikevichModel/NeuroML2/LEMS_2007One.xml"));
        lemsFiles.add(new File("../git/HindmarshRose1984/NeuroML2/LEMS_Regular_HindmarshRose.xml"));

        for(File lemsFile : lemsFiles) {

            Lems lems = Utils.readLemsNeuroMLFile(lemsFile).getLems();
            System.out.println("Loaded: " + lemsFile.getAbsolutePath());

            NestWriter nw = new NestWriter(lems, lemsFile.getParentFile(), lemsFile.getName().replaceAll(".xml", "_nest.py"));
            List<File> files = nw.convert(); 
            for (File f: files) {
                System.out.println("Have created: "+f.getAbsolutePath());
            }
        }
    }

}