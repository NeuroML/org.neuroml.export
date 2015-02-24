package org.lemsml.export.modelica;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.lemsml.export.base.ABaseWriter;
import org.lemsml.export.dlems.DLemsKeywords;
import org.lemsml.export.dlems.DLemsWriter;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.logging.MinimalMessageHandler;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Format;
import org.neuroml.export.utils.support.ModelFeature;
import org.neuroml.export.utils.support.SupportLevelInfo;
import org.neuroml.model.util.NeuroMLException;

@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public class ModelicaWriter extends ABaseWriter
{

	private final String classTemplateFile = "modelica/main_class.vm";
	private final String runTemplateFile = "modelica/run.vm";

	String comm = "// ";
	String commPre = "/*";
	String commPost = "*/";

	private List<File> outputFiles = new ArrayList<File>();
    private final DLemsWriter dlemsw;

	public ModelicaWriter(Lems lems) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(lems, Format.MODELICA);
        dlemsw = new DLemsWriter(lems, null);
		initializeWriter();
	}

	public ModelicaWriter(Lems lems, File outputFolder, String outputFileName) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(lems, Format.MODELICA, outputFolder, outputFileName);
        dlemsw = new DLemsWriter(lems, null);
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
		sli.addSupportInfo(format, ModelFeature.SINGLE_COMP_MODEL, SupportLevelInfo.Level.MEDIUM);
		sli.addSupportInfo(format, ModelFeature.NETWORK_MODEL, SupportLevelInfo.Level.LOW);
		sli.addSupportInfo(format, ModelFeature.MULTI_CELL_MODEL, SupportLevelInfo.Level.NONE);
		sli.addSupportInfo(format, ModelFeature.MULTI_POPULATION_MODEL, SupportLevelInfo.Level.NONE);
		sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_INPUTS_MODEL, SupportLevelInfo.Level.NONE);
		sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.NONE);
		sli.addSupportInfo(format, ModelFeature.MULTICOMPARTMENTAL_CELL_MODEL, SupportLevelInfo.Level.NONE);
		sli.addSupportInfo(format, ModelFeature.HH_CHANNEL_MODEL, SupportLevelInfo.Level.LOW);
		sli.addSupportInfo(format, ModelFeature.KS_CHANNEL_MODEL, SupportLevelInfo.Level.NONE);
	}

	@Override
	protected void addComment(StringBuilder sb, String comment)
	{

		if(!comment.contains("\n")) sb.append(comm + comment + "\n");
		else sb.append(commPre + "\n" + comment + "\n" + commPost + "\n");
	}

	@Override
	public List<File> convert() throws GenerationException, IOException
	{
		
		StringBuilder mainRunScript = new StringBuilder();
		StringBuilder compScript = new StringBuilder();

		addComment(mainRunScript, format + " simulator compliant export for:\n\n" + lems.textSummary(false, false));
		addComment(compScript, format + " simulator compliant export for:\n\n" + lems.textSummary(false, false));

		Velocity.init();
		VelocityContext context = new VelocityContext();

		try
		{

			String dlems = dlemsw.getMainScript();

			DLemsWriter.putIntoVelocityContext(dlems, context);

			Properties props = new Properties();
			props.put("resource.loader", "class");
			props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
			VelocityEngine ve = new VelocityEngine();
			ve.init(props);
			Template template = ve.getTemplate(runTemplateFile);

			StringWriter sw1 = new StringWriter();

			template.merge(context, sw1);

			mainRunScript.append(sw1);

			template = ve.getTemplate(classTemplateFile);

			StringWriter sw2 = new StringWriter();

			template.merge(context, sw2);

			compScript.append(sw2);

			E.info("Writing Modelica files to: " + this.getOutputFolder());
			String name = (String) context.internalGet(DLemsKeywords.NAME.get());
			File mainScriptFile = new File(this.getOutputFolder(), this.getOutputFileName());
			File compScriptFile = new File(this.getOutputFolder(), name + ".mo");
			FileUtil.writeStringToFile(mainRunScript.toString(), mainScriptFile);
			this.outputFiles.add(mainScriptFile);
			FileUtil.writeStringToFile(compScript.toString(), compScriptFile);
			this.outputFiles.add(compScriptFile);

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


		
		return this.outputFiles;
	}

}