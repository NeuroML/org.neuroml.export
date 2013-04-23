package org.neuroml.export.neuron;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.logging.MinimalMessageHandler;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.ParseException;
import org.lemsml.jlems.core.type.BuildException;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.xml.XMLException;
import org.neuroml.export.AppTest;
import org.neuroml.export.Utils;

import junit.framework.TestCase;

public class NeuronWriterTest extends TestCase {

	public void testGetMainScript() throws ContentError, ParseError,
			ParseException, BuildException, XMLException, IOException {


    	MinimalMessageHandler.setVeryMinimal(true);

		String exampleFileName = "LEMS_NML2_Ex9_FN.xml";

		File exampleFile = new File(AppTest.getLemsExamplesDir(),
				exampleFileName);

		//exampleFile = new File("/home/padraig/Dropbox/work/calmod/Calmodulin_LEMS.xml");
		exampleFile = new File("../org.neuroml.import/src/test/resources/Simple3Species_LEMS.xml");

		Lems lems = Utils.loadLemsFile(exampleFile);

		NeuronWriter nw = new NeuronWriter(lems);


        File mainFile = new File(AppTest.getTempDir(),exampleFile.getName().replaceAll(".xml", "_nrn.py"));

        E.info("Generating NEURON from "+exampleFile);

		ArrayList<File> genFiles = nw.generateMainScriptAndMods(mainFile);

		assertTrue(genFiles.size() >= 2);

		for (File f : genFiles) {
			E.info("Written model behaviour to: " + f.getAbsolutePath());
			assertTrue(f.exists());
			assertTrue(f.length() > 0);
		}

	}

}
