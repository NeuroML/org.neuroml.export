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
import org.lemsml.jlems.core.type.DimensionalQuantity;
import org.lemsml.jlems.core.type.QuantityReader;
import java.util.*;

public class NRNUtilsTest extends TestCase {


    public void testUnits() throws LEMSException, IOException, GenerationException, NeuroMLException, ModelFeatureSupportException {


        List<String> nrnVals = Arrays.asList("1", "1mV","1ms","1per_mV","1per_ms","1uS","1um","1nA","1S_per_cm2","1mA_per_cm2");

        for (String nrnVal : nrnVals) {
          float si = Utils.getMagnitudeInSI(nrnVal);
          String dim = Utils.getDimension(nrnVal).getName();
          float nrnSi = NRNUtils.convertToNeuronUnits(1, dim);
          float exp = 1/si;

          System.out.println("Checking "+nrnVal+" = "+si+" "+dim+" in SI; so 1 in SI should be "+exp+", is "+nrnSi+" "+NRNUtils.getNeuronUnit(dim));

          assertEquals(exp, nrnSi, nrnSi*0.0001);
        }


        //assertEquals(x*1000, NRNUtils.convertToNeuronUnits(x, "conductanceDensity"));

    }
    public static void main(String args[]) throws LEMSException, IOException, GenerationException, NeuroMLException, ModelFeatureSupportException
    {
      NRNUtilsTest n = new NRNUtilsTest();
      n.testUnits();
    }

}
