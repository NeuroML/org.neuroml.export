package org.neuroml.export.brian;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Utils;

import junit.framework.TestCase;
import org.lemsml.jlems.core.sim.LEMSException;
import org.neuroml.model.util.NeuroMLException;

public class BrianWriterTest extends TestCase
{

	public void testFN() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
	{

		String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
		generateMainScript(exampleFilename);
	}

	/*
	 * public void testHH() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError {
	 * 
	 * String exampleFilename = "LEMS_NML2_Ex1_HH.xml"; generateMainScript(exampleFilename); }
	 */

	public void testSBML() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
	{

		File exampleSBML = new File("src/test/resources/BIOMD0000000185_LEMS.xml");
		generateMainScript(exampleSBML);
	}

	public void generateMainScript(File localFile) throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
	{

		Lems lems = Utils.readLemsNeuroMLFile(FileUtil.readStringFromFile(localFile)).getLems();
		generateMainScript(lems, localFile.getName(), false);

		lems = Utils.readLemsNeuroMLFile(FileUtil.readStringFromFile(localFile)).getLems();
		generateMainScript(lems, localFile.getName(), true);
	}

	public void generateMainScript(String exampleFilename) throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
	{

		Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);
		generateMainScript(lems, exampleFilename, false);

		lems = AppTest.readLemsFileFromExamples(exampleFilename);
		generateMainScript(lems, exampleFilename, true);
	}

	public void generateMainScript(Lems lems, String filename, boolean brian2) throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
	{

		BrianWriter bw = new BrianWriter(lems, AppTest.getTempDir(), filename.replaceAll(".xml", "_brian.py"));
		bw.setBrian2(brian2);

		//AQ: Remove this is a test and there is no point for this
		// String suffix = brian2 ? "_brian2.py" : "_brian.py";

		List<File> outputFiles = bw.convert();
		for(File outputFile : outputFiles)
		{
			assertTrue(outputFile.exists());
		}

	}
}
