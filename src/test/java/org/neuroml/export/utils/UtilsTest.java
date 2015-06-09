package org.neuroml.export.utils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import javax.xml.bind.JAXBException;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertEquals;
import junit.framework.TestCase;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.io.util.JUtil;
import org.neuroml.export.AppTest;
import org.neuroml.model.Cell;
import org.neuroml.model.IafTauCell;
import org.neuroml.model.IonChannelHH;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.Standalone;
import org.neuroml.model.util.NeuroML2Validator;
import org.neuroml.model.util.NeuroMLConverter;
import org.neuroml.model.util.NeuroMLException;

public class UtilsTest extends TestCase {
/*
	private void loadNeuroMLExample(String filename) throws ContentError, ParseError, ParseException, BuildException, XMLException, ConnectionError, RuntimeError {
		
		NeuroML2Validator nmlv = new NeuroML2Validator();
		String content = JUtil.getRelativeResource(nmlv.getClass(),
				AppTest.getNeuroMLExamplesResourcesDir()
						+ "/"+filename);

		System.out.println("Found: " + content);
		String lemsVer = NeuroMLConverter.convertNeuroML2ToLems(content);

		System.out.println("Now: " + lemsVer);
		Sim sim = Utils.readLemsNeuroMLFile(lemsVer);

		System.out.println("Parsed the NeuroML file: ");
	}
	
	public void testReadLemsNeuroMLFile() throws ContentError, ParseError, ParseException, BuildException, XMLException, ConnectionError, RuntimeError {

		System.out.println("Testing: readLemsNeuroMLFile(String contents)");
		String[] filenames = new String[]{"NML2_SynapseTypes.nml", "NML2_SimpleIonChannel.nml", "NML2_SimpleMorphology.nml", "NML2_FullNeuroML.nml", "NML2_PyNNCells.nml"};
		
		for (String filename: filenames) {
            
			Utils.readLemsNeuroMLFile(new File(filename));
		}
	}*/
    
    

    public static Lems readLemsFileFromExamples(String exampleFilename) throws LEMSException
    {
    	NeuroML2Validator nmlv = new NeuroML2Validator();
    	
		String content = JUtil.getRelativeResource(nmlv.getClass(), Utils.LEMS_EXAMPLES_RESOURCES_DIR + "/" + exampleFilename);
		
		return Utils.readLemsNeuroMLFile(content).getLems();
    }
    
    public static Lems readNeuroMLFileFromExamples(String exampleFilename) throws LEMSException
    {
    	NeuroML2Validator nmlv = new NeuroML2Validator();
    	
		String content = JUtil.getRelativeResource(nmlv.getClass(), Utils.NEUROML_EXAMPLES_RESOURCES_DIR+"/"+exampleFilename);
		
		return Utils.readNeuroMLFile(content).getLems();
    }
    
    public static File getTempDir()
    {
	    String tempDirName = System.getProperty("user.dir") + File.separator + "src/test/resources/tmp";
	    File tempDir = new File(tempDirName);
	    if (!tempDir.exists())
	    	tempDir.mkdir();
	    return tempDir;
    }
    
	
	public void testGetMagnitudeInSI() throws NeuroMLException {
		System.out.println("Testing: getMagnitudeInSI()");
		
		assertEquals(-0.06, Utils.getMagnitudeInSI("-60mV"), 1e-6);
		assertEquals(50.0, Utils.getMagnitudeInSI("50 Hz"), 1e-6);
		assertEquals(0.3, Utils.getMagnitudeInSI("0.3 ohm_m"), 1e-6);
		assertEquals(0.3, Utils.getMagnitudeInSI("0.03 kohm_cm"), 1e-6);
		assertEquals(60, Utils.getMagnitudeInSI("1 min"), 1e-6);
		assertEquals(1f/3600, Utils.getMagnitudeInSI("1 per_hour"), 1e-6);
		assertEquals(1e-3, Utils.getMagnitudeInSI("1 litre"), 1e-6);
	}
    
    
    public void testFilesInJar() throws IOException, ContentError
    {
        String ret = JUtil.getRelativeResource(this.getClass(), "/LEMSexamples/LEMS_NML2_Ex0_IaF.xml");
        ret = JUtil.getRelativeResource(this.getClass(), "/examples/NML2_SingleCompHHCell.nml");
        //ret = JUtil.getRelativeResource(this.getClass(), "/examples/../examples/NML2_SimpleIonChannel.nml");
    }
    
    public void testConvertNeuroMLToComponent() throws JAXBException, Exception {
        
        IafTauCell iaf = new IafTauCell();
        iaf.setTau("10ms");
        iaf.setLeakReversal("-60mV");
        iaf.setReset("-70mV");
        iaf.setThresh("-40mV");
        iaf.setId("iaf00");
        System.out.println("Converting: "+iaf);
        Component comp = Utils.convertNeuroMLToComponent(iaf);
        System.out.println("Now: "+comp.details("    "));
        
        assertEquals(comp.getStringValue("tau"), iaf.getTau());
        assertEquals((float)comp.getParamValue("tau").getDoubleValue(), Utils.getMagnitudeInSI(iaf.getTau()));
        
    }
    
    
    public void testInteractionLemsNeuroMLModels() throws LEMSException, NeuroMLException {
        
		String exampleFilename = "NML2_SingleCompHHCell.nml";
		Lems lems = UtilsTest.readNeuroMLFileFromExamples(exampleFilename);
        
        NeuroMLConverter nc = new NeuroMLConverter();
        
		String content = JUtil.getRelativeResource(nc.getClass(), Utils.NEUROML_EXAMPLES_RESOURCES_DIR+"/"+exampleFilename);
        NeuroMLDocument nmlDoc = nc.loadNeuroML(content);
        
        LinkedHashMap<String,Standalone> stands = NeuroMLConverter.getAllStandaloneElements(nmlDoc);
        
        
        System.out.println("Comparing contents of LEMS (CTs: "+lems.getComponentTypes().size()+"; Cs: "+lems.getComponents().size()
                +") and "+nmlDoc.getId()+" (standalones: "+stands.size()+")");
        
        for (String id: stands.keySet()) {
            System.out.println("-- NeuroML element: "+id);
            Standalone s = stands.get(id);
            System.out.println("    NML Element: "+s.getId());
            Component comp = lems.getComponent(id);
            System.out.println("    LEMS comp: "+comp.getID());
            
            if (s instanceof IonChannelHH) {
                IonChannelHH ic = (IonChannelHH)s;
                System.out.println("    Found IonChannelHH: "+ic.getId()+" (LEMS: "+comp.getID()+")");
                System.out.println("    Conductance: "+ic.getConductance()+" (LEMS: "+comp.getAttributeValue("conductance")+")");
            }
            
        }
    }
   
    
    public void testParseCellRefString() throws JAXBException, Exception {
        
        String r1 = "../Pop0[0]";
        String r2 = "../Gran/0/Granule_98";
        assertEquals("Pop0", Utils.parseCellRefStringForPopulation(r1));        
        assertEquals("Gran", Utils.parseCellRefStringForPopulation(r2));
        assertEquals(0, Utils.parseCellRefStringForCellNum(r1));
        assertEquals(0, Utils.parseCellRefStringForCellNum(r2));

    }
    
	public void testReplaceInExpression() {
        String before = "before";
        String after = "after";
        
        String[] simpleCatch = new String[]{"g + before","before + before", "before^2", "(before)+2", "before"};
        String[] dontCatch = new String[]{"beforee", "hbefore + after", "5+_before"};
        
        for (String s: simpleCatch) {
            System.out.println("From: "+s);
            String n = Utils.replaceInExpression(s, before, after);
            System.out.println("To:   "+n);
            assertEquals(s.replaceAll(before, after).replaceAll("\\s+",""), n.replaceAll("\\s+",""));
        }
        for (String s: dontCatch) {
            System.out.println("From: "+s);
            String n = Utils.replaceInExpression(s, before, after);
            System.out.println("To:   "+n);
            assertEquals(s.replaceAll("\\s+",""), n.replaceAll("\\s+",""));
        }
    }
    
    public static void checkConvertedFiles(List<File> files) 
    {
		assertTrue(files.size()>=1);
        
        for(File genFile : files)
		{
            E.info("Checking generated file: " + genFile.getAbsolutePath());
			assertTrue(genFile.exists());
			assertTrue(genFile.length()>0);
		}
    }
    
        

}
