package org.neuroml.export.dnsim;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.neuroml.export.exception.GenerationException;
import org.neuroml.export.exception.ModelFeatureSupportException;
import org.neuroml.export.utils.Utils;
import org.neuroml.export.utils.support.ModelFeature;
import org.neuroml.export.utils.support.SupportLevelInfo;
import org.neuroml.model.util.NeuroMLException;

@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public class DNSimWriter extends ANeuroMLBaseWriter
{

	public final String TEMPLATE_MAIN = "dnsim/dnsim.m.vm";
	public final String TEMPLATE_MODULE = "dnsim/dnsim.txt.vm";

	public DNSimWriter(Lems lems) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(lems, "DNSim");
		sli.checkConversionSupported(format, lems);
	}

	@Override
	protected void setSupportedFeatures()
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
		ArrayList<File> files = generateMainScriptAndModules(null);
		return FileUtil.readStringFromFile(files.get(0));
	}

	public ArrayList<File> generateMainScriptAndModules(File dirForFiles) throws GenerationException
	{

		ArrayList<File> allGeneratedFiles = new ArrayList<File>();

		E.info("-Writing " + format + " files to: " + dirForFiles.getAbsolutePath());

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

		DLemsWriter dlemsw = new DLemsWriter(lems);

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

			if(dirForFiles != null && dirForFiles.exists())
			{
				E.info("Writing " + format + " files to: " + dirForFiles.getAbsolutePath());
				String name = (String) context.internalGet(DLemsKeywords.NAME.get());
				File mainScriptFile = new File(dirForFiles, name + ".m");
				File compScriptFile = new File(dirForFiles, name + "_dnsim.txt");
				FileUtil.writeStringToFile(mainScript.toString(), mainScriptFile);
				allGeneratedFiles.add(mainScriptFile);
				FileUtil.writeStringToFile(moduleScript.toString(), compScriptFile);
				allGeneratedFiles.add(compScriptFile);
			}
			else
			{
				throw new GenerationException("Not writing Modelica scripts to files! Problem with target dir: " + dirForFiles);
			}
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

		return allGeneratedFiles;

	}

	@Override
	public List<File> convert(Lems lems)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
