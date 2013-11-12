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
				"			reverse rate: 4000.00 * exp((v - -0.0650000)/-0.0180000)\n" + 
				"			forward rate plot: PlotNode [Title=RatePlot, X=mV, Y=ms-1, Data=[Data [X=[-8.0, -7.9, -7.8, -7.7000003, -7.6, -7.5, -7.4, -7.3, -7.2000003, -7.1, -7.0, -6.9, -6.8, -6.7000003, -6.6, -6.5, -6.4, -6.3, -6.2000003, -6.1, -6.0, -5.9, -5.8, -5.7000003, -5.6, -5.5, -5.4, -5.3, -5.2000003, -5.1, -5.0, -4.9, -4.8, -4.7000003, -4.6, -4.5, -4.4, -4.3, -4.2000003, -4.1, -4.0, -3.9, -3.8, -3.7, -3.6000001, -3.5, -3.4, -3.3, -3.2, -3.1000001, -3.0, -2.9, -2.8, -2.7, -2.6000001, -2.5, -2.4, -2.3, -2.2, -2.1000001, -2.0, -1.9, -1.8000001, -1.7, -1.6, -1.5, -1.4, -1.3000001, -1.2, -1.1, -1.0, -0.90000004, -0.8, -0.7, -0.6, -0.5, -0.4, -0.3, -0.2, -0.1, 0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.90000004, 1.0, 1.1, 1.2, 1.3000001, 1.4, 1.5, 1.6, 1.7, 1.8000001, 1.9, 2.0, 2.1000001, 2.2, 2.3, 2.4, 2.5, 2.6000001, 2.7, 2.8, 2.9, 3.0, 3.1000001, 3.2, 3.3, 3.4, 3.5, 3.6000001, 3.7, 3.8, 3.9, 4.0, 4.1, 4.2000003, 4.3, 4.4, 4.5, 4.6, 4.7000003, 4.8, 4.9, 5.0, 5.1, 5.2000003, 5.3, 5.4, 5.5, 5.6, 5.7000003, 5.8, 5.9, 6.0, 6.1, 6.2000003, 6.3, 6.4, 6.5, 6.6, 6.7000003, 6.8, 6.9, 7.0, 7.1, 7.2000003, 7.3, 7.4, 7.5, 7.6, 7.7000003, 7.8, 7.9, 8.0, 8.1, 8.2, 8.3, 8.400001, 8.5, 8.6, 8.7, 8.8, 8.900001, 9.0, 9.1, 9.2, 9.3, 9.400001, 9.5, 9.6, 9.7, 9.8, 9.900001], Y=[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 9.8E-45, 1.94985E-40, 3.8474506E-36, 7.489158E-32, 1.4325445E-27, 2.6773002E-23, 4.8440844E-19, 8.3502515E-15, 1.3283609E-10, 1.800563E-6, 0.014909465, 4.07463, 14.000012, 24.000004, 34.0, 44.000004, 54.000008, 64.00001, 74.0, 84.000015, 94.000015, 104.00001, 114.00001, 124.00001, 134.0, 144.0, 154.0, 164.00002, 174.00002, 184.00002, 194.00002, 204.00002, 214.00003, 224.00002, 234.00002, 244.00003, 254.00002, 264.00003, 274.0, 284.0, 294.0, 304.0, 314.00006, 324.00003, 334.00003, 344.00003, 354.00003, 364.00006, 374.00003, 384.00003, 394.00003, 404.00003, 414.00003, 424.00006, 434.00006, 444.00006, 454.00003, 464.00003, 474.00006, 484.00006, 494.00006, 504.00003, 514.0, 524.00006, 534.0, 544.0, 554.0, 564.0, 574.00006, 584.0, 594.0, 604.0, 614.0, 624.0001, 634.0, 644.00006, 654.00006, 664.00006, 674.0001, 684.0001, 694.00006, 704.00006, 714.00006, 724.0001, 734.0001, 744.00006, 754.00006, 764.00006, 774.0001, 784.0001, 794.00006, 804.00006, 814.0001, 824.00006, 834.0001, 844.0001, 854.00006, 864.0001, 874.00006, 884.0001, 894.0001, 904.00006, 914.0001, 924.00006, 934.0001, 944.0001, 954.00006, 964.0001, 974.00006, 984.0001, 994.0001], Label=HHExpLinearRate]]]\n" + 
				"			reverse rate plot: PlotNode [Title=RatePlot, X=mV, Y=ms-1, Data=[Data [X=[-8.0, -7.9, -7.8, -7.7000003, -7.6, -7.5, -7.4, -7.3, -7.2000003, -7.1, -7.0, -6.9, -6.8, -6.7000003, -6.6, -6.5, -6.4, -6.3, -6.2000003, -6.1, -6.0, -5.9, -5.8, -5.7000003, -5.6, -5.5, -5.4, -5.3, -5.2000003, -5.1, -5.0, -4.9, -4.8, -4.7000003, -4.6, -4.5, -4.4, -4.3, -4.2000003, -4.1, -4.0, -3.9, -3.8, -3.7, -3.6000001, -3.5, -3.4, -3.3, -3.2, -3.1000001, -3.0, -2.9, -2.8, -2.7, -2.6000001, -2.5, -2.4, -2.3, -2.2, -2.1000001, -2.0, -1.9, -1.8000001, -1.7, -1.6, -1.5, -1.4, -1.3000001, -1.2, -1.1, -1.0, -0.90000004, -0.8, -0.7, -0.6, -0.5, -0.4, -0.3, -0.2, -0.1, 0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.90000004, 1.0, 1.1, 1.2, 1.3000001, 1.4, 1.5, 1.6, 1.7, 1.8000001, 1.9, 2.0, 2.1000001, 2.2, 2.3, 2.4, 2.5, 2.6000001, 2.7, 2.8, 2.9, 3.0, 3.1000001, 3.2, 3.3, 3.4, 3.5, 3.6000001, 3.7, 3.8, 3.9, 4.0, 4.1, 4.2000003, 4.3, 4.4, 4.5, 4.6, 4.7000003, 4.8, 4.9, 5.0, 5.1, 5.2000003, 5.3, 5.4, 5.5, 5.6, 5.7000003, 5.8, 5.9, 6.0, 6.1, 6.2000003, 6.3, 6.4, 6.5, 6.6, 6.7000003, 6.8, 6.9, 7.0, 7.1, 7.2000003, 7.3, 7.4, 7.5, 7.6, 7.7000003, 7.8, 7.9, 8.0, 8.1, 8.2, 8.3, 8.400001, 8.5, 8.6, 8.7, 8.8, 8.900001, 9.0, 9.1, 9.2, 9.3, 9.400001, 9.5, 9.6, 9.7, 9.8, 9.900001], Y=[Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, 1.67873E35, 6.4898747E32, 2.5089486E30, 9.699304E27, 3.749696E25, 1.4495945E23, 5.604028E20, 2.16646892E18, 8.3753816E15, 3.23786239E13, 1.25172965E11, 4.839088E8, 1870753.1, 7232.1733, 27.959002, 0.10808723, 4.1785647E-4, 1.6154E-6, 6.2450027E-9, 2.4142688E-11, 9.333372E-14, 3.6082013E-16, 1.3949048E-18, 5.392581E-21, 2.0847251E-23, 8.059367E-26, 3.1157057E-28, 1.2044957E-30, 4.656512E-33, 1.8001809E-35, 6.959291E-38, 2.69042E-40, 1.04E-42, 4.2E-45, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], Label=HHExpRate]]]\n" + 
				"		gate h:\n" + 
				"			instances: 1\n" + 
				"			forward rate: 70.0000 * exp((v - -0.0650000)/-0.0200000)\n" + 
				"			reverse rate: 1000.00 /(1 + exp((v - -0.0350000)/0.0100000))\n" + 
				"			forward rate plot: PlotNode [Title=RatePlot, X=mV, Y=ms-1, Data=[Data [X=[-8.0, -7.9, -7.8, -7.7000003, -7.6, -7.5, -7.4, -7.3, -7.2000003, -7.1, -7.0, -6.9, -6.8, -6.7000003, -6.6, -6.5, -6.4, -6.3, -6.2000003, -6.1, -6.0, -5.9, -5.8, -5.7000003, -5.6, -5.5, -5.4, -5.3, -5.2000003, -5.1, -5.0, -4.9, -4.8, -4.7000003, -4.6, -4.5, -4.4, -4.3, -4.2000003, -4.1, -4.0, -3.9, -3.8, -3.7, -3.6000001, -3.5, -3.4, -3.3, -3.2, -3.1000001, -3.0, -2.9, -2.8, -2.7, -2.6000001, -2.5, -2.4, -2.3, -2.2, -2.1000001, -2.0, -1.9, -1.8000001, -1.7, -1.6, -1.5, -1.4, -1.3000001, -1.2, -1.1, -1.0, -0.90000004, -0.8, -0.7, -0.6, -0.5, -0.4, -0.3, -0.2, -0.1, 0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.90000004, 1.0, 1.1, 1.2, 1.3000001, 1.4, 1.5, 1.6, 1.7, 1.8000001, 1.9, 2.0, 2.1000001, 2.2, 2.3, 2.4, 2.5, 2.6000001, 2.7, 2.8, 2.9, 3.0, 3.1000001, 3.2, 3.3, 3.4, 3.5, 3.6000001, 3.7, 3.8, 3.9, 4.0, 4.1, 4.2000003, 4.3, 4.4, 4.5, 4.6, 4.7000003, 4.8, 4.9, 5.0, 5.1, 5.2000003, 5.3, 5.4, 5.5, 5.6, 5.7000003, 5.8, 5.9, 6.0, 6.1, 6.2000003, 6.3, 6.4, 6.5, 6.6, 6.7000003, 6.8, 6.9, 7.0, 7.1, 7.2000003, 7.3, 7.4, 7.5, 7.6, 7.7000003, 7.8, 7.9, 8.0, 8.1, 8.2, 8.3, 8.400001, 8.5, 8.6, 8.7, 8.8, 8.900001, 9.0, 9.1, 9.2, 9.3, 9.400001, 9.5, 9.6, 9.7, 9.8, 9.900001], Y=[Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, Infinity, 2.2318858E34, 1.5038442E32, 1.01327455E30, 6.82739E27, 4.6002946E25, 3.0996305E23, 2.0885225E21, 1.4072301E19, 9.4818773E16, 6.3888142E14, 4.3047492E12, 2.9005228E10, 1.95435312E8, 1316832.9, 8872.759, 59.784145, 0.40282232, 0.0027141946, 1.82881E-5, 1.2322424E-7, 8.3027846E-10, 5.594372E-12, 3.7694582E-14, 2.539841E-16, 1.7113314E-18, 1.15308606E-20, 7.769403E-23, 5.2349825E-25, 3.5273172E-27, 2.3766693E-29, 1.6013993E-31, 1.0790145E-33, 7.270287E-36, 4.898718E-38, 3.3007E-40, 2.224E-42, 1.5E-44, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], Label=HHExpRate]]]\n" + 
				"			reverse rate plot: PlotNode [Title=RatePlot, X=mV, Y=ms-1, Data=[Data [X=[-8.0, -7.9, -7.8, -7.7000003, -7.6, -7.5, -7.4, -7.3, -7.2000003, -7.1, -7.0, -6.9, -6.8, -6.7000003, -6.6, -6.5, -6.4, -6.3, -6.2000003, -6.1, -6.0, -5.9, -5.8, -5.7000003, -5.6, -5.5, -5.4, -5.3, -5.2000003, -5.1, -5.0, -4.9, -4.8, -4.7000003, -4.6, -4.5, -4.4, -4.3, -4.2000003, -4.1, -4.0, -3.9, -3.8, -3.7, -3.6000001, -3.5, -3.4, -3.3, -3.2, -3.1000001, -3.0, -2.9, -2.8, -2.7, -2.6000001, -2.5, -2.4, -2.3, -2.2, -2.1000001, -2.0, -1.9, -1.8000001, -1.7, -1.6, -1.5, -1.4, -1.3000001, -1.2, -1.1, -1.0, -0.90000004, -0.8, -0.7, -0.6, -0.5, -0.4, -0.3, -0.2, -0.1, 0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.90000004, 1.0, 1.1, 1.2, 1.3000001, 1.4, 1.5, 1.6, 1.7, 1.8000001, 1.9, 2.0, 2.1000001, 2.2, 2.3, 2.4, 2.5, 2.6000001, 2.7, 2.8, 2.9, 3.0, 3.1000001, 3.2, 3.3, 3.4, 3.5, 3.6000001, 3.7, 3.8, 3.9, 4.0, 4.1, 4.2000003, 4.3, 4.4, 4.5, 4.6, 4.7000003, 4.8, 4.9, 5.0, 5.1, 5.2000003, 5.3, 5.4, 5.5, 5.6, 5.7000003, 5.8, 5.9, 6.0, 6.1, 6.2000003, 6.3, 6.4, 6.5, 6.6, 6.7000003, 6.8, 6.9, 7.0, 7.1, 7.2000003, 7.3, 7.4, 7.5, 7.6, 7.7000003, 7.8, 7.9, 8.0, 8.1, 8.2, 8.3, 8.400001, 8.5, 8.6, 8.7, 8.8, 8.900001, 9.0, 9.1, 9.2, 9.3, 9.400001, 9.5, 9.6, 9.7, 9.8, 9.900001], Y=[0.9664253, 0.96302474, 0.9592944, 0.9552051, 0.95072603, 0.9458246, 0.94046617, 0.9346143, 0.9282312, 0.9212774, 0.91371244, 0.90549517, 0.89658386, 0.8869372, 0.87651455, 0.8652771, 0.8531884, 0.84021527, 0.826329, 0.8115067, 0.7957321, 0.7789969, 0.7613019, 0.74265814, 0.7230877, 0.70262516, 0.68131685, 0.65922225, 0.6364133, 0.61297387, 0.5889993, 0.5645945, 0.5398727, 0.51495314, 0.48995882, 0.46501476, 0.4402445, 0.41576824, 0.39170054, 0.36814803, 0.34520805, 0.32296693, 0.30149892, 0.28086597, 0.26111716, 0.24228911, 0.22440636, 0.20748205, 0.19151899, 0.17651056, 0.16244191, 0.14929132, 0.13703115, 0.12562916, 0.115049414, 0.10525335, 0.09620075, 0.08785027, 0.08016036, 0.07308965, 0.06659746, 0.06064421, 0.05519167, 0.050203163, 0.045643758, 0.041480355, 0.037681717, 0.03421854, 0.031063365, 0.028190628, 0.025576547, 0.023199083, 0.021037843, 0.019074012, 0.017290264, 0.015670663, 0.014200579, 0.012866602, 0.011656457, 0.01055891, 0.009563707, 0.0086614825, 0.007843699, 0.0071025738, 0.0064310213, 0.0058225915, 0.00527142, 0.004772172, 0.004320001, 0.0039105066, 0.0035396903, 0.003203923, 0.0028999136, 0.0026246747, 0.0023754975, 0.0021499249, 0.0019457305, 0.0017608958, 0.0015935914, 0.00144216, 0.0013050992, 0.0011810492, 0.0010687778, 9.671685E-4, 8.752107E-4, 7.9198944E-4, 7.1667554E-4, 6.4851914E-4, 5.868407E-4, 5.3102494E-4, 4.805156E-4, 4.3480832E-4, 3.9344715E-4, 3.5601907E-4, 3.2215024E-4, 2.9150254E-4, 2.637697E-4, 2.3867471E-4, 2.1596672E-4, 1.9541878E-4, 1.7682556E-4, 1.6000109E-4, 1.4477712E-4, 1.3100157E-4, 1.1853662E-4, 1.0725759E-4, 9.705168E-5, 8.781676E-5, 7.9460566E-5, 7.189945E-5, 6.505776E-5, 5.8867066E-5, 5.32654E-5, 4.8196776E-5, 4.3610453E-5, 3.9460538E-5, 3.5705507E-5, 3.2307777E-5, 2.9233377E-5, 2.645153E-5, 2.3934397E-5, 2.165679E-5, 1.9595906E-5, 1.7731145E-5, 1.6043832E-5, 1.4517082E-5, 1.3135618E-5, 1.1885609E-5, 1.0754557E-5, 9.731137E-6, 8.805106E-6, 7.967197E-6, 7.20902E-6, 6.522996E-6, 5.9022555E-6, 5.340585E-6, 4.832364E-6, 4.3725045E-6, 3.9564075E-6, 3.5799073E-6, 3.2392352E-6, 2.9309813E-6, 2.6520636E-6, 2.399686E-6, 2.1713256E-6, 1.9646982E-6, 1.777732E-6, 1.6085596E-6, 1.4554846E-6, 1.3169765E-6, 1.1916505E-6, 1.0782497E-6, 9.756413E-7, 8.8279654E-7, 7.987871E-7, 7.2277294E-7, 6.539918E-7, 5.9175665E-7, 5.3544335E-7, 4.84489E-7], Label=HHSigmoidRate]]]";

		Assert.assertEquals(expected, getInfoTreeAsString("NML2_SimpleIonChannel.nml"));
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
