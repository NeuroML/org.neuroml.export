package org.neuroml.export.jneuroml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.lemsml.export.base.ABaseWriter;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.logging.MinimalMessageHandler;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Format;
import org.neuroml.export.utils.Utils;
import org.neuroml.export.utils.support.ModelFeature;
import org.neuroml.export.utils.support.SupportLevelInfo;
import org.neuroml.model.util.NeuroMLException;

/*
    This simply takes a Lems with full network/simulation specified and writes a single 
    LEMS *.xml file with the <Simulation> element and a single *.net.nml XML file (possibly valid 
    NML, but doesn't have to be) with the network, ready for simulation with jNeuroML
 */
public class JNeuroMLWriter extends ABaseWriter
{

    List<File> outputFiles = new ArrayList<File>();

    public JNeuroMLWriter(Lems lems) throws ModelFeatureSupportException, LEMSException, NeuroMLException
    {
        super(lems, Format.JNEUROML);
        initializeWriter();
    }

    public JNeuroMLWriter(Lems lems, File outputFolder, String outputFileName) throws ModelFeatureSupportException, LEMSException, NeuroMLException
    {
        super(lems, Format.JNEUROML, outputFolder, outputFileName);
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
        sli.addSupportInfo(format, ModelFeature.ABSTRACT_CELL_MODEL, SupportLevelInfo.Level.HIGH);
        sli.addSupportInfo(format, ModelFeature.COND_BASED_CELL_MODEL, SupportLevelInfo.Level.HIGH);
        sli.addSupportInfo(format, ModelFeature.SINGLE_COMP_MODEL, SupportLevelInfo.Level.HIGH);
        sli.addSupportInfo(format, ModelFeature.NETWORK_MODEL, SupportLevelInfo.Level.HIGH);
        sli.addSupportInfo(format, ModelFeature.MULTI_CELL_MODEL, SupportLevelInfo.Level.HIGH);
        sli.addSupportInfo(format, ModelFeature.MULTI_POPULATION_MODEL, SupportLevelInfo.Level.HIGH);
        sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_INPUTS_MODEL, SupportLevelInfo.Level.HIGH);
        sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_GAP_JUNCTIONS_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_ANALOG_CONNS_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.HIGH);
        sli.addSupportInfo(format, ModelFeature.MULTICOMPARTMENTAL_CELL_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(format, ModelFeature.HH_CHANNEL_MODEL, SupportLevelInfo.Level.HIGH);
        sli.addSupportInfo(format, ModelFeature.KS_CHANNEL_MODEL, SupportLevelInfo.Level.HIGH);
    }

    @Override
    protected void addComment(StringBuilder sb, String comment)
    {
        throw new UnsupportedOperationException();
    }

    public String getMainScript() throws GenerationException
    {

        StringBuilder sb = new StringBuilder();

        try
        {

            Target target = lems.getTarget();

            Component simCpt = target.getComponent();

            String targetId = simCpt.getStringValue("target");

            Component tgtNet = lems.getComponent(targetId);
            String netFileName = "NET_" + tgtNet.getID() + ".net.nml";
            String reportFileName = "report.txt";

            File netFile = new File(this.getOutputFolder(), netFileName);
            FileUtil.writeStringToFile(Utils.convertLemsToNeuroMLLikeXml(lems, tgtNet.getID()), netFile);
            outputFiles.add(netFile);

            sb.append(Utils.extractLemsSimulationXml(lems, netFileName, reportFileName));
        }
        catch (IOException ex)
        {
            throw new GenerationException("Problem saving to NeuroML XML", ex);
        }
        catch (LEMSException ex)
        {
            throw new GenerationException("Problem saving to NeuroML XML", ex);
        }
        catch (NeuroMLException ex)
        {
            throw new GenerationException("Problem saving to NeuroML XML", ex);
        }

        return sb.toString();

    }

    @Override
    public List<File> convert() throws GenerationException, IOException
    {

        String code = this.getMainScript();

        File outputFile = new File(this.getOutputFolder(), this.getOutputFileName());
        FileUtil.writeStringToFile(code, outputFile);
        outputFiles.add(outputFile);

        // TODO Auto-generated method stub
        return outputFiles;
    }

    public static void main(String[] args) throws Exception
    {

        ArrayList<File> lemsFiles = new ArrayList<File>();

        /*lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex9_FN.xml"));
		//lemsFiles.add(new File("../NeuroML2/LEMSexamples/NoInp0.xml"));
		lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/barnacle/MorrisLecarModel/NeuroML2/Run_MorrisLecarSCell.xml"));
		lemsFiles.add(new File("../git/HindmarshRose1984/NeuroML2/LEMS_Regular_HindmarshRose.xml"));
		//lemsFiles.add(new File("../git/PinskyRinzelModel/NeuroML2/LEMS_Figure3.xml"));
		//lemsFiles.add(new File("../neuroConstruct/osb/hippocampus/CA1_pyramidal_neuron/FergusonEtAl2014-CA1PyrCell/NeuroML2/LEMS_TwoCells.xml"));
		lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/IzhikevichModel/NeuroML2/LEMS_FiveCells.xml"));
		lemsFiles.add(new File("../neuroConstruct/osb/hippocampus/CA1_pyramidal_neuron/FergusonEtAl2014-CA1PyrCell/NeuroML2/LEMS_TwoCells.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/hippocampus/interneurons/FergusonEtAl2013-PVFastFiringCell/NeuroML2/LEMS_PVBC.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/neocortical_pyramidal_neuron/MainenEtAl_PyramidalCell/neuroConstruct/generatedNeuroML2/LEMS_OneComp.xml"));
        
        lemsFiles.add(new File("../neuroConstruct/osb/generic/hodgkin_huxley_tutorial/Tutorial/Source/LEMS_HH_SingleAP.xml"));
         */
        lemsFiles.add(new File("../neuroConstruct/osb/showcase/BrianShowcase/NeuroML2/LEMS_2007One.xml"));
        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex1_HH.xml"));

        lemsFiles.add(new File("../neuroConstruct/osb/hippocampus/CA1_pyramidal_neuron/FergusonEtAl2014-CA1PyrCell/NeuroML2/LEMS_TwoCells.xml"));
        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex5_DetCell.xml"));
        lemsFiles.add(new File("../OpenCortex/examples/LEMS_Balanced_24cells_336conns.xml"));

        for (File lemsFile : lemsFiles)
        {

            Lems lems = Utils.readLemsNeuroMLFile(lemsFile).getLems();
            System.out.println("Loaded: " + lemsFile.getAbsolutePath());

            JNeuroMLWriter bw = new JNeuroMLWriter(lems, lemsFile.getParentFile(), "LEMSYMcLEMSFace.xml");

            List<File> ff = bw.convert();
            for (File f : ff)
            {
                System.out.println("Created: " + f.getCanonicalPath());
            }

        }

    }

}
