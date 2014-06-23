package org.neuroml.export.xineml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.AppTest;
import org.neuroml.export.xineml.XineMLWriter.Variant;

import junit.framework.TestCase;
import org.lemsml.export.base.GenerationException;
import org.lemsml.jlems.core.sim.LEMSException;
import org.neuroml.export.ModelFeatureSupportException;
import org.neuroml.model.util.NeuroMLException;

public class XineMLWriterTest extends TestCase {

	public void testFN() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException  {

    	String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
    	generateMainScript(exampleFilename);
	}
	public void testIzh() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException  {

    	String exampleFilename = "LEMS_NML2_Ex2_Izh.xml";
    	generateMainScript(exampleFilename);
	}
	
	/*
	public void testHH() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError {

    	String exampleFilename = "LEMS_NML2_Ex1_HH.xml";
    	generateMainScript(exampleFilename);
	}*/
	
	public void generateMainScript(String exampleFilename) throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException  {


    	Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);

        System.out.println("Loaded: "+exampleFilename);

        XineMLWriter nw = new XineMLWriter(lems, Variant.NineML);
            
        File mainFile = new File(AppTest.getTempDir(), exampleFilename.replaceAll(".xml", ".9ml"));

        ArrayList<File> nr = nw.generateAllFiles(mainFile);
        
        System.out.println("Generated: "+nr);
        
        for (File f: nr) {
            System.out.println("Checking file: "+f.getAbsolutePath());
	        assertTrue(f.exists());
        }
        assertTrue(!nw.getFilesGenerated().isEmpty());

        
        // Refresh..
    	lems = AppTest.readLemsFileFromExamples(exampleFilename);

        XineMLWriter sw = new XineMLWriter(lems, Variant.SpineML);
        mainFile = new File(AppTest.getTempDir(), exampleFilename.replaceAll(".xml", ".spineml"));

        ArrayList<File> sr = sw.generateAllFiles(mainFile);
        
        System.out.println("Generated: "+sr);
        
        for (File f: sw.getFilesGenerated()) {
            System.out.println("Checking file: "+f.getAbsolutePath());
	        assertTrue(f.exists());
        }
        assertTrue(!sw.getFilesGenerated().isEmpty());
	}

}
