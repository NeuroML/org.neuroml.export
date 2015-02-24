package org.neuroml.export.dnsim;

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
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.lemsml.export.dlems.DLemsKeywords;
import org.lemsml.export.dlems.DLemsWriter;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.base.ANeuroMLBaseWriter;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Format;
import org.neuroml.export.utils.Utils;
import org.neuroml.export.utils.support.ModelFeature;
import org.neuroml.export.utils.support.SupportLevelInfo;
import org.neuroml.model.util.NeuroMLException;

@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public class DNSimWriter extends ANeuroMLBaseWriter
{

	public final String TEMPLATE_MAIN = "dnsim/dnsim.m.vm";
	public final String TEMPLATE_MODULE = "dnsim/dnsim.txt.vm";
	
	private final List<File> outputFiles = new ArrayList<File>();
	private String outputFileName;
    private final DLemsWriter dlemsw;

	public DNSimWriter(Lems lems) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(lems, Format.DN_SIM);
        dlemsw = new DLemsWriter(lems, null);
	}

	public DNSimWriter(Lems lems, File outputFolder, String outputFileName) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(lems, Format.DN_SIM, outputFolder);
		this.outputFileName = outputFileName;
        dlemsw = new DLemsWriter(lems, null);
	}

	@Override
	public void setSupportedFeatures()
	{
		sli.addSupportInfo(format, ModelFeature.ABSTRACT_CELL_MODEL, SupportLevelInfo.Level.LOW);
		sli.addSupportInfo(format, ModelFeature.COND_BASED_CELL_MODEL, SupportLevelInfo.Level.LOW);
		sli.addSupportInfo(format, ModelFeature.SINGLE_COMP_MODEL, SupportLevelInfo.Level.LOW);
		sli.addSupportInfo(format, ModelFeature.NETWORK_MODEL, SupportLevelInfo.Level.LOW);
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

		String comm = "% ";
		sb.append(comm + comment.replaceAll("\n", "\n% ") + "\n");
	}

	public String getMainScript() throws GenerationException, IOException
	{
		E.info("-Writing " + format + " files to: " + this.getOutputFolder().getAbsolutePath());

		StringBuilder mainScript = new StringBuilder();
		StringBuilder moduleScript = new StringBuilder();

		addComment(mainScript,
				format + " export from LEMS\n\nPlease note that this is a work in progress " + "and only works for a limited subset of LEMS/NeuroML 2!!\n" + Utils.getHeaderComment(format) + "\n"
						+ lems.textSummary(false, false));

		addComment(moduleScript,
				format + " export from LEMS\n\nPlease note that this is a work in progress " + "and only works for a limited subset of LEMS/NeuroML 2!!\n" + Utils.getHeaderComment(format) + "\n"
						+ lems.textSummary(false, false));

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
			Template template = ve.getTemplate(TEMPLATE_MAIN);

			StringWriter sw1 = new StringWriter();

			template.merge(context, sw1);

			mainScript.append(sw1);

			template = ve.getTemplate(TEMPLATE_MODULE);

			StringWriter sw2 = new StringWriter();

			template.merge(context, sw2);

			moduleScript.append(sw2);

			E.info("Writing " + format + " files to: " + this.getOutputFolder().getAbsolutePath());
			String name = (String) context.internalGet(DLemsKeywords.NAME.get());
			File mainScriptFile = new File(this.getOutputFolder(), name + ".m");
			File compScriptFile = new File(this.getOutputFolder(), name + "_dnsim.txt");
			FileUtil.writeStringToFile(mainScript.toString(), mainScriptFile);
			outputFiles.add(mainScriptFile);
			FileUtil.writeStringToFile(moduleScript.toString(), compScriptFile);
			outputFiles.add(compScriptFile);
		}
		catch(IOException e1)
		{
			throw new GenerationException("Problem converting LEMS to dLEMS", e1);
		}
		catch(ResourceNotFoundException e)
		{
			throw new GenerationException("Problem finding template", e);
		}
		catch(ParseErrorException e)
		{
			throw new GenerationException("Problem parsing", e);
		}
		catch(MethodInvocationException e)
		{
			throw new GenerationException("Problem finding template", e);
		}
		catch(LEMSException e)
		{
			throw new GenerationException("Problem using template", e);
		}

		return mainScript.toString();

	}

	@Override
	public List<File> convert() throws GenerationException, IOException
	{
        String code = this.getMainScript();

        File outputFile = new File(this.getOutputFolder(), this.outputFileName);
        FileUtil.writeStringToFile(code, outputFile);
        outputFiles.add(outputFile);
		
		return this.outputFiles;
	}

}
