package org.neuroml.export.neuron;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.xml.bind.JAXBException;

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
import org.lemsml.jlems.io.util.JUtil;
import org.neuroml.export.Main;
import org.neuroml.model.util.NeuroMLConverter;

public class NeuronWriterTest extends TestCase {

    
    public void testHH() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException {

        String exampleFilename = "LEMS_NML2_Ex5_DetCell.xml";
        Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);
        testGetMainScript(exampleFilename, lems);
    }
    /*
    public void testQ10() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException {

        String exampleFilename = "LEMS_NML2_Ex10_Q10.xml";
        Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);
        testGetMainScript(exampleFilename, lems);
    }*/

    public void testFN() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException {

        String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
        Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);
        testGetMainScript(exampleFilename, lems);
    }
    
    public void testChannel() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError {

        testComponentToMod("NML2_SimpleIonChannel.nml", "na");
    }

    public void testSynapse() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError {

        testComponentToMod("NML2_SynapseTypes.nml", "blockStpSynDepFac");
    }

    public void testIaFCells() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError {

        testComponentToMod("NML2_AbstractCells.nml", "iafTau");
        testComponentToMod("NML2_AbstractCells.nml", "iafTauRef");
        testComponentToMod("NML2_AbstractCells.nml", "iaf");
        testComponentToMod("NML2_AbstractCells.nml", "iafRef");
    }
    
    public void testInputs() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError {

        testComponentToMod("NML2_Inputs.nml", "pulseGen");
        testComponentToMod("NML2_Inputs.nml", "sineGen");
        testComponentToMod("NML2_Inputs.nml", "rampGen");
        testComponentToMod("NML2_Inputs.nml", "vClamp");
    }

    public void testComponentToMod(String nmlFilename, String compId) throws ContentError, ParseError, ParseException, BuildException, XMLException, ConnectionError, RuntimeError, IOException {
        E.info("Loading: " + nmlFilename);
        
		String content = JUtil.getRelativeResource(this.getClass(), Main.getNeuroMLExamplesResourcesDir()+"/"+nmlFilename);
        
    	String nmlLems = NeuroMLConverter.convertNeuroML2ToLems(content);
        
        Lems lems = Utils.readLemsNeuroMLFile(nmlLems).getLems();
        Component comp = lems.getComponent(compId);
        E.info("Found component: " + comp);

        String modFile = NeuronWriter.generateModFile(comp);

        String origName = comp.getComponentType().getName();
        String newName = "MOD_" + compId;

        modFile = modFile.replaceAll(origName, newName);
        File newMechFile = new File(AppTest.getTempDir(), newName + ".mod");

        FileUtil.writeStringToFile(modFile, newMechFile);
        E.info("Written to file: " + newMechFile);
    }

    public void testGetMainScript(String exampleFilename, Lems lems) throws ContentError, ParseError,
            ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException {

        MinimalMessageHandler.setVeryMinimal(true);

        NeuronWriter nw = new NeuronWriter(lems);

        File mainFile = new File(AppTest.getTempDir(), exampleFilename.replaceAll(".xml", "_nrn.py"));

        E.info("Generating NEURON from " + exampleFilename);

        ArrayList<File> genFiles = nw.generateMainScriptAndMods(mainFile);

        assertTrue(genFiles.size() >= 2);

        for (File f : genFiles) {
            E.info("Written model behaviour to: " + f.getAbsolutePath());
            assertTrue(f.exists());
            assertTrue(f.length() > 0);
        }

    }

}
