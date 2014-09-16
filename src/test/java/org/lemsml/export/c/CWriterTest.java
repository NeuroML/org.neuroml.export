package org.lemsml.export.c;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.lemsml.export.base.GenerationException;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.ModelFeatureSupportException;
import org.neuroml.export.Utils;
import org.neuroml.model.util.NeuroMLException;

public class CWriterTest extends TestCase {
	
	
	public void testFN() throws LEMSException, GenerationException, IOException, ModelFeatureSupportException, NeuroMLException {

    	String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
        
        System.out.println("Converting: "+exampleFilename);
    	generateMainScript(exampleFilename);
	}
	/*
	public void testIaF() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, GenerationException {

    	String exampleFilename = "LEMS_NML2_Ex0_IaF.xml";
    	generateMainScript(exampleFilename);
	}*/
	
	public void testSBML() throws LEMSException, GenerationException, IOException, ModelFeatureSupportException, NeuroMLException {

    	File exampleSBML = new File("src/test/resources/BIOMD0000000185_LEMS.xml");
    	generateMainScript(exampleSBML);
	}
    
	
	
	public void generateMainScript(File localFile) throws LEMSException, GenerationException, IOException, ModelFeatureSupportException, NeuroMLException {

    	Lems lems = Utils.readLemsNeuroMLFile(FileUtil.readStringFromFile(localFile)).getLems();
        
        System.out.println("Loaded from: "+localFile);

        CWriter cw = new CWriter(lems);

        String code = cw.getMainScript();

        System.out.println(code);

        File cFile = new File(AppTest.getTempDir(),localFile.getName().replaceAll(".xml", ".c"));
        
        FileUtil.writeStringToFile(code, cFile);
        
        assertTrue(cFile.exists());
        
        File mFile = new File(AppTest.getTempDir(),"Makefile");
        String makefile = cw.getMakefile();
        FileUtil.writeStringToFile(makefile, mFile);
	}
	
	public void generateMainScript(String exampleFilename) throws LEMSException, GenerationException, IOException, ModelFeatureSupportException, NeuroMLException {


    	Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);
        
        System.out.println("Loaded: "+exampleFilename);

        CWriter cw = new CWriter(lems);

        String code = cw.getMainScript();

        System.out.println(code);

        File cFile = new File(AppTest.getTempDir(),exampleFilename.replaceAll(".xml", ".c"));
        System.out.println("Writing to: "+cFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(code, cFile);
        
        assertTrue(cFile.exists());
        
        File mFile = new File(AppTest.getTempDir(),"Makefile");
        String makefile = cw.getMakefile();
        FileUtil.writeStringToFile(makefile, mFile);
	}

}
