package org.neuroml.export.sbml;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.model.util.NeuroML2Validator;
import org.xml.sax.SAXException;

import junit.framework.TestCase;
import org.lemsml.jlems.core.sim.LEMSException;
import org.neuroml.export.utils.UtilsTest;
import org.neuroml.model.util.NeuroMLException;

public class SBMLWriterTest extends TestCase
{

	String LOCAL_SBML_SCHEMA = "src/test/resources/Schemas/sbml-l2v2-schema/sbml.xsd";

	public void testGetMainScript1() throws LEMSException, IOException, GenerationException, SAXException, ModelFeatureSupportException, NeuroMLException
	{

		String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
		Lems lems = UtilsTest.readLemsFileFromExamples(exampleFilename);
		generateSBMLAndTestScript(lems, exampleFilename);

	}/*
	 * public void testGetMainScript2() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, SAXException, ConnectionError, RuntimeError, GenerationException {
	 *
	 * String exampleFilename = "/home/padraig/git/HindmarshRose1984/NeuroML2/Run_Regular_HindmarshRose.xml"; Lems lems = Utils.readLemsNeuroMLFile(new File(exampleFilename)).getLems();
	 * generateSBMLAndTestScript(lems, "Run_Regular_HindmarshRose.xml");
	 *
	 * }
	 *
	 * public void testGetMainScript3() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, SAXException, ConnectionError, RuntimeError {
	 *
	 * String exampleFilename = "LEMS_NML2_Ex0_IaF.xml"; Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);
	 *
	 * generateSBMLAndTestScript(lems, exampleFilename);
	 *
	 * }
	 */

	public void generateSBMLAndTestScript(Lems lems, String exampleFileName) throws LEMSException, IOException, GenerationException, SAXException, ModelFeatureSupportException, NeuroMLException
	{

		SBMLWriter sbmlw = new SBMLWriter(lems, UtilsTest.getTempDir(), exampleFileName.replaceAll("xml", "sbml"));
		List<File> outputFiles = sbmlw.convert();

		UtilsTest.checkConvertedFiles(outputFiles);

		//NeuroML2Validator.testValidity(outputFiles.get(0), LOCAL_SBML_SCHEMA);

		File testSbmlFile = new File(UtilsTest.getTempDir(), exampleFileName.replaceAll("xml", "sh"));

		Target target = lems.getTarget();
		Component simCpt = target.getComponent();
		String len = simCpt.getStringValue("length");
		if(len.indexOf("ms") > 0)
		{
			len = len.replaceAll("ms", "").trim();
			len = "" + Float.parseFloat(len) / 1000;
		}
		len = len.replaceAll("s", "");

		String dt = simCpt.getStringValue("step");
		if(dt.indexOf("ms") > 0)
		{
			dt = dt.replaceAll("ms", "").trim();
			dt = "" + Float.parseFloat(dt) / 1000;
		}
		dt = dt.replaceAll("s", "").trim();

		String run = "# This is a file which can be used to test the generated SBML in " + outputFiles.get(0).getAbsolutePath() + "\n"
				+ "# Uses SBMLSimulator: http://www.ra.cs.uni-tuebingen.de/software/SBMLsimulator/downloads/index.html\n"
				+ "java -jar ~/SBMLsimulator/SBMLsimulator-1.0-rc2-full.jar --gui=true --sim-end-time=" + len + " --sbml-input-file=" + outputFiles.get(0).getAbsolutePath();

		System.out.println("Writing shell script to test SBML: " + testSbmlFile.getAbsolutePath());
		FileUtil.writeStringToFile(run, testSbmlFile);

		assertTrue(testSbmlFile.exists());
	}

}
