package org.neuroml.export.graph;

import java.io.File;
import java.io.IOException;
import java.util.List;
import junit.framework.TestCase;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.UtilsTest;
import org.neuroml.model.util.NeuroMLException;

public class GraphWriterTest extends TestCase
{

	public void testGetMainScript() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
	{
		// Note: only works with this example at the moment!!
		String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
		Lems lems = UtilsTest.readLemsFileFromExamples(exampleFilename);
		// /exampleFile = new File("/home/padraig/org.neuroml.import/src/test/resources/Simple3Species_SBML.xml");

		GraphWriter gw = new GraphWriter(lems, UtilsTest.getTempDir(), exampleFilename.replaceAll("xml", "gv"));

		List<File> outputFiles = gw.convert();
		for(File outputFile : outputFiles)
		{
			assertTrue(outputFile.exists());
		}

	}

}
