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
import org.neuroml.export.neuron.NeuronWriter;
import org.neuroml.export.pynn.PyNNWriter;
import org.neuroml.export.sbml.SBMLWriter;
import org.neuroml.export.xpp.XppWriter;
import org.neuroml.model.util.NeuroMLException;

public class ExportFactory
{

	public IBaseWriter getExportWriter(Lems lems, File outputFolder, String outputFileName, String stringFormat) throws ModelFeatureSupportException, NeuroMLException, LEMSException{
		Format format = Format.valueOf(stringFormat);
		return getExportWriter(lems, outputFolder, outputFileName, format);
	}			
	
	public IBaseWriter getExportWriter(Lems lems, File outputFolder, String outputFileName, Format format) throws ModelFeatureSupportException, NeuroMLException, LEMSException{
		switch(format)
		{
			case C:
				return new CWriter(lems, outputFolder, outputFileName);
			case DLEMS:
				return new DLemsWriter(lems, outputFolder, outputFileName);
			case MATLAB:
				return new MatlabWriter(lems, outputFolder, outputFileName);
			case MODELICA:
				return new ModelicaWriter(lems, outputFolder, outputFileName);
			case SEDML:	
				//FIXME
				//String inputFileName = ((URL)((ModelWrapper) model).getModel(NeuroMLAccessUtility.URL_ID)).getPath();
				String inputFileName = "";
				return new SEDMLWriter(lems, outputFolder, outputFileName, inputFileName);
			case BRIAN:
				return new BrianWriter(lems, outputFolder, outputFileName);
			case CELLML:
				return new CellMLWriter(lems, outputFolder, outputFileName);
			case DN_SIM:
				return new DNSimWriter(lems, outputFolder, outputFileName);
			case GRAPH_VIZ:
				return new GraphWriter(lems, outputFolder, outputFileName);
			case NEST:
				return new NestWriter(lems, outputFolder, outputFileName);
			case NEURON:
				NeuronWriter neuronWriter = new NeuronWriter(lems, outputFolder, outputFileName);
				neuronWriter.setNoGui(true);
				return neuronWriter;
			case PYNN:
				return new PyNNWriter(lems, outputFolder, outputFileName);
			case SBML:
				return new SBMLWriter(lems, outputFolder, outputFileName);
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
				return new XppWriter(lems, outputFolder, outputFileName);
			default:
				break;
		}
		return null;
	}
}
