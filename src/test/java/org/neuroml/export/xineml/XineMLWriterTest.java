package org.neuroml.export.xineml;

import java.io.File;
import java.io.IOException;

import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.run.ConnectionError;
import org.lemsml.jlems.core.run.RuntimeError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.ParseException;
import org.lemsml.jlems.core.type.BuildException;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.xml.XMLException;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.xineml.XineMLWriter.Variant;

import junit.framework.TestCase;

public class XineMLWriterTest extends TestCase {

	public void testFN() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError {

    	String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
    	generateMainScript(exampleFilename);
	}
	public void testIzh() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError {

    	String exampleFilename = "LEMS_NML2_Ex2_Izh.xml";
    	generateMainScript(exampleFilename);
	}
	
	/*
	public void testHH() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError {

    	String exampleFilename = "LEMS_NML2_Ex1_HH.xml";
    	generateMainScript(exampleFilename);
	}*/
	
	public void generateMainScript(String exampleFilename) throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError {


    	Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);

        System.out.println("Loaded: "+exampleFilename);

        XineMLWriter nw = new XineMLWriter(lems, Variant.NineML);

        String nr = nw.getMainScript();
        
        System.out.println("Generated: "+nr);

        File nrFile = new File(AppTest.getTempDir(),exampleFilename.replaceAll(".xml", ".9ml"));
        
        System.out.println("Writing to: "+nrFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(nr, nrFile);

        assertTrue(nrFile.exists());

        // Refresh..
    	lems = AppTest.readLemsFileFromExamples(exampleFilename);

        XineMLWriter sw = new XineMLWriter(lems, Variant.SpineML);

        String sr = sw.getMainScript();
        
        System.out.println("Generated: "+sr);

        File srFile = new File(AppTest.getTempDir(),exampleFilename.replaceAll(".xml", ".spineml"));
        
        System.out.println("Writing to: "+srFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(sr, srFile);

        assertTrue(srFile.exists());
	}

}
