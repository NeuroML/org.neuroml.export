package org.neuroml.export.base;

import java.io.File;

import org.lemsml.export.base.ABaseWriter;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.model.NeuroMLDocument;

public abstract class ANeuroMLBaseWriter extends ABaseWriter
{

	protected NeuroMLDocument nmlDocument;

	public ANeuroMLBaseWriter(Lems lems, String format)
	{
		super(lems, format);
	}

	public ANeuroMLBaseWriter(NeuroMLDocument nmlDocument, String format)
	{
		super(null, format);
		this.nmlDocument = nmlDocument;
	}

	public ANeuroMLBaseWriter(NeuroMLDocument nmlDocument, String format, File outputFolder)
	{
		super(null, format, outputFolder);
		this.nmlDocument = nmlDocument;
	}
	
	// public abstract String getMainScript() throws GenerationException, JAXBException, Exception;

	public class CompInfo
	{
		public StringBuilder stateVars = new StringBuilder();
		public StringBuilder params = new StringBuilder();
		public StringBuilder eqns = new StringBuilder();
		public StringBuilder initInfo = new StringBuilder();
		public StringBuilder conditionInfo = new StringBuilder();
	}

}
