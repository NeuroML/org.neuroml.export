package org.lemsml.export.matlab;

import java.io.StringWriter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.base.BaseWriter;

public class MatlabWriter extends BaseWriter {
	

	public MatlabWriter(Lems lems) {
		super(lems, "MATLAB");
	}

	String comm = "% ";
	String commPre = "%{";
	String commPost = "}%";
	
	@Override
	protected void addComment(StringBuilder sb, String comment) {
	
		if (comment.indexOf("\n") < 0)
			sb.append(comm + comment + "\n");
		else
			sb.append(commPre + "\n" + comment + "\n" + commPost + "\n");
	}

	@Override
	public String getMainScript() throws ContentError, ParseError {

		StringBuilder sb = new StringBuilder();

		addComment(sb, this.format+" simulator compliant export for:\n\n"
		+ lems.textSummary(false, false));
		
		Velocity.init();
		
		VelocityContext context = new VelocityContext();
		

		context.put( "name", new String("VelocityOnOSB") );

		Template template = null;

		try
		{
		   template = Velocity.getTemplate("./src/main/resources/matlab/matlab_euler.vm");
		}
		catch( ResourceNotFoundException rnfe )
		{
		   // couldn't find the template
		}
		catch( ParseErrorException pee )
		{
		  // syntax error: problem parsing the template
		}
		catch( MethodInvocationException mie )
		{
		  // something invoked in the template
		  // threw an exception
		}
		catch( Exception e )
		{}

		StringWriter sw = new StringWriter();

		template.merge( context, sw );
		
		sb.append(sw);
		
		return sb.toString();	

	}

}
