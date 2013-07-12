package org.neuroml.export;

import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.run.ConnectionError;
import org.lemsml.jlems.core.run.RuntimeError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.ParseException;
import org.lemsml.jlems.core.sim.Sim;
import org.lemsml.jlems.core.type.BuildException;
import org.lemsml.jlems.core.xml.XMLException;
import org.lemsml.jlems.io.util.JUtil;
import org.neuroml.model.util.NeuroML2Validator;
import org.neuroml.model.util.NeuroMLConverter;

import junit.framework.TestCase;

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
			loadNeuroMLExample(filename);
		}
		
	}*/
	
	public void testGetMagnitudeInSI() throws ParseError, ContentError {
		System.out.println("Testing: getMagnitudeInSI()");
		
		assertEquals(-0.06, Utils.getMagnitudeInSI("-60mV"), 1e-6);
		
		assertEquals(50.0, Utils.getMagnitudeInSI("50 Hz"), 1e-6);
	}
	

}
