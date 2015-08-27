package org.neuroml.export.neuron;

import java.util.ArrayList;
import java.util.HashMap;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Dimension;
import org.lemsml.jlems.core.type.Exposure;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.utils.LEMSQuantityPath;
import org.neuroml.model.Cell;
import org.neuroml.model.Segment;
import org.neuroml.model.util.CellUtils;
import org.neuroml.model.util.NeuroMLElements;
import org.neuroml.model.util.NeuroMLException;

/**
 * @author Padraig Gleeson
 */
public class LEMSQuantityPathNeuron extends LEMSQuantityPath
{

	HashMap<String, String> compMechNamesHoc = null;
	ArrayList<Component> popsOrComponents = null;
	HashMap<String, Cell> compIdsVsCells = null;
	Component targetComp = null;
	Lems lems = null;
	Component popComp = null;

	public LEMSQuantityPathNeuron(String q, String s, Component targetComp, HashMap<String, String> compMechNamesHoc, ArrayList<Component> popsOrComponents, HashMap<String, Cell> compIdsVsCells,
			Lems lems) throws ContentError
	{
		super(q, s);
		this.targetComp = targetComp;
		this.compMechNamesHoc = compMechNamesHoc;
		this.popsOrComponents = popsOrComponents;
		this.compIdsVsCells = compIdsVsCells;
		this.lems = lems;

		if(myType != Type.VAR_IN_SINGLE_COMP)
		{
			for(Component popsOrComponent : popsOrComponents)
			{
				if(popsOrComponent.getID().equals(population))
				{
					popComp = lems.getComponent(popsOrComponent.getStringValue("component"));
				}
			}
		}

		// System.out.println("----------------------\nCreated:"+this+"\n----------------------");

	}

	private Exposure getExposure(Component c, String path) throws ContentError
	{

		// System.out.println("Path: "+path+", Comp: "+c);
		try
		{
			Exposure e = c.getComponentType().getExposure(path);
			return e;
		}
		catch(ContentError e)
		{
			String child = path.substring(0, path.indexOf("/"));
			String pathInChild = path.substring(path.indexOf("/") + 1);
			Component ch = null;
			for(Component chi : c.getAllChildren())
				if(chi.getID() != null && chi.getID().equals(child)) ch = chi;
			if(ch == null) for(Component chi : c.getAllChildren())
				if(chi.getTypeName().equals(child)) ch = chi;

			return getExposure(ch, pathInChild);
		}

	}

	public Dimension getDimension() throws ContentError
	{
		String path = getVariablePathInPopComp();
		Component comp = (myType != Type.VAR_IN_SINGLE_COMP) ? popComp : targetComp;
		return getExposure(comp, path).getDimension();
	}

	public String getNeuronVariableLabel() throws ContentError
	{

		if(!isVariableInPopulation())
		{
			return getVariable();
		}
		else
		{
			return getPopulationArray() + "[" + populationIndex + "]." + getVariable();
		}
	}

	private String convertToNeuronVariable() throws ContentError
	{

		HashMap<String, String> topSubstitutions = new HashMap<String, String>();
		topSubstitutions.put("caConc", "cai");

		String var = new String();

		if(variableParts.length == 1)
		{
			var = variableParts[0];
		}
		else
		{
			if(variableParts[1].contains("membraneProperties"))
			{

				if(variableParts.length == 4)
				{

					var = variableParts[3];

					if(var.equals("gDensity") || var.equals("iDensity"))
					{
						String channelDensId = variableParts[2];
						ArrayList<Component> channelDensityComps = popComp.getChild("biophysicalProperties").getChild("membraneProperties").getChildrenAL("channelDensities");
						if(var.equals("gDensity"))
						{
							for(Component c : channelDensityComps)
							{
								if(c.getID().equals(channelDensId))
								{
									var = "gion_" + c.getStringValue("ionChannel");
								}
							}
						}
						else if(var.equals("iDensity"))
						{
							for(Component c : channelDensityComps)
							{
								if(c.getID().equals(channelDensId))
								{
									var = "i" + c.getStringValue("ion");
								}
							}
						}
					}
				}

				if(variableParts.length > 4)
				{
					for(int i = 4; i < variableParts.length; i++)
					{
						var += variableParts[i] + "_";
					}

					var += variableParts[3];
				}
			}
		}
		if(var.length() == 0)
		{
			var = getVariable();
		}

		for(String key : topSubstitutions.keySet())
		{
			if(var.equals(key))
			{
				var = topSubstitutions.get(key);
			}
		}

		return var;

	}

	public String getNeuronVariableReference() throws ContentError, NeuroMLException
	{

		if(myType == Type.VAR_IN_SINGLE_COMP)
		{
			String hoc = getPopulation() + targetComp.getName() + "[i]";
			String mechRef = compMechNamesHoc.get(hoc).replaceAll("\\[i\\]", "[" + populationIndex + "]");
			String varRef = mechRef + "." + getVariable();
			return varRef;

		}
		else
		{

			if(popComp != null
					&& (popComp.getComponentType().isOrExtends(NeuroMLElements.CELL_COMP_TYPE) || ((popComp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_CAP_COMP_TYPE) || popComp
							.getComponentType().isOrExtends(NeuroMLElements.BASE_IAF_CELL)) && convertToNeuronVariable().equals(NRNUtils.NEURON_VOLTAGE))))
			{

				if(compIdsVsCells.containsKey(popComp.getID()))
				{
					Cell cell = compIdsVsCells.get(popComp.getID());
					NamingHelper nh = new NamingHelper(cell);
					Segment segment = CellUtils.getSegmentWithId(cell, segmentId);
					String varInst = nh.getNrnSectionName(segment);

					float fract;
					if(cell.getMorphology().getSegment().size() == 1) {
                        fract = 0.5f;
                    }
                    else if (!CellUtils.hasSegmentGroup(cell, varInst) && segment.getName().equals(varInst)) {
                        // No real segment group, segment ids being used for sections...
                        fract = 0.5f;
                    } else {
                        fract = (float) CellUtils.getFractionAlongSegGroupLength(cell, varInst, segmentId, 0.5f);
                    }
                    
					String varRef = getPopulationArray() + "[" + populationIndex + "]." + varInst + "." + convertToNeuronVariable() + "(" + fract + ")";
					return varRef;
				}
				else
				{
					String nrnVar = convertToNeuronVariable();
					String varRef = getPopulation() + "[" + populationIndex + "]." + nrnVar;

					if(nrnVar.equals(NRNUtils.NEURON_VOLTAGE))
					{ // redundant..?
						varRef += "(0.5)";
					}
					return varRef;
				}

			}
			else
			{
				String hoc = population + "[i]";
				// System.out.println(this);
				// System.out.println(popComp);
				String mechRef = compMechNamesHoc.get(hoc).replaceAll("\\[i\\]", "[" + populationIndex + "]");
				String varRef = mechRef + "." + getVariable();
				return varRef;
			}
		}

	}

	@Override
	public String toString()
	{
		String ref;
		try
		{
			ref = getNeuronVariableReference();
		}
		catch(ContentError ex)
		{
			ref = "=== Unable to determine reference: " + ex;
		}
		catch(NeuroMLException ex)
		{
			ref = "=== Unable to determine reference: " + ex;
		}

		return super.toString() + "\nNeuron ref:     " + ref
		/* + "\ncompIdsVsCells: " + compIdsVsCells */
		+ "\npopsOrComponents: " + popsOrComponents + "\ntargetComp: " + targetComp + "\npopComp: " + popComp;
	}

	public static void main(String[] args) throws Exception
	{
		HashMap<String, String> compMechNamesHoc = new HashMap<String, String>();
		compMechNamesHoc.put("fnPop1[i]", "m_fitzHughNagumoCell[i]");
		ArrayList<String> paths = new ArrayList<String>();
		paths.add("X1__S");
		paths.add("hhpop[6]/bioPhys1/membraneProperties/naChans/naChan/m/q");
		paths.add("fnPop1[0]/V");

		for(String path : paths)
		{
			LEMSQuantityPathNeuron l1 = new LEMSQuantityPathNeuron(path, "1", null, compMechNamesHoc, null, null, null);
			System.out.println("\n--------\n" + l1);
		}
	}

}
