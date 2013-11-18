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

	public void testAbstract() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException, GenerationException
	{
		String expected="Element iafRef:\n" + 
				"	ID: iafRef\n" + 
				"Element adExBurst:\n" + 
				"	ID: adExBurst";
		Assert.assertEquals(expected, getInfoTreeAsString("NML2_AbstractCells.nml"));
	}

	public void testCell() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException, GenerationException
	{
		String expected="Cell SpikingCell:\n" + 
				"	ID: SpikingCell\n" + 
				"	Description: A Simple Spiking cell for testing purposes\n" + 
				"	Number of segments: 4";
		Assert.assertEquals(expected, getInfoTreeAsString("NML2_FullCell.nml"));
	}

	public void testIonChannel() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException, GenerationException
	{
		String expected="Ion Channel na:\n" + 
				"	ID: na\n" + 
				"	Gates:\n" + 
				"		gate m:\n" + 
				"			instances: 3\n" + 
				"			forward rate: 1000.00 * (v - -0.0400000)/0.0100000 / ( 1 - exp(-(v - -0.0400000) / 0.0100000))\n" + 
				"			reverse rate: 4000.00 * exp((v - -0.0650000)/-0.0180000)\n";
				 

		Assert.assertTrue(getInfoTreeAsString("NML2_SimpleIonChannel.nml").startsWith(expected));
	}

	public void testNetwork() throws ContentError, ParseError, ParseException, BuildException, XMLException, IOException, ConnectionError, RuntimeError, JAXBException, GenerationException
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
	private String getInfoTreeAsString(String nmlFilename) throws ContentError, JAXBException
	{
		String content = JUtil.getRelativeResource(this.getClass(), Main.getNeuroMLExamplesResourcesDir()+"/"+nmlFilename);
		NeuroMLConverter nmlc = new NeuroMLConverter();
    	NeuroMLDocument nmlDocument = nmlc.loadNeuroML(content);
		InfoNode root = InfoTreeCreator.createInfoTree(nmlDocument);
		Assert.assertFalse(root.isEmpty());
		return root.toString();
	}

}
