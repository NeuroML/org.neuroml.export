package org.neuroml.export.base;

import java.io.File;

import org.lemsml.export.base.ABaseWriter;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Format;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.util.NeuroMLException;

public abstract class ANeuroMLBaseWriter extends ABaseWriter
{

	protected NeuroMLDocument nmlDocument;

	public ANeuroMLBaseWriter(Lems lems, Format format) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(lems, format);
	}
	
	public ANeuroMLBaseWriter(Lems lems, Format format, File outputFolder, String outputFileName) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(lems, format, outputFolder, outputFileName);
	}
	
	public ANeuroMLBaseWriter(Lems lems, NeuroMLDocument nmlDocument, Format format) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(lems, format);
		this.nmlDocument = nmlDocument;
	}
	
	public ANeuroMLBaseWriter(Lems lems, NeuroMLDocument nmlDocument, Format format, File outputFolder, String outputFileName) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(lems, format, outputFolder, outputFileName);
		this.nmlDocument = nmlDocument;
	}
	
	public ANeuroMLBaseWriter(NeuroMLDocument nmlDocument, Format format) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(null, format);
		this.nmlDocument = nmlDocument;
	}

	public ANeuroMLBaseWriter(NeuroMLDocument nmlDocument, Format format, File outputFolder, String outputFileName) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(null, format, outputFolder, outputFileName);
		this.nmlDocument = nmlDocument;
	}
	
	public class CompInfo
	{
		public StringBuilder stateVars = new StringBuilder();
		public StringBuilder params = new StringBuilder();
		public StringBuilder eqns = new StringBuilder();
		public StringBuilder initInfo = new StringBuilder();
		public StringBuilder conditionInfo = new StringBuilder();
	}

}
