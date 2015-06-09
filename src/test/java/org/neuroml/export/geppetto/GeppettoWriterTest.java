package org.neuroml.export.geppetto;

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

public class GeppettoWriterTest extends TestCase
{

	public void testGetMainScript1() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
	{
		String exampleFilename = "LEMS_NML2_Ex5_DetCell.xml";
		Lems lems = UtilsTest.readLemsFileFromExamples(exampleFilename);

		GeppettoWriter gw = new GeppettoWriter(lems, UtilsTest.getTempDir(), exampleFilename.replaceAll("xml", "g.xml"), new File(exampleFilename));

		List<File> outputFiles = gw.convert();
		for(File outputFile : outputFiles)
		{
			assertTrue(outputFile.exists());
		}
	}

}
