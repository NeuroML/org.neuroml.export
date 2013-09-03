package org.lemsml.export.sedml;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

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
import org.neuroml.model.util.NeuroML2Validator;
import org.xml.sax.SAXException;

public class SEDMLWriterTest extends TestCase {

	public void testGetMainScript() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, SAXException, ConnectionError, RuntimeError {

    	String exampleFilename = "LEMS_NML2_Ex9_FN.xml";    
    	
    	Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);

		SEDMLWriter sedmlw = new SEDMLWriter(lems, exampleFilename, SEDMLWriter.ModelFormat.SBML);
        String sedml = sedmlw.getMainScript();

        //System.out.println(sedmlw);

        File sedmlFile = new File(AppTest.getTempDir(),exampleFilename.replaceAll("xml", "sedml"));
        System.out.println("Writing file to: "+sedmlFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(sedml, sedmlFile);

        assertTrue(sedmlFile.exists());
        
        NeuroML2Validator.testValidity(sedmlFile, SEDMLWriter.LOCAL_SEDML_SCHEMA);
        System.out.println("File is valid SED-ML");
	}


}
