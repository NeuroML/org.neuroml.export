package org.neuroml.export.neuron;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.logging.MinimalMessageHandler;
import org.lemsml.jlems.core.run.ConnectionError;
import org.lemsml.jlems.core.run.RuntimeError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.ParseException;
import org.lemsml.jlems.core.type.BuildException;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.xml.XMLException;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.Utils;

import junit.framework.TestCase;


public class NeuronWriterTest extends TestCase {

	public void testFN() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError {

		String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
    	Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);
		testGetMainScript(exampleFilename, lems);
	}
	
	public void testChannel() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError {

		//TODO: Find in jar!!
		File exampleFile = new File("../org.neuroml.model/src/main/resources/examples/NML2_SimpleIonChannel.nml");

    	testComponentToMod(exampleFile, "na");
	}

	public void testSynapse() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError {

		//TODO: Find in jar!!
		File exampleFile = new File("../org.neuroml.model/src/main/resources/examples/NML2_SynapseTypes.nml");

    	testComponentToMod(exampleFile, "blockStpSynDepFac");
	}
	public void testIaFCells() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError {

		//TODO: Find in jar!!
		File exampleFile = new File("../org.neuroml.model/src/main/resources/examples/NML2_AbstractCells.nml");

    	testComponentToMod(exampleFile, "iafTau");
    	testComponentToMod(exampleFile, "iafTauRef");
    	testComponentToMod(exampleFile, "iaf");
    	testComponentToMod(exampleFile, "iafRef");
	}
	
	public void testComponentToMod(File exampleFile, String compId) throws ContentError, ParseError, ParseException, BuildException, XMLException, ConnectionError, RuntimeError, IOException
	{
		E.info("Loading: "+exampleFile.getCanonicalPath());
    	Lems lems = Utils.readNeuroMLFile(exampleFile).getLems();
        Component comp = lems.getComponent(compId);
        E.info("Found component: " + comp);

        String modFile = NeuronWriter.generateModFile(comp);

        String origName = comp.getComponentType().getName();
        String newName = "MOD_"+compId;

        modFile = modFile.replaceAll(origName, newName);
        File newMechFile = new File(AppTest.getTempDir(), newName + ".mod");

        FileUtil.writeStringToFile(modFile, newMechFile);
        E.info("Written to file: " + newMechFile);
	}

	public void testGetMainScript(String exampleFilename, Lems lems) throws ContentError, ParseError,
			ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError {

    	MinimalMessageHandler.setVeryMinimal(true);

		NeuronWriter nw = new NeuronWriter(lems);

        File mainFile = new File(AppTest.getTempDir(),exampleFilename.replaceAll(".xml", "_nrn.py"));

        E.info("Generating NEURON from "+exampleFilename);

		ArrayList<File> genFiles = nw.generateMainScriptAndMods(mainFile);

		assertTrue(genFiles.size() >= 2);

		for (File f : genFiles) {
			E.info("Written model behaviour to: " + f.getAbsolutePath());
			assertTrue(f.exists());
			assertTrue(f.length() > 0);
		}

	}

}
