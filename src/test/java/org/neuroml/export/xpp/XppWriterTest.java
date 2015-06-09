package org.neuroml.export.xpp;

import java.io.File;
import java.io.IOException;

import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Utils;

import junit.framework.TestCase;
import org.lemsml.jlems.core.sim.LEMSException;
import org.neuroml.export.utils.UtilsTest;
import org.neuroml.model.util.NeuroMLException;

public class XppWriterTest extends TestCase
{

	public void testFN() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
	{

		String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
		Lems lems = UtilsTest.readLemsFileFromExamples(exampleFilename);

		XppWriter xppw = new XppWriter(lems, UtilsTest.getTempDir(), exampleFilename.replaceAll("xml", "ode"));
		
		UtilsTest.checkConvertedFiles(xppw.convert());
	}

	public void testSBML() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
	{

		File exampleSBML = new File("src/test/resources/BIOMD0000000185_LEMS.xml");
		Lems lems = Utils.readLemsNeuroMLFile(FileUtil.readStringFromFile(exampleSBML)).getLems();

		XppWriter xppw = new XppWriter(lems, UtilsTest.getTempDir(), exampleSBML.getName().replaceAll("xml", "ode"));

		UtilsTest.checkConvertedFiles(xppw.convert());

	}

}
