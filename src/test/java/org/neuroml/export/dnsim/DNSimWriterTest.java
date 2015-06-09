package org.neuroml.export.dnsim;

import java.io.File;
import java.io.IOException;

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

public class DNSimWriterTest extends TestCase
{

	public void testFN() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
	{

		String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
		Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);
		DNSimWriter dw = new DNSimWriter(lems, AppTest.getTempDir(), exampleFilename.replaceAll(".xml", ".m"));
		
		UtilsTest.checkConvertedFiles(dw.convert());
	}

	public void testSBML() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
	{

		File exampleSBML = new File("src/test/resources/BIOMD0000000185_LEMS.xml");
		Lems lems = Utils.readLemsNeuroMLFile(FileUtil.readStringFromFile(exampleSBML)).getLems();
		DNSimWriter dw = new DNSimWriter(lems, exampleSBML.getParentFile(), exampleSBML.getName().replaceAll(".xml", ".m"));
		
		UtilsTest.checkConvertedFiles(dw.convert());
	}

}
