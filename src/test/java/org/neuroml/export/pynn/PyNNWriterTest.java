package org.neuroml.export.pynn;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import org.lemsml.export.base.GenerationException;

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
import org.neuroml.export.Utils;

public class PyNNWriterTest extends TestCase {


	public void testFN() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, GenerationException {

    	String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
    	generateMainScript(exampleFilename);
	}
	public void testPyNN() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, GenerationException {

    	String exampleFilename = "LEMS_NML2_Ex14_PyNN.xml";
    	generateMainScript(exampleFilename);
	}
	/*
	public void testHH() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, GenerationException {

    	String exampleFilename = "LEMS_NML2_Ex1_HH.xml";
    	generateMainScript(exampleFilename);
	}
    
	public void testSBML() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, GenerationException {

    	File exampleSBML = new File("src/test/resources/BIOMD0000000185_LEMS.xml");
    	generateMainScript(exampleSBML);
	}*/
	
	public void generateMainScript(File localFile) throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, GenerationException {

    	Lems lems = Utils.readLemsNeuroMLFile(FileUtil.readStringFromFile(localFile)).getLems();
        
        System.out.println("Loaded from: "+localFile);

        PyNNWriter mw = new PyNNWriter(lems);

        String mod = mw.generateMainScriptAndCellFiles(AppTest.getTempDir());
        
        for (File genFile: mw.allGeneratedFiles)
        {
        	assertTrue(genFile.exists());
            System.out.println("------------------"+genFile.getAbsolutePath()+"------------------------------------");
            System.out.println(FileUtil.readStringFromFile(genFile));
        }
	}
	
	public void generateMainScript(String exampleFilename) throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, GenerationException {


    	Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);
        
        System.out.println("Loaded: "+exampleFilename);

        PyNNWriter mw = new PyNNWriter(lems);

        String mod = mw.generateMainScriptAndCellFiles(AppTest.getTempDir());
        
        for (File genFile: mw.allGeneratedFiles)
        {
        	assertTrue(genFile.exists());
            System.out.println("------------------"+genFile.getAbsolutePath()+"------------------------------------");
            System.out.println(FileUtil.readStringFromFile(genFile));
        }
	}

}
