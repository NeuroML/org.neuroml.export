package org.neuroml.export.pynn;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.AppTest;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.model.util.NeuroMLException;

public class PyNNWriterTest extends TestCase
{

	public void testFN() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
	{

		String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
		generateMainScript(exampleFilename);
	}

	public void testPyNN() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
	{

		String exampleFilename = "LEMS_NML2_Ex14_PyNN.xml";
		generateMainScript(exampleFilename);
	}

	public void generateMainScript(String exampleFilename) throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
	{

		Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);

		PyNNWriter pw = new PyNNWriter(lems, AppTest.getTempDir());
		for(File genFile : pw.convert())
		{
			assertTrue(genFile.exists());
			// System.out.println("------------------" + genFile.getAbsolutePath() + "------------------------------------");
			// System.out.println(FileUtil.readStringFromFile(genFile));
		}

	}

}
