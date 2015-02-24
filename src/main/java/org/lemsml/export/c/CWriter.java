package org.lemsml.export.c;

import static org.lemsml.jlems.io.util.JUtil.getRelativeResource;

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
import org.lemsml.export.dlems.DLemsWriter;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.logging.MinimalMessageHandler;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Format;
import org.neuroml.export.utils.support.ModelFeature;
import org.neuroml.export.utils.support.SupportLevelInfo;
import org.neuroml.model.util.NeuroMLException;

public class CWriter extends ABaseWriter
{

	private Solver solver = Solver.CVODE;

	String comm = "// ";
	String commPre = "/*";
	String commPost = "*/";

	private String outputFileName;
    private final DLemsWriter dlemsw;

	public CWriter(Lems lems) throws ModelFeatureSupportException, NeuroMLException, LEMSException
	{
		super(lems, Format.C);
        
        dlemsw = new DLemsWriter(lems, new CVisitors());
		initializeWriter();
	}

	public CWriter(Lems lems, File outputFolder, String outputFileName) throws ModelFeatureSupportException, NeuroMLException, LEMSException
	{
		super(lems, Format.C, outputFolder);
		this.outputFileName = outputFileName;
        
        dlemsw = new DLemsWriter(lems, new CVisitors());
		initializeWriter();
	}

	private void initializeWriter()
	{
		MinimalMessageHandler.setVeryMinimal(true);
		E.setDebug(false);
	}

	public enum Solver
	{

		// Default (& only supported version) is CVODE
		CVODE("cvode/cvode.vm", "cvode/Makefile");

		private final String template;
		private final String makefile;

		private Solver(String t, String m)
		{
			template = t;
			makefile = m;
		}

		public String getTemplate()
		{
			return template;
		}

		public String getMakefile()
		{
			return makefile;
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

	public Solver getSolver()
	{
		return solver;
	}

	public void setSolver(Solver solver)
	{
		this.solver = solver;
	}

	@Override
	@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
	protected void addComment(StringBuilder sb, String comment)
	{

		if(comment.indexOf("\n") < 0) sb.append(comm + comment + "\n");
		else sb.append(commPre + "\n" + comment + "\n" + commPost + "\n");
	}

	public String getMakefile() throws GenerationException, ContentError
	{

		try
		{
			String makefile = getRelativeResource(this.getClass(), "/" + solver.getMakefile());
			return makefile;
		}
		catch(ContentError ex)
		{
			throw new GenerationException("Problem finding makefile: " + solver.getMakefile(), ex);
		}
	}

	public String getMainScript() throws LEMSException, GenerationException, NeuroMLException
	{

		StringBuilder sb = new StringBuilder();

		addComment(sb, format + " simulator compliant export for:\n\n" + lems.textSummary(false, false));

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
			Template template = ve.getTemplate(solver.getTemplate());

			StringWriter sw = new StringWriter();

			template.merge(context, sw);

			sb.append(sw);
		}
		catch(IOException e1)
		{
			throw new GenerationException("Problem converting LEMS to SOM", e1);
		}
		catch(VelocityException e)
		{
			throw new GenerationException("Problem using template", e);
		}
		catch(LEMSException e)
		{
			throw new GenerationException("Problem with LEMS", e);
		}

		return sb.toString();

	}

	@Override
	public List<File> convert() throws GenerationException, IOException
	{
		List<File> outputFiles = new ArrayList<File>();

		try
		{
			String code = this.getMainScript();

			File cFile = new File(this.getOutputFolder(), this.outputFileName);
			FileUtil.writeStringToFile(code, cFile);
			outputFiles.add(cFile);

			File mFile = new File(this.getOutputFolder(), "Makefile");
			String makefile = this.getMakefile();
			FileUtil.writeStringToFile(makefile, mFile);
			outputFiles.add(mFile);

		}
		catch(LEMSException e)
		{
			throw new GenerationException("Issue when converting files", e);
		}
		catch(NeuroMLException e)
		{
			throw new GenerationException("Issue when converting files", e);
		}

		return outputFiles;
	}

}
