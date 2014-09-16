package org.neuroml.export.graph;

import java.io.File;
import java.io.IOException;

import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.AppTest;

import junit.framework.TestCase;
import org.lemsml.export.base.GenerationException;
import org.lemsml.jlems.core.sim.LEMSException;
import org.neuroml.export.ModelFeatureSupportException;
import org.neuroml.model.util.NeuroMLException;

public class GraphWriterTest extends TestCase {

	public void testGetMainScript() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException  {
        //Note: only works with this example at the moment!!

    	String exampleFilename = "LEMS_NML2_Ex9_FN.xml";

    	Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);
        
        ///exampleFile = new File("/home/padraig/org.neuroml.import/src/test/resources/Simple3Species_SBML.xml");
        
        System.out.println("Loaded: "+exampleFilename);

		GraphWriter gw = new GraphWriter(lems);
        //System.out.println("new GraphWriter(lems)");
        String gv = gw.getMainScript();

        //System.out.println(gv);

        File gvFile = new File(AppTest.getTempDir(),exampleFilename.replaceAll("xml", "gv"));
        System.out.println("Writing to: "+gvFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(gv, gvFile);

        
        assertTrue(gvFile.exists());
	}

}
