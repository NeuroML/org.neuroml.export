package org.neuroml.export.cellml;

import java.io.File;
import java.io.IOException;

import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.exception.GenerationException;
import org.neuroml.export.exception.ModelFeatureSupportException;

import junit.framework.TestCase;
import org.lemsml.jlems.core.sim.LEMSException;
import org.neuroml.model.util.NeuroMLException;

public class CellMLWriterTest extends TestCase {
	

	public void testGetMainScript1() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException  {

    	String exampleFilename = "LEMS_NML2_Ex9_FN.xml"; 
    	Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);
        generateCellMLAndTestScript(lems, exampleFilename);
        
	}
	/*
	public void testGetMainScript2() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, SAXException, ConnectionError, RuntimeError {

    	String exampleFilename = "LEMS_NML2_Ex0_IaF.xml"; 
    	Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);

        generateCellMLAndTestScript(lems, exampleFilename);
        
	}*/
        
    public void generateCellMLAndTestScript(Lems lems, String exampleFileName)  throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException  {


        CellMLWriter cellmlw = new CellMLWriter(lems);
        String cellml = cellmlw.getMainScript();

        File cellmlFile = new File(AppTest.getTempDir(),exampleFileName.replaceAll("xml", "cellml"));
        System.out.println("Writing CellML file to: "+cellmlFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(cellml, cellmlFile);
        
        assertTrue(cellmlFile.exists());
        
        
	}

}
