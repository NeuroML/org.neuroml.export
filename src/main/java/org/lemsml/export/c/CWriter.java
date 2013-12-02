package org.lemsml.export.c;

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

public class CWriter extends BaseWriter {
	
    public enum Solver {

        // Default (& only supported version) is CVODE
        CVODE("cvode/cvode.vm", "cvode/Makefile");

        private String template;
        private String makefile;

        private Solver(String t, String m) {
            template = t;
            makefile = m;
        }

        public String getTemplate() {
            return template;
        }

        public String getMakefile() {
            return makefile;
        }

    };
	
	private Solver solver = Solver.CVODE;

	public CWriter(Lems lems) {
		super(lems, "C");
		MinimalMessageHandler.setVeryMinimal(true);
		E.setDebug(false);
	}

	String comm = "// ";
	String commPre = "/*";
	String commPost = "*/";
	
	@Override
	protected void addComment(StringBuilder sb, String comment) {
	
		if (comment.indexOf("\n") < 0)
			sb.append(comm + comment + "\n");
		else
			sb.append(commPre + "\n" + comment + "\n" + commPost + "\n");
	}
	
	public void setSolver(Solver solver){
		this.solver = solver;
	}

    public Solver getSolver() {
        return solver;
    }
    
    

	@Override
	public String getMainScript() throws GenerationException {

		StringBuilder sb = new StringBuilder();

		addComment(sb, this.format+" simulator compliant export for:\n\n"
		+ lems.textSummary(false, false));
		
		Velocity.init();
		
		VelocityContext context = new VelocityContext();

		//context.put( "name", new String("VelocityOnOSB") );

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
			Template template = ve.getTemplate(solver.getTemplate());
		   
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
