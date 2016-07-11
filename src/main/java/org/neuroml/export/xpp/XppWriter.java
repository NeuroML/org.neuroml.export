package org.neuroml.export.xpp;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.lemsml.export.dlems.DLemsWriter;
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
public class XppWriter extends ANeuroMLBaseWriter
{

	public HashMap<String, String> keywordSubstitutions = new HashMap<String, String>();

    private final DLemsWriter dlemsw;

	public XppWriter(Lems lems) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(lems, Format.XPP);
        dlemsw = new DLemsWriter(lems, null);
		keywordSubstitutions.put("compartment", "compart");
	}

	public XppWriter(Lems lems, File outputFolder, String outputFileName) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(lems, Format.XPP, outputFolder, outputFileName);
        dlemsw = new DLemsWriter(lems, null);
		keywordSubstitutions.put("compartment", "compart");
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

		String comm = "# ";
		sb.append(comm + comment.replaceAll("\n", "\n# ") + "\n");
	}

	public String getMainScript() throws GenerationException
	{
		StringBuilder sb = new StringBuilder();

		addComment(sb, format + " export from LEMS\n\nPlease note that this is a work in progress " + "and only works for a limited subset of LEMS/NeuroML 2!!\n");

		addComment(sb, Utils.getHeaderComment(format) + "\n");

		addComment(sb, lems.textSummary(false, false));

		VelocityUtils.initializeVelocity();
		VelocityContext context = new VelocityContext();

		try
		{
            
			String dlems = dlemsw.getMainScript();

			DLemsWriter.putIntoVelocityContext(dlems, context);

			VelocityEngine ve = VelocityUtils.getVelocityEngine();

			StringWriter sw = new StringWriter();
			boolean generationStatus = ve.evaluate(context, sw, "LOG", VelocityUtils.getTemplateAsReader(VelocityUtils.xppTemplateFile));
			String mapped = sw.toString();
			for(String old : keywordSubstitutions.keySet())
			{
				String new_ = keywordSubstitutions.get(old);
				mapped = Utils.replaceInExpression(mapped, old, new_);
			}

			sb.append(mapped);
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

		return sb.toString();

	}

	@Override
	public List<File> convert() throws GenerationException, IOException
	{
		List<File> outputFiles = new ArrayList<File>();

        String code = this.getMainScript();

        File outputFile = new File(this.getOutputFolder(), this.getOutputFileName());
        FileUtil.writeStringToFile(code, outputFile);
        outputFiles.add(outputFile);


		return outputFiles;
	}

}
