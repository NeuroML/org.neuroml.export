package org.neuroml.export.xpp;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Properties;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.lemsml.export.base.GenerationException;
import org.lemsml.export.dlems.DLemsWriter;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.ModelFeature;
import org.neuroml.export.ModelFeatureSupportException;
import org.neuroml.export.SupportLevelInfo;
import org.neuroml.export.Utils;
import org.neuroml.export.base.BaseWriter;
import org.neuroml.model.util.NeuroMLException;

@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public class XppWriter extends BaseWriter {

    public final String TEMPLATE = "xpp/xpp.vm";
    
    public HashMap<String, String> keywordSubstitutions = new HashMap<String, String>();
    
	public XppWriter(Lems lems) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(lems, "XPP");
        sli.checkAllFeaturesSupported(FORMAT, lems);
        keywordSubstitutions.put("compartment", "compart");
	}
    
    
    @Override
    protected void setSupportedFeatures() {
        sli.addSupportInfo(FORMAT, ModelFeature.ABSTRACT_CELL_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(FORMAT, ModelFeature.COND_BASED_CELL_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(FORMAT, ModelFeature.SINGLE_COMP_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(FORMAT, ModelFeature.NETWORK_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(FORMAT, ModelFeature.MULTI_POPULATION_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(FORMAT, ModelFeature.NETWORK_WITH_INPUTS_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(FORMAT, ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(FORMAT, ModelFeature.MULTICOMPARTMENTAL_CELL_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(FORMAT, ModelFeature.HH_CHANNEL_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(FORMAT, ModelFeature.KS_CHANNEL_MODEL, SupportLevelInfo.Level.LOW);
    }

	@Override
	protected void addComment(StringBuilder sb, String comment) {

		String comm = "# ";
		sb.append(comm+comment.replaceAll("\n", "\n# ")+"\n");
	}


    @Override
	public String getMainScript() throws GenerationException {
		StringBuilder sb = new StringBuilder();
		addComment(sb,"XPP export from LEMS\n\nPlease note that this is a work in progress " +
				"and only works for a limited subset of LEMS/NeuroML 2!!\n\n"+lems.textSummary(false, false));

        Velocity.init();
		
		VelocityContext context = new VelocityContext();

        DLemsWriter somw = new DLemsWriter(lems);

		try
		{
			String som = somw.getMainScript();
			
			DLemsWriter.putIntoVelocityContext(som, context);
        
			Properties props = new Properties();
			props.put("resource.loader", "class");
			props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
			VelocityEngine ve = new VelocityEngine();
			ve.init(props);
			Template template = ve.getTemplate(TEMPLATE);
		   
			StringWriter sw = new StringWriter();

			template.merge( context, sw );
            
            String mapped = sw.toString();
            for (String old: keywordSubstitutions.keySet()) {
                String new_ = keywordSubstitutions.get(old);
                mapped = Utils.replaceInExpression(mapped, old, new_);
            }
			
			sb.append(mapped);
		} 
		catch (IOException e1) {
			throw new GenerationException("Problem converting LEMS to SOM",e1);
		}
		catch( ResourceNotFoundException e )
		{
			throw new GenerationException("Problem finding template",e);
		}
		catch( ParseErrorException e )
		{
			throw new GenerationException("Problem parsing",e);
		}
		catch( MethodInvocationException e )
		{
			throw new GenerationException("Problem finding template",e);
		}
		catch( LEMSException e )
		{
			throw new GenerationException("Problem using template",e);
		}
		
		return sb.toString();	
        

	}





}



