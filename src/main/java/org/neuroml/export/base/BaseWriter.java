package org.neuroml.export.base;


import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.type.Lems;

public abstract class BaseWriter {

	protected Lems lems;
	protected String format;

	public BaseWriter(Lems lems, String format) {
		this.lems = lems;
		this.format = format;
	}

	protected abstract void addComment(StringBuilder sb, String comment);


	public abstract String getMainScript() throws ContentError, ParseError;

	public class CompInfo
	{
		public StringBuilder stateVars = new StringBuilder();
		public StringBuilder params = new StringBuilder();
		public StringBuilder eqns = new StringBuilder();
		public StringBuilder initInfo = new StringBuilder();
	}


}

