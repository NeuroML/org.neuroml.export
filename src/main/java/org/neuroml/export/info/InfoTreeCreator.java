package org.neuroml.export.info;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.ComponentType;
import org.lemsml.jlems.core.type.ParamValue;
import org.lemsml.jlems.core.type.Unit;
import org.neuroml.export.Utils;

import org.neuroml.export.info.model.ChannelInfoExtractor;
import org.neuroml.export.info.model.InfoNode;
import org.neuroml.model.Cell;
import org.neuroml.model.IonChannel;
import org.neuroml.model.Morphology;
import org.neuroml.model.Network;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.Population;
import org.neuroml.model.Projection;
import org.neuroml.model.Standalone;
import org.neuroml.model.util.NeuroMLConverter;
import org.neuroml.model.util.NeuroMLException;

/**
 * @author matteocantarelli
 * 
 */
public class InfoTreeCreator
{
	
	public static InfoNode createInfoTree(final NeuroMLDocument nmlDocument) throws NeuroMLException
	{
		InfoNode infoRoot = new InfoNode();
        
        LinkedHashMap<String,Standalone> standalones = NeuroMLConverter.getAllStandaloneElements(nmlDocument);
        
        for (Standalone element: standalones.values())
        {
            if(element instanceof Cell)
            {
                Cell cell = (Cell)element;
                InfoNode cellProps = new InfoNode();

                cellProps.put("ID", cell.getId());
                if(cell.getNotes() != null && cell.getNotes().length() > 0) cellProps.put("Description", formatNotes(cell.getNotes()));
                Morphology morph = cell.getMorphology();
                cellProps.put("Number of segments", morph.getSegment().size());

                infoRoot.put("Cell " + cell.getId(), cellProps);
            }
            else if(element instanceof IonChannel)
            {
                IonChannel chan = (IonChannel)element;

                InfoNode chanProps = new InfoNode();
                ChannelInfoExtractor cinfo = new ChannelInfoExtractor(chan);

                chanProps.put("ID", chan.getId());

                chanProps.put("Gates", cinfo.getGates());

                if(chan.getNotes() != null && chan.getNotes().length() > 0) chanProps.put("Description", formatNotes(chan.getNotes()));

                infoRoot.put("Ion Channel " + chan.getId(), chanProps);
            }
            else if(element instanceof Network)
            {
                Network network = (Network)element;

                InfoNode elementProps = new InfoNode();

                elementProps.put("ID", network.getId());
                if(network.getNotes() != null && network.getNotes().length() > 0) elementProps.put("Description", formatNotes(element.getNotes()));

                elementProps.put("Number of populations", network.getPopulation().size());

                for(Population sub : network.getPopulation())
                {
                    InfoNode subProps = new InfoNode();

                    subProps.put("ID", sub.getId());
                    subProps.put("Component", sub.getComponent());
                    if(sub.getNotes() != null && sub.getNotes().length() > 0) subProps.put("Description", formatNotes(sub.getNotes()));

                    if(sub.getSize() != null) subProps.put("Size", sub.getSize());
                    else if(!sub.getInstance().isEmpty())
                    {
                        subProps.put("Size (number of instances)", sub.getInstance().size());
                    }

                    elementProps.put("Population " + sub.getId(), subProps);
                }

                elementProps.put("Number of projections", network.getProjection().size());

                for(Projection sub : network.getProjection())
                {
                    InfoNode subProps = new InfoNode();

                    subProps.put("ID", sub.getId());
                    subProps.put("Presynaptic population", sub.getPresynapticPopulation());
                    subProps.put("Postsynaptic population", sub.getPostsynapticPopulation());

                    elementProps.put("Projection " + sub.getId(), subProps);
                }

                infoRoot.put("Network " + network.getId(), elementProps);
            }

            else {
                InfoNode elementProps = new InfoNode();
                elementProps.put("ID", element.getId());
                if(element.getNotes() != null && element.getNotes().length() > 0) elementProps.put("Description", formatNotes(element.getNotes()));

                infoRoot.put("Element " + element.getId(), elementProps);
                Component comp = Utils.convertNeuroMLToComponent(element);
                ComponentType ct = comp.getComponentType();
                try {
                    for (ParamValue pv: comp.getParamValues()) {
                        if (comp.hasAttribute(pv.getName())) {
                            String orig = comp.getStringValue(pv.getName());
                            String siSymbol = Utils.getSIUnitInNeuroML(pv.getFinalParam().getDimension()).getSymbol();
                            String si = (float)pv.getDoubleValue()+ (siSymbol.equals(Unit.NO_UNIT)?"": " "+siSymbol);
                            if (!orig.equals(si))
                                orig = orig +" ("+si+")";
                            elementProps.put(pv.getName(), orig);
                        }
                    }
                } catch (ContentError ce) {
                    throw new NeuroMLException("Problem extracting info from NeuroML component",ce);
                }

            }
        }

		return infoRoot;
	}
	
	/**
	 * @param notes
	 * @return
	 */
	private static String formatNotes(String notes)
	{
		String formatted = notes.replaceAll("\n", " ");
		while(formatted.indexOf("  ") >= 0)
			formatted = formatted.replaceAll("  ", " ");
		return formatted;
	}

}
