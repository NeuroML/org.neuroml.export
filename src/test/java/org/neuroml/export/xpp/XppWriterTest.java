package org.neuroml.export.xpp;

import java.io.File;
import java.io.IOException;

import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.run.ConnectionError;
import org.lemsml.jlems.core.run.RuntimeError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.ParseException;
import org.lemsml.jlems.core.type.BuildException;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.xml.XMLException;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.AppTest;

import junit.framework.TestCase;
import org.lemsml.export.base.GenerationException;
import org.neuroml.export.Utils;

public class XppWriterTest extends TestCase {

    
	public void testFN() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, GenerationException {

    	String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
    	generateMainScript(exampleFilename);
	}
    
    public void testSBML() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, GenerationException {

    	File exampleSBML = new File("src/test/resources/BIOMD0000000185_LEMS.xml");
    	generateMainScript(exampleSBML);
	}
	
	public void generateMainScript(File localFile) throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, GenerationException {

    	Lems lems = Utils.readLemsNeuroMLFile(FileUtil.readStringFromFile(localFile)).getLems();
        
        System.out.println("Loaded from: "+localFile);
        
		XppWriter xppw = new XppWriter(lems);
        String ode = xppw.getMainScript();

        File odeFile = new File(AppTest.getTempDir(),localFile.getName().replaceAll("xml", "ode"));
        System.out.println("Writing to: "+odeFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(ode, odeFile);

        
        assertTrue(odeFile.exists());
	}
    
	public void generateMainScript(String exampleFilename) throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, GenerationException {
        
    	Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);

		XppWriter xppw = new XppWriter(lems);
        String ode = xppw.getMainScript();

        File odeFile = new File(AppTest.getTempDir(),exampleFilename.replaceAll("xml", "ode"));
        System.out.println("Writing to: "+odeFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(ode, odeFile);

        
        assertTrue(odeFile.exists());
	}

}
