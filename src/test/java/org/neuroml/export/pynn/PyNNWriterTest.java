package org.neuroml.export.pynn;

import java.io.IOException;

import junit.framework.TestCase;

import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.utils.UtilsTest;
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
	public void testIaF() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
	{

		String exampleFilename = "LEMS_NML2_Ex0_IaF.xml";
		generateMainScript(exampleFilename);
	}
/*
    
	public void testPyNN() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
	{

		String exampleFilename = "LEMS_NML2_Ex14_PyNN.xml";
		generateMainScript(exampleFilename);
	}*/

	public void generateMainScript(String exampleFilename) throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
	{

		Lems lems = UtilsTest.readLemsFileFromExamples(exampleFilename);

		PyNNWriter pw = new PyNNWriter(lems, UtilsTest.getTempDir(), exampleFilename.replaceAll(".xml", "_pynn.py"));
	
		UtilsTest.checkConvertedFiles(pw.convert());

	}

}
