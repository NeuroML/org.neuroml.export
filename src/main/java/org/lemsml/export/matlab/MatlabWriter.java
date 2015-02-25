package org.lemsml.export.matlab;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.lemsml.export.base.ABaseWriter;
import org.lemsml.export.dlems.DLemsWriter;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.logging.MinimalMessageHandler;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Format;
import org.neuroml.export.utils.VelocityUtils;
import org.neuroml.export.utils.support.ModelFeature;
import org.neuroml.export.utils.support.SupportLevelInfo;
import org.neuroml.model.util.NeuroMLException;

public class MatlabWriter extends ABaseWriter
{

	private Method method = Method.ODE;

	String comm = "% ";
	String commPre = "%{";
	String commPost = "%}";
	
	public MatlabWriter(Lems lems) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(lems, Format.MATLAB);
		initializeWriter();
	}
	
	public MatlabWriter(Lems lems, File outputFolder, String outputFileName) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(lems, Format.MATLAB, outputFolder, outputFileName);
		initializeWriter();
	}

	private void initializeWriter()
	{
		MinimalMessageHandler.setVeryMinimal(true);
		E.setDebug(false);
	}

	public enum Method
	{
		// Default (& only supported version) is matlab_ode.vm
		ODE(VelocityUtils.matlabOdeFile), EULER(VelocityUtils.matlabEulerFile);

		private String filename;

		private Method(String f)
		{
			filename = f;
		}

		public String getFilename()
		{
			return filename;
		}
	};

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

		if(comment.indexOf("\n") < 0) sb.append(comm + comment + "\n");
		else sb.append(commPre + "\n" + comment + "\n" + commPost + "\n");
	}

	public void setMethod(Method method)
	{
		this.method = method;
	}

	public String getMainScript() throws GenerationException
	{

		StringBuilder sb = new StringBuilder();

		addComment(sb, format + " simulator compliant export for:\n\n" + lems.textSummary(false, false));

		VelocityUtils.initializeVelocity();

		VelocityContext context = new VelocityContext();

		try
		{
            DLemsWriter writer = new DLemsWriter(lems, new MatlabVisitors());
            
			String som = writer.getMainScript();

			DLemsWriter.putIntoVelocityContext(som, context);

			VelocityEngine ve = VelocityUtils.getVelocityEngine();
			
			StringWriter sw = new StringWriter();
			boolean generationStatus = ve.evaluate(context, sw, "LOG", VelocityUtils.getTemplateAsReader(method.getFilename()));

			sb.append(sw);
		}
		catch(IOException e1)
		{
			throw new GenerationException("Problem converting LEMS to SOM", e1);
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
		catch(Exception e)
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


		// TODO Auto-generated method stub
		return outputFiles;
	}

}
