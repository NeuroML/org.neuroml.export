package org.neuroml.export.neuron;

import java.io.File;
import java.io.IOException;
import junit.framework.TestCase;
import org.lemsml.jlems.core.logging.MinimalMessageHandler;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Utils;
import org.neuroml.model.Cell;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.util.NeuroMLConverter;
import org.neuroml.model.util.NeuroMLException;

public class JsonSerializerTest extends TestCase {

    
    public void testPyr() throws LEMSException, IOException, GenerationException, NeuroMLException, ModelFeatureSupportException {

        String exampleFilename = "pyr_4_sym.cell.nml";
        testCreateJson(new File("src/test/resources/examples/"+exampleFilename));
    }
    public void testBask() throws LEMSException, IOException, GenerationException, NeuroMLException, ModelFeatureSupportException {

        String exampleFilename = "bask.cell.nml";
        testCreateJson(new File("src/test/resources/examples/"+exampleFilename));
    }

	

    public void testCreateJson(File exampleFile) throws LEMSException, IOException, GenerationException, NeuroMLException, ModelFeatureSupportException {

        MinimalMessageHandler.setVeryMinimal(true);
        NeuroMLConverter nc = new NeuroMLConverter();
        
        
        NeuroMLDocument nmlDoc = nc.loadNeuroML(exampleFile);
        Cell cell = nmlDoc.getCell().get(0);
        
        String siCell = JSONCellSerializer.cellToJson(cell, NeuronWriter.SupportedUnits.SI);
        System.out.println("SI: \n"+siCell);
        
        String physCell = JSONCellSerializer.cellToJson(cell, NeuronWriter.SupportedUnits.PHYSIOLOGICAL);
        System.out.println("Phys: \n"+physCell);
        

    }

}
