package org.lemsml.export.modelica;

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
import org.lemsml.export.som.SOMKeywords;
import org.lemsml.export.som.SOMWriter;
import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.logging.MinimalMessageHandler;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.base.BaseWriter;

public class ModelicaWriter extends BaseWriter {

	private String classTemplateFile = "modelica/main_class.vm";
	private String runTemplateFile = "modelica/run.vm";

	String comm = "// ";
	String commPre = "/*";
	String commPost = "*/";

    public ArrayList<File> allGeneratedFiles = new ArrayList<File>();

	public ModelicaWriter(Lems lems) {
		super(lems, "Modelica");
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
	public String getMainScript() throws ContentError, ParseError {
		return generateMainScriptAndCompFiles(null);
	}
		
	public String generateMainScriptAndCompFiles(File dirForFiles) throws ContentError, ParseError {

		StringBuilder mainRunScript = new StringBuilder();
		StringBuilder compScript = new StringBuilder();

		addComment(mainRunScript, this.format+" simulator compliant export for:\n\n"
		+ lems.textSummary(false, false));
		
		addComment(compScript, this.format+" simulator compliant export for:\n\n"
		+ lems.textSummary(false, false));
		
		Velocity.init();
		
		VelocityContext context = new VelocityContext();

        SOMWriter somw = new SOMWriter(lems);

		try
		{
			String som = somw.getMainScript();
			
			SOMWriter.putIntoVelocityContext(som, context);
        
			Properties props = new Properties();
			props.put("resource.loader", "class");
			props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
			VelocityEngine ve = new VelocityEngine();
			ve.init(props);
			Template template = ve.getTemplate(runTemplateFile);
		   
			StringWriter sw1 = new StringWriter();

			template.merge( context, sw1 );
			
			mainRunScript.append(sw1);
			
			template = ve.getTemplate(classTemplateFile);

			StringWriter sw2 = new StringWriter();

			template.merge( context, sw2 );
			
			compScript.append(sw2);
			
			if (dirForFiles!=null && dirForFiles.exists())
			{
				E.info("Writing Modelica files to: "+dirForFiles);
				String name = (String)context.internalGet(SOMKeywords.NAME.get());
				File mainScriptFile = new File(dirForFiles, "run_"+name+".mos");
				File compScriptFile = new File(dirForFiles, name+".mo");
	            FileUtil.writeStringToFile(mainRunScript.toString(), mainScriptFile);
	            allGeneratedFiles.add(mainScriptFile);
	            FileUtil.writeStringToFile(compScript.toString(), compScriptFile);
	            allGeneratedFiles.add(compScriptFile);
			}
			else
			{
				E.info("Not writing Modelica scripts to files! Problem with target dir: "+dirForFiles);
			}
			
			
		} 
		catch (IOException e1) {
			throw new ParseError("Problem converting LEMS to SOM",e1);
		}
		catch( ResourceNotFoundException e )
		{
			throw new ParseError("Problem finding template",e);
		}
		catch( ParseErrorException e )
		{
			throw new ParseError("Problem parsing",e);
		}
		catch( MethodInvocationException e )
		{
			throw new ParseError("Problem finding template",e);
		}
		catch( Exception e )
		{
			throw new ParseError("Problem using template",e);
		}
		
		return mainRunScript.toString();	

	}

}