package org.lemsml.export.c;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.lemsml.export.base.GenerationException;
import org.lemsml.export.dlems.DLemsWriter;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.logging.MinimalMessageHandler;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import static org.lemsml.jlems.io.util.JUtil.getRelativeResource;
import org.neuroml.export.base.BaseWriter;
import org.neuroml.model.util.NeuroMLException;
import org.neuroml.utils.ModelFeature;
import org.neuroml.utils.ModelFeatureSupportException;
import org.neuroml.utils.SupportLevelInfo;

public class CWriter extends BaseWriter {
	
    public enum Solver {

        // Default (& only supported version) is CVODE
        CVODE("cvode/cvode.vm", "cvode/Makefile");

        private final String template;
        private final String makefile;

        private Solver(String t, String m) {
            template = t;
            makefile = m;
        }

        public String getTemplate() {
            return template;
        }

        public String getMakefile() {
            return makefile;
        }

    };
	
	private Solver solver = Solver.CVODE;

	String comm = "// ";
	String commPre = "/*";
	String commPost = "*/";
    
    
	public CWriter(Lems lems) throws ModelFeatureSupportException, NeuroMLException, LEMSException {
		super(lems, "C");
		MinimalMessageHandler.setVeryMinimal(true);
		E.setDebug(false);
        sli.checkAllFeaturesSupported(FORMAT, lems);
	}
    
    
    @Override
    protected void setSupportedFeatures() {
        sli.addSupportInfo(FORMAT, ModelFeature.ABSTRACT_CELL_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(FORMAT, ModelFeature.COND_BASED_CELL_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(FORMAT, ModelFeature.SINGLE_COMP_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(FORMAT, ModelFeature.NETWORK_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(FORMAT, ModelFeature.MULTI_CELL_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(FORMAT, ModelFeature.MULTI_POPULATION_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(FORMAT, ModelFeature.NETWORK_WITH_INPUTS_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(FORMAT, ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(FORMAT, ModelFeature.MULTICOMPARTMENTAL_CELL_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(FORMAT, ModelFeature.HH_CHANNEL_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(FORMAT, ModelFeature.KS_CHANNEL_MODEL, SupportLevelInfo.Level.NONE);
    }
	
	@Override
    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
	protected void addComment(StringBuilder sb, String comment) {
	
		if (comment.indexOf("\n") < 0)
			sb.append(comm + comment + "\n");
		else
			sb.append(commPre + "\n" + comment + "\n" + commPost + "\n");
	}
	
	public void setSolver(Solver solver){
		this.solver = solver;
	}

    public Solver getSolver() {
        return solver;
    }
    
	public String getMakefile() throws GenerationException, ContentError {
        
        try {
            String makefile = getRelativeResource(this.getClass(), "/"+solver.getMakefile());
            return makefile;
        } catch (ContentError ex) {
            throw new GenerationException("Problem finding makefile: "+solver.getMakefile(), ex);
        }
    }
    

	@Override
	public String getMainScript() throws LEMSException, GenerationException {

		StringBuilder sb = new StringBuilder();

		addComment(sb, FORMAT+" simulator compliant export for:\n\n"
		+ lems.textSummary(false, false));
		
		Velocity.init();
		
		VelocityContext context = new VelocityContext();
		
        DLemsWriter somw = new DLemsWriter(lems, new CVisitors());

		try
		{
			String som = somw.getMainScript();
			
			DLemsWriter.putIntoVelocityContext(som, context);
        
			Properties props = new Properties();
			props.put("resource.loader", "class");
			props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
			VelocityEngine ve = new VelocityEngine();
			ve.init(props);
			Template template = ve.getTemplate(solver.getTemplate());
		   
			StringWriter sw = new StringWriter();

			template.merge( context, sw );
			
			sb.append(sw);
		} 
		catch (IOException e1) {
			throw new GenerationException("Problem converting LEMS to SOM",e1);
		}
		catch( VelocityException e )
		{
			throw new GenerationException("Problem using template",e);
		}
		catch( LEMSException e )
		{
			throw new GenerationException("Problem with LEMS",e);
		}
		
		return sb.toString();	

	}

}
