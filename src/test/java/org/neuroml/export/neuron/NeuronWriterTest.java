package org.neuroml.export.neuron;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.ParseException;
import org.lemsml.jlems.core.type.BuildException;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.xml.XMLException;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.Utils;

import junit.framework.TestCase;

public class NeuronWriterTest extends TestCase {

	public void testGetMainScript() throws ContentError, ParseError,
			ParseException, BuildException, XMLException, IOException {
		// Note: only works with this example at the moment!!

		String exampleFilename = "LEMS_NML2_Ex9_FN.xml";

		File exampleFile = new File(AppTest.getLemsExamplesDir(),
				exampleFilename);

		Lems lems = Utils.loadLemsFile(exampleFile);

		NeuronWriter nw = new NeuronWriter(lems);


        File mainFile = new File(AppTest.getTempDir(),exampleFilename.replaceAll("xml", "_nrn.py"));

		ArrayList<File> genFiles = nw.generateMainScriptAndMods(mainFile);

		assertTrue(genFiles.size() >= 2);

		for (File f : genFiles) {
			E.info("Written model behaviour to: " + f.getAbsolutePath());
			assertTrue(f.exists());
			assertTrue(f.length() > 0);
		}

	}

}
