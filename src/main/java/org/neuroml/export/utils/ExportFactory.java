package org.neuroml.export.utils;

import java.io.File;

import org.lemsml.export.base.IBaseWriter;
import org.lemsml.export.c.CWriter;
import org.lemsml.export.dlems.DLemsWriter;
import org.lemsml.export.matlab.MatlabWriter;
import org.lemsml.export.modelica.ModelicaWriter;
import org.lemsml.export.sedml.SEDMLWriter;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.brian.BrianWriter;
import org.neuroml.export.cellml.CellMLWriter;
import org.neuroml.export.dnsim.DNSimWriter;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.graph.GraphWriter;
import org.neuroml.export.nest.NestWriter;
import org.neuroml.export.netpyne.NetPyNEWriter;
import org.neuroml.export.neuron.NeuronWriter;
import org.neuroml.export.pynn.PyNNWriter;
import org.neuroml.export.sbml.SBMLWriter;
import org.neuroml.export.xpp.XppWriter;
import org.neuroml.model.util.NeuroMLException;

public class ExportFactory
{

	public static IBaseWriter getExportWriter(Lems lems, File outputFolder, String outputFileName, String stringFormat) throws ModelFeatureSupportException, NeuroMLException, LEMSException{
		Format format = Format.valueOf(stringFormat);
		return getExportWriter(lems, outputFolder, outputFileName, format);
	}			
	
	public static IBaseWriter getExportWriter(Lems lems, Format format) throws ModelFeatureSupportException, NeuroMLException, LEMSException{
		IBaseWriter writer = null;
		switch(format)
		{
			case C:
				writer = new CWriter(lems);
				break;
			case DLEMS:
				writer = new DLemsWriter(lems);
				break;
			case MATLAB:
				writer = new MatlabWriter(lems);
				break;
			case MODELICA:
				writer = new ModelicaWriter(lems);
				break;
			case SEDML:	
				//FIXME
				//String inputFileName = ((URL)((ModelWrapper) model).getModel(NeuroMLAccessUtility.URL_ID)).getPath();
				writer = new SEDMLWriter(lems);
				break;
			case BRIAN:
				writer = new BrianWriter(lems);
				break;
			case CELLML:
				writer = new CellMLWriter(lems);
				break;
			case DN_SIM:
				writer = new DNSimWriter(lems);
				break;
			case GRAPH_VIZ:
				writer = new GraphWriter(lems);
				break;
			case NEST:
				writer = new NestWriter(lems);
				break;
			case NETPYNE:
				writer = new NetPyNEWriter(lems);
				//((NetPyNEWriter)writer).setNoGui(true);
				break;
			case NEURON:
				writer = new NeuronWriter(lems);
				((NeuronWriter)writer).setNoGui(true);
				break;
			case PYNN:
				writer = new PyNNWriter(lems);
				break;
			case SBML:
				writer = new SBMLWriter(lems);
				break;
			case SVG:
				//FIXME: We need to look for a method which converts from lems to neuroml
				// String outputFileName = "";
				//exportWriter = new SVGWriter(lems, outputFolder, outputFileName);
				break;
			case XINEML:
				//FIXME: This conversion allows to two input formats : SPINEML and NINEML
				// String outputFileName = "";
//						exportWriter = new XineMLWriter(lems, outputFolder, outputFileName);
				break;
			case XPP:
				writer = new XppWriter(lems);
			default:
				break;
		}
		
		return writer;
	}
	
	public static IBaseWriter getExportWriter(Lems lems, File outputFolder, String outputFileName, Format format) throws ModelFeatureSupportException, NeuroMLException, LEMSException{
		IBaseWriter writer = getExportWriter(lems, format);
		if (writer != null){
			writer.setOutputFileName(outputFileName);
			writer.setOutputFolder(outputFolder);
		}
		return writer;
	}
}
