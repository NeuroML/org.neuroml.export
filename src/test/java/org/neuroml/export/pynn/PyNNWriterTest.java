package org.neuroml.export.pynn;

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

public class PyNNWriterTest extends TestCase {


	public void testFN() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException  {

    	String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
    	generateMainScript(exampleFilename);
	}
	public void testPyNN() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException  {

    	String exampleFilename = "LEMS_NML2_Ex14_PyNN.xml";
    	generateMainScript(exampleFilename);
	}
	
	public void generateMainScript(File localFile) throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException  {

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
	
	public void generateMainScript(String exampleFilename) throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException  {


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
