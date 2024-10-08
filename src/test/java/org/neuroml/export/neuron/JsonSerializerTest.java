package org.neuroml.export.neuron;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import junit.framework.TestCase;
import org.lemsml.jlems.core.logging.MinimalMessageHandler;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.io.util.FileUtil;
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
    
    public void testPyrComplex() throws LEMSException, IOException, GenerationException, NeuroMLException, ModelFeatureSupportException {

        String exampleFilename = "pyrComplex.cell.nml";
        testCreateJson(new File("src/test/resources/examples/"+exampleFilename));
    }
    public void testBask() throws LEMSException, IOException, GenerationException, NeuroMLException, ModelFeatureSupportException {

        String exampleFilename = "bask.cell.nml";
        testCreateJson(new File("src/test/resources/examples/"+exampleFilename));
    }
    public void testL5PC() throws LEMSException, IOException, GenerationException, NeuroMLException, ModelFeatureSupportException {

        String exampleFilename = "L5PC.cell.nml";
        testCreateJson(new File("src/test/resources/examples/"+exampleFilename));
    }


    public void testCreateJson(File exampleFile) throws LEMSException, IOException, GenerationException, NeuroMLException, ModelFeatureSupportException {

        MinimalMessageHandler.setVeryMinimal(true);
        NeuroMLConverter nc = new NeuroMLConverter();
        
        
        NeuroMLDocument nmlDoc = nc.loadNeuroML(exampleFile);
        Cell cell = nmlDoc.getCell().get(0);
        
        String siCell = JSONCellSerializer.cellToJson(cell, NeuronWriter.SupportedUnits.SI);
        //System.out.println("SI: \n"+siCell);
        
        String physCell = JSONCellSerializer.cellToJson(cell, NeuronWriter.SupportedUnits.PHYSIOLOGICAL);
        //System.out.println("Phys: \n"+physCell);
        
        File outFile = new File(exampleFile.getParentFile().getAbsolutePath(), exampleFile.getName().replace(".nml", ".json"));
        FileUtil.writeStringToFile(physCell, outFile);
        System.out.println("Written JSON file to: "+outFile.getAbsolutePath());
        
        
       
    }
    
    public void testConvertFromLems() throws LEMSException, NeuroMLException, IOException {
        
    	Lems lems = Utils.readLemsNeuroMLFile(new File("src/test/resources/examples/LEMS_SomeCells.xml")).getLems();
        
        //String[] cellNames = new String[]{"bask", "pyr_4_sym"};
        String[] cellNames = new String[]{"bask"};
        
        for (String cellName: cellNames) 
        {
            Component cellComp = lems.getComponent(cellName);
            System.out.println("cellComp: "+cellComp);

            NeuroMLConverter nc = new NeuroMLConverter();
            NeuroMLDocument nmlDoc = nc.loadNeuroML(new File("src/test/resources/examples/"+cellName+".cell.nml"));
            Cell cell0 = nmlDoc.getCell().get(0);
            cell0.setNotes(""); // as notes contents doesn't get copied to LEMS

            Cell cell1 = Utils.getCellFromComponent(cellComp, lems);

            String siCell0 = JSONCellSerializer.cellToJson(cell0, NeuronWriter.SupportedUnits.SI);
            System.out.println("Pure NeuroML: \n"+siCell0);
            String siCell1 = JSONCellSerializer.cellToJson(cell1, NeuronWriter.SupportedUnits.SI);
            System.out.println("LEMS -> NeuroML: \n"+siCell1);

            assertEquals(siCell0, siCell1);
        }
    }

}
