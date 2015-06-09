package org.neuroml.export.cellml;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.AppTest;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;

import junit.framework.TestCase;
import org.lemsml.jlems.core.sim.LEMSException;
import org.neuroml.export.utils.UtilsTest;
import org.neuroml.model.util.NeuroMLException;

public class CellMLWriterTest extends TestCase
{

	public void testGetMainScript1() throws LEMSException, IOException, GenerationException, ModelFeatureSupportException, NeuroMLException
	{
		String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
		Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);

		CellMLWriter cw = new CellMLWriter(lems, AppTest.getTempDir(), exampleFilename.replaceAll("xml", "cellml"));

		UtilsTest.checkConvertedFiles(cw.convert());
	}
	
	/*
	 * public void testGetMainScript2() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, SAXException, ConnectionError, RuntimeError {
	 * 
	 * String exampleFilename = "LEMS_NML2_Ex0_IaF.xml"; Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);
	 * 
	 * generateCellMLAndTestScript(lems, exampleFilename);
	 * 
	 * }
	 */

}
