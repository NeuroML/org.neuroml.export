package org.neuroml.export.base;


import org.lemsml.jlems.expression.ParseError;
import org.lemsml.jlems.sim.ContentError;
import org.lemsml.jlems.type.Lems;;

public abstract class BaseWriter {

	protected Lems lems;

	public BaseWriter(Lems l) {
		lems = l;
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

