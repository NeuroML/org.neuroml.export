package org.neuroml.export.sbml;

import java.io.File;
import java.io.IOException;

import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.ParseException;
import org.lemsml.jlems.core.type.BuildException;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.core.xml.XMLException;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.Utils;
import org.xml.sax.SAXException;

import junit.framework.TestCase;

public class SBMLWriterTest extends TestCase {
	

	public void testGetMainScript1() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, SAXException {

    	String exampleFilename = "LEMS_NML2_Ex9_FN.xml"; 
        File exampleFile = new File(AppTest.getLemsExamplesDir(), exampleFilename);
        generateSBMLAndTestScript(exampleFile);
        
	}
	public void testGetMainScript2() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, SAXException {

    	String exampleFilename = "LEMS_NML2_Ex0_IaF.xml"; 
        File exampleFile = new File(AppTest.getLemsExamplesDir(), exampleFilename);
        generateSBMLAndTestScript(exampleFile);
        
	}
        
    public void generateSBMLAndTestScript(File exampleFile)  throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, SAXException {
        
		Lems lems = Utils.loadLemsFile(exampleFile);

        SBMLWriter sbmlw = new SBMLWriter(lems);
        String sbml = sbmlw.getMainScript();

        File sbmlFile = new File(AppTest.getTempDir(),exampleFile.getName().replaceAll("xml", "sbml"));
        System.out.println("Writing file to: "+sbmlFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(sbml, sbmlFile);
        
        assertTrue(sbmlFile.exists());
        
        Utils.testValidity(sbmlFile, SBMLWriter.PREF_SBML_SCHEMA);
        
        File testSbmlFile = new File(AppTest.getTempDir(),exampleFile.getName().replaceAll("xml", "sh"));

        Target target = lems.getTarget();
        Component simCpt = target.getComponent();
        String len = simCpt.getStringValue("length");
        if (len.indexOf("ms") > 0) {
            len = len.replaceAll("ms", "").trim();
            len = "" + Float.parseFloat(len) / 1000;
        }
        len = len.replaceAll("s", "");

        String dt = simCpt.getStringValue("step");
        if (dt.indexOf("ms") > 0) {
            dt = dt.replaceAll("ms", "").trim();
            dt = "" + Float.parseFloat(dt) / 1000;
        }
        dt = dt.replaceAll("s", "").trim();
        
        String run = "# This is a file which can be used to test the generated SBML in "+sbmlFile.getAbsolutePath()+"\n"+
        			 "# Uses SBMLSimulator: http://www.ra.cs.uni-tuebingen.de/software/SBMLsimulator/downloads/index.html\n"+
        		     "java -jar ~/SBMLsimulator/SBMLsimulator-1.0-rc2-full.jar --gui=true --sim-end-time="+len+" --sbml-input-file="+sbmlFile.getAbsolutePath();

        System.out.println("Writing file to: "+testSbmlFile.getAbsolutePath());
        FileUtil.writeStringToFile(run, testSbmlFile);
        
        assertTrue(testSbmlFile.exists());
	}

}
