package org.neuroml.export.sedml;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.lemsml.jlems.expression.ParseError;
import org.lemsml.jlems.sim.ContentError;
import org.lemsml.jlems.sim.ParseException;
import org.lemsml.jlems.type.BuildException;
import org.lemsml.jlems.type.Lems;
import org.lemsml.jlems.xml.XMLException;
import org.lemsml.jlemsio.util.FileUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.Utils;

public class SEDMLWriterTest extends TestCase {

	public void testGetMainScript() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException {

    	String exampleFilename = "LEMS_NML2_Ex9_FN.xml";    	
    	
        File exampleFile = new File(AppTest.getLemsExamplesDir(), exampleFilename);

        System.out.println("Generating SED-ML from: "+exampleFile);
        
		Lems lems = Utils.loadLemsFile(exampleFile);

		SEDMLWriter sedmlw = new SEDMLWriter(lems, exampleFilename, SEDMLWriter.ModelFormat.SBML);
        String sedml = sedmlw.getMainScript();

        //System.out.println(sedmlw);

        File sedmlFile = new File(AppTest.getTempDir(),exampleFilename.replaceAll("xml", "sedml"));
        System.out.println("Writing file to: "+sedmlFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(sedml, sedmlFile);

        assertTrue(sedmlFile.exists());
	}


}
