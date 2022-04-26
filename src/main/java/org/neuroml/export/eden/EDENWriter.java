package org.neuroml.export.eden;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.logging.MinimalMessageHandler;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.Target;
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
public class EDENWriter extends ANeuroMLBaseWriter
{
	String comm = "#";
	String commPre = "'''";
	String commPost = "'''";

	boolean nogui = true;

	private final List<File> outputFiles = new ArrayList<File>();
	private File lemsFile = null;

	public EDENWriter(Lems lems, File lemsFile) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(lems, Format.EDEN);
		this.lemsFile = lemsFile;
		initializeWriter();
	}

	public EDENWriter(Lems lems, File lemsFile, File outputFolder, String outputFileName) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(lems, Format.EDEN, outputFolder, outputFileName);
		this.lemsFile = lemsFile;
		initializeWriter();
	}


  @Override
	public void setOutputFolder(File outputFolder)
	{
		super.setOutputFolder(outputFolder);
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
		sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.HIGH);
		sli.addSupportInfo(format, ModelFeature.MULTICOMPARTMENTAL_CELL_MODEL, SupportLevelInfo.Level.HIGH);
		sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_GAP_JUNCTIONS_MODEL, SupportLevelInfo.Level.HIGH);
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


	public String getMainScript() throws GenerationException, LEMSException, NeuroMLException, IOException
	{

		StringBuilder mainRunScript = new StringBuilder();

		Target target = lems.getTarget();
		Component simCpt = target.getComponent();

		addComment(mainRunScript, format + " simulator compliant export for:\n\n" + lems.textSummary(false, false) + "\n\n" + Utils.getHeaderComment(format) + "\n");

		VelocityUtils.initializeVelocity();
		VelocityContext context = new VelocityContext();

		try
		{


			context.internalPut("main_lems_file", lemsFile.getAbsolutePath());

			VelocityEngine ve = VelocityUtils.getVelocityEngine();
			StringWriter sw1 = new StringWriter();

			ve.evaluate(context, sw1, "LOG", VelocityUtils.getTemplateAsReader(VelocityUtils.edenRunTemplateFile));
			mainRunScript.append(sw1);

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

		E.info("Saving main EDEN file to: " + outputFile.getAbsolutePath());

		return this.outputFiles;
	}

    public static void main(String[] args) throws Exception
    {

				ArrayList<File> lemsFiles = new ArrayList<File>();
				//lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex0_IaF.xml"));
				//lemsFiles.add(new File("../neuroConstruct/osb/showcase/NetPyNEShowcase/NeuroML2/LEMS_Spikers.xml"));
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
				lemsFiles.add(new File("../OpenCortex/examples/LEMS_SimpleNet.xml"));
				lemsFiles.add(new File("../OpenCortex/examples/LEMS_IClamps.xml"));
				lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/Thalamocortical/NeuroML2/pythonScripts/netbuild/LEMS_Figure7AeLoSS.xml"));*/

				//lemsFiles.add(new File("../git/TestHippocampalNetworks/NeuroML2/cells/tests/LEMS_axoaxonic.xml"));
				//lemsFiles.add(new File("../neuroConstruct/osb/showcase/NetPyNEShowcase/NeuroML2/chanDens/LEMS_cck.xml"));
				//lemsFiles.add(new File("../neuroConstruct/osb/showcase/NetPyNEShowcase/NeuroML2/scaling/LEMS_Balanced_0.2.xml"));
				//lemsFiles.add(new File("../neuroConstruct/osb/showcase/NetPyNEShowcase/NeuroML2/scaling/LEMS_Balanced.xml"));
				//lemsFiles.add(new File("../OpenCortex/examples/HDF5/LEMS_SpikingNet.xml"));
				//lemsFiles.add(new File("../neuroConstruct/osb/generic/hodgkin_huxley_tutorial/Tutorial2/NeuroML2/LEMS_HHTutorial.xml"));
				lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex5_DetCell.xml"));
				//lemsFiles.add(new File("../git/osb-model-validation/utilities/tests/LEMS_NML2_Ex5_DetCell.xml"));
				//lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/neocortical_pyramidal_neuron/MainenEtAl_PyramidalCell/neuroConstruct/generatedNeuroML2/LEMS_OneComp.xml"));
				//lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/multiple/PospischilEtAl2008/NeuroML2/cells/FS/LEMS_FS.xml"));
				//lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/ACnet2/neuroConstruct/generatedNeuroML2/LEMS_ACnet2.xml"));
				//lemsFiles.add(new File("../neuroConstruct/osb/generic/hodgkin_huxley_tutorial/Tutorial/Source/LEMS_HH_Simulation.xml"));

				for (File lemsFile : lemsFiles)
				{
				    Lems lems = Utils.readLemsNeuroMLFile(lemsFile, false).getLems();
				    System.out.println("lems c"+lems.components);
				    EDENWriter pw = new EDENWriter(lems, lemsFile);
				    pw.setOutputFolder(lemsFile.getParentFile());
				    pw.setOutputFileName(lemsFile.getName().replaceAll(".xml", "_eden.py"));

				    List<File> files = pw.convert();
				    for (File f : files)
				    {
				        System.out.println("Have created: " + f.getAbsolutePath());
				    }

				}
    }

}
