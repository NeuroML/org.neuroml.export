package org.neuroml.export.matlab;

import java.io.File;
import java.io.IOException;

import org.lemsml.export.som.SOMWriter;
import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.run.ConnectionError;
import org.lemsml.jlems.core.run.RuntimeError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.ParseException;
import org.lemsml.jlems.core.type.BuildException;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.xml.XMLException;
import org.lemsml.jlems.io.util.FileUtil;
import org.lemsml.jlems.io.util.JUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.Utils;

import junit.framework.TestCase;

import java.io.StringWriter;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.MethodInvocationException;

public class MatlabWriterTest extends TestCase {

	
	public void testVelocity() throws Exception {
		Velocity.init();
		
		VelocityContext context = new VelocityContext();
		

		context.put( "name", new String("VelocityOnOSB") );

		Template template = null;

		try
		{
		   template = Velocity.getTemplate("./src/test/resources/mytemplate.vm");
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
		
		System.out.println(sw);
	}
	
	
	public void testFN() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError {

    	String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
    	//generateMainScript(exampleFilename);
	}
	/*
	public void testHH() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError {

    	String exampleFilename = "LEMS_NML2_Ex1_HH.xml";
    	generateMainScript(exampleFilename);
	}*/
	
	public void generateMainScript(String exampleFilename) throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError {


    	Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);
        
        System.out.println("Loaded: "+exampleFilename);

        SOMWriter mw = new SOMWriter(lems);

        String mat = mw.getMainScript();


        File mFile = new File(AppTest.getTempDir(),exampleFilename.replaceAll(".xml", ".m"));
        System.out.println("Writing to: "+mFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(mat, mFile);

        
        assertTrue(mFile.exists());
	}

}
