package org.neuroml.export.brian;

import java.io.File;
import java.util.ArrayList;

import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.flatten.ComponentFlattener;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.run.ConnectionError;
import org.lemsml.jlems.core.sim.*;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.ComponentType;
import org.lemsml.jlems.core.type.Constant;
import org.lemsml.jlems.core.type.Dimension;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.LemsCollection;
import org.lemsml.jlems.core.type.ParamValue;
import org.lemsml.jlems.core.type.Parameter;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.core.type.Unit;
import org.lemsml.jlems.core.type.dynamics.DerivedVariable;
import org.lemsml.jlems.core.type.dynamics.Dynamics;
import org.lemsml.jlems.core.type.dynamics.OnStart;
import org.lemsml.jlems.core.type.dynamics.StateAssignment;
import org.lemsml.jlems.core.type.dynamics.StateVariable;
import org.lemsml.jlems.core.type.dynamics.TimeDerivative;
import org.lemsml.jlems.io.util.FileUtil;
import org.lemsml.jlems.io.xmlio.XMLSerializer;
import org.neuroml.export.Utils;
import org.neuroml.export.base.BaseWriter;


public class BrianWriter extends BaseWriter {
	
	static String DEFAULT_POP = "OneComponentPop";

	public BrianWriter(Lems lems) {
		super(lems, "Brian");
	}

	@Override
	protected void addComment(StringBuilder sb, String comment) {

		String comm = "# ";
		String commPre = "'''";
		String commPost = "'''";
		if (comment.indexOf("\n") < 0)
			sb.append(comm + comment + "\n");
		else
			sb.append(commPre + "\n" + comment + "\n" + commPost + "\n");
	}

	public String getMainScript() throws ContentError, ParseError {
		StringBuilder sb = new StringBuilder();
		addComment(sb, "Brian simulator compliant Python export for:\n\n"
				+ lems.textSummary(false, false));
		
		addComment(sb, Utils.getHeaderComment(format));

		sb.append("from brian import *\n\n");
		sb.append("from math import *\n\n");

		Target target = lems.getTarget();

		Component simCpt = target.getComponent();
		E.info("simCpt: " + simCpt);

		String targetId = simCpt.getStringValue("target");

		Component tgtNet = lems.getComponent(targetId);
		addComment(sb, "Adding simulation " + simCpt + " of network: " + tgtNet.summary());

		ArrayList<Component> pops = tgtNet.getChildrenAL("populations");

		if (pops.size()>0) {
			for (Component pop : pops) {
				String compRef = pop.getStringValue("component");
				Component popComp = lems.getComponent(compRef);
				addComment(sb, "   Population " + pop.getID()
						+ " contains components of: " + popComp + " ");
	
				String prefix = popComp.getID() + "_";
	
				CompInfo compInfo = new CompInfo();
				ArrayList<String> stateVars = new ArrayList<String>();
	
				getCompEqns(compInfo, popComp, pop.getID(), stateVars, "");
	
				sb.append("\n" + compInfo.params.toString());
	
				sb.append(prefix + "eqs=Equations('''\n");
				sb.append(compInfo.eqns.toString());
				sb.append("''')\n\n");
	
				String flags = "";// ,implicit=True, freeze=True
				sb.append(pop.getID() + " = NeuronGroup("
						+ pop.getStringValue("size") + ", model=" + prefix + "eqs"
						+ flags + ")\n");
	
				sb.append(compInfo.initInfo.toString());
			}
		} else {
			String prefix = "";
			
			CompInfo compInfo = new CompInfo();
			ArrayList<String> stateVars = new ArrayList<String>();

			getCompEqns(compInfo, tgtNet, DEFAULT_POP, stateVars, "");

			sb.append("\n" + compInfo.params.toString());

			sb.append(prefix + "eqs=Equations('''\n");
			sb.append(compInfo.eqns.toString());
			sb.append("''')\n\n");

			String flags = "";// ,implicit=True, freeze=True
			sb.append(DEFAULT_POP + " = NeuronGroup("
					+ "1" + ", model=" + prefix + "eqs"
					+ flags + ")\n");

			sb.append(compInfo.initInfo.toString());
		}

		StringBuilder toTrace = new StringBuilder();
		StringBuilder toPlot = new StringBuilder();

		for (Component dispComp : simCpt.getAllChildren()) {
			if (dispComp.getName().indexOf("Display") >= 0) {
				toTrace.append("\n# Display: " + dispComp + "\n");
				toPlot.append("\n# Display: " + dispComp + "\nfigure(\""
						+ dispComp.getTextParam("title") + "\")\n");
				for (Component lineComp : dispComp.getAllChildren()) {
					if (lineComp.getName().indexOf("Line") >= 0) {
						// trace=StateMonitor(hhpop,'v',record=[0])
						String trace = "trace_" + dispComp.getID() + "_"
								+ lineComp.getID();
						String ref = lineComp.getStringValue("quantity");
						
						String pop, num, var;
						if (ref.indexOf("/")>0) {
							pop = ref.split("/")[0].split("\\[")[0];
							num = ref.split("\\[")[1].split("\\]")[0];
							var = ref.split("/")[1];
						} else {
							pop = DEFAULT_POP;
							num = "0";
							var = ref;
						}
							

						// if (var.equals("v")){

						toTrace.append(trace + " = StateMonitor(" + pop + ",'"
								+ var + "',record=[" + num + "]) # "
								+ lineComp.summary() + "\n");
						toPlot.append("plot(" + trace + ".times/second,"
								+ trace + "[" + num + "], color=\""
								+ lineComp.getStringValue("color") + "\")\n");
						// }
					}
				}
			}
		}
		sb.append(toTrace);

		String len = simCpt.getStringValue("length");
		String dt = simCpt.getStringValue("step");

		len = len.replaceAll("ms", "*msecond");
		len = len.replaceAll("0s", "0*second"); // TODO: Fix!!!
		dt = dt.replaceAll("ms", "*msecond");

		if (dt.endsWith("s"))
			dt = dt.substring(0, dt.length() - 1) + "*second"; // TODO: Fix!!!

		sb.append("\ndefaultclock.dt = " + dt + "\n");
		sb.append("run(" + len + ")\n");

		sb.append(toPlot);

		sb.append("show()\n");

		System.out.println(sb);
		return sb.toString();
	}

	private String getBrianSIUnits(Dimension dim) {
		if (dim.getName().equals("voltage"))
			return "volt";
		if (dim.getName().equals("conductance"))
			return "siemens";
		if (dim.getName().equals("time"))
			return "second";
		if (dim.getName().equals("per_time"))
			return "1/second";
		if (dim.getName().equals("capacitance"))
			return "farad";
		if (dim.getName().equals("current"))
			return "amp";
		return "NotRecognised__"+dim.getName()+"???";
	}

	public void getCompEqns(CompInfo compInfo, Component compOrig, String popName,
			ArrayList<String> stateVars, String prefix) throws ContentError,
			ParseError {

        ComponentFlattener cf = new ComponentFlattener(lems, compOrig);

        ComponentType ctFlat;
        Component cpFlat;
		try {
			ctFlat = cf.getFlatType();
			cpFlat = cf.getFlatComponent();

			lems.addComponentType(ctFlat);
			lems.addComponent(cpFlat);
		
	        String typeOut = XMLSerializer.serialize(ctFlat);
	        String cptOut = XMLSerializer.serialize(cpFlat);
	      
	        E.info("Flat type: \n" + typeOut);
	        E.info("Flat cpt: \n" + cptOut);
	        
			lems.resolve(ctFlat);
			lems.resolve(cpFlat);

	        
		} catch (ConnectionError e) {
			throw new ParseError("Error when flattening component: "+compOrig, e);
		}
		
		LemsCollection<Parameter> ps = ctFlat.getDimParams();
		/*
		 * String localPrefix = comp.getID()+"_";
		 * 
		 * if (comp.getID()==null) localPrefix = comp.getName()+"_";
		 */

		for (Constant c : ctFlat.getConstants()) {
			String units = "";
			if (!c.getDimension().getName().equals(Dimension.NO_DIMENSION))
				units = " * " + getBrianSIUnits(c.getDimension());

			compInfo.params.append(prefix + c.getName() + " = "
					+ (float) c.getValue() + units + " \n");
		}

		for (Parameter p : ps) {
			ParamValue pv = cpFlat.getParamValue(p.getName());
			String units = " * "+getBrianSIUnits(p.getDimension());
			if (units.indexOf(Unit.NO_UNIT) >= 0)
				units = "";
			String val = pv==null ? "???" : (float)pv.getDoubleValue()+"";
			compInfo.params.append(prefix + p.getName() + " = " + val + units + " \n");
		}

		if (ps.size() > 0)
			compInfo.params.append("\n");

		Dynamics dyn = ctFlat.getDynamics();
		LemsCollection<TimeDerivative> tds = dyn.getTimeDerivatives();

		for (TimeDerivative td : tds) {
			String localName = prefix + td.getStateVariable().name;
			stateVars.add(localName);
			String units = " "+getBrianSIUnits(td.getStateVariable().getDimension());
			if (units.indexOf(Unit.NO_UNIT) >= 0)
				units = " 1";
			String expr = td.getValueExpression();
			expr = expr.replace("^", "**");
			compInfo.eqns.append("    d" + localName + "/dt = " + expr
					+ " : "+units+"\n");
		}

		for (StateVariable svar : dyn.getStateVariables()) {
			String localName = prefix + svar.getName();
			String units = " "+getBrianSIUnits(svar.getDimension());
			if (units.indexOf(Unit.NO_UNIT) >= 0)
				units = " 1";
			if (!stateVars.contains(localName)) // i.e. no TimeDerivative of
												// StateVariable
			{
				stateVars.add(localName);
				compInfo.eqns.append("    d" + localName + "/dt = 0 * 1/second : "+units+"\n");
			}
		}

		/*
		 * ArrayList<PathDerivedVariable> pathDevVar =
		 * comp.getComponentBehavior().getPathderiveds();
		 * for(PathDerivedVariable pdv: pathDevVar) { String path =
		 * pdv.getPath(); String bits[] = pdv.getBits(); StringBuilder info =
		 * new StringBuilder("# "+path +" ("); for (String bit: bits)
		 * info.append(bit+", "); info.append("), simple: "+pdv.isSimple());
		 * 
		 * String right = "";
		 * 
		 * if (pdv.isSimple()) { Component parentComp = comp; for(int
		 * i=0;i<bits.length-1;i++){ String type = bits[i]; Component child =
		 * parentComp.getChild(type); if (child.getID()!=null)
		 * right=right+prefix+child.getID()+"_"; else
		 * right=right+prefix+child.getName()+"_"; }
		 * right=right+bits[bits.length-1]; } else { Component parentComp =
		 * comp; ArrayList<String> found = new ArrayList<String>(); //String ref
		 * = ""; for(int i=0;i<bits.length-1;i++){ String type = bits[i];
		 * E.info("Getting children of "+comp+" of type "+type);
		 * ArrayList<Component> children = parentComp.getChildrenAL(type); if
		 * (children!=null && children.size()>0){
		 * 
		 * for(Component child: children){ E.info("child: "+ child); if
		 * (child.getID()!=null) found.add(child.getID()); else
		 * found.add(child.getName()); } } } for(String el: found){ if
		 * (!found.get(0).equals(el)) right=right+" + ";
		 * right=right+prefix+el+"_"+bits[bits.length-1]; } if (found.isEmpty())
		 * right = "0"; E.info("found: "+found);
		 * 
		 * } String line =
		 * prefix+pdv.getVariableName()+" = "+right+" : 1        # "+info;
		 * E.info("line: "+line);
		 * 
		 * compInfo.eqns.append(line+"\n"); }
		 */

		LemsCollection<DerivedVariable> expDevVar = dyn.getDerivedVariables();
		for (DerivedVariable edv : expDevVar) {
			// String expr = ((DVal)edv.getRateexp().getRoot()).toString(prefix,
			// stateVars);
			String units = " "+getBrianSIUnits(edv.getDimension());
			if (units.indexOf(Unit.NO_UNIT) >= 0)
				units = " 1";
			
			String expr = edv.getValueExpression();
			if (expr.startsWith("0 "))
				expr = "(0 *"+units+") "+ expr.substring(2);
			if (expr.equals("0"))
				expr = expr+" * "+units;
			expr = expr.replace("^", "**");
			compInfo.eqns.append("    " + prefix + edv.getName() + " = " + expr
					+ " : "+units+"\n");
		}

		LemsCollection<OnStart> initBlocks = dyn.getOnStarts();

		for (OnStart os : initBlocks) {
			LemsCollection<StateAssignment> assigs = os.getStateAssignments();

			for (StateAssignment va : assigs) {
				String initVal = va.getValueExpression();
				for (DerivedVariable edv : expDevVar) {
					if (edv.getName().equals(initVal)) {
						String expr = edv.getValueExpression();
						expr = expr.replace("^", "**");
						initVal = expr;

						for (StateAssignment va2 : assigs) {
							String expr2 = va2.getValueExpression();
							expr2 = expr2.replace("^", "**");
							initVal = Utils.replaceInExpression(initVal, va2.getStateVariable().getName(), expr2);
						}
					}
				}

				for (DerivedVariable edv : expDevVar) {
					initVal = Utils.replaceInExpression(initVal, edv.getName(), popName + "." + prefix + edv.getName());
				}
				compInfo.initInfo.append(popName + "." + prefix
						+ va.getStateVariable().getName() + " = " + initVal
						+ "\n");
			}

		}

		/*
		for (Component child : cp.getAllChildren()) {
			String childPre = child.getID() + "_";
			if (child.getID() == null)
				childPre = child.getName() + "_";

			getCompEqns(compInfo, child, popName, stateVars, prefix + childPre);
		}*/

		return;

	}

    public static void main(String[] args) throws Exception {

    	
        File exampleFile = new File("/home/padraig/org.neuroml.import/sbmlTestSuite/cases/semantic/00001/00001-sbml-l3v1_LEMS.xml");
        
		Lems lems = Utils.readLemsNeuroMLFile(exampleFile).getLems();
        System.out.println("Loaded: "+exampleFile.getAbsolutePath());

        BrianWriter bw = new BrianWriter(lems);

        String br = bw.getMainScript();


        File brFile = new File(exampleFile.getAbsolutePath().replaceAll(".xml", "_brian.py"));
        System.out.println("Writing to: "+brFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(br, brFile);

      
	}

}
