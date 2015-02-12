/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.neuroml.export.info;

import java.io.File;
import org.lemsml.jlems.core.sim.LEMSException;

import org.neuroml.export.base.BaseWriter;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.util.NeuroMLConverter;
import org.neuroml.model.util.NeuroMLException;
import org.neuroml.utils.ModelFeature;
import org.neuroml.utils.ModelFeatureSupportException;
import org.neuroml.utils.SupportLevelInfo;
import org.neuroml.utils.Utils;

/**
 * 
 * @author padraig
 */
public class InfoWriter extends BaseWriter
{
	/**
	 * @param nmlDocument
	 */
	public InfoWriter(NeuroMLDocument nmlDocument) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(nmlDocument, "Information");
        sli.checkAllFeaturesSupported(FORMAT, Utils.convertNeuroMLToSim(nmlDocument).getLems());
	}
    
    
    @Override
    protected void setSupportedFeatures() {
        sli.addSupportInfo(FORMAT, ModelFeature.ABSTRACT_CELL_MODEL, SupportLevelInfo.Level.HIGH);
        sli.addSupportInfo(FORMAT, ModelFeature.COND_BASED_CELL_MODEL, SupportLevelInfo.Level.HIGH);
        sli.addSupportInfo(FORMAT, ModelFeature.SINGLE_COMP_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(FORMAT, ModelFeature.NETWORK_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(FORMAT, ModelFeature.MULTI_POPULATION_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(FORMAT, ModelFeature.NETWORK_WITH_INPUTS_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(FORMAT, ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(FORMAT, ModelFeature.MULTICOMPARTMENTAL_CELL_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(FORMAT, ModelFeature.HH_CHANNEL_MODEL, SupportLevelInfo.Level.HIGH);
        sli.addSupportInfo(FORMAT, ModelFeature.KS_CHANNEL_MODEL, SupportLevelInfo.Level.LOW);
    }

	/* (non-Javadoc)
	 * @see org.neuroml.export.base.BaseWriter#getMainScript()
	 */
	@Override
	public String getMainScript() throws NeuroMLException
	{
		StringBuilder main = new StringBuilder();
		main.append("Information on contents of NeuroML 2 file\n");
		main.append(InfoTreeCreator.createInfoTree(nmlDocument));
		return main.toString();
	}

	/**
	 * FIXME: Why a main method?
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception
	{
		String fileName = "../neuroConstruct/osb/cerebellum/cerebellar_nucleus_cell/CerebellarNucleusNeuron/NeuroML2/DCN.nml";
		//fileName = "../neuroConstruct/osb/hippocampus/CA1_pyramidal_neuron/CA1PyramidalCell/neuroConstruct/generatedNeuroML2/CA1.nml";
		// fileName = "../neuroConstruct/osb/cerebral_cortex/networks/Thalamocortical/neuroConstruct/generatedNeuroML2/SupAxAx.nml";
		// fileName = "../neuroConstruct/osb/cerebellum/cerebellar_granule_cell/GranuleCell/neuroConstruct/generatedNeuroML2/Gran_KDr_98.nml";
		fileName = "../NeuroML2/examples/NML2_AbstractCells.nml";
		fileName = "../NeuroML2/examples/NML2_SimpleMorphology.nml";
		fileName = "../NeuroML2/examples/NML2_SingleCompHHCell.nml";
		//fileName = "../NeuroML2/examples/NML2_InstanceBasedNetwork.nml";
		//fileName = "../neuroConstruct/osb/cerebral_cortex/networks/ACnet2/neuroConstruct/generatedNeuroML2/ACnet2.nml";
		fileName = "../neuroConstruct/osb/cerebral_cortex/networks/ACnet2/neuroConstruct/generatedNeuroML2/Na_bask.channel.nml";

		NeuroMLConverter nmlc = new NeuroMLConverter();
		NeuroMLDocument nmlDocument = nmlc.loadNeuroML(new File(fileName));
		InfoWriter infow = new InfoWriter(nmlDocument);
		String info = infow.getMainScript();

		System.out.println(info);

	}

	/* (non-Javadoc)
	 * @see org.neuroml.export.base.BaseWriter#addComment(java.lang.StringBuilder, java.lang.String)
	 */
	@Override
	protected void addComment(StringBuilder sb, String comment)
	{
		sb.append("#    " + comment + "\n");
	}

}
