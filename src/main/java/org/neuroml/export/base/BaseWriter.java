package org.neuroml.export.base;


import javax.xml.bind.JAXBException;

import org.lemsml.export.base.GenerationException;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.SupportLevelInfo;
import org.neuroml.model.NeuroMLDocument;

public abstract class BaseWriter {

	protected Lems lems;
	protected NeuroMLDocument nmlDocument;
	protected static String FORMAT;
    
    protected static SupportLevelInfo sli = SupportLevelInfo.getSupportLevelInfo();

	private BaseWriter() {
        
    }
    
	public BaseWriter(Lems lems, String format) {
		this.lems = lems;
		this.FORMAT = format;
        setSupportedFeatures();
	}
	
	public BaseWriter(NeuroMLDocument nmlDocument, String format) {
		this.nmlDocument = nmlDocument;
		this.FORMAT = format;
        setSupportedFeatures();
	}

	protected abstract void setSupportedFeatures();
    
	protected abstract void addComment(StringBuilder sb, String comment);


	public abstract String getMainScript() throws GenerationException, JAXBException, Exception;

	public class CompInfo
	{
		public StringBuilder stateVars = new StringBuilder();
		public StringBuilder params = new StringBuilder();
		public StringBuilder eqns = new StringBuilder();
		public StringBuilder initInfo = new StringBuilder();
		public StringBuilder conditionInfo = new StringBuilder();
	}


}

