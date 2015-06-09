package org.lemsml.export.c;

import java.io.File;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.utils.UtilsTest;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Utils;
import org.neuroml.model.util.NeuroMLException;

public class CWriterTest extends TestCase
{

	public void testFN() throws LEMSException, GenerationException, IOException, ModelFeatureSupportException, NeuroMLException
	{

		String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
		Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);

		CWriter cw = new CWriter(lems, AppTest.getTempDir(), exampleFilename.replaceAll(".xml", ".c"));

		
		UtilsTest.checkConvertedFiles(cw.convert());
	}

	/*
	 * public void testIaF() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, GenerationException {
	 * 
	 * String exampleFilename = "LEMS_NML2_Ex0_IaF.xml"; generateMainScript(exampleFilename); }
	 */

	public void testSBML() throws LEMSException, GenerationException, IOException, ModelFeatureSupportException, NeuroMLException
	{
		String exampleFilepath = "src/test/resources/";
		String exampleFilename = "BIOMD0000000185_LEMS.xml";
		Lems lems = Utils.readLemsNeuroMLFile(FileUtil.readStringFromFile(new File(exampleFilepath + exampleFilename))).getLems();

		CWriter cw = new CWriter(lems, AppTest.getTempDir(), exampleFilename.replaceAll(".xml", ".c"));

		UtilsTest.checkConvertedFiles(cw.convert());
	}

}
