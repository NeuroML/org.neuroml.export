package org.neuroml.export.graph;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.lemsml.jlems.core.expression.Valued;

import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.*;
import org.lemsml.jlems.core.type.dynamics.*;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.base.ANeuroMLBaseWriter;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Format;
import org.neuroml.export.utils.Utils;
import org.neuroml.export.utils.support.ModelFeature;
import org.neuroml.export.utils.support.SupportLevelInfo;
import org.neuroml.model.util.NeuroMLException;

@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public class GraphWriter extends ANeuroMLBaseWriter
{

	String netShape = "rectangle";
	String popShape = "ellipse";
	String compShape = "ellipse";
	String compTypeShape = "box";

	String compTypeStyle = "\"rounded, filled\"";

	String extendsLink = "onormal";
	String popElementLink = "diamond";
	String childLink = "diamond";

	String compTypeCol = "#D6E0EA";

	String paramsColour = "#666666";

	StringBuilder main = null;
	StringBuilder net = null;
	StringBuilder comps = null;
	StringBuilder compTypes = null;
	StringBuilder extern = null;
	ArrayList<String> edgesMade = new ArrayList<String>();
	HashMap<Integer, StringBuilder> compTypeSubnets = new HashMap<Integer, StringBuilder>();

	static boolean compTypesOnly = false;
	static boolean paramInfo = true;

	static ComponentType rootCompType = null;

	static boolean rankdirLR = false;

	private static final ArrayList<String> suppressChildren = new ArrayList<String>();

	public GraphWriter(Lems lems) throws ModelFeatureSupportException, NeuroMLException, LEMSException
	{
		super(lems, Format.GRAPH_VIZ);
	}

	public GraphWriter(Lems lems, File outputFolder, String outputFileName) throws ModelFeatureSupportException, NeuroMLException, LEMSException
	{
		super(lems, Format.GRAPH_VIZ, outputFolder, outputFileName);
	}

	@Override
	public void setSupportedFeatures()
	{
		sli.addSupportInfo(format, ModelFeature.ABSTRACT_CELL_MODEL, SupportLevelInfo.Level.HIGH);
		sli.addSupportInfo(format, ModelFeature.COND_BASED_CELL_MODEL, SupportLevelInfo.Level.HIGH);
		sli.addSupportInfo(format, ModelFeature.SINGLE_COMP_MODEL, SupportLevelInfo.Level.HIGH);
		sli.addSupportInfo(format, ModelFeature.NETWORK_MODEL, SupportLevelInfo.Level.HIGH);
		sli.addSupportInfo(format, ModelFeature.MULTI_CELL_MODEL, SupportLevelInfo.Level.HIGH);
		sli.addSupportInfo(format, ModelFeature.MULTI_POPULATION_MODEL, SupportLevelInfo.Level.HIGH);
		sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_INPUTS_MODEL, SupportLevelInfo.Level.HIGH);
		sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.HIGH);
		sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_GAP_JUNCTIONS_MODEL, SupportLevelInfo.Level.HIGH);
		sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_ANALOG_CONNS_MODEL, SupportLevelInfo.Level.HIGH);
		sli.addSupportInfo(format, ModelFeature.MULTICOMPARTMENTAL_CELL_MODEL, SupportLevelInfo.Level.HIGH);
		sli.addSupportInfo(format, ModelFeature.HH_CHANNEL_MODEL, SupportLevelInfo.Level.HIGH);
		sli.addSupportInfo(format, ModelFeature.KS_CHANNEL_MODEL, SupportLevelInfo.Level.HIGH);
	}

	@Override
	protected void addComment(StringBuilder sb, String comment)
	{
		String comm = "    # ";
		sb.append(comm + comment + "\n");
	}

	public String getMainScript() throws GenerationException
	{
		try
		{
			main = new StringBuilder();
			net = new StringBuilder();
			comps = new StringBuilder();
			compTypes = new StringBuilder();
			extern = new StringBuilder();

			if(!compTypesOnly)
			{
				Target target = lems.getTarget();

				Component simCpt = target.getComponent();
				E.info("simCpt: " + simCpt);

				String targetId = simCpt.getStringValue("target");

				Component tgtNet = lems.getComponent(targetId);

				addComment(main, "GraphViz compliant export for:" + tgtNet.summary() + "\n");

				main.append("digraph " + simCpt.getID().replaceAll("-", "_") + " {\n");
				main.append("fontsize=10;\n");
				main.append("overlap=false;\n\n");
				if(rankdirLR) main.append("rankdir=\"LR\"\n");

				net.append("    node [shape=" + netShape + "]; " + tgtNet.getID() + ";\n");

				ArrayList<Component> pops = tgtNet.getChildrenAL("populations");

				for(Component pop : pops)
				{
					String compRef = pop.getStringValue("component");
					Component popComp = lems.getComponent(compRef);
                    String color = "white";
                    String fontcolor = "black";
					addComment(net, "   Population " + pop.getID() + " contains components of: " + popComp + " ");
                    for (Component cc: pop.getChildrenAL("property"))
                    {
                        if (cc.getStringValue("tag").equals("color"))
                        {
                            String colorRgb = cc.getStringValue("value");
                            String[] w = colorRgb.split(" ");
                            Color c = new Color((int)Math.floor(Float.parseFloat(w[0])*255),
                                                (int)Math.floor(Float.parseFloat(w[1])*255), 
                                                (int)Math.floor(Float.parseFloat(w[2])*255));
                            int totDark = c.getRed()+c.getBlue()+c.getGreen();
                            if (totDark<250)
                                fontcolor = "white";
                            color = Integer.toHexString(c.getRGB() & 0xffffff);
                            
                            while (color.length() < 6) {
                                color = "0" + color;
                            }
                            color = "#"+color;
                        }
                    }
					net.append("    node [shape=" + popShape + ",color=\""+color+"\",fontcolor=\""+fontcolor+"\"]; " + pop.getID() + ";    \n");
					net.append("    "+tgtNet.getID() + " -> " + pop.getID() + " [len=1.00, arrowhead=" + childLink + "]\n\n");

					addCompAndChildren(popComp, pop.getID(), pop.getStringValue("size"));

				}
				if(pops.isEmpty())
				{ // i.e. simulate 1 component
					addCompAndChildren(tgtNet, tgtNet.getID(), null);
				}

				main.append("\nsubgraph cluster_network {\n");
				main.append("    style=filled;\n");
				main.append("    color=\"#D6eeEA\";\n");
				main.append("    node [style=filled,color=white];\n");
				main.append("    label = \"Network to be simulated\";\n\n");

				main.append(net.toString());
				main.append("}\n\n");

				main.append("subgraph cluster_comps {\n");
				main.append("    style=filled;\n");
				main.append("    color=\"#CCFFCC\";\n");
				main.append("    node [style=filled,color=white];\n");
				main.append("    label = \"Components\";\n\n");
				main.append(comps.toString());

				main.append("}\n\n");
			}
			else
			{
				main.append("digraph " + rootCompType.getName() + " {\n");
				main.append("fontsize=10;\n");
				main.append("bgcolor=\"#D6E0EA\";\n");
				addCompTypeAndChildrenAndExtends(rootCompType, null, null, 0);
			}

			if(!compTypesOnly)
			{
				main.append("subgraph cluster_compTypes {\n");
				main.append("    style=filled;\n");
				main.append("    color=\"" + compTypeCol + "\";\n");
				main.append("    node [style=" + compTypeStyle + ",color=white];\n");
				main.append("    label = \"Component Types\";\n");
			}

			int maxDepth = 30;

			for(int depth : compTypeSubnets.keySet())
			{
				if(depth <= maxDepth)
				{
					main.append("    subgraph cluster_" + depth + " {\n");
					main.append("        node[style=filled];\n");
					main.append("        color=\"" + compTypeCol + "\";\n");
					// main.append("        color=black;\n");
					main.append("        node [style=" + compTypeStyle + ",color=white];\n");
					main.append("        " + compTypeSubnets.get(depth).toString().replaceAll("\n", "\n        "));
					main.append("\n    }\n\n");
				}
				else
				{
					compTypes.append("    " + compTypeSubnets.get(depth).toString().replaceAll("\n", "\n    "));
				}
			}

			main.append("    node [style=" + compTypeStyle + ",color=white];\n");
			main.append(compTypes.toString());

			if(!compTypesOnly)
			{
				main.append("}\n\n");
			}

			main.append(extern.toString());
			main.append("}\n");

			// System.out.println(main);
			return main.toString();

		}
		catch(ContentError e)
		{
			throw new GenerationException("Error with LEMS content", e);
		}
	}

	HashMap<String, Integer> noIdComps = new HashMap<String, Integer>();
    
    protected String getDimensionString(String dim)
    {
        if (dim.equals("none")) return "";
        else return " ("+dim+")"; 
    }
    
    

	protected String getCompTypeInfo(ComponentType compType) throws ContentError
	{

		String label = " label=<<table border=\"0\" cellborder=\"0\"><tr><td>" + compType.getName() + "" + "</td></tr>";
		ArrayList<String> expAdded = new ArrayList<String>();
		
		int count = 0;
		int maxLine = 3;

		if(compType.getParameters().size() > 0) label = label + "<tr><td><font color=\"#669999\">Params: ";

		for(Parameter p : compType.getParameters())
		{
			if(count > 0) label = label + ", ";
			if(!expAdded.contains(p.getName()))
			{
				if(count == maxLine)
				{
					label = label + "<br/>";
					count = 0;
				}
				label = label + "" + p.getName() + getDimensionString(p.getDimension().getName());

				count++;
			}
		}
		if(compType.getParameters().size() > 0) label = label + " </font></td></tr>";

		for(Constant c : compType.getConstants())
		{
			//label = label + "<tr><td><font color=\"#662211\">" + c.getName() + getDimensionString(c.getDimension().getName()) + " = " + c.getValue() + "</font></td></tr>";
		}
        
        count = 0;
        if(compType.getConstants().size() > 0) label = label + "<tr><td><font color=\"#662211\">Consts: ";

        for(Constant c : compType.getConstants())
        {
            if(count > 0) label = label + ", ";

            if(count == maxLine)
            {
                label = label + "<br/>";
                count = 0;
            }
            String vu = getValAndUnit(c.getValue(), c.getDimension().getName());
            label = label + "" + c.getName() + " = " + vu;

            count++;
        }
        if(compType.getConstants().size() > 0) label = label + " </font></td></tr>";
            

		for(Requirement r : compType.getRequirements())
		{
			label = label + "<tr><td><font color=\"#666699\">REQUIRES: " + r.getName() + getDimensionString(r.getDimension().getName()) + "</font></td></tr>";
		}
        
		for(Text t : compType.getTexts())
		{
			label = label + "<tr><td><font color=\"#B2C0D9\">" + t.getName() + "</font></td></tr>";
		}
        
		if(compType.getDynamics() != null)
		{
            count = 0;
            if(compType.getDynamics().getStateVariables().size() > 0) label = label + "<tr><td><font color=\"#FF9966\">State vars: ";
            
			for(StateVariable sv : compType.getDynamics().getStateVariables())
			{
				if(!expAdded.contains(sv.getName()))
                {
                    if(count > 0) label = label + ", ";

                    if(count == maxLine)
                    {
                        label = label + "<br/>";
                        count = 0;
                    }
                    label = label + "" + sv.getName() + getDimensionString(sv.getDimension().getName());

                    count++;
                    
                    expAdded.add(sv.getName());
                }
			}
            if(compType.getDynamics().getStateVariables().size() > 0) label = label + " </font></td></tr>";
            
            
			for(DerivedVariable dv : compType.getDynamics().getDerivedVariables())
			{
                String expr = dv.getValueExpression();
                if (dv.getSelect()!=null)
                {
                    if (dv.getReduce()!=null && dv.getReduce().equals("add"))
                        expr = "SUM OF: " + dv.getSelect();
                    else if (dv.getReduce()!=null && dv.getReduce().equals("multiply"))
                        expr = "PRODUCT OF: " + dv.getSelect();
                    else
                        expr = dv.getSelect();
                }
				label = label + "<tr><td><font color=\"#99CC00\">" + dv.getName() + " = " + expr + "</font></td></tr>";
			}
            
			for(TimeDerivative td : compType.getDynamics().getTimeDerivatives())
			{
				label = label + "<tr><td><font color=\"#666633\">" + td.getStateVariable().getName() + "' = " + td.value + "</font></td></tr>";
			}
            
			for(OnCondition oc : compType.getDynamics().getOnConditions())
			{
				label = label + "<tr><td><font color=\"#996633\">IF "
						+ oc.test.replaceAll("<=", "lte").replaceAll(">=", "gte").replaceAll("less_than", "&lt;").replaceAll("greater_than", "&gt;").replaceAll(".and.", "AND")
						+ " THEN </font></td></tr><tr><td><font color=\"#996633\">";
				int c = 0;
				for(StateAssignment sa : oc.getStateAssignments())
				{
					if(c >= 1) label = label + "  <br/>AND ";
					label = label + "(" + sa.getStateVariable().getName() + " = " + sa.value + ")";
					c++;
				}
                for(EventOut eo : oc.getEventOuts())
				{
					if(c >= 1) label = label + "  <br/>AND ";
					label = label + "(EVENT: " + eo.getPortName() + ")";
					c++;
				}
				label = label + "</font></td></tr>";
			}

			for(Regime r : compType.getDynamics().getRegimes())
			{
				label = label + "<tr><td><font color=\"#555555\">-------- REGIME: " + r.getName() + "" + (r.isInitial() ? " (Initial)" : "") + " --------</font></td></tr>";
				for(OnEntry oe : r.getOnEntrys())
				{
					label = label + "<tr><td><font color=\"#229933\">ON ENTRY: ";
					int c = 0;
					for(StateAssignment sa : oe.getStateAssignments())
					{
						if(c >= 1) label = label + "  <br/>AND ";
						label = label + "(" + sa.getStateVariable().getName() + " = " + sa.value + ")";
						c++;
					}
					label = label + "</font></td></tr>";
				}

				for(TimeDerivative td : r.getTimeDerivatives())
				{
					label = label + "<tr><td><font color=\"#666633\">" + td.getStateVariable().getName() + "' = " + td.value + "</font></td></tr>";
				}

				for(OnCondition oc : r.getOnConditions())
				{
					label = label + "<tr><td><font color=\"#996633\">IF "
							+ oc.test.toString().replaceAll("<=", "lte").replaceAll(">=", "gte").replaceAll("less_than", "&lt;").replaceAll("greater_than", "&gt;").replaceAll(".and.", "AND")
							+ " THEN </font></td></tr><tr><td><font color=\"#996633\">";
					int c = 0;
					for(StateAssignment sa : oc.getStateAssignments())
					{
						if(c >= 1) label = label + "  <br/>AND  ";
						label = label + "(" + sa.getStateVariable().getName() + " = " + sa.value + ")";
						c++;
					}
					for(Transition t : oc.getTransitions())
					{
						if(c >= 1) label = label + "  <br/>AND  ";
						label = label + " GOTO " + t.getRegime() + "";
						c++;
					}
					label = label + "</font></td></tr>";
				}
				label = label + "<tr><td><font color=\"#555555\">---------------------------------------</font></td></tr>";
			}

		}
        
        count = 0;
        if(compType.getExposures().size() > 0) label = label + "<tr><td><font color=\"" + paramsColour + "\">Exposures: ";
        for(Exposure e : compType.getExposures())
		{
            if(count > 0) label = label + ", ";
            if(count == maxLine)
            {
                label = label + "<br/>";
                count = 0;
            }
            label = label + "" + e.getName() + getDimensionString(e.getDimension().getName());
            count++;
		}
        if(compType.getExposures().size() > 0) label = label + " </font></td></tr>";
        
		label = label + "</table>>";
		return label;
	}

	protected void addCompTypeAndChildrenAndExtends(ComponentType compType, String parent, String extendedType, int depth) throws ContentError
	{
		if(compTypeSubnets.get(depth) == null) compTypeSubnets.put(depth, new StringBuilder());

		StringBuilder compTypeSub = compTypeSubnets.get(depth);

		String label = ", label=\"" + compType.getName() + "\"";

		if(paramInfo) label = getCompTypeInfo(compType);

		compTypeSub.append("    node [shape=" + compTypeShape + label + "]; " + compType.getName() + ";\n");

		if(suppressChildren.contains(compType.getName()))
		{
			String dummy = "ChildrenRemoved_" + compType.getName();
			String nodeInfo = "    node [shape=" + compTypeShape + ", label=\"...\"]; " + dummy + ";\n";
			StringBuilder compTypeSubChild = compTypeSubnets.get(depth + 1);
			compTypeSubChild.append(nodeInfo);
			compTypes.append("    "+compType.getName() + " -> " + dummy + " [len=1.00, arrowhead=" + childLink + "]\n");
		}
		else
		{
			for(Child c : compType.childs)
			{
				E.info("+++ ComponentType " + compType.getName() + " has child " + c + " +++");
				addCompTypeAndChildrenAndExtends(c.getComponentType(), compType.getName(), null, depth + 1);

				String edge = "    "+compType.getName() + " -> " + c.getComponentType().getName() + " [len=1.00, arrowhead=" + childLink + "]\n";

				if(!edgesMade.contains(edge))
				{
					compTypes.append(edge);
					edgesMade.add(edge);
				}
			}
			for(Children c : compType.childrens)
			{
				addCompTypeAndChildrenAndExtends(c.getComponentType(), compType.getName(), null, depth + 1);
				String edge = "    "+compType.getName() + " -> " + c.getComponentType().getName() + " [len=1.00, arrowhead=" + childLink + "]\n";

				if(!edgesMade.contains(edge))
				{
					compTypes.append(edge);
					edgesMade.add(edge);
				}
			}

			LemsCollection<ComponentType> extendingTypes = lems.getComponentTypesExtending(compType.getName());

			String extended = compType.getName();

			for(ComponentType extType : extendingTypes)
			{
				E.info("... ComponentType " + extType.getName() + " extends " + extended + "...");

				addCompTypeAndChildrenAndExtends(extType, null, extended, depth + 1);
				String edge = "    "+extType.getName() + " -> " + extended + " [len=1.00, arrowhead=" + extendsLink + "]\n";

				if(!edgesMade.contains(edge))
				{
					compTypes.append(edge);
					edgesMade.add(edge);
				}
			}
		}

	}
    
    // Quick and dirty...
    private String getUnitSubstitution(String siUnit)
    {
        if (siUnit.equals(" kg m^2 s^-3 A^-1")) return " V";
        if (siUnit.equals(" kg^-1 m^-2 s^3 A^2")) return " S";
        if (siUnit.equals(" kg^-1 m^-2 s^4 A^2")) return " F";
        return siUnit;
    }
    
    ArrayList<String> addedCompToCompTypes = new ArrayList<String>();
    
    protected String getValAndUnit(double value, String dimName) throws ContentError
    {
        String unit = "";
		Dimension d = lems.getDimensions().getByName(dimName);
		if(d != null && Dimension.getSIUnit(d).length() > 0) unit = " " + Dimension.getSIUnit(d);
                    
        unit = getUnitSubstitution(unit);

	    String val = value +"";

		if(val.endsWith(".0")) val = val.substring(0, val.length() - 2);
        
        return val + unit;
    }

	protected void addCompAndChildren(Component comp, String parent, String arrowLabel) throws ContentError
	{
		String ref = comp.getName()==null ? comp.getID() +" ("+comp.getTypeName()+")" : "" + comp.getName() + " (id = " + comp.getID() + ")" + "";

		if(comp.getID() == null)
		{
			if(!noIdComps.containsKey(comp.getName()))
			{
				noIdComps.put(comp.getName(), 0);
			}
			int numSoFar = noIdComps.get(comp.getName());

			ref = "" + comp.getName() + " (" + numSoFar + ")" + "";
			numSoFar++;
			noIdComps.put(comp.getName(), numSoFar);

		}
		String label = "";

		if(paramInfo)
		{
			label = " label=<<table border=\"0\" cellborder=\"0\"><tr><td>" + ref + "</td></tr><tr><td><font color=\"" + paramsColour + "\">";
			int count = 0;
			int maxLine = 3;

			for(ParamValue pv : comp.getParamValues())
			{
				if(!comp.getComponentType().constants.hasName(pv.getName()))
				{
					if(count > 0) label = label + ", ";
					if(count == maxLine)
					{
						label = label + "<br/>";
						count = 0;
					}
					
                    String vu = getValAndUnit(pv.getDoubleValue(), pv.getDimensionName());
                    
					label = label + "" + pv.getName() + " = " + vu;
					count++;
				}
			}

			label = label + "</font></td></tr></table>>";
		}

		comps.append("    node [shape=" + compShape + label + "]; \"" + ref + "\";\n\n");
		String al = "";
		if(arrowLabel != null) al = "label=\"" + arrowLabel + "\",";

		String edge = "    \"" + parent + "\"" + " -> \"" + ref + "\" [" + al + "len=1.00, arrowhead=" + childLink + "]\n";
		if(!edgesMade.contains(edge))
		{
			comps.append(edge);
			edgesMade.add(edge);
		}

		ComponentType compType = comp.getComponentType();

		label = "";

		if(paramInfo) label = getCompTypeInfo(compType);

		compTypes.append("    node [shape=" + compTypeShape + label + "]; " + compType.getName() + ";\n");

        String link = ref + "____" + compType.getName();
        if (!addedCompToCompTypes.contains(link))
        {
            if(!compTypesOnly) extern.append("    \"" + ref + "\" -> " + compType.getName() + " [len=1.00]\n");
            addedCompToCompTypes.add(link);
        }

		ComponentType extType = compType.getExtends();
		String par = compType.getName();

		while(extType != null)
		{
			label = "";

			if(paramInfo) label = getCompTypeInfo(extType);

			compTypes.append("    node [shape=" + compTypeShape + label + "]; " + extType.getName() + ";\n");
			edge = "    "+par + " -> " + extType.getName() + " [len=1.00, arrowhead=" + extendsLink + "]\n";

			if(!edgesMade.contains(edge))
			{
				compTypes.append(edge);
				edgesMade.add(edge);
			}
			par = extType.getName();
			extType = extType.getExtends();
		}

		for(Component c : comp.getAllChildren())
		{
			addCompAndChildren(c, ref, null);
		}
	}

	public static void main(String[] args) throws Exception
	{
		//compTypesOnly = true;
		File xml = new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex0_IaF.xml");
        //xml = new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex9_FN.xml");
        xml = new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex5_DetCell.xml");
        xml = new File("../NeuroMLlite/examples/LEMS_SimExample9.xml");
        xml = new File("../NeuroMLlite/examples/LEMS_SimExample4.xml");
        //xml = new File("../NeuroMLlite/examples/LEMS_SimExample6_PyNN.xml");
        //xml = new File("../neuroConstruct/osb/showcase/PsyNeuLinkShowcase/NeuroML2/LEMS_FitzHughNagumo.xml");
        //xml = new File("../NeuroMLlite/examples/LEMS_SimExample10.xml");
        //xml = new File("../NeuroMLlite/examples/LEMS_SimExample6_PyNN.xml");
        //xml = new File("../NeuroMLlite/examples/LEMS_SimExample3.xml");
        //rootCompType = new ComponentType("fnCell");
        //xml = new File("../neuroConstruct/osb/showcase/PsyNeuLinkShowcase/NeuroML2/LEMS_SimABC.xml");
		File targetDir = new File(".");

		//rootCompType = new ComponentType("neuroml");

		Lems lems = Utils.readLemsNeuroMLFile(xml).getLems();
        //System.out.println("Summary: "+lems.textSummary());
        
		generatePng(lems, targetDir, "Test");
        if (3==3) return;

		lems.addComponentType(rootCompType);

		paramInfo = false;
		// paramInfo = true;

		rootCompType.childs.add(new Child("network", lems.getComponentTypeByName("network")));
		rootCompType.childs.add(new Child("basePointCurrent", lems.getComponentTypeByName("basePointCurrent")));
		rootCompType.childs.add(new Child("baseCell", lems.getComponentTypeByName("baseCell")));
		rootCompType.childs.add(new Child("baseIonChannel", lems.getComponentTypeByName("baseIonChannel")));
		//rootCompType.childs.add(new Child("extracellularProperties", lems.getComponentTypeByName("extracellularProperties")));
		//rootCompType.childs.add(new Child("intracellularProperties", lems.getComponentTypeByName("intracellularProperties")));
		rootCompType.childs.add(new Child("morphology", lems.getComponentTypeByName("morphology")));

		suppressChildren.add("baseIonChannelPassive");
		suppressChildren.add("baseIonChannelKS");
		suppressChildren.add("baseIonChannelHH");
		suppressChildren.add("baseCellMembPot");

		String overview = "NML2_Overview";
		generatePng(lems, targetDir, overview);

		suppressChildren.clear();

		rootCompType = new ComponentType("neuroml");
		lems.addComponentType(rootCompType);

		paramInfo = true;

		// rootCompType.childs.add(new Child("network", lems.getComponentTypeByName("network")));
		rootCompType.childs.add(new Child("basePointCurrent", lems.getComponentTypeByName("basePointCurrent")));
		// rootCompType.childs.add(new Child("baseCell", lems.getComponentTypeByName("baseCell")));
		// rootCompType.childs.add(new Child("baseIonChannel", lems.getComponentTypeByName("baseIonChannel")));
		// rootCompType.childs.add(new Child("extracellularProperties", lems.getComponentTypeByName("extracellularProperties")));
		// rootCompType.childs.add(new Child("intracellularProperties", lems.getComponentTypeByName("intracellularProperties")));
		// rootCompType.childs.add(new Child("morphology", lems.getComponentTypeByName("morphology")));

		String synapses = "NML2_Synapses";
		generatePng(lems, targetDir, synapses);

		rootCompType = new ComponentType("neuroml");
		lems.addComponentType(rootCompType);

		paramInfo = true;

		// rootCompType.childs.add(new Child("network", lems.getComponentTypeByName("network")));
		// rootCompType.childs.add(new Child("pointCurrent", lems.getComponentTypeByName("pointCurrent")));
		// rootCompType.childs.add(new Child("baseCell", lems.getComponentTypeByName("baseCell")));
		rootCompType.childs.add(new Child("baseIonChannel", lems.getComponentTypeByName("baseIonChannel")));
		// rootCompType.childs.add(new Child("extracellularProperties", lems.getComponentTypeByName("extracellularProperties")));
		// rootCompType.childs.add(new Child("intracellularProperties", lems.getComponentTypeByName("intracellularProperties")));
		// rootCompType.childs.add(new Child("morphology", lems.getComponentTypeByName("morphology")));

		String channels = "NML2_Channels";
		generatePng(lems, targetDir, channels);

		rootCompType = new ComponentType("neuroml");
		lems.addComponentType(rootCompType);

		paramInfo = true;

		// rootCompType.childs.add(new Child("network", lems.getComponentTypeByName("network")));
		// rootCompType.childs.add(new Child("pointCurrent", lems.getComponentTypeByName("pointCurrent")));
		rootCompType.childs.add(new Child("baseCell", lems.getComponentTypeByName("baseCell")));
		// rootCompType.childs.add(new Child("baseIonChannel", lems.getComponentTypeByName("baseIonChannel")));
		// rootCompType.childs.add(new Child("extracellularProperties", lems.getComponentTypeByName("extracellularProperties")));
		// rootCompType.childs.add(new Child("intracellularProperties", lems.getComponentTypeByName("intracellularProperties")));
		// rootCompType.childs.add(new Child("morphology", lems.getComponentTypeByName("morphology")));

		String cells = "NML2_Cells";
		generatePng(lems, targetDir, cells);

	}

    
	private static void generatePng(Lems lems, File targetDir, String name) throws IOException, InterruptedException, GenerationException, ModelFeatureSupportException
	{
		try
		{
			GraphWriter gw = new GraphWriter(lems);

			File imgFile = new File(targetDir, name + ".png");
			File gv = new File(targetDir, name + ".gv");

			FileUtil.writeStringToFile(gw.getMainScript(), gv);

			E.info("Graph details written to file: " + gv);

			String cmd = "dot -Tpng  " + gv.getAbsolutePath() + " -o " + imgFile.getAbsolutePath();
			String[] env = new String[] {};
			Runtime run = Runtime.getRuntime();
			Process pr = run.exec(cmd, env, gv.getParentFile());

			int ret = pr.waitFor();

			E.info("Written out to image file: " + imgFile + "\nUsed: " + cmd + "\nReturn value: " + ret);
		}
		catch(NeuroMLException e)
		{
			throw new GenerationException("Problem generating PNG", e);
		}
		catch(LEMSException e)
		{
			throw new GenerationException("Problem generating PNG", e);
		}
	}

	@Override
	public List<File> convert()
	{
		List<File> outputFiles = new ArrayList<File>();

		try
		{
			String code = this.getMainScript();

			File outputFile = new File(this.getOutputFolder(), this.getOutputFileName());
			FileUtil.writeStringToFile(code, outputFile);
			outputFiles.add(outputFile);
		}
		catch(GenerationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return outputFiles;
	}
}
