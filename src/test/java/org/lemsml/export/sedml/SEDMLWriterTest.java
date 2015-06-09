package org.lemsml.export.sedml;

import java.io.IOException;

import junit.framework.TestCase;

import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.utils.UtilsTest;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Format;
import org.neuroml.model.util.NeuroMLException;
import org.xml.sax.SAXException;

public class SEDMLWriterTest extends TestCase
{

	public void testGetMainScript() throws LEMSException, IOException, GenerationException, SAXException, ModelFeatureSupportException, NeuroMLException
	{

		String LOCAL_SEDML_SCHEMA = "src/test/resources/Schemas/SED-ML/sed-ml-L1-V1.xsd";
		String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
		Lems lems = UtilsTest.readLemsFileFromExamples(exampleFilename);

		SEDMLWriter sw = new SEDMLWriter(lems, UtilsTest.getTempDir(), exampleFilename.replaceAll("xml", "sedml"), exampleFilename, Format.SBML);

		UtilsTest.checkConvertedFiles(sw.convert());
	}
}
