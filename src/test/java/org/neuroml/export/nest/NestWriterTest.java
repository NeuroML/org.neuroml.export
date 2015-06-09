package org.neuroml.export.nest;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.utils.UtilsTest;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.model.util.NeuroMLException;

public class NestWriterTest extends TestCase
{

	public void testFN() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
	{

		String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
		Lems lems = UtilsTest.readLemsFileFromExamples(exampleFilename);

		NestWriter nw = new NestWriter(lems, UtilsTest.getTempDir(), exampleFilename.replaceAll(".xml", "_nest.py"));

		UtilsTest.checkConvertedFiles(nw.convert());
	}

}
