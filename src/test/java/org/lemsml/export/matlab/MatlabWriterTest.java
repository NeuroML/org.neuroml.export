package org.lemsml.export.matlab;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import org.lemsml.export.base.GenerationException;

import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.utils.ModelFeatureSupportException;
import org.neuroml.export.utils.Utils;
import org.neuroml.model.util.NeuroMLException;

public class MatlabWriterTest extends TestCase {
	
	
	public void testFN() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException  {

    	String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
    	generateMainScript(exampleFilename);
	}
	
	
	public void testSBML() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException  {

    	File exampleSBML = new File("src/test/resources/BIOMD0000000185_LEMS.xml");
    	generateMainScript(exampleSBML);
	}
    
	/*
	public void testIaF() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException  {

    	String exampleFilename = "LEMS_NML2_Ex0_IaF.xml";
    	generateMainScript(exampleFilename);
	}
	public void testHH() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError {

    	String exampleFilename = "LEMS_NML2_Ex1_HH.xml";
    	generateMainScript(exampleFilename);
	}*/
	
	public void generateMainScript(File localFile) throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException {

    	Lems lems = Utils.readLemsNeuroMLFile(FileUtil.readStringFromFile(localFile)).getLems();
        
        System.out.println("Loaded from: "+localFile);

        MatlabWriter mw = new MatlabWriter(lems);

        String mat = mw.getMainScript();

        System.out.println(mat);

        File mFile = new File(AppTest.getTempDir(),localFile.getName().replaceAll(".xml", ".m"));
        
        FileUtil.writeStringToFile(mat, mFile);

        
        assertTrue(mFile.exists());
	}
	
	public void generateMainScript(String exampleFilename) throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException  {


    	Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);
        
        System.out.println("Loaded: "+exampleFilename);

        MatlabWriter mw = new MatlabWriter(lems);

        String mat = mw.getMainScript();

        System.out.println(mat);

        File mFile = new File(AppTest.getTempDir(),exampleFilename.replaceAll(".xml", ".m"));
        System.out.println("Writing to: "+mFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(mat, mFile);

        
        assertTrue(mFile.exists());
	}

}
