package org.neuroml.export.sbml;

import java.io.File;
import java.io.IOException;

import org.lemsml.jlems.expression.ParseError;
import org.lemsml.jlems.sim.ContentError;
import org.lemsml.jlems.sim.ParseException;
import org.lemsml.jlems.type.BuildException;
import org.lemsml.jlems.type.Lems;
import org.lemsml.jlems.xml.XMLException;
import org.lemsml.jlemsio.util.FileUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.Utils;
import org.xml.sax.SAXException;

import junit.framework.TestCase;

public class SBMLWriterTest extends TestCase {

	public void testGetMainScript() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, SAXException {

        //Note: only works with this example at the moment!!

    	String exampleFilename = "LEMS_NML2_Ex9_FN.xml"; 
    	
        File exampleFile = new File(AppTest.getLemsExamplesDir(), exampleFilename);
        
		Lems lems = Utils.loadLemsFile(exampleFile);

        SBMLWriter sbmlw = new SBMLWriter(lems);
        String sbml = sbmlw.getMainScript();

        //System.out.println(sbml);

        File sbmlFile = new File(AppTest.getTempDir(),exampleFilename.replaceAll("xml", "sbml"));
        System.out.println("Writing file to: "+sbmlFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(sbml, sbmlFile);
        
        assertTrue(sbmlFile.exists());
        
        Utils.testValidity(sbmlFile, SBMLWriter.PREF_SBML_SCHEMA);
	}

}
