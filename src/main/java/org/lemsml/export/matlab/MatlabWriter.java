package org.lemsml.export.matlab;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.lemsml.export.base.GenerationException;
import org.lemsml.export.dlems.DLemsWriter;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.logging.MinimalMessageHandler;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.base.BaseWriter;

public class MatlabWriter extends BaseWriter {
	
	public enum Method {
        // Default (& only supported version) is matlab_ode.vm
		ODE("matlab/matlab_ode.vm"), 
		EULER("matlab/matlab_euler.vm");
		
	 private String filename;
	 
	 private Method(String f) {
		 filename = f;
	 }
	 
	 public String getFilename() {
	   return filename;
	 }};
	
	private Method method = Method.ODE;

	public MatlabWriter(Lems lems) {
		super(lems, "MATLAB");
		MinimalMessageHandler.setVeryMinimal(true);
		E.setDebug(false);
	}

	String comm = "% ";
	String commPre = "%{";
	String commPost = "%}";
	
	@Override
	protected void addComment(StringBuilder sb, String comment) {
	
		if (comment.indexOf("\n") < 0)
			sb.append(comm + comment + "\n");
		else
			sb.append(commPre + "\n" + comment + "\n" + commPost + "\n");
	}
	
	public void setMethod(Method method){
		this.method = method;
	}

	@Override
	public String getMainScript() throws GenerationException {

		StringBuilder sb = new StringBuilder();

		addComment(sb, this.format+" simulator compliant export for:\n\n"
		+ lems.textSummary(false, false));
		
		Velocity.init();
		
		VelocityContext context = new VelocityContext();

		//context.put( "name", new String("VelocityOnOSB") );

        DLemsWriter somw = new DLemsWriter(lems, new MatlabVisitors());

		try
		{
			String som = somw.getMainScript();
			
			DLemsWriter.putIntoVelocityContext(som, context);
        
			Properties props = new Properties();
			props.put("resource.loader", "class");
			props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
			VelocityEngine ve = new VelocityEngine();
			ve.init(props);
			Template template = ve.getTemplate(method.getFilename());
		   
			StringWriter sw = new StringWriter();

			template.merge( context, sw );
			
			sb.append(sw);
		} 
		catch (IOException e1) {
			throw new GenerationException("Problem converting LEMS to SOM",e1);
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
		
		return sb.toString();	

	}

}
