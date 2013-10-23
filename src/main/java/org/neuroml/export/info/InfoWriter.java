/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.neuroml.export.info;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import org.lemsml.jlems.core.sim.ContentError;

import org.neuroml.export.base.BaseWriter;
import org.neuroml.model.Cell;
import org.neuroml.model.IonChannel;
import org.neuroml.model.Morphology;
import org.neuroml.model.Network;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.Population;
import org.neuroml.model.Projection;
import org.neuroml.model.Standalone;
import org.neuroml.model.util.NeuroMLConverter;

/**
 *
 * @author padraig
 */
public class InfoWriter extends BaseWriter 
{
    private static final String INDENT = "    ";
            
    public InfoWriter(NeuroMLDocument nmlDocument) {
        super(nmlDocument, "Information");
    }
    
	@Override
	public String getMainScript() throws ContentError {

        StringBuilder main = new StringBuilder();
        main.append("Information on contents of NeuroML 2 file\n");
        LinkedHashMap<String, Object> props = getProperties();
        
        main.append(propsToString(props, ""));
        
        return main.toString();
    }
    
    public String propsToString(LinkedHashMap<String, Object> props, String indent) {
        
        StringBuilder main = new StringBuilder();
        for (String key: props.keySet()) {
            Object obj = props.get(key);
            if (obj instanceof LinkedHashMap) {
                main.append(indent+key+":\n");
                main.append(propsToString((LinkedHashMap<String, Object>)obj, indent+INDENT));
                
            } else {
                main.append(indent+key+": "+obj+"\n");
            }
        }
        return main.toString();
    }
    
    public LinkedHashMap<String, Object> getProperties() {
        LinkedHashMap<String, Object> props = new LinkedHashMap<String, Object>();
        
        for (Cell cell: nmlDocument.getCell()){
            LinkedHashMap<String, Object> cellProps = new LinkedHashMap<String, Object>();
       
            cellProps.put("ID", cell.getId());
            if (cell.getNotes()!=null && cell.getNotes().length()>0)
                cellProps.put("Description", formatNotes(cell.getNotes()));
            Morphology morph = cell.getMorphology();
            cellProps.put("Number of segments", morph.getSegment().size());
            
            props.put("Cell "+cell.getId(), cellProps);
        }
        for (IonChannel chan: nmlDocument.getIonChannel()) {
            LinkedHashMap<String, Object> chanProps = new LinkedHashMap<String, Object>();
            chanProps.put("ID", chan.getId());
            if (chan.getNotes()!=null && chan.getNotes().length()>0)
                chanProps.put("Description", formatNotes(chan.getNotes()));
            
            props.put("Ion Channel "+chan.getId(), chanProps);
        }
        for (Network element: nmlDocument.getNetwork()) {
            LinkedHashMap<String, Object> elementProps = new LinkedHashMap<String, Object>();
            elementProps.put("ID", element.getId());
            if (element.getNotes()!=null && element.getNotes().length()>0)
                elementProps.put("Description", formatNotes(element.getNotes()));
            
            
            elementProps.put("Number of populations", element.getPopulation().size());
            
            for (Population sub: element.getPopulation()) {
                LinkedHashMap<String, Object> subProps = new LinkedHashMap<String, Object>();
                
                subProps.put("ID", sub.getId());
                if (sub.getNotes()!=null && sub.getNotes().length()>0)
                    subProps.put("Description", formatNotes(sub.getNotes()));
                
                if (sub.getSize()!=null)
                    subProps.put("Size", sub.getSize());
                else if (!sub.getInstance().isEmpty()) {
                    subProps.put("Size (number of instances)", sub.getInstance().size());
                }
           
                
                elementProps.put("Population "+sub.getId(), subProps);
            }
            
            elementProps.put("Number of projections", element.getProjection().size());
            
            for (Projection sub: element.getProjection()) {
                LinkedHashMap<String, Object> subProps = new LinkedHashMap<String, Object>();
                
                subProps.put("ID", sub.getId());
                subProps.put("Presynaptic population", sub.getPresynapticPopulation());
                subProps.put("Postsynaptic population", sub.getPostsynapticPopulation());
                
                elementProps.put("Projection "+sub.getId(), subProps);
            }
            
            props.put("Network "+element.getId(), elementProps);
        }
        
        //Testing...
        List remainder = nmlDocument.getIafRefCell();
        remainder.addAll(nmlDocument.getAdExIaFCell());
        
        for (Object obj: remainder) {
            Standalone element = (Standalone)obj;
            LinkedHashMap<String, Object> elementProps = new LinkedHashMap<String, Object>();
            elementProps.put("ID", element.getId());
            if (element.getNotes()!=null && element.getNotes().length()>0)
                elementProps.put("Description", formatNotes(element.getNotes()));
            
            props.put("Element "+element.getId(), elementProps);
            
        }
        
        return props;
    }
    
    private String formatNotes(String notes) {
        String formatted = notes.replaceAll("\n"," ");
        while (formatted.indexOf("  ")>=0)
            formatted = formatted.replaceAll("  "," ");
        return formatted;
    }
    
    
    
    public static void main(String[] args) throws Exception {

		String fileName = "../neuroConstruct/osb/cerebellum/cerebellar_nucleus_cell/CerebellarNucleusNeuron/NeuroML2/DCN.nml";
		fileName = "../neuroConstruct/osb/hippocampus/CA1_pyramidal_neuron/CA1PyramidalCell/neuroConstruct/generatedNeuroML2/CA1.nml";
		//fileName = "../neuroConstruct/osb/cerebral_cortex/networks/Thalamocortical/neuroConstruct/generatedNeuroML2/SupAxAx.nml";
        //fileName = "../neuroConstruct/osb/cerebellum/cerebellar_granule_cell/GranuleCell/neuroConstruct/generatedNeuroML2/Gran_KDr_98.nml";
        fileName = "../NeuroML2/examples/NML2_AbstractCells.nml";
        fileName = "../NeuroML2/examples/NML2_InstanceBasedNetwork.nml";
        fileName = "../neuroConstruct/osb/cerebral_cortex/networks/ACnet2/neuroConstruct/generatedNeuroML2/ACnet2.nml";
        
		NeuroMLConverter nmlc = new NeuroMLConverter();
    	NeuroMLDocument nmlDocument = nmlc.loadNeuroML(new File(fileName));

    	InfoWriter infow = new InfoWriter(nmlDocument);
        String info = infow.getMainScript();

        System.out.println(info);
        
	}

    @Override
    protected void addComment(StringBuilder sb, String comment)
    {
        sb.append("#    "+comment+"\n");
    }
    
}
