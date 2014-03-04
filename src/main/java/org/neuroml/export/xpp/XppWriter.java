package org.neuroml.export.xpp;

import java.util.ArrayList;
import org.lemsml.export.base.GenerationException;

import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Constant;
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
import org.neuroml.export.base.BaseWriter;

@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public class XppWriter extends BaseWriter {


	public XppWriter(Lems lems)
	{
		super(lems, "XPP");
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

        try {
            Target target = lems.getTarget();

            Component simCpt = target.getComponent();
            E.info("simCpt: "+simCpt);


            String targetId = simCpt.getStringValue("target");

            Component tgtNet = lems.getComponent(targetId);
            addComment(sb,"Adding simulation "+simCpt+" of network: "+tgtNet.summary());

            ArrayList<Component> pops = tgtNet.getChildrenAL("populations");
            E.info("pops: "+pops);

            if (pops==null || pops.isEmpty()) {
                E.info("Adding component: "+tgtNet);
                addComment(sb,"   Adding: "+tgtNet+"\n" );

                CompInfo compInfo = new CompInfo();
                ArrayList<String> stateVars = new ArrayList<String>();

                getCompEqns(compInfo, tgtNet, null, stateVars, "");

                sb.append("# Initial values\n"+compInfo.initInfo.toString()+"\n");

                sb.append("# Main parameters\n"+compInfo.params.toString());

                sb.append("# Main equations\n"+compInfo.eqns.toString()+"\n");

            } else {

                for(Component pop: pops) {
                    String compRef = pop.getStringValue("component");
                    Component popComp = lems.getComponent(compRef);
                    addComment(sb,"   Population "+pop.getID()+" contains components of: "+popComp+" ");
                    sb.append("\n\n");

                    //String prefix = popComp.getID()+"_";

                    CompInfo compInfo = new CompInfo();
                    ArrayList<String> stateVars = new ArrayList<String>();

                    getCompEqns(compInfo, popComp, popComp.getID(), stateVars, "");

                    sb.append("# Initial values\n"+compInfo.initInfo.toString()+"\n");

                    sb.append("# Main parameters\n"+compInfo.params.toString());

                    sb.append("# Main equations\n"+compInfo.eqns.toString()+"\n");


                }

                StringBuilder toTrace = new StringBuilder();
                //StringBuilder toPlot = new StringBuilder();

                for(Component dispComp: simCpt.getAllChildren()){
                    if(dispComp.getName().indexOf("Display")>=0){
                        toTrace.append("# Display: "+dispComp+"\n");
                        for(Component lineComp: dispComp.getAllChildren()){
                            if(lineComp.getName().indexOf("Line")>=0){
                                /*
                                //trace=StateMonitor(hhpop,'v',record=[0])
                                String trace = "trace_"+lineComp.getID();
                                String ref = lineComp.getStringValue("quantity");
                                String pop = ref.split("/")[0].split("\\[")[0];
                                String num = ref.split("\\[")[1].split("\\]")[0];
                                String var = ref.split("/")[1];

                                //if (var.equals("v")){

                                toTrace.append(trace+" = StateMonitor("+pop+",'"+var+"',record=["+num+"]) # "+lineComp.summary()+"\n");
                                toPlot.append("plot("+trace+".times/second,"+trace+"["+num+"])\n");
                                //}
                                */
                            }
                        }
                    }
                }
                //////sb.append(toTrace);
            }

            float len = (float)simCpt.getParamValue("length").value;
            float dt = (float)simCpt.getParamValue("step").value;


            //if (dt.endsWith("s")) dt=dt.substring(0, dt.length()-1)+"*second";  //TODO: Fix!!!

            sb.append("@ total="+len+",dt="+dt+",maxstor=10000\n");
            sb.append("@ xhi="+len+",yhi=1.5,ylo=-1.5\n");

        } catch (ContentError e) {
            throw new GenerationException("Error with LEMS content", e);
        } catch (ParseError e) {
            throw new GenerationException("Error parsing LEMS content", e);
        } 

		return sb.toString();
	}


	/*
        private String getBrianUnits(String siDim)
        {
                if(siDim.equals("voltage")) return "V";
                if(siDim.equals("conductance")) return "S";
                return null;
        }*/


	public void getCompEqns(CompInfo compInfo, Component comp, String popName, ArrayList<String> stateVars, String prefix) throws ContentError, ParseError
	{
		LemsCollection<Parameter> ps = comp.getComponentType().getDimParams();
		/*
		String localPrefix = comp.getID()+"_";

		if (comp.getID()==null)
			localPrefix = comp.getName()+"_";
			*/

		for(Parameter p: ps)
		{
			ParamValue pv = comp.getParamValue(p.getName());
			//////////////String units = "*"+getBrianUnits(pv.getDimensionName());
			String units = "";
			if (units.indexOf(Unit.NO_UNIT)>=0)
				units = "";
			compInfo.params.append("par "+prefix+p.getName()+"="+(float)pv.getDoubleValue()+units+" \n");
		}
		
		for(Constant c: comp.getComponentType().getConstants())
		{

			String units = "";
			if (units.indexOf(Unit.NO_UNIT)>=0)
				units = "";
			compInfo.params.append("par "+prefix+c.getName()+"="+(float)c.getValue()+units+" \n");
		}

		if (ps.size()>0)
			compInfo.params.append("\n");

		Dynamics dyn = comp.getComponentType().getDynamics();

		LemsCollection<TimeDerivative> tds = dyn.getTimeDerivatives();

		for(TimeDerivative td: tds)
		{
			String localName = prefix+td.getVariable();
			stateVars.add(localName);
			//String expr = ((DVal)td.getRateexp().getRoot()).toString(prefix, stateVars);
			String expr = td.getValueExpression();
			expr = expr.replace("^", "**");
			compInfo.eqns.append(""+localName+"' = "+expr+" \n");
		}

		for(StateVariable svar: dyn.getStateVariables())
		{
			String localName = prefix+svar.getName();
			if (!stateVars.contains(localName)) // i.e. no TimeDerivative of StateVariable
			{
				stateVars.add(localName);
				//////compInfo.eqns.append(""+localName+"' = 0\n");
			}
		}

		/*
                ArrayList<PathDerivedVariable> pathDevVar = dyn.getPathderiveds();
                for(PathDerivedVariable pdv: pathDevVar)
                {
                        String path = pdv.getPath();
                        String bits[] = pdv.getBits();
                        StringBuilder info = new StringBuilder("# "+path +" (");
                        for (String bit: bits)
                                info.append(bit+", ");
                        info.append("), simple: "+pdv.isSimple());

                        String right = "";

                        if (pdv.isSimple())
                        {
                                Component parentComp = comp;
                                for(int i=0;i<bits.length-1;i++){
                                        String type = bits[i];
                                        Component child = parentComp.getChild(type);
                                        if (child.getID()!=null)
                                                right=right+prefix+child.getID()+"_";
                                        else
                                                right=right+prefix+child.getName()+"_";
                                }
                                right=right+bits[bits.length-1];
                        } else {
                                Component parentComp = comp;
                                ArrayList<String> found = new ArrayList<String>();
                                //String ref = "";
                                for(int i=0;i<bits.length-1;i++){
                                        String type = bits[i];
                                        E.info("Getting children of "+comp+" of type "+type);
                                        ArrayList<Component>  children = parentComp.getChildrenAL(type);
                                        if (children!=null && children.size()>0){

                                                for(Component child: children){
                                                        E.info("child: "+ child);
                                                        if (child.getID()!=null)
                                                                found.add(child.getID());
                                                        else
                                                                found.add(child.getName());
                                                }
                                        }
                                }
                                for(String el: found){
                                        if (!found.get(0).equals(el))
                                                right=right+" + ";
                                        right=right+prefix+el+"_"+bits[bits.length-1];
                                }
                                if (found.isEmpty())
                                        right = "0";
                                E.info("found: "+found);

                        }
                        String line = prefix+pdv.getVariableName()+" = "+right+" : 1        # "+info;
                        E.info("line: "+line);

                        compInfo.eqns.append(line+"\n");
                }*/

		LemsCollection<DerivedVariable> expDevVar = dyn.getDerivedVariables();
		for(DerivedVariable edv: expDevVar)
		{
			//String expr = ((DVal)edv.getRateexp().getRoot()).toString(prefix, stateVars);
			String expr = edv.getValueExpression();
			expr = expr.replace("^", "**");
			compInfo.eqns.append(prefix+edv.getName()+" = "+expr+" \n");
		}

		LemsCollection<OnStart> initBlocks = dyn.getOnStarts();
		
		String popPrefix = popName+".";
		if (popName==null || popName.length()==0)
			popPrefix = "";

		for(OnStart os: initBlocks)
		{
			LemsCollection<StateAssignment> assigs = os.getStateAssignments();

			for(StateAssignment va: assigs)
			{
				compInfo.initInfo.append("init "+popPrefix+prefix+va.getStateVariable().getName()+"="+va.getValueExpression()+" \n");
			}
		}

		for(Component child: comp.getAllChildren()) {
			String childPre = child.getID()+"_";
			if (child.getID()==null)
				childPre = child.getName()+"_";

			getCompEqns(compInfo, child, popName, stateVars, prefix+childPre);
		}

		return;

	}



}



