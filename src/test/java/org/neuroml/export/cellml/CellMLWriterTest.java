package org.neuroml.export.cellml;

import java.io.File;
import java.io.IOException;

import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.run.ConnectionError;
import org.lemsml.jlems.core.run.RuntimeError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.ParseException;
import org.lemsml.jlems.core.type.BuildException;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.core.xml.XMLException;
import org.lemsml.jlems.io.util.FileUtil;
import org.lemsml.jlems.io.util.JUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.Utils;
import org.neuroml.model.util.NeuroML2Validator;
import org.xml.sax.SAXException;

import junit.framework.TestCase;
import org.lemsml.export.base.GenerationException;
import org.neuroml.export.cellml.CellMLWriter;

public class CellMLWriterTest extends TestCase {
	

	public void testGetMainScript1() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, SAXException, ConnectionError, RuntimeError, GenerationException {

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
        
    public void generateCellMLAndTestScript(Lems lems, String exampleFileName)  throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, SAXException, ConnectionError, RuntimeError, GenerationException {


        CellMLWriter cellmlw = new CellMLWriter(lems);
        String cellml = cellmlw.getMainScript();

        File cellmlFile = new File(AppTest.getTempDir(),exampleFileName.replaceAll("xml", "cellml"));
        System.out.println("Writing CellML file to: "+cellmlFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(cellml, cellmlFile);
        
        assertTrue(cellmlFile.exists());
        
        
	}

}
