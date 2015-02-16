package org.lemsml.export.sedml;

import java.io.File;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.AppTest;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Formats;
import org.neuroml.model.util.NeuroML2Validator;
import org.neuroml.model.util.NeuroMLException;
import org.xml.sax.SAXException;

public class SEDMLWriterTest extends TestCase
{

	public void testGetMainScript() throws LEMSException, IOException, GenerationException, SAXException, ModelFeatureSupportException, NeuroMLException
	{

		String LOCAL_SEDML_SCHEMA = "src/test/resources/Schemas/SED-ML/sed-ml-L1-V1.xsd";
		String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
		Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);

		SEDMLWriter sw = new SEDMLWriter(lems, AppTest.getTempDir(), exampleFilename.replaceAll("xml", "sedml"), exampleFilename, Formats.SBML);

		List<File> outputFiles = sw.convert();
		for(File outputFile : outputFiles)
		{
			assertTrue(outputFile.exists());
			NeuroML2Validator.testValidity(outputFile, LOCAL_SEDML_SCHEMA);
		}
	}
}
