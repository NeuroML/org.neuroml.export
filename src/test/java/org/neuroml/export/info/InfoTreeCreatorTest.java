package org.neuroml.export.info;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.lemsml.export.base.GenerationException;
import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.run.ConnectionError;
import org.lemsml.jlems.core.run.RuntimeError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.ParseException;
import org.lemsml.jlems.core.type.BuildException;
import org.lemsml.jlems.core.xml.XMLException;
import org.lemsml.jlems.io.util.JUtil;
import org.neuroml.export.Main;
import org.neuroml.export.info.model.InfoNode;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.util.NeuroMLConverter;

/**
 * @author matteocantarelli
 *
 */
public class InfoTreeCreatorTest extends TestCase {

    public void testAbstract() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException, GenerationException, Exception {
        String expected = "Element iafTau:\n"
            + "    ID: iafTau\n"
            + "    leakReversal: -50mV (-0.05 V)\n"
            + "    tau: 30ms (0.03 s)\n"
            + "    thresh: -55mV (-0.055 V)\n"
            + "    reset: -70mV (-0.07 V)\n"
            + "Element iafTauRef:\n"
            + "    ID: iafTauRef\n"
            + "    refract: 5ms (0.005 s)\n"
            + "    leakReversal: -50mV (-0.05 V)\n"
            + "    tau: 30ms (0.03 s)\n"
            + "    thresh: -55mV (-0.055 V)\n"
            + "    reset: -70mV (-0.07 V)\n"
            + "Element iaf:\n"
            + "    ID: iaf\n"
            + "    leakConductance: 0.01uS (1.0E-8 S)\n"
            + "    leakReversal: -50mV (-0.05 V)\n"
            + "    thresh: -55mV (-0.055 V)\n"
            + "    reset: -70mV (-0.07 V)\n"
            + "    C: 0.2nF (2.0E-10 F)\n"
            + "Element iafRef:\n"
            + "    ID: iafRef\n"
            + "    refract: 5ms (0.005 s)\n"
            + "    leakConductance: 0.01uS (1.0E-8 S)\n"
            + "    leakReversal: -50mV (-0.05 V)\n"
            + "    thresh: -55mV (-0.055 V)\n"
            + "    reset: -70mV (-0.07 V)\n"
            + "    C: 0.2nF (2.0E-10 F)\n"
            + "Element izBurst:\n"
            + "    ID: izBurst\n"
            + "    v0: -70mV (-0.07 V)\n"
            + "    a: 0.02\n"
            + "    b: 0.2\n"
            + "    c: -50.0\n"
            + "    d: 2.0\n"
            + "    thresh: 30mV (0.03 V)\n"
            + "Element adExBurst:\n"
            + "    ID: adExBurst\n"
            + "    gL: 30nS (3.0E-8 S)\n"
            + "    EL: -70.6mV (-0.0706 V)\n"
            + "    VT: -50.4mV (-0.0504 V)\n"
            + "    thresh: -40.4mV (-0.0404 V)\n"
            + "    reset: -48.5mV (-0.0485 V)\n"
            + "    delT: 2mV (0.002 V)\n"
            + "    tauw: 40ms (0.04 s)\n"
            + "    refract: 0ms (0.0 s)\n"
            + "    a: 4nS (4.0E-9 S)\n"
            + "    b: 0.08nA (8.0E-11 A)\n"
            + "    C: 281pF (2.81E-10 F)\n"
            + "Element fn1:\n"
            + "    ID: fn1\n"
            + "    I: 0.8";
        String tree = getInfoTreeAsString("NML2_AbstractCells.nml");
        compare(expected, tree);
    }

    public void testCell() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException, GenerationException, Exception {
        String expected = "Cell SpikingCell:\n"
            + "    ID: SpikingCell\n"
            + "    Description: A Simple Spiking cell for testing purposes\n"
            + "    Number of segments: 4\n"
            + "    Number of segment groups: 3\n"
            + "    Channel density: pasChans:\n"
            + "        ID: pasChans\n"
            + "        IonChannel: pas\n"
            + "        Ion: null\n"
            + "        Reversal potential: -70mV (-0.07 V)\n"
            + "        Conductance density: 3.0 S_per_m2\n"
            + "        Segment group: all\n"
            + "    Channel density: naChansSoma:\n"
            + "        ID: naChansSoma\n"
            + "        IonChannel: NaConductance\n"
            + "        Ion: null\n"
            + "        Reversal potential: 50mV (0.05 V)\n"
            + "        Conductance density: 120.0 mS_per_cm2 (1200.0 S_per_m2)\n"
            + "        Segment group: soma_group\n"
            + "    Specific capacitance on group soma_group:\n"
            + "        Value: 1.0 uF_per_cm2 (0.01 F_per_m2)\n"
            + "        Segment group: soma_group\n"
            + "    Specific capacitance on group dendrite_group:\n"
            + "        Value: 2.0 uF_per_cm2 (0.02 F_per_m2)\n"
            + "        Segment group: dendrite_group\n"
            + "    Resistivity on group all:\n"
            + "        Value: 0.1 kohm_cm (1.0 ohm_m)\n"
            + "        Segment group: all";
        
        compare(expected, getInfoTreeAsString("NML2_FullCell.nml"));
    }

    public void testIonChannel() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException, GenerationException, Exception {
        String expected = "Ion Channel na:\n"
            + "    ID: na\n"
            + "    Gates:\n"
            + "        gate m:\n"
            + "            forward rate: FunctionNode [Expression=1000 * (v - (-0.04))/0.01 / ( 1 - exp(-(v - (-0.04)) / 0.01))]\n"
            + "            reverse rate: FunctionNode [Expression=4000 * exp((v - (-0.06))/-0.02)]\n"
            + "            forward rate plot: PlotNode [Title=Standard ChannelML Expression:HHExpLinearRate, X=V, Y=ms-1, Num data points=1]\n"
            + "            reverse rate plot: PlotNode [Title=Standard ChannelML Expression:HHExpRate, X=V, Y=ms-1, Num data points=1]\n"
            + "            instances: 3\n"
            + "        gate h:\n"
            + "            forward rate: FunctionNode [Expression=70 * exp((v - (-0.06))/-0.02)]\n"
            + "            reverse rate: FunctionNode [Expression=1000 /(1 + exp((v - (-0.04))/0.01))]\n"
            + "            forward rate plot: PlotNode [Title=Standard ChannelML Expression:HHExpRate, X=V, Y=ms-1, Num data points=1]\n"
            + "            reverse rate plot: PlotNode [Title=Standard ChannelML Expression:HHSigmoidRate, X=V, Y=ms-1, Num data points=1]\n"
            + "            instances: 1";

        compare(expected, getInfoTreeAsString("NML2_SimpleIonChannel.nml"));
    }

    public void testNetwork() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException, GenerationException, Exception {
        String expected = "Element syn1:\n"
            + "    ID: syn1\n"
            + "    tauDecay: 3ms (0.003 s)\n"
            + "    gbase: 5nS (5.0E-9 S)\n"
            + "    erev: 0mV (0.0 V)\n"
            + "Element syn2:\n"
            + "    ID: syn2\n"
            + "    tauDecay: 2ms (0.002 s)\n"
            + "    gbase: 10nS (1.0E-8 S)\n"
            + "    erev: 0mV (0.0 V)\n"
            + "Element iaf:\n"
            + "    ID: iaf\n"
            + "    leakConductance: 0.05uS (5.0E-8 S)\n"
            + "    leakReversal: -60mV (-0.06 V)\n"
            + "    thresh: -55mV (-0.055 V)\n"
            + "    reset: -62mV (-0.062 V)\n"
            + "    C: 1.0nF (1.0E-9 F)\n"
            + "Element pulseGen1:\n"
            + "    ID: pulseGen1\n"
            + "    delay: 100ms (0.1 s)\n"
            + "    duration: 100ms (0.1 s)\n"
            + "    amplitude: 0.3nA (3.0E-10 A)\n"
            + "Element pulseGen2:\n"
            + "    ID: pulseGen2\n"
            + "    delay: 100ms (0.1 s)\n"
            + "    duration: 100ms (0.1 s)\n"
            + "    amplitude: 0.4nA (4.0E-10 A)\n"
            + "Network InstanceBasedNetwork:\n"
            + "    ID: InstanceBasedNetwork\n"
            + "    Number of populations: 1\n"
            + "    Population iafCells:\n"
            + "        ID: iafCells\n"
            + "        Component: iaf\n"
            + "        Size (number of instances): 3\n"
            + "    Number of projections: 2\n"
            + "    Projection internal1:\n"
            + "        ID: internal1\n"
            + "        Presynaptic population: iafCells\n"
            + "        Postsynaptic population: iafCells\n"
            + "    Projection internal2:\n"
            + "        ID: internal2\n"
            + "        Presynaptic population: iafCells\n"
            + "        Postsynaptic population: iafCells";
        compare(expected, getInfoTreeAsString("NML2_InstanceBasedNetwork.nml"));
    }
    
    private void compare(String s1, String s2)
    {
        //System.out.println("Comparing\n"+"\n----------------------------\n"+s1+"\n----------------------------\n"+s2+"\n----------------------------\n");
        String[] s1a = s1.split("\n");
        String[] s2a = s2.split("\n");
        for (int i=0;i< Math.min(s2a.length, s1a.length);i++) {
            if (!s1a[i].equals(s2a[i]))
                System.out.println("Mismatch:\n"+"\n----------------------------\n"+s1a[i]+"\n----------------------------\n"+s2a[i]+"\n----------------------------\n");
            //Assert.assertEquals(s1, s2);
        }
        if (!s1.equals(s2))
        {
            for (String s2a1 : s2a) {
                System.out.println("            + \"" + s2a1 + "\\n\"");
            }
        }
        Assert.assertEquals(s1, s2);
    }

    /**
     * Test method for
     * {@link org.neuroml.export.info.InfoTreeCreator#createInfoTree(org.neuroml.model.NeuroMLDocument)}.
     *
     * @throws ContentError
     * @throws JAXBException
     */
    private String getInfoTreeAsString(String nmlFilename) throws ContentError, JAXBException, Exception {
        String content = JUtil.getRelativeResource(this.getClass(), Main.getNeuroMLExamplesResourcesDir() + "/" + nmlFilename);
        NeuroMLConverter nmlc = new NeuroMLConverter();
        NeuroMLDocument nmlDocument = nmlc.loadNeuroML(content);
        InfoNode root = InfoTreeCreator.createInfoTree(nmlDocument);
        Assert.assertFalse(root.isEmpty());

        //System.out.println("----------\n" + root.toString() + "\n------------");
        return root.toString();
    }

}
