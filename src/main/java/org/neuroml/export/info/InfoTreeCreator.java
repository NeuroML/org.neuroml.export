package org.neuroml.export.info;

import java.util.LinkedHashMap;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.ComponentType;
import org.lemsml.jlems.core.type.ParamValue;
import org.lemsml.jlems.core.type.QuantityReader;
import org.lemsml.jlems.core.type.Unit;
import org.neuroml.export.Utils;

import org.neuroml.export.info.model.ChannelInfoExtractor;
import org.neuroml.export.info.model.InfoNode;
import org.neuroml.model.Cell;
import org.neuroml.model.ChannelDensity;
import org.neuroml.model.IntracellularProperties;
import org.neuroml.model.IonChannel;
import org.neuroml.model.MembraneProperties;
import org.neuroml.model.Morphology;
import org.neuroml.model.Network;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.Population;
import org.neuroml.model.Projection;
import org.neuroml.model.Resistivity;
import org.neuroml.model.SpecificCapacitance;
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
                cellProps.put("Number of segment groups", morph.getSegmentGroup().size());
                if (cell.getBiophysicalProperties()==null) 
                {
                    cellProps.put("Biophysical properties", "none");
                }
                else
                {
                    MembraneProperties mp = cell.getBiophysicalProperties().getMembraneProperties();
                    for (ChannelDensity cd: mp.getChannelDensity()) 
                    {
                        
                        InfoNode cProps = new InfoNode();
                        cellProps.put("Channel density: "+cd.getId(), cProps);
                        cProps.put("ID", cd.getId());
                        cProps.put("IonChannel", cd.getIonChannel());
                        cProps.put("Ion", cd.getIon());
                        if (cd.getErev()!=null)
                        {
                            cProps.put("Reversal potential", formatDimensionalQuantity(cd.getErev()));
                        }
                        cProps.put("Conductance density", formatDimensionalQuantity(cd.getCondDensity()));
                        if (cd.getSegmentGroup()!=null)
                        {
                            cProps.put("Segment group", cd.getSegmentGroup());
                        } 
                        else if (cd.getSegment()!=null)
                        {
                            cProps.put("Segment", cd.getSegment());
                        }
                        else
                        {
                            cProps.put("Segment group", "all");
                        }
                    }
                    for (SpecificCapacitance sc: mp.getSpecificCapacitance()) 
                    {
                        InfoNode scProps = new InfoNode();
                        scProps.put("Value", formatDimensionalQuantity(sc.getValue()));
                        
                        if (sc.getSegmentGroup() !=null) {
                            cellProps.put("Specific capacitance on group "+sc.getSegmentGroup(), scProps);
                            scProps.put("Segment group", sc.getSegmentGroup());
                        } else if (sc.getSegment() !=null) {
                            cellProps.put("Specific capacitance on segment "+sc.getSegment(), scProps);
                            scProps.put("Segment", sc.getSegment());
                        } else {
                            cellProps.put("Specific capacitance", scProps);
                            scProps.put("Segment group", "all");
                        } 
                    }
                    IntracellularProperties ip = cell.getBiophysicalProperties().getIntracellularProperties();
                    
                    for (Resistivity res: ip.getResistivity()) 
                    {
                        InfoNode resProps = new InfoNode();
                        resProps.put("Value", formatDimensionalQuantity(res.getValue()));
                        
                        if (res.getSegmentGroup() !=null) {
                            cellProps.put("Resistivity on group "+res.getSegmentGroup(), resProps);
                            resProps.put("Segment group", res.getSegmentGroup());
                        } else if (res.getSegment() !=null) {
                            cellProps.put("Resistivity on segment "+res.getSegment(), resProps);
                            resProps.put("Segment", res.getSegment());
                        } else {
                            cellProps.put("Resistivity", resProps);
                            resProps.put("Segment group", "all");
                        } 
                    }
                    
                }

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
                try {
                    Component comp = Utils.convertNeuroMLToComponent(element);
                    ComponentType ct = comp.getComponentType();
                    for (ParamValue pv: comp.getParamValues()) {
                        if (comp.hasAttribute(pv.getName())) {
                            String orig = comp.getStringValue(pv.getName());
                            String formatted = formatDimensionalQuantity(orig);
                            elementProps.put(pv.getName(), formatted);
                        }
                    }
                } catch (LEMSException ce) {
                    throw new NeuroMLException("Problem extracting info from NeuroML component",ce);
                }

            }
        }

		return infoRoot;
	}
    
    private static String formatDimensionalQuantity(String value) throws NeuroMLException 
    {
        if (value==null)
            return "Null quantity!!";
        String returnVal = value;
        try
        {
            float v = Float.parseFloat(value);
            return v+"";
        }
        catch (NumberFormatException e) 
        {
            // continue...
        }
        String siSymbol = Utils.getSIUnitInNeuroML(Utils.getDimension(value)).getSymbol();
        
        String[] magUnit = QuantityReader.splitToMagnitudeAndUnit(value);

        //TODO: replace with DecimalFormat
        String val = Utils.getMagnitudeInSI(value)+"";
        if (val.endsWith("0") && val.indexOf("E") < 0 && !val.endsWith(".0") && val.indexOf(".") > 0) {
            val = val.substring(0, val.length() - 1);
        }

        String si = val + (siSymbol.equals(Unit.NO_UNIT) ? "" : " " + siSymbol);
        if (!value.equals(si)) {
            returnVal = returnVal + " (" + si + ")";
        }
        return returnVal;
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
