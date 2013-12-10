/**
 * 
 */
package org.neuroml.export.neuron;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBException;

import junit.framework.TestCase;

import org.lemsml.export.base.GenerationException;
import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.run.ConnectionError;
import org.lemsml.jlems.core.run.RuntimeError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.ParseException;
import org.lemsml.jlems.core.sim.Sim;
import org.lemsml.jlems.core.type.BuildException;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.xml.XMLException;
import org.lemsml.jlems.io.reader.JarResourceInclusionReader;
import org.lemsml.jlems.io.util.JUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.Main;
import org.neuroml.model.util.NeuroMLException;

/**
 * @author boris
 *
 */
public class NeuronGHKTest extends TestCase {

	public void testGHK() throws ContentError, JAXBException, NeuroMLException, ParseError, ParseException, BuildException, XMLException, ConnectionError, RuntimeError, IOException, GenerationException {
		String exampleFilename = "ghk_na_k_ca.xml";
		String content = JUtil.getRelativeResource(this.getClass(), Main.getNeuroMLExamplesResourcesDir()+ "/" + exampleFilename);

		JarResourceInclusionReader.addSearchPathInJar("/examples");
		JarResourceInclusionReader jrir = new JarResourceInclusionReader(content);
		JUtil.setResourceRoot(this.getClass());

		Sim sim = new Sim(jrir.read());
        sim.readModel();
		Lems lems =  sim.getLems();

		NeuronWriter nw = new NeuronWriter(lems);
		File mainFile = new File(AppTest.getTempDir(), exampleFilename.replaceAll(".xml", "_nrn.py"));

		List<File> genFiles = nw.generateMainScriptAndMods(mainFile);

		assertTrue(genFiles.size() >= 2);

		for (File f : genFiles) {
			E.info("Written model behaviour to: " + f.getAbsolutePath());
			assertTrue(f.exists());
			assertTrue(f.length() > 0);
		}
	}
}
