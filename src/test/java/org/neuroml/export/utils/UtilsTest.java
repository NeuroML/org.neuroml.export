package org.neuroml.export.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import javax.xml.bind.JAXBException;
import junit.framework.TestCase;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.DimensionalQuantity;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.ParamValue;
import org.lemsml.jlems.core.type.QuantityReader;
import org.lemsml.jlems.io.util.JUtil;
import org.neuroml.export.neuron.LEMSQuantityPathNeuron;
import org.neuroml.model.Cell;
import org.neuroml.model.IafTauCell;
import org.neuroml.model.IonChannelHH;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.Standalone;
import org.neuroml.model.util.NeuroML2Validator;
import org.neuroml.model.util.NeuroMLConverter;
import org.neuroml.model.util.NeuroMLException;

public class UtilsTest extends TestCase
{

    public static Lems readLemsFileFromExamples(String exampleFilename) throws LEMSException
    {
        NeuroML2Validator nmlv = new NeuroML2Validator();

        String content = JUtil.getRelativeResource(nmlv.getClass(), Utils.LEMS_EXAMPLES_RESOURCES_DIR + "/" + exampleFilename);

        return Utils.readLemsNeuroMLFile(content).getLems();
    }

    public static Lems readNeuroMLFileFromExamples(String exampleFilename) throws LEMSException
    {
        NeuroML2Validator nmlv = new NeuroML2Validator();

        String content = JUtil.getRelativeResource(nmlv.getClass(), Utils.NEUROML_EXAMPLES_RESOURCES_DIR + "/" + exampleFilename);

        return Utils.readNeuroMLFile(content).getLems();
    }

    public static NeuroMLDocument readNeuroMLDocumentFromExamples(String exampleFilename) throws LEMSException, NeuroMLException, IOException
    {
        NeuroML2Validator nmlv = new NeuroML2Validator();
        NeuroMLConverter nmlc = new NeuroMLConverter();
        
        String content = JUtil.getRelativeResource(nmlv.getClass(), Utils.NEUROML_EXAMPLES_RESOURCES_DIR + "/" + exampleFilename);

        return nmlc.loadNeuroML(content);
    }

    public static File getTempDir()
    {
        String tempDirName = System.getProperty("user.dir") + File.separator + "src/test/resources/tmp";
        File tempDir = new File(tempDirName);
        if (!tempDir.exists())
        {
            tempDir.mkdir();
        }
        return tempDir;
    }

    public void testGetMagnitudeInSI() throws NeuroMLException
    {
        System.out.println("Testing: getMagnitudeInSI()");

        assertEquals(-0.06, Utils.getMagnitudeInSI("-60mV"), 1e-6);
        assertEquals(50.0, Utils.getMagnitudeInSI("50 Hz"), 1e-6);
        assertEquals(0.3, Utils.getMagnitudeInSI("0.3 ohm_m"), 1e-6);
        assertEquals(0.3, Utils.getMagnitudeInSI("0.03 kohm_cm"), 1e-6);
        assertEquals(60, Utils.getMagnitudeInSI("1 min"), 1e-6);
        assertEquals(1f / 3600, Utils.getMagnitudeInSI("1 per_hour"), 1e-6);
        assertEquals(1e-3, Utils.getMagnitudeInSI("1 litre"), 1e-6);
    }

    public void testFilesInJar() throws IOException, ContentError
    {
        String ret = JUtil.getRelativeResource(this.getClass(), "/LEMSexamples/LEMS_NML2_Ex0_IaF.xml");
        ret = JUtil.getRelativeResource(this.getClass(), "/examples/NML2_SingleCompHHCell.nml");
        //ret = JUtil.getRelativeResource(this.getClass(), "/examples/../examples/NML2_SimpleIonChannel.nml");
    }

    public void testConvertNeuroMLToComponent() throws JAXBException, Exception
    {

        IafTauCell iaf = new IafTauCell();
        iaf.setTau("10ms");
        iaf.setLeakReversal("-60mV");
        iaf.setReset("-70mV");
        iaf.setThresh("-40mV");
        iaf.setId("iaf00");
        System.out.println("Converting: " + iaf);
        Component comp = Utils.convertNeuroMLToComponent(iaf);
        System.out.println("Now: " + comp.details("    "));

        assertEquals(comp.getStringValue("tau"), iaf.getTau());
        assertEquals((float) comp.getParamValue("tau").getDoubleValue(), Utils.getMagnitudeInSI(iaf.getTau()));

    }

    public void testInteractionLemsNeuroMLModels() throws LEMSException, NeuroMLException
    {

        String exampleFilename = "NML2_SingleCompHHCell.nml";
        Lems lems = UtilsTest.readNeuroMLFileFromExamples(exampleFilename);

        NeuroMLConverter nc = new NeuroMLConverter();

        String content = JUtil.getRelativeResource(nc.getClass(), Utils.NEUROML_EXAMPLES_RESOURCES_DIR + "/" + exampleFilename);
        NeuroMLDocument nmlDoc = nc.loadNeuroML(content);

        LinkedHashMap<String, Standalone> stands = NeuroMLConverter.getAllStandaloneElements(nmlDoc);

        System.out.println("Comparing contents of LEMS (CTs: " + lems.getComponentTypes().size() + "; Cs: " + lems.getComponents().size()
                + ") and " + nmlDoc.getId() + " (standalones: " + stands.size() + ")");

        for (String id : stands.keySet())
        {
            System.out.println("-- NeuroML element: " + id);
            Standalone s = stands.get(id);
            System.out.println("    NML Element: " + s.getId());
            Component comp = lems.getComponent(id);
            System.out.println("    LEMS comp: " + comp.getID());

            if (s instanceof IonChannelHH)
            {
                IonChannelHH ic = (IonChannelHH) s;
                String conductance = "conductance";
                
                System.out.println("    Found IonChannelHH: " + ic.getId() + " (LEMS: " + comp.getID() + ")");
                
                ParamValue lemsParam = comp.getParamValue(conductance);
                String siSymbol = Utils.getSIUnitInNeuroML(lemsParam.getFinalParam().getDimension()).getSymbol();
                
                System.out.println("    Conductance: " + ic.getConductance() + 
                        " (LEMS: " + lemsParam.getDoubleValue()+ " " + siSymbol+")");
                String newCond = "20pS";
                ic.setConductance(newCond);
                
                DimensionalQuantity dq = QuantityReader.parseValue(newCond, lems.getUnits());
                lemsParam.setDoubleValue(dq.getDoubleValue());
                
                System.out.println("    Conductance: " + ic.getConductance() + 
                        " (LEMS: " + lemsParam.getDoubleValue()+ " " + siSymbol+")");
                
                System.out.println("       LEMS comp:"+comp.summary());
            }

        }
    }

    public void testParseCellRefString() throws JAXBException, Exception
    {

        String r1 = "../Pop0[0]";
        String r2 = "../Gran/0/Granule_98";
        assertEquals("Pop0", Utils.parseCellRefStringForPopulation(r1));
        assertEquals("Gran", Utils.parseCellRefStringForPopulation(r2));
        assertEquals(0, Utils.parseCellRefStringForCellNum(r1));
        assertEquals(0, Utils.parseCellRefStringForCellNum(r2));

    }

    public void testReplaceInExpression()
    {
        String before = "before";
        String after = "after";

        String[] simpleCatch = new String[]
        {
            "g + before", "before + before", "before^2", "(before)+2", "before"
        };
        String[] dontCatch = new String[]
        {
            "beforee", "hbefore + after", "5+_before"
        };

        for (String s : simpleCatch)
        {
            System.out.println("From: " + s);
            String n = Utils.replaceInExpression(s, before, after);
            System.out.println("To:   " + n);
            assertEquals(s.replaceAll(before, after).replaceAll("\\s+", ""), n.replaceAll("\\s+", ""));
        }
        for (String s : dontCatch)
        {
            System.out.println("From: " + s);
            String n = Utils.replaceInExpression(s, before, after);
            System.out.println("To:   " + n);
            assertEquals(s.replaceAll("\\s+", ""), n.replaceAll("\\s+", ""));
        }
    }

    public static void checkConvertedFiles(List<File> files)
    {
        assertTrue(files.size() >= 1);

        for (File genFile : files)
        {
            E.info("Checking generated file: " + genFile.getAbsolutePath());
            assertTrue(genFile.exists());
            assertTrue(genFile.length() > 0);
        }
    }
    
    public void testLEMSQuantityPath()
    {
        ArrayList<String> paths = new ArrayList<String>();
        paths.add("X1__S");
        paths.add("hhpop[6]/bioPhys1/membraneProperties/naChans/naChan/m/q");
        paths.add("fnPop1[0]/V");
        paths.add("Gran/0/Granule_98/v");
        paths.add("TestBasket/0/pvbasketcell/v");
        paths.add("TestBasket/0/pvbasketcell/3/v");
        paths.add("One_ChannelML/0/OneComp_ChannelML/biophys/membraneProperties/Na_ChannelML_all/Na_ChannelML/m/q");
        paths.add("One_ChannelML/0/OneComp_ChannelML/4/biophys/membraneProperties/Na_ChannelML_all/Na_ChannelML/m/q");
        paths.add("pasPop1[0]/synapses:nmdaSyn1:0/g");
        paths.add("pasPop1/0/pasCell/synapses:nmdaSyn1:0/g");
        
        paths.add("pasPop1/0/pasCell/0/synapses:nmdaSyn1:0/g");
        paths.add("pop0/1/MultiCompCell/2/synapses:AMPA:0/g");
        paths.add("pop0/1/MultiCompCell/synapses:AMPA:0/g");
        paths.add("pop0/1/MultiCompCell/synapses:AMPA:0/g");
        paths.add("hhpop/0/hhneuron/biophysics/membraneProperties/kChans/gDensity");
        paths.add("hhpop/0/hhneuron/IClamp/i");

        for (String path : paths)
        {
            LEMSQuantityPath l1 = new LEMSQuantityPath(path);
            System.out.println("\n--------\n" + l1);
        }
        
    }
    
    
    public void testLEMSQuantityPathNeuron() throws ContentError, LEMSException, NeuroMLException, IOException
    {
        HashMap<String, String> compMechNamesHoc = new HashMap<String, String>();
        
        compMechNamesHoc.put("fnPop1[i]", "m_fitzHughNagumoCell[i]");
        ArrayList<Component> popsOrComponents = new ArrayList<Component>();
        ArrayList<String> paths = new ArrayList<String>();
        paths.add("fnPop1[0]/V");

        for (String path : paths)
        {
            LEMSQuantityPathNeuron l1 = new LEMSQuantityPathNeuron(path, "1", null, compMechNamesHoc, popsOrComponents, null, null, null);
            System.out.println("\n--------\n" + l1);
            assertTrue(l1.valid());
        }
        
        compMechNamesHoc = new HashMap<String, String>();
        
        paths = new ArrayList<String>();
        paths.add("hhpop[0]/v");
        paths.add("hhpop[0]/bioPhys1/membraneProperties/naChans/gDensity");
        paths.add("hhpop[0]/bioPhys1/membraneProperties/naChans/naChan/m/q");
        paths.add("hhpop[0]/bioPhys1/membraneProperties/naChans/iDensity");
        //paths.add("hhpop/0/hhneuron/IClamp/i");
        
        Lems lems = UtilsTest.readLemsFileFromExamples("LEMS_NML2_Ex5_DetCell.xml");
        NeuroMLDocument nmldoc = readNeuroMLDocumentFromExamples("NML2_SingleCompHHCell.nml");
        
        Component hhnet = lems.getComponent("net1");
        popsOrComponents = hhnet.getChildrenAL("populations");
        Cell c = nmldoc.getCell().get(0);
        HashMap<String, Cell> compIdsVsCells = new HashMap<String, Cell>();
        HashMap<String, String> hocRefsVsInputs = new HashMap<String, String>();
        
        for (String path : paths)
        {
            LEMSQuantityPathNeuron l1 = new LEMSQuantityPathNeuron(path, "1", hhnet, compMechNamesHoc, popsOrComponents, compIdsVsCells, hocRefsVsInputs, lems);
            System.out.println("\n===================\n" + l1);
            assertTrue(l1.valid());
            
        }
        
    }
    
    

}
