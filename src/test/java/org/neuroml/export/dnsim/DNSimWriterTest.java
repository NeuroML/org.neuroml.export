package org.neuroml.export.dnsim;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import junit.framework.TestCase;
import org.lemsml.export.base.GenerationException;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.ModelFeatureSupportException;
import org.neuroml.export.Utils;
import org.neuroml.model.util.NeuroMLException;

public class DNSimWriterTest extends TestCase {

    
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
        
        System.out.println("-- Loaded from: "+localFile);
        
		DNSimWriter dnsimw = new DNSimWriter(lems);
        ArrayList<File> files = dnsimw.generateMainScriptAndModules(localFile.getParentFile());

        for (File f: files) {
            assertTrue("Checking: "+f.getAbsolutePath(),f.exists());
        }
	}
    
	public void generateMainScript(String exampleFilename) throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException {
        
    	Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);

		DNSimWriter dnsimw = new DNSimWriter(lems);
        ArrayList<File> files = dnsimw.generateMainScriptAndModules(AppTest.getTempDir());

        for (File f: files) {
            assertTrue("Checking: "+f.getAbsolutePath(),f.exists());
        }
	}

}
