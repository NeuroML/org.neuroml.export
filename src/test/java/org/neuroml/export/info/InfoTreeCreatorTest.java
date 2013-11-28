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
public class InfoTreeCreatorTest extends TestCase
{

	public void testAbstract() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException, GenerationException, Exception
	{
		String expected="Element iafRef:\n" + 
				"	ID: iafRef\n" + 
				"Element adExBurst:\n" + 
				"	ID: adExBurst";
		Assert.assertEquals(expected, getInfoTreeAsString("NML2_AbstractCells.nml"));
	}

	public void testCell() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException, GenerationException, Exception
	{
		String expected="Cell SpikingCell:\n" + 
				"	ID: SpikingCell\n" + 
				"	Description: A Simple Spiking cell for testing purposes\n" + 
				"	Number of segments: 4";
		Assert.assertEquals(expected, getInfoTreeAsString("NML2_FullCell.nml"));
	}

	public void testIonChannel() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException, GenerationException, Exception
	{
		String expected="Ion Channel na:\n" + 
				"	ID: na\n" + 
				"	Gates:\n" + 
				"		gate m:\n" + 
				"			forward rate: 1000.00 * (v - -0.0400000)/0.0100000 / ( 1 - exp(-(v - -0.0400000) / 0.0100000))\n" + 
				"			reverse rate: 4000.00 * exp((v - -0.0650000)/-0.0180000)\n" + 
				"			forward rate plot: PlotNode [Title=Standard ChannelML Expression:HHExpLinearRate, X=V, Y=ms-1, Data=-0.0800 74.6294-0.0750 108.9818-0.0700 157.1871-0.0650 223.5637-0.0600 313.0353-0.0550 430.8253-0.0500 581.9767-0.0450 770.7470-0.0400 1000.0000-0.0350 1270.7470-0.0300 1581.9767-0.0250 1930.8253-0.0200 2313.0352-0.0150 2723.5637-0.0100 3157.1871-0.0050 3608.98180.0000 4074.62940.0050 4550.55210.0100 5033.91830.0150 5522.56950.0200 6014.90950.0250 6509.78710.0300 7006.38910.0350 7504.15050.0400 8002.68470.0450 8501.72990.0500 9001.11090.0550 9500.71130.0600 10000.45420.0650 10500.28930.0700 11000.18390.0750 11500.11670.0800 12000.07390.0850 12500.04680.0900 13000.02960.0950 13500.0187]\n" + 
				"			reverse rate plot: PlotNode [Title=Standard ChannelML Expression:HHExpRate, X=V, Y=ms-1, Data=-0.0800 9203.9051-0.0750 6971.6371-0.0700 5280.7719-0.0650 4000.0005-0.0600 3029.8609-0.0550 2295.0139-0.0500 1738.3930-0.0450 1316.7721-0.0400 997.4089-0.0350 755.5025-0.0300 572.2668-0.0250 433.4721-0.0200 328.3400-0.0150 248.7061-0.0100 188.3862-0.0050 142.69600.0000 108.08720.0050 81.87230.0100 62.01540.0150 46.97450.0200 35.58160.0250 26.95180.0300 20.41500.0350 15.46370.0400 11.71320.0450 8.87230.0500 6.72050.0550 5.09050.0600 3.85590.0650 2.92070.0700 2.21230.0750 1.67580.0800 1.26930.0850 0.96150.0900 0.72830.0950 0.5517]\n"+
				"			instances: 3\n";
				 

		Assert.assertTrue(getInfoTreeAsString("NML2_SimpleIonChannel.nml").startsWith(expected));
	}

	public void testNetwork() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException, GenerationException, Exception
	{
		String expected="Network InstanceBasedNetwork:\n" + 
				"	ID: InstanceBasedNetwork\n" + 
				"	Number of populations: 1\n" + 
				"	Population iafCells:\n" + 
				"		ID: iafCells\n" + 
				"		Size (number of instances): 3\n" + 
				"	Number of projections: 2\n" + 
				"	Projection internal1:\n" + 
				"		ID: internal1\n" + 
				"		Presynaptic population: iafCells\n" + 
				"		Postsynaptic population: iafCells\n" + 
				"	Projection internal2:\n" + 
				"		ID: internal2\n" + 
				"		Presynaptic population: iafCells\n" + 
				"		Postsynaptic population: iafCells";
		Assert.assertEquals(expected, getInfoTreeAsString("NML2_InstanceBasedNetwork.nml"));
	}
	
	/**
	 * Test method for {@link org.neuroml.export.info.InfoTreeCreator#createInfoTree(org.neuroml.model.NeuroMLDocument)}.
	 * @throws ContentError 
	 * @throws JAXBException 
	 */
	private String getInfoTreeAsString(String nmlFilename) throws ContentError, JAXBException, Exception
	{
		String content = JUtil.getRelativeResource(this.getClass(), Main.getNeuroMLExamplesResourcesDir()+"/"+nmlFilename);
		NeuroMLConverter nmlc = new NeuroMLConverter();
    	NeuroMLDocument nmlDocument = nmlc.loadNeuroML(content);
		InfoNode root = InfoTreeCreator.createInfoTree(nmlDocument);
		Assert.assertFalse(root.isEmpty());
		return root.toString();
	}

}
