package org.neuroml.export.base;

import java.io.File;

import org.lemsml.export.base.ABaseWriter;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.utils.Formats;
import org.neuroml.model.NeuroMLDocument;

public abstract class ANeuroMLBaseWriter extends ABaseWriter
{

	protected NeuroMLDocument nmlDocument;

	public ANeuroMLBaseWriter(Lems lems, Formats format)
	{
		super(lems, format);
	}

	public ANeuroMLBaseWriter(Lems lems, Formats format, File outputFolder)
	{
		super(lems, format, outputFolder);
	}
	
	public ANeuroMLBaseWriter(Lems lems, NeuroMLDocument nmlDocument, Formats format, File outputFolder)
	{
		super(lems, format, outputFolder);
		this.nmlDocument = nmlDocument;
	}
	
	public ANeuroMLBaseWriter(NeuroMLDocument nmlDocument, Formats format)
	{
		super(null, format);
		this.nmlDocument = nmlDocument;
	}

	public ANeuroMLBaseWriter(NeuroMLDocument nmlDocument, Formats format, File outputFolder)
	{
		super(null, format, outputFolder);
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
