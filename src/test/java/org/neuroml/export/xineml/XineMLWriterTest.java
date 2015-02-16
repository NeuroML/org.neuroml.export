package org.neuroml.export.xineml;

import java.io.File;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.AppTest;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Formats;
import org.neuroml.model.util.NeuroMLException;

public class XineMLWriterTest extends TestCase
{

	public void testFN() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
	{

		String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
		generateMainScript(exampleFilename);
	}

	public void testIzh() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
	{

		String exampleFilename = "LEMS_NML2_Ex2_Izh.xml";
		generateMainScript(exampleFilename);
	}

	/*
	 * public void testHH() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError {
	 * 
	 * String exampleFilename = "LEMS_NML2_Ex1_HH.xml"; generateMainScript(exampleFilename); }
	 */

	public void generateMainScript(String exampleFilename) throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
	{

		Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);

		XineMLWriter nw = new XineMLWriter(lems, Formats.NINEML, AppTest.getTempDir(), exampleFilename.replaceAll(".xml", ".9ml"));

		List<File> outputFiles = nw.convert();
		for(File outputFile : outputFiles)
		{
			assertTrue(outputFile.exists());
		}
		assertFalse(outputFiles.isEmpty());

		// Refresh..
		lems = AppTest.readLemsFileFromExamples(exampleFilename);
		XineMLWriter sw = new XineMLWriter(lems, Formats.SPINEML, AppTest.getTempDir(), exampleFilename.replaceAll(".xml", ".spineml"));
		outputFiles = nw.convert();
		for(File outputFile : outputFiles)
		{
			assertTrue(outputFile.exists());
		}
		assertFalse(outputFiles.isEmpty());
	}

}
