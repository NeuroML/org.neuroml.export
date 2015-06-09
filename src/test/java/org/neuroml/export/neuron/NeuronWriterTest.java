package org.neuroml.export.neuron;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBException;

import junit.framework.TestCase;

import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.logging.MinimalMessageHandler;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.io.util.FileUtil;
import org.lemsml.jlems.io.util.JUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.utils.UtilsTest;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Utils;
import org.neuroml.model.util.NeuroMLConverter;
import org.neuroml.model.util.NeuroMLException;

public class NeuronWriterTest extends TestCase {

    
    public void testHH() throws LEMSException, IOException, GenerationException, NeuroMLException, ModelFeatureSupportException {

        String exampleFilename = "LEMS_NML2_Ex5_DetCell.xml";
        testGetMainScript(exampleFilename);
    }
    /*
    public void testQ10() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException {

        String exampleFilename = "LEMS_NML2_Ex10_Q10.xml";
        Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);
        testGetMainScript(exampleFilename, lems);
    }*/

    public void testFN() throws LEMSException, IOException, GenerationException, NeuroMLException, ModelFeatureSupportException {

        String exampleFilename = "LEMS_NML2_Ex9_FN.xml";
        testGetMainScript(exampleFilename);
    }

    public void testGHK() throws LEMSException, IOException, GenerationException, NeuroMLException, JAXBException, ModelFeatureSupportException {
    	
    	 String exampleFilename = "LEMS_NML2_Ex18_GHK.xml";
         testGetMainScript(exampleFilename);
     }
       
    
    public void testChannel() throws LEMSException, IOException, GenerationException {

        testComponentToMod("NML2_SimpleIonChannel.nml", "na");
    }

    public void testSynapse() throws LEMSException, IOException, GenerationException {

        testComponentToMod("NML2_SynapseTypes.nml", "blockStpSynDepFac");
    }

    public void testIaFCells() throws LEMSException, IOException, GenerationException {

        testComponentToMod("NML2_AbstractCells.nml", "iafTau");
        testComponentToMod("NML2_AbstractCells.nml", "iafTauRef");
        testComponentToMod("NML2_AbstractCells.nml", "iaf");
        testComponentToMod("NML2_AbstractCells.nml", "iafRef");
    }
    
    public void testInputs() throws LEMSException, IOException, GenerationException {

        testComponentToMod("NML2_Inputs.nml", "pulseGen");
        testComponentToMod("NML2_Inputs.nml", "sineGen");
        testComponentToMod("NML2_Inputs.nml", "rampGen");
        testComponentToMod("NML2_Inputs.nml", "vClamp");
    }

    public void testComponentToMod(String nmlFilename, String compId) throws LEMSException, IOException, GenerationException {
        E.info("Loading: " + nmlFilename);
        
		String content = JUtil.getRelativeResource(this.getClass(), Utils.NEUROML_EXAMPLES_RESOURCES_DIR+"/"+nmlFilename);
        
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
    
    public void testSBML() throws LEMSException, IOException, GenerationException, JAXBException, NeuroMLException, ModelFeatureSupportException {

    	File exampleSBML = new File("src/test/resources/BIOMD0000000185_LEMS.xml");
    	generateMainScript(exampleSBML);
	}
	
	public void generateMainScript(File localFile) throws LEMSException, IOException, GenerationException, JAXBException, NeuroMLException, ModelFeatureSupportException {

    	Lems lems = Utils.readLemsNeuroMLFile(FileUtil.readStringFromFile(localFile)).getLems();
       
        NeuronWriter nw = new NeuronWriter(lems, AppTest.getTempDir(), localFile.getName().replaceAll(".xml", "_nrn.py"));
        List<File> outputFiles = nw.convert();

        assertTrue(outputFiles.size() >= 2);

		UtilsTest.checkConvertedFiles(outputFiles);
    }

    public void testGetMainScript(String exampleFilename) throws LEMSException, IOException, GenerationException, NeuroMLException, ModelFeatureSupportException {

        MinimalMessageHandler.setVeryMinimal(true);
        Lems lems = AppTest.readLemsFileFromExamples(exampleFilename);

        NeuronWriter nw = new NeuronWriter(lems, AppTest.getTempDir(), exampleFilename.replaceAll(".xml", "_nrn.py"));
        List<File> outputFiles = nw.convert();

        assertTrue(outputFiles.size() >= 2);

		UtilsTest.checkConvertedFiles(outputFiles);

    }

}
