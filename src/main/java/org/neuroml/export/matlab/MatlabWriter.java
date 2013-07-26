package org.neuroml.export.matlab;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.flatten.ComponentFlattener;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.run.ConnectionError;
import org.lemsml.jlems.core.sim.*;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.ComponentType;
import org.lemsml.jlems.core.type.Constant;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.LemsCollection;
import org.lemsml.jlems.core.type.ParamValue;
import org.lemsml.jlems.core.type.Parameter;
import org.lemsml.jlems.core.type.dynamics.Dynamics;
import org.lemsml.jlems.core.type.dynamics.OnStart;
import org.lemsml.jlems.core.type.dynamics.StateAssignment;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.core.type.dynamics.DerivedVariable;
import org.lemsml.jlems.core.type.dynamics.TimeDerivative;
import org.lemsml.jlems.io.util.FileUtil;
import org.lemsml.jlems.io.xmlio.XMLSerializer;
import org.neuroml.export.Utils;
import org.neuroml.export.base.BaseWriter;


public class MatlabWriter extends BaseWriter {
	
	static String DEFAULT_POP = "OneComponentPop";

	public MatlabWriter(Lems lems) throws FileNotFoundException {
		super(lems, "MATLAB");
		System.out.println(System.getProperty("user.dir"));
	}

	String comm = "% ";
	String commPre = "%{";
	String commPost = "%}";

	String startIndex = "(";
	String endIndex = ")";
	Integer idxBase = 1;
	String assignmentOp = " = ";
		
	String parArrayName = "pars";
	String stateArrayName = "state";
	String derivsArrayName = "dstate";
	String dynFunctionName = "dXdt";
	String timeScalarName = "t";
	
	String[] dynFunctionArgs = new String[]{timeScalarName, stateArrayName, parArrayName};
	String dynFunctionHeader = "function " + derivsArrayName
			+ " = " + dynFunctionName + "(" + StringUtils.join(dynFunctionArgs, ',') + ")\n"	;
	String dynBlockInitializers = derivsArrayName + " = zeros(length("+stateArrayName+"),1)";

	String indenter = "    ";
	String functionCloser = "\nend\n";
	String lineCloser = ";\n";
		
	@Override
	protected void addComment(StringBuilder sb, String comment) {
		if (comment.indexOf("\n") < 0)
			sb.append(comm + comment + "\n");
		else
			sb.append(commPre + "\n" + comment + "\n" + commPost + "\n");
	}

	private String indexed(String s, int i){
		return s + startIndex + i + endIndex; 
	}

	private String dynBlockEncloseInBoiler(StringBuilder parsEqs){
		return dynFunctionHeader + parsEqs + functionCloser;
	}
	
	private String assembleLine(String li){
		return indenter + li + lineCloser;
	}

	private String unpackIntoNames(List<String>intoNames, String containerName){
		ListIterator<String> it = intoNames.listIterator();
		StringBuilder sb = new StringBuilder();

		while(it.hasNext()){
			Integer i = it.nextIndex();
			String into = intoNames.get(i);
			sb.append(assembleLine(into + assignmentOp + indexed(containerName, i + idxBase)));
			it.next();
		}
		return sb.toString();
	}

	private String indexedArrayFromList(List<String>fromNames, String containerName){
		ListIterator<String> it = fromNames.listIterator();
		StringBuilder sb = new StringBuilder();

		while(it.hasNext()){
			Integer i = it.nextIndex();
			String from = fromNames.get(i);
			sb.append(assembleLine(indexed(containerName, i + idxBase) + assignmentOp + from));
			it.next();
		}
		return sb.toString();
	}

	public String joinNamesExprs(List<String> names, List<String> exprs){
		ListIterator<String> it = names.listIterator();
		StringBuilder sb = new StringBuilder();

		while(it.hasNext()){
			Integer i = it.nextIndex();
			sb.append(assembleLine(names.get(i) + assignmentOp + exprs.get(i)));
			it.next();
		}
		return sb.toString();
	}

	public String getMainScript() throws ContentError, ParseError {
		StringBuilder sb = new StringBuilder();
		StringBuilder dynBlock = new StringBuilder();
		
		addComment(sb, this.format+" simulator compliant export for:\n\n"
				+ lems.textSummary(false, false));
		
		addComment(sb, Utils.getHeaderComment(format));

		Target target = lems.getTarget();

		Component simCpt = target.getComponent();
		E.info("simCpt: " + simCpt);
		String targetId = simCpt.getStringValue("target");
		Component tgtNet = lems.getComponent(targetId);
		addComment(sb, "Adding simulation " + simCpt + " of network: " + tgtNet.summary());

		ArrayList<Component> pops = tgtNet.getChildrenAL("populations");
		
		ComponentType ctFlat = null;
		Component cpFlat = null;
		if (pops.size()>0) {
			for (Component pop : pops) {
				String compRef = pop.getStringValue("component");
				Component popComp = lems.getComponent(compRef);
				addComment(sb, "   Population " + pop.getID()
						+ " contains components of: " + popComp + " ");
				ctFlat = getFlattenedCompType(popComp);
				cpFlat = getFlattenedComp(popComp);
				dynBlock.append(generateDynamicsBlock(cpFlat, ctFlat));
			}
		}else{
			throw new ParseError("can't yet handle models with no defined population");
		}
		sb.append(generateSolverBlock(simCpt, ctFlat, cpFlat));
		//sb.append(plotterBlockEncloseInBoiler(generatePlotterBlock(cpFlat, ctFlat)));
		sb.append("\n\n"+dynBlockEncloseInBoiler(dynBlock).toString());

		System.out.println(sb);
		return sb.toString();
	}


	public List<String> getConstantNameList(ComponentType compt){
		List<String> cNames = new ArrayList<String>();
		for (Constant c : compt.getConstants()) {
			cNames.add(c.getName());
		}
		return cNames;
	}

	public List<String> getConstantValueList(ComponentType compt){
		List<String> cVals = new ArrayList<String>();
		for (Constant c : compt.getConstants()) {
			cVals.add(Double.toString(c.getValue()));
		}
		return cVals;
	}

	public List<String> getStateVariableList(ComponentType compt)
		throws ContentError{
		List<String> svs = new ArrayList<String>();
		for (TimeDerivative td : compt.getDynamics().getTimeDerivatives()) {
			svs.add(td.getStateVariable().name);
		}
		return svs;
	}

	public List<String> getDynamics(ComponentType compt)
		throws ContentError{
		List<String> dyn = new ArrayList<String>();
		for (TimeDerivative td : compt.getDynamics().getTimeDerivatives()) {
			dyn.add(td.getValueExpression());
		}
		return dyn;
	}

	public List<String> getDerivedVariableNameList(ComponentType compt)
		throws ContentError{
		List<String> derV = new ArrayList<String>();
		for (DerivedVariable d : compt.getDynamics().getDerivedVariables()) {
			derV.add(d.getName());
		}
		return derV;
	}

	public List<String> getDerivedVariableExprList(ComponentType compt)
		throws ContentError{
		List<String> derV = new ArrayList<String>();
		for (DerivedVariable d : compt.getDynamics().getDerivedVariables()) {
			derV.add(d.getValueExpression());
		}
		return derV;
	}

	public List<String> getParameterNameList(ComponentType compt, Component comp){
		List<String> parNames = new ArrayList<String>();
		for (Parameter p : compt.getDimParams()) {
			parNames.add(p.getName());
		}
		return parNames;
	}

	public List<String> getParameterValueList(ComponentType compt, Component comp)
			throws ContentError{
		ArrayList<String> parVals = new ArrayList<String>();
		for (Parameter p : compt.getDimParams()) {
			ParamValue pv = comp.getParamValue(p.getName());
			parVals.add(String.valueOf(pv.getDoubleValue()));
		}
		return parVals;
	}

	public List<String> getInitialConditions(ComponentType popCompType)
			throws ContentError{

		ArrayList<String> initVals = new ArrayList<String>();

	    Dynamics dyn = popCompType.getDynamics();
		LemsCollection<OnStart> initBlocks = dyn.getOnStarts();
		
		
        for (OnStart os : initBlocks) {
                LemsCollection<StateAssignment> assigs = os.getStateAssignments();

                for (StateAssignment va : assigs) {
                	String initVal = va.getValueExpression();
                	initVals.add(initVal);
                }

        }
		return initVals;
	}

	
	public StringBuilder generateSolverBlock(Component simComp, ComponentType popCompType, Component popComp) throws ContentError, ParseError{
	    StringBuilder sol = new StringBuilder();

	    //TODO: sanitize strings with units properly 
		String len = simComp.getStringValue("length");
		String dt = simComp.getStringValue("step");
		len = len.replaceAll("ms", "*1000").replaceAll("[^\\d.]", "");;
		dt = dt.replaceAll("ms", "*1000").replaceAll("[^\\d.]", "");;

		String parVal = joinNamesExprs(getParameterNameList(popCompType, popComp), 
												getParameterValueList(popCompType, popComp)).toString();
		
		Map<String, String>valuesMap = new HashMap<String, String>();

		valuesMap.put("ID", simComp.getID());
		valuesMap.put("unpackedParValPairs", parVal);
		valuesMap.put("parameterArray", getParameterNameList(popCompType, popComp).toString());
		valuesMap.put("initCondArray", getInitialConditions(popCompType).toString());
		valuesMap.put("dynFunctionName", dynFunctionName);
		valuesMap.put("t0", "0");//TODO: Hardcoded!! is it definable in LEMS?
		valuesMap.put("dt", dt);
		valuesMap.put("tf", len);

		InputStream templFile = getClass() .getResourceAsStream("SolverFunctionTemplate.tem");
		String solverBlockTemplate = new Scanner(templFile).useDelimiter("\\A").next();
		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		String header = sub.replace(solverBlockTemplate);
		sol.append(header);

		return sol;
	}

	public StringBuilder generatePlotterBlock(Component comp) throws ContentError, ParseError{
	    StringBuilder plt = new StringBuilder();

	    
		for (Component dispComp : comp.getAllChildren()) {
			if (dispComp.getName().indexOf("Display") >= 0) {
				//Trace.append("\n"+comm+" Display: " + dispComp + "\n");
				plt.append("\n"+comm+" Display: " + dispComp + "\nfigure(\""
						+ dispComp.getTextParam("title") + "\")\n");
				for (Component lineComp : dispComp.getAllChildren()) {
					if (lineComp.getName().indexOf("Line") >= 0) {
						// trace=StateMonitor(hhpop,'v',record=[0])
						String trace = "trace_" + dispComp.getID() + "_"
								+ lineComp.getID();
						String ref = lineComp.getStringValue("quantity");
					
						String pop, num, var;
						if (ref.indexOf("/") > 0) {
							pop = ref.split("/")[0].split("\\[")[0];
							num = ref.split("\\[")[1].split("\\]")[0];
							var = ref.split("/")[1];
						} else {
							pop = DEFAULT_POP;
							num = "0";
							var = ref;
						}
					//	toTrace.append(trace + " = StateMonitor(" + pop + ",'"
					//			+ var + "',record=[" + num + "])  "
					//			+ lineComp.summary() + "\n");
						plt.append("plot(" + trace + ".times/second,"
								+ trace + "[" + num + "], color=\""
								+ lineComp.getStringValue("color") + "\")\n");
						// }
					}
				}
			}
		}

		return plt;
	}
	
	public StringBuilder generateDynamicsBlock(Component cpFlat, ComponentType ctFlat) throws ContentError, ParseError{

			    StringBuilder dyn = new StringBuilder();

				List<String> constNames = getConstantNameList(ctFlat);
				List<String> constVals = getConstantValueList(ctFlat);
				List<String> parNames =getParameterNameList(ctFlat, cpFlat);
				List<String> stateVars = getStateVariableList(ctFlat);
				List<String> dynamics = getDynamics(ctFlat);
				List<String> derVarNames = getDerivedVariableNameList(ctFlat);
				List<String> derVarExprs = getDerivedVariableExprList(ctFlat);
				
				//preallocations
				dyn.append(assembleLine(dynBlockInitializers));

				//constants
				dyn.append(joinNamesExprs(constNames, constVals));
				
				//unpack parameters
				dyn.append(unpackIntoNames(parNames, parArrayName));

				//unpack current state 
				dyn.append(unpackIntoNames(stateVars, stateArrayName));

				//unpack derived variables
				dyn.append(joinNamesExprs(derVarNames, derVarExprs));

				//unpack equations
				dyn.append(indexedArrayFromList(dynamics, derivsArrayName));

		return dyn;
	}
	

	public ComponentType getFlattenedCompType(Component compOrig) throws ContentError,
			ParseError {

		ComponentType ctFlat = new ComponentType();
        ComponentFlattener cf = new ComponentFlattener(lems, compOrig);

		try {
			ctFlat = cf.getFlatType();
			lems.addComponentType(ctFlat);
	        String typeOut = XMLSerializer.serialize(ctFlat);
	        E.info("Flat type: \n" + typeOut);
			lems.resolve(ctFlat);
		} catch (ConnectionError e) {
			throw new ParseError("Error when flattening component: "+compOrig, e);
		}
		return ctFlat;
	}	

	public Component getFlattenedComp(Component compOrig) throws ContentError,
			ParseError {

		Component comp = new Component();
        ComponentFlattener cf = new ComponentFlattener(lems, compOrig);

		try {
			comp = cf.getFlatComponent();
			lems.addComponent(comp);
	        String compOut = XMLSerializer.serialize(comp);
	        E.info("Flat component: \n" + compOut);
			lems.resolve(comp);
		} catch (ConnectionError e) {
			throw new ParseError("Error when flattening component: "+compOrig, e);
		}
		return comp;
	}	
	

	public static void main(String[] args) throws Exception {

        File exampleFile = new File("../NeuroML2/NeuroML2CoreTypes/LEMS_NML2_Ex9_FN.xml");
        
		Lems lems = Utils.readLemsNeuroMLFile(exampleFile).getLems();
        System.out.println("Loaded: "+exampleFile.getAbsolutePath());

        MatlabWriter mw = new MatlabWriter(lems);

        String br = mw.getMainScript();

        File brFile = new File(exampleFile.getAbsolutePath().replaceAll(".xml", ".m"));
        System.out.println("Writing to: " + brFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(br, brFile);
	}


    
}
