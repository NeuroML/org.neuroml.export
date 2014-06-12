package org.lemsml.export.modelica;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import org.lemsml.export.base.GenerationException;

import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.Utils;

public class ModelicaWriterTest extends TestCase {


	public void testFN() throws LEMSException, IOException, GenerationException {

    	String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
    	generateMainScript(exampleFilename);
	}
	
	public void testHH() throws LEMSException, IOException, GenerationException {

    	String exampleFilename = "LEMS_NML2_Ex1_HH.xml";
    	generateMainScript(exampleFilename);
	}
    
	public void testSBML() throws LEMSException, IOException, GenerationException {

    	File exampleSBML = new File("src/test/resources/BIOMD0000000185_LEMS.xml");
    	generateMainScript(exampleSBML);
	}
	
	public void generateMainScript(File localFile) throws LEMSException, IOException, GenerationException {

    	Lems lems = Utils.readLemsNeuroMLFile(FileUtil.readStringFromFile(localFile)).getLems();
        
        System.out.println("Loaded from: "+localFile);

        ModelicaWriter mw = new ModelicaWriter(lems);

        String mod = mw.generateMainScriptAndCompFiles(AppTest.getTempDir());
        
        for (File genFile: mw.allGeneratedFiles)
        {
        	assertTrue(genFile.exists());
            System.out.println("------------------"+genFile.getAbsolutePath()+"------------------------------------");
            System.out.println(FileUtil.readStringFromFile(genFile));
        }
	}
	
	public void generateMainScript(String exampleFilename) throws LEMSException, IOException, GenerationException {


    	Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);
        
        System.out.println("Loaded: "+exampleFilename);

        ModelicaWriter mw = new ModelicaWriter(lems);

        String mod = mw.generateMainScriptAndCompFiles(AppTest.getTempDir());
        
        for (File genFile: mw.allGeneratedFiles)
        {
        	assertTrue(genFile.exists());
            System.out.println("------------------"+genFile.getAbsolutePath()+"------------------------------------");
            System.out.println(FileUtil.readStringFromFile(genFile));
        }
	}

}
