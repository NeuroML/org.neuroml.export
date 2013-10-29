package org.neuroml.export.sbml;

import java.io.File;
import java.io.IOException;

import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.run.ConnectionError;
import org.lemsml.jlems.core.run.RuntimeError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.ParseException;
import org.lemsml.jlems.core.type.BuildException;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.core.xml.XMLException;
import org.lemsml.jlems.io.util.FileUtil;
import org.lemsml.jlems.io.util.JUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.Utils;
import org.neuroml.model.util.NeuroML2Validator;
import org.xml.sax.SAXException;

import junit.framework.TestCase;
import org.lemsml.export.base.GenerationException;

public class SBMLWriterTest extends TestCase {
	

	public void testGetMainScript1() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, SAXException, ConnectionError, RuntimeError, GenerationException {

    	String exampleFilename = "LEMS_NML2_Ex9_FN.xml"; 
    	Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);
        generateSBMLAndTestScript(lems, exampleFilename);
        
	}
	/*
	public void testGetMainScript2() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, SAXException, ConnectionError, RuntimeError {

    	String exampleFilename = "LEMS_NML2_Ex0_IaF.xml"; 
    	Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);

        generateSBMLAndTestScript(lems, exampleFilename);
        
	}*/
        
    public void generateSBMLAndTestScript(Lems lems, String exampleFileName)  throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, SAXException, ConnectionError, RuntimeError, GenerationException {


        SBMLWriter sbmlw = new SBMLWriter(lems);
        String sbml = sbmlw.getMainScript();

        File sbmlFile = new File(AppTest.getTempDir(),exampleFileName.replaceAll("xml", "sbml"));
        System.out.println("Writing SBML file to: "+sbmlFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(sbml, sbmlFile);
        
        assertTrue(sbmlFile.exists());
        
        
        NeuroML2Validator.testValidity(sbmlFile, SBMLWriter.LOCAL_SBML_SCHEMA);
        System.out.println("File is valid SBML");
        
        File testSbmlFile = new File(AppTest.getTempDir(),exampleFileName.replaceAll("xml", "sh"));

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

        System.out.println("Writing shell script to test SBML: "+testSbmlFile.getAbsolutePath());
        FileUtil.writeStringToFile(run, testSbmlFile);
        
        assertTrue(testSbmlFile.exists());
	}

}
