package org.lemsml.export.matlab;

import java.io.File;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Utils;
import org.neuroml.model.util.NeuroMLException;

public class MatlabWriterTest extends TestCase
{

	public void testFN() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
	{

		String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
		Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);

		MatlabWriter mw = new MatlabWriter(lems, AppTest.getTempDir(), exampleFilename.replaceAll(".xml", ".m"));

		List<File> outputFiles = mw.convert();
		for(File outputFile : outputFiles)
		{
			assertTrue(outputFile.exists());
		}
	}

	public void testSBML() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
	{
		String exampleFilepath = "src/test/resources/";
		String exampleFilename = "BIOMD0000000185_LEMS.xml";
		Lems lems = Utils.readLemsNeuroMLFile(FileUtil.readStringFromFile(new File(exampleFilepath + exampleFilename))).getLems();

		MatlabWriter mw = new MatlabWriter(lems, AppTest.getTempDir(), exampleFilename.replaceAll(".xml", ".m"));

		List<File> outputFiles = mw.convert();
		for(File outputFile : outputFiles)
		{
			assertTrue(outputFile.exists());
		}
	}

	/*
	 * public void testIaF() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException {
	 * 
	 * String exampleFilename = "LEMS_NML2_Ex0_IaF.xml"; generateMainScript(exampleFilename); } public void testHH() throws ContentError, ParseError, ParseException, BuildException, XMLException,
	 * IOException, ConnectionError, RuntimeError {
	 * 
	 * String exampleFilename = "LEMS_NML2_Ex1_HH.xml"; generateMainScript(exampleFilename); }
	 */

}
