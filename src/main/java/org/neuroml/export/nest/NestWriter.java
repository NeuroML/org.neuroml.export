package org.neuroml.export.nest;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.lemsml.export.base.GenerationException;
import org.lemsml.export.dlems.DLemsKeywords;
import org.lemsml.export.dlems.DLemsWriter;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.logging.MinimalMessageHandler;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.base.BaseWriter;


@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public class NestWriter extends BaseWriter {

	private final String runTemplateFile = "nest/run.vm";
	private final String cellTemplateFile = "nest/cell.vm";

	String comm = "'''";
	String commPre = "#";
	String commPost = "'''";

    public ArrayList<File> allGeneratedFiles = new ArrayList<File>();

	public NestWriter(Lems lems) {
		super(lems, "NEST");
		MinimalMessageHandler.setVeryMinimal(true);
		E.setDebug(false);
	}
	
	@Override
	protected void addComment(StringBuilder sb, String comment) {
	
		if (comment.indexOf("\n") < 0)
			sb.append(comm + comment + "\n");
		else
			sb.append(commPre + "\n" + comment + "\n" + commPost + "\n");
	}
	

	@Override
	public String getMainScript() throws GenerationException {
		return generateMainScriptAndCellFiles(null);
	}
		
	public String generateMainScriptAndCellFiles(File dirForFiles) throws GenerationException {

		StringBuilder mainRunScript = new StringBuilder();
		StringBuilder cellScript = new StringBuilder();

		addComment(mainRunScript, this.format+" simulator compliant export for:\n\n"
		+ lems.textSummary(false, false));
		
		addComment(cellScript, this.format+" simulator compliant export for:\n\n"
		+ lems.textSummary(false, false));
		
		Velocity.init();
		
		VelocityContext context = new VelocityContext();

        DLemsWriter somw = new DLemsWriter(lems);

		try
		{
			String som = somw.getMainScript();
			
			DLemsWriter.putIntoVelocityContext(som, context);
        
			Properties props = new Properties();
			props.put("resource.loader", "class");
			props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
			VelocityEngine ve = new VelocityEngine();
			ve.init(props);
			Template template = ve.getTemplate(runTemplateFile);
		   
			StringWriter sw1 = new StringWriter();

			template.merge( context, sw1 );
			
			mainRunScript.append(sw1);
			
			template = ve.getTemplate(cellTemplateFile);

			StringWriter sw2 = new StringWriter();

			template.merge( context, sw2 );
			
			cellScript.append(sw2);
			
			if (dirForFiles!=null && dirForFiles.exists())
			{
				E.info("Writing "+format+" files to: "+dirForFiles);
				String name = (String)context.internalGet(DLemsKeywords.NAME.get());
				File mainScriptFile = new File(dirForFiles, "run_"+name+"_nest.py");
				File cellScriptFile = new File(dirForFiles, name+"_nest.py");
	            FileUtil.writeStringToFile(mainRunScript.toString(), mainScriptFile);
	            allGeneratedFiles.add(mainScriptFile);
	            FileUtil.writeStringToFile(cellScript.toString(), cellScriptFile);
	            allGeneratedFiles.add(cellScriptFile);
			}
			else
			{
				E.info("Not writing "+format+" scripts to files! Problem with target dir: "+dirForFiles);
			}
			
			
		} 
		catch (IOException e1) {
			throw new GenerationException("Problem converting LEMS to "+format,e1);
		}
		catch( ResourceNotFoundException e )
		{
			throw new GenerationException("Problem finding template",e);
		}
		catch( ParseErrorException e )
		{
			throw new GenerationException("Problem parsing",e);
		}
		catch( MethodInvocationException e )
		{
			throw new GenerationException("Problem finding template",e);
		}
		catch( Exception e )
		{
			throw new GenerationException("Problem using template",e);
		}
		
		return mainRunScript.toString();	

	}

}