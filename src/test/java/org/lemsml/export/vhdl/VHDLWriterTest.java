package org.lemsml.export.vhdl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import junit.framework.TestCase;

import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.UtilsTest;
import org.neuroml.model.util.NeuroMLException;

public class VHDLWriterTest extends TestCase {
	
	
	public void test_iaf_Conversion() throws IOException, LEMSException {

    	String exampleFilename = "LEMS_NML2_Ex0_IaF.xml";
    	
    	String[] models = {"iafTau", "iafTauRef","iafRef","iaf" };
    	
//    	for (int i = 0; i < models.length; i ++)
//    	{
//    		generateMainScript(exampleFilename,models[i],exampleFilename.replaceAll(".xml", ".m") + "_" + models[i]);
//    		
//    	}

	}
	/*
	public void testHH() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError {

    	String exampleFilename = "LEMS_NML2_Ex1_HH.xml";
    	generateMainScript(exampleFilename);
	}*/
	
	public Map<String,String> generateMainScript(String exampleFilename, String modelToConvert, String outputDir) throws IOException, LEMSException, ModelFeatureSupportException, NeuroMLException {

    	Lems lems = UtilsTest.readLemsFileFromExamples(exampleFilename);
        
        System.out.println("Loaded: "+exampleFilename);
        
        VHDLWriter vw = new VHDLWriter(lems);
		
		Map<String,String> componentScripts = vw.getNeuronModelScripts(modelToConvert,false);
		
		String testbenchScript = vw.getSimulationScript(VHDLWriter.ScriptType.TESTBENCH,
				"neuron_model",true);
		String tclScript = vw.getTCLScript(1,0.0001);
		String prjScript = vw.getPrjFile(componentScripts.keySet());
		//String defaultJSON = vw.getMainScript(Method.DEFAULTPARAMJSON);
		//String defaultReadbackJSON = vw.getMainScript(Method.DEFAULTREADBACKJSON);

		File theDir = new File(UtilsTest.getTempDir() + "/" + outputDir );
		if (theDir.exists())
		{
			delete(theDir);		
		}
		theDir.mkdir();
		for (Map.Entry<String, String> entry : componentScripts.entrySet()) {
	        File mFile = new File(UtilsTest.getTempDir(),outputDir + "/" + entry.getKey() + ".vhdl");
	        System.out.println("Writing to: "+mFile.getAbsolutePath());
	        FileUtil.writeStringToFile(entry.getValue(), mFile);
	        assertTrue(mFile.exists());	        			
		}
		File mFile = new File(UtilsTest.getTempDir(),outputDir + "/testbench.vhdl");
        FileUtil.writeStringToFile(testbenchScript, mFile);
        assertTrue(mFile.exists());	        
        mFile = new File(UtilsTest.getTempDir(),outputDir + "/isim.cmd");
        FileUtil.writeStringToFile(tclScript, mFile);
        assertTrue(mFile.exists());	        
        mFile = new File(UtilsTest.getTempDir(),outputDir + "/testbench.prj");
        FileUtil.writeStringToFile(prjScript, mFile);
        assertTrue(mFile.exists());	        
        //mFile = new File(AppTest.getTempDir(),outputDir + "/defaultJSON.json");
        //FileUtil.writeStringToFile(defaultJSON, mFile);
        //assertTrue(mFile.exists());	        
        //mFile = new File(AppTest.getTempDir(),outputDir + "/defaultReadbackJSON.json");
        ///FileUtil.writeStringToFile(defaultReadbackJSON, mFile);
        //assertTrue(mFile.exists());	        

        return componentScripts;
	}
	
	void delete(File f) throws IOException {
	  if (f.isDirectory()) {
	    for (File c : f.listFiles())
	      delete(c);
	  }
	  if (!f.delete())
	    throw new FileNotFoundException("Failed to delete file: " + f);
	}
}
