package org.neuroml.export.xineml;

import java.io.IOException;

import junit.framework.TestCase;

import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.utils.UtilsTest;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Format;
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

		Lems lems = UtilsTest.readLemsFileFromExamples(exampleFilename);

		XineMLWriter nw = new XineMLWriter(lems, Format.NINEML, UtilsTest.getTempDir(), exampleFilename.replaceAll(".xml", ".9ml"));

		UtilsTest.checkConvertedFiles(nw.convert());

		// Refresh..
		lems = UtilsTest.readLemsFileFromExamples(exampleFilename);
		XineMLWriter sw = new XineMLWriter(lems, Format.SPINEML, UtilsTest.getTempDir(), exampleFilename.replaceAll(".xml", ".spineml"));
		
		UtilsTest.checkConvertedFiles(nw.convert());
	}

}
