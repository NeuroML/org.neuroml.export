package org.neuroml.export.brian;

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

public class BrianWriterTest extends TestCase {

	public void testFN() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, GenerationException {

    	String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
    	generateMainScript(exampleFilename);
	}
	/*
	public void testHH() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError {

    	String exampleFilename = "LEMS_NML2_Ex1_HH.xml";
    	generateMainScript(exampleFilename);
	}*/
    
    
	public void testSBML() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, GenerationException {

    	File exampleSBML = new File("src/test/resources/BIOMD0000000185_LEMS.xml");
    	generateMainScript(exampleSBML);
	}
    
	public void generateMainScript(File localFile) throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, GenerationException {

    	Lems lems = Utils.readLemsNeuroMLFile(FileUtil.readStringFromFile(localFile)).getLems();
        System.out.println("Loaded: "+localFile);

        generateMainScript(lems, localFile.getName(), false);
        
        lems = Utils.readLemsNeuroMLFile(FileUtil.readStringFromFile(localFile)).getLems();
        generateMainScript(lems, localFile.getName(), true);
	}
	
	public void generateMainScript(String exampleFilename) throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, GenerationException {

    	Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);

        System.out.println("Loaded: "+exampleFilename);

        generateMainScript(lems, exampleFilename, false);
        
        lems = AppTest.readLemsFileFromExamples(exampleFilename);
        generateMainScript(lems, exampleFilename, true);
	}
    	
	public void generateMainScript(Lems lems, String filename, boolean brian2) throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, GenerationException {

        BrianWriter bw = new BrianWriter(lems);
        bw.setBrian2(brian2);

        String br = bw.getMainScript();

        String suffix = brian2 ? "_brian2.py" : "_brian.py";
        File brFile = new File(AppTest.getTempDir(),filename.replaceAll(".xml", suffix));
        System.out.println("Writing to: "+brFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(br, brFile);

        assertTrue(brFile.exists());
	}
    

}
