package org.neuroml.export.base;


import org.lemsml.export.base.GenerationException;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.model.NeuroMLDocument;

public abstract class BaseWriter {

	protected Lems lems;
	protected NeuroMLDocument nmlDocument;
	protected String format;

	public BaseWriter(Lems lems, String format) {
		this.lems = lems;
		this.format = format;
	}
	
	public BaseWriter(NeuroMLDocument nmlDocument, String format) {
		this.nmlDocument = nmlDocument;
		this.format = format;
	}

	protected abstract void addComment(StringBuilder sb, String comment);


	public abstract String getMainScript() throws GenerationException;

	public class CompInfo
	{
		public StringBuilder stateVars = new StringBuilder();
		public StringBuilder params = new StringBuilder();
		public StringBuilder eqns = new StringBuilder();
		public StringBuilder initInfo = new StringBuilder();
	}


}

