package org.neuroml.export.netpyne;

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
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.base.ANeuroMLBaseWriter;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.neuron.NRNUtils;
import org.neuroml.export.neuron.NeuronWriter;
import org.neuroml.export.neuron.ProcessManager;
import static org.neuroml.export.neuron.ProcessManager.findNeuronHome;
import org.neuroml.export.utils.Format;
import org.neuroml.export.utils.ProcessOutputWatcher;
import org.neuroml.export.utils.Utils;
import org.neuroml.export.utils.VelocityUtils;
import org.neuroml.export.utils.support.ModelFeature;
import org.neuroml.export.utils.support.SupportLevelInfo;
import org.neuroml.model.util.NeuroMLElements;
import org.neuroml.model.util.NeuroMLException;

@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public class NetPyNEWriter extends ANeuroMLBaseWriter
{

	String comm = "#";
	String commPre = "'''";
	String commPost = "'''";

    boolean nogui = true;
    boolean regenerateNeuroMLNet = false;

	private final List<File> outputFiles = new ArrayList<File>();
    private final DLemsWriter dlemsw;
    private String mainDlemsFile = null;

    public final String CELL_DEFINITION_SUFFIX = "_celldefinition";
    public final String INPUT_DEFINITION_SUFFIX = "_inputdefinition";

	public NetPyNEWriter(Lems lems) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(lems, Format.NETPYNE);
        dlemsw = new DLemsWriter(lems, null, false);
        dlemsw.setPopulationMode(true);
        dlemsw.setNeuronMode(true);
		initializeWriter();
	}

	public NetPyNEWriter(Lems lems, File outputFolder, String outputFileName) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(lems, Format.NETPYNE, outputFolder, outputFileName);
        dlemsw = new DLemsWriter(lems, outputFolder, mainDlemsFile, null, false);
        dlemsw.setPopulationMode(true);
        dlemsw.setNeuronMode(true);
		initializeWriter();
	}


    @Override
	public void setOutputFolder(File outputFolder)
	{
		super.setOutputFolder(outputFolder);
        dlemsw.setOutputFolder(outputFolder);
	}

	private void initializeWriter()
	{
		MinimalMessageHandler.setVeryMinimal(true);
		E.setDebug(false);
	}

    public void setRegenerateNeuroMLNet(boolean regenerateNeuroMLNet)
    {
        this.regenerateNeuroMLNet = regenerateNeuroMLNet;
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
		sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.HIGH);
		sli.addSupportInfo(format, ModelFeature.MULTICOMPARTMENTAL_CELL_MODEL, SupportLevelInfo.Level.HIGH);
		sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_GAP_JUNCTIONS_MODEL, SupportLevelInfo.Level.MEDIUM);
		sli.addSupportInfo(format, ModelFeature.HH_CHANNEL_MODEL, SupportLevelInfo.Level.HIGH);
		sli.addSupportInfo(format, ModelFeature.KS_CHANNEL_MODEL, SupportLevelInfo.Level.HIGH);
	}

	@Override
	protected void addComment(StringBuilder sb, String comment)
	{

		if(!comment.contains("\n")) sb.append(comm + comment + "\n");
		else sb.append(commPre + "\n" + comment + "\n" + commPost + "\n");
	}

    public void setNoGui(boolean nogui)
    {
        this.nogui = nogui;
    }

    public boolean isNoGui()
    {
        return nogui;
    }


    public List<File> generateAndRun(boolean nogui, boolean runNrn, int np, boolean json) throws LEMSException, GenerationException, NeuroMLException, IOException, ModelFeatureSupportException
    {
        List<File> files = convert();
        this.setNoGui(true);

        if(runNrn || json)
        {
            E.info("Trying to compile mods in: " + this.getOutputFolder());
            ProcessManager.compileFileWithNeuron(this.getOutputFolder(), false);


						List<String> commandToExecute = new ArrayList<String>();
						List<String> runAndOrJson = new ArrayList<String>();

						if (json)
						{
								runAndOrJson.add(" -json");
						};
						if (!runNrn) {runAndOrJson.add(" -norun");};

            if (np==1)
            {
                commandToExecute.add("python");
                commandToExecute.add(new File(this.getOutputFolder(), this.getOutputFileName()).getCanonicalPath());
								commandToExecute.addAll(runAndOrJson);
            }
            else
            {
                File neuronHome = findNeuronHome();

		            commandToExecute.add("mpiexec");
		            commandToExecute.add("-np");
		            commandToExecute.add(Integer.toString(np));
		            commandToExecute.add(neuronHome.getCanonicalPath() + System.getProperty("file.separator") + "bin" + System.getProperty("file.separator")  +"nrniv");
								commandToExecute.add("-mpi");
								commandToExecute.add(new File(this.getOutputFolder(), this.getOutputFileName()).getCanonicalPath());

								commandToExecute.addAll(runAndOrJson);
                //commandToExecute = "mpiexec -np "+np+" "+ neuronHome.getCanonicalPath() + System.getProperty("file.separator") + "bin" + System.getProperty("file.separator")  +"nrniv -mpi "
                //    + new File(this.getOutputFolder(), this.getOutputFileName()).getCanonicalPath() + runAndOrJson;
            }

            E.info("Going to execute thee command: " + commandToExecute);

            Runtime rt = Runtime.getRuntime();
						String[] commandToExecuteArr = new String[ commandToExecute.size() ];
						commandToExecute.toArray( commandToExecuteArr );
            Process currentProcess = rt.exec(commandToExecuteArr, null, this.getOutputFolder());
            ProcessOutputWatcher procOutputMain = new ProcessOutputWatcher(currentProcess.getInputStream(), "NRN Output >>");
            procOutputMain.start();

            ProcessOutputWatcher procOutputError = new ProcessOutputWatcher(currentProcess.getErrorStream(), "NRN Error  >>");
            procOutputError.start();

            E.info("Have successfully executed command: " + commandToExecute);

            try
            {
                currentProcess.waitFor();
                E.info("Exit value for running PyNEURON: " + currentProcess.exitValue());
								if (currentProcess.exitValue()!=0)
                {
                    throw new GenerationException("Exit value for running PyNEURON for NetPyNE: " + currentProcess.exitValue());
                }
            }
            catch(InterruptedException e)
            {
                E.info("Problem executing Neuron " + e);
            }

        }

        return files;
    }

	public String getMainScript() throws GenerationException, LEMSException, NeuroMLException, IOException
	{
        boolean cleanup = true;
        mainDlemsFile = getOutputFileName()+"_main.json";
        dlemsw.setOutputFileName(mainDlemsFile);
        NRNUtils nrnUtils = new NRNUtils();
        dlemsw.setUnitConverter(nrnUtils);
        dlemsw.setOnlyFlattenIfNecessary(true);
        dlemsw.setFlattenSynapses(false);

        StringBuilder mainRunScript = new StringBuilder();

        Target target = lems.getTarget();
        Component simCpt = target.getComponent();

		addComment(mainRunScript, format + " simulator compliant export for:\n\n" + lems.textSummary(false, false) + "\n\n" + Utils.getHeaderComment(format) + "\n");

        String mainNetworkFile = null;
        String onlyNmlFile = null;
        int nmlFiles = 0;
        for (String included: lems.getAllIncludedFiles())
        {
            //E.info(">>> Included: "+included);
            if (included.endsWith(".net.nml"))
            {
                if (mainNetworkFile!=null)
                {
                    throw new GenerationException("Cannot (currently) handle case where 2 or more *.net.nml files are included!");
                }
                mainNetworkFile = included;
            }
            if (included.endsWith(".h5"))
            {
                if (mainNetworkFile!=null)
                {
                    throw new GenerationException("Cannot (currently) handle case where 2 or more *.net.nml/*.h5 files are included!");
                }
                mainNetworkFile = included;
            }

            if (included.endsWith(".nml"))
            {
                nmlFiles+=1;
                if (nmlFiles==1)
                {
                    onlyNmlFile = included;
                }
                else
                {
                    onlyNmlFile = null;
                }
            }
        }
        if (onlyNmlFile!=null && mainNetworkFile==null)
        {
            // Assume the only one is the correct file;
            mainNetworkFile = onlyNmlFile;
        }
        if (mainNetworkFile==null || regenerateNeuroMLNet) {

            String nmlString = Utils.convertLemsToNeuroMLLikeXml(lems, simCpt.getStringValue("target"));

            if (getOutputFileName().indexOf("_netpyne.py")>0)
            {
                mainNetworkFile = getOutputFileName().replaceAll("_netpyne.py", ".net.nml").replaceAll("LEMS_", "NET_");
            }
            else
            {
                mainNetworkFile = "NET_"+simCpt.getRefComponents().get("target").getID()+".net.nml";
            }

            File newNet = new File(getOutputFolder(),mainNetworkFile);
            FileUtil.writeStringToFile(nmlString, newNet);
            outputFiles.add(newNet);

            E.info(">>> Written network info to: "+newNet.getAbsolutePath());

        }

		VelocityUtils.initializeVelocity();
		VelocityContext context = new VelocityContext();

		try
		{
            NeuronWriter nrnWriter = new NeuronWriter(lems,getOutputFolder(),"---");
            nrnWriter.setParallelMode(true);
            List<File> dlemsFiles = dlemsw.convert();


            for (File dlemsFile: dlemsFiles) {

                //E.info(">>> Processing DLEMS file: " + dlemsFile.getAbsolutePath());

                String dlems = FileUtil.readStringFromFile(dlemsFile);

                DLemsWriter.putIntoVelocityContext(dlems, context);

                context.internalPut("main_network_file", mainNetworkFile);

                VelocityEngine ve = VelocityUtils.getVelocityEngine();
                StringWriter sw1 = new StringWriter();

                if (dlemsFile.getName().equals(mainDlemsFile)) {
                    ve.evaluate(context, sw1, "LOG", VelocityUtils.getTemplateAsReader(VelocityUtils.netpyneRunTemplateFile));
                    mainRunScript.append(sw1);

                    if (cleanup)
                        dlemsFile.delete();
                }
                else
                {
                    StringBuilder script = new StringBuilder();
                    addComment(script, format + " simulator compliant export for:\n\n" + lems.textSummary(false, false) + "\n\n" + Utils.getHeaderComment(format) + "\n");

                    String name = (String) context.internalGet(DLemsKeywords.NAME.get());
                    Component comp = lems.components.getByID(name);
                    //E.info("Component LEMS: " + comp.summary());
                    String suffix = null;
                    String template = null;

                    if(comp.getComponentType().isOrExtends(NeuroMLElements.CELL_COMP_TYPE))
                    {
                        nrnWriter.convertCellWithMorphology(comp);
                        //suffix = ".hoc";
                        //template = VelocityUtils.neuronCellTemplateFile;
                    }
                    else if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_COMP_TYPE) || dlemsFile.getName().endsWith(".cell.json"))
                    {
                        String mod = nrnWriter.generateModFile(comp);
                        nrnWriter.saveModToFile(comp, mod);
                        //suffix = CELL_DEFINITION_SUFFIX +".py";
                        //template = VelocityUtils.netpyneCellTemplateFile;
                    }
                    else if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_POINT_CURR_COMP_TYPE)
                            || dlemsFile.getName().endsWith(".input.json")
                            || dlemsFile.getName().endsWith(".synapse.json"))
                    {
                        String mod = nrnWriter.generateModFile(comp);
                        nrnWriter.saveModToFile(comp, mod);
                        //suffix = INPUT_DEFINITION_SUFFIX +".py";
                        //template = VelocityUtils.netpyneInputNeuronTemplateFile;
                    }
                    else
                    {
                        throw new NeuroMLException("Cannot determine type of Component: "+comp.summary());
                    }


                    if (template!=null)
                    {
                        ve.evaluate(context, sw1, "LOG", VelocityUtils.getTemplateAsReader(template));
                        script.append(sw1);

                        E.info("Writing " + format + " file to: " + dlemsFile.getAbsolutePath());
                        File scriptFile = new File(this.getOutputFolder(), name + suffix);
                        FileUtil.writeStringToFile(script.toString(), scriptFile);
                        outputFiles.add(scriptFile);
                    }

                    if (cleanup)
                        dlemsFile.delete();
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
		catch(ContentError e)
		{
			throw new GenerationException("Problem using Velocity template", e);
		}
		catch(LEMSException e)
		{
			throw new GenerationException("Problem using Velocity template", e);
		}
		catch(NeuroMLException e)
		{
			throw new GenerationException("Problem using Velocity template", e);
		}

		return mainRunScript.toString();

	}

	@Override
	public List<File> convert() throws GenerationException, IOException
	{

        String code;
        try
        {
            code = this.getMainScript();
        }
        catch (LEMSException ex)
        {
            throw new GenerationException("Error on generation", ex);
        }
        catch (NeuroMLException ex)
        {
            throw new GenerationException("Error on generation", ex);
        }

        File outputFile = new File(this.getOutputFolder(), this.getOutputFileName());
        FileUtil.writeStringToFile(code, outputFile);
        outputFiles.add(outputFile);

        E.info("Saving main NetPyNE file to: " + outputFile.getAbsolutePath());

		return this.outputFiles;
	}

    public static void main(String[] args) throws Exception
    {

        ArrayList<File> lemsFiles = new ArrayList<File>();
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex0_IaF.xml"));
        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex5_DetCell.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/showcase/NetPyNEShowcase/NeuroML2/LEMS_Spikers.xml"));
        //lemsFiles.add(new File("../git/GoC_Varied_Inputs/Tests/single_cell/LEMS_sim_test_sim.xml"));
		//lemsFiles.add(new File("../neuroConstruct/osb/hippocampus/CA1_pyramidal_neuron/FergusonEtAl2014-CA1PyrCell/NeuroML2/LEMS_TwoCells.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex19a_GapJunctionInstances.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex25_MultiComp.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex19a_GapJunctionInstances.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex14_PyNN.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/IzhikevichModel/NeuroML2/LEMS_SmallNetwork.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/IzhikevichModel/NeuroML2/LEMS_FiveCells.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/IzhikevichModel/NeuroML2/LEMS_2007Cells.xml"));
        //lemsFiles.add(new File("../OpenCortex/examples/LEMS_SpikingNet.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/showcase/NetPyNEShowcase/NeuroML2/scaling/LEMS_Balanced_0.2.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/showcase/StochasticityShowcase/NeuroML2/LEMS_Inputs.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/showcase/NetPyNEShowcase/NeuroML2/LEMS_Spikers.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/showcase/ghk-nernst/NeuroML2/LEMS_nernst_na_k_ca.xml"));
        //lemsFiles.add(new File("/home/padraig/git/osb-model-validation/utilities/local_test/netpyneshowcase/NeuroML2/scaling/LEMS_Balanced.xml"));
        //lemsFiles.add(new File("/home/padraig/git/osb-model-validation/utilities/local_test/netpyneshowcase/NeuroML2/scaling/LEMS_Balanced_hdf5.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex25_MultiComp.xml"));
        /*
        lemsFiles.add(new File("../neuroConstruct/osb/showcase/NetPyNEShowcase/NeuroML2/LEMS_2007One.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/ACnet2/neuroConstruct/generatedNeuroML2/LEMS_TwoCell.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/cerebellum/cerebellar_granule_cell/GranuleCell/neuroConstruct/generatedNeuroML2/LEMS_GranuleCell.xml"));
      lemsFiles.add(new File("../OpenCortex/examples/LEMS_IClamps.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/Thalamocortical/NeuroML2/pythonScripts/netbuild/LEMS_Figure7AeLoSS.xml"));*/
        //lemsFiles.add(new File("../OpenCortex/examples/LEMS_SimpleNet.xml"));

		//lemsFiles.add(new File("../git/TestHippocampalNetworks/NeuroML2/cells/tests/LEMS_axoaxonic.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/showcase/NetPyNEShowcase/NeuroML2/chanDens/LEMS_cck.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/showcase/NetPyNEShowcase/NeuroML2/scaling/LEMS_Balanced_0.2.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/showcase/NetPyNEShowcase/NeuroML2/scaling/LEMS_Balanced.xml"));
        //lemsFiles.add(new File("../OpenCortex/examples/HDF5/LEMS_SpikingNet.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/ACnet2/neuroConstruct/generatedNeuroML2/LEMS_MediumNet.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/olfactorybulb/networks/MiglioreEtAl14_OlfactoryBulb3D/NeuroML2/Channels/test/LEMS_OlfactoryTest_12.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/generic/hodgkin_huxley_tutorial/Tutorial/Source/LEMS_HH_Simulation.xml"));
        //lemsFiles.add(new File("../git/multi/temp/LEMS_ISN_net.xml"));

        for (File lemsFile : lemsFiles)
        {
            Lems lems = Utils.readLemsNeuroMLFile(lemsFile, false).getLems();
            System.out.println("lems c"+lems.components);
            NetPyNEWriter pw = new NetPyNEWriter(lems);
            pw.setOutputFolder(lemsFile.getParentFile());
            pw.setOutputFileName(lemsFile.getName().replaceAll(".xml", "_netpyne.py"));

            //pw.setRegenerateNeuroMLNet(true);

            List<File> files = pw.generateAndRun(true, false, 1, false);
            for (File f : files)
            {
                System.out.println("Have created: " + f.getAbsolutePath());
            }



        }
    }

}
