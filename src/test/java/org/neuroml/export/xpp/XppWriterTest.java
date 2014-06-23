package org.neuroml.export.xpp;

import java.io.File;
import java.io.IOException;

import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.AppTest;

import junit.framework.TestCase;
import org.lemsml.export.base.GenerationException;
import org.lemsml.jlems.core.sim.LEMSException;
import org.neuroml.export.ModelFeatureSupportException;
import org.neuroml.export.Utils;
import org.neuroml.model.util.NeuroMLException;

public class XppWriterTest extends TestCase {

    
	public void testFN() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException {

    	String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
    	generateMainScript(exampleFilename);
	}
    
    public void testSBML() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException {

    	File exampleSBML = new File("src/test/resources/BIOMD0000000185_LEMS.xml");
    	generateMainScript(exampleSBML);
	}
	
	public void generateMainScript(File localFile) throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException {

    	Lems lems = Utils.readLemsNeuroMLFile(FileUtil.readStringFromFile(localFile)).getLems();
        
        System.out.println("Loaded from: "+localFile);
        
		XppWriter xppw = new XppWriter(lems);
        String ode = xppw.getMainScript();

        File odeFile = new File(AppTest.getTempDir(),localFile.getName().replaceAll("xml", "ode"));
        System.out.println("Writing to: "+odeFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(ode, odeFile);

        
        assertTrue(odeFile.exists());
	}
    
	public void generateMainScript(String exampleFilename) throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException {
        
    	Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);

		XppWriter xppw = new XppWriter(lems);
        String ode = xppw.getMainScript();

        File odeFile = new File(AppTest.getTempDir(),exampleFilename.replaceAll("xml", "ode"));
        System.out.println("Writing to: "+odeFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(ode, odeFile);

        
        assertTrue(odeFile.exists());
	}

}
