package org.neuroml.export.brian;

import java.io.File;
import java.util.ArrayList;
import org.lemsml.export.base.GenerationException;

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
import org.neuroml.export.ModelFeature;
import org.neuroml.export.ModelFeatureSupportException;
import org.neuroml.export.SupportLevelInfo;
import org.neuroml.export.Utils;
import org.neuroml.export.base.BaseWriter;
import org.neuroml.model.util.NeuroMLException;


@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public class BrianWriter extends BaseWriter {
	
	static String DEFAULT_POP = "OneComponentPop";
    
    boolean brian2 = false;

	public BrianWriter(Lems lems) throws ModelFeatureSupportException, LEMSException, NeuroMLException {
		super(lems, "Brian");
        sli.checkAllFeaturesSupported(FORMAT, lems);
	}
    
    
    @Override
    protected void setSupportedFeatures() {
        sli.addSupportInfo(FORMAT, ModelFeature.ABSTRACT_CELL_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(FORMAT, ModelFeature.COND_BASED_CELL_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(FORMAT, ModelFeature.SINGLE_COMP_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(FORMAT, ModelFeature.NETWORK_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(FORMAT, ModelFeature.MULTI_POPULATION_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(FORMAT, ModelFeature.NETWORK_WITH_INPUTS_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(FORMAT, ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(FORMAT, ModelFeature.MULTICOMPARTMENTAL_CELL_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(FORMAT, ModelFeature.HH_CHANNEL_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(FORMAT, ModelFeature.KS_CHANNEL_MODEL, SupportLevelInfo.Level.NONE);
    }

    public void setBrian2(boolean brian2) {
        this.brian2 = brian2;
    }
    
    

	@Override
	protected void addComment(StringBuilder sb, String comment) {

		String comm = "# ";
		String commPre = "'''";
		String commPost = "'''";
		if (!comment.contains("\n"))
			sb.append(comm).append(comment).append("\n");
		else
			sb.append(commPre).append("\n").append(comment).append("\n").append(commPost).append("\n");
	}

    @Override
	public String getMainScript() throws GenerationException {
		StringBuilder sb = new StringBuilder();
        try {
            addComment(sb, "Brian simulator compliant Python export for:\n\n"
                    + lems.textSummary(false, false));

            addComment(sb, Utils.getHeaderComment(FORMAT));

            if (!brian2)
                sb.append("from brian import *\n\n");
            else
                sb.append("from brian2 import *\n\n");
            
            sb.append("from math import *\n");
            sb.append("import sys\n\n");
            
            
            sb.append("\nif len(sys.argv) > 1 and sys.argv[1] == '-nogui':\n    show_gui = False\nelse:\n    show_gui = True\n\n");
            
            //lems.build(null, null)

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

                    sb.append("\n").append(compInfo.params.toString());

                    sb.append(prefix).append("eqs=Equations('''\n");
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

            StringBuilder preRunSave = new StringBuilder();
            StringBuilder postRunSave = new StringBuilder();
            
            for (Component outComp : simCpt.getAllChildren()) {
                if (outComp.getTypeName().equals("OutputFile")) {
                    
                    String fileName = outComp.getTextParam("fileName");
                    String info = "\n# Saving to file: "+fileName+", reference: " + outComp.id + "\n";
                    preRunSave.append(info);
                    postRunSave.append(info);
                    preRunSave.append("record_" + outComp.getID() + " = {}\n");
                    postRunSave.append("all_" + outComp.getID() + " = np.array( [ ");
                    
                    for (Component colComp : outComp.getAllChildren()) {
                        if (colComp.getTypeName().equals("OutputColumn")) {
                            
                            String monitor = "record_" + outComp.getID() + "[\"" + colComp.getID()+"\"]";
                            String ref = colComp.getStringValue("quantity");

                            String pop, num, var;
                            if (ref.indexOf("/")>0) {
                                String[] splitSlash = ref.split("/");
                                pop = splitSlash[0].split("\\[")[0];
                                num = ref.split("\\[")[1].split("\\]")[0];
                                var = "";
                                for (int i=1;i<splitSlash.length;i++) {
                                    if (var.length()>0)
                                        var += "_";
                                    var += splitSlash[i];
                                }

                            } else {
                                pop = DEFAULT_POP;
                                num = "0";
                                var = ref;
                            }
                            preRunSave.append(monitor + " = StateMonitor(" + pop + ",'" + var + "',record=[" + num + "]) # " + colComp.summary() + "\n");
                            
                            if (postRunSave.indexOf("[0]")>0)
                                postRunSave.append(", ");
                            postRunSave.append(monitor+"[0] ");
                            /*
                            postRunPlot.append("plot(" + trace + ".times/second,"
                                    + trace + "[" + num + "], color=\""
                                    + lineComp.getStringValue("color") + "\")\n");*/
                        }
                    }
                    
                    postRunSave.append(" ] )\n");
                    postRunSave.append("all_"+outComp.id+" = all_"+outComp.id+".transpose()\n");
                    postRunSave.append("file_"+outComp.id+" = open(\""+fileName+"\", 'w')\n");
                    postRunSave.append("for l in all_"+outComp.id+":\n");
                    postRunSave.append("    line = ''\n");
                    postRunSave.append("    for c in l: \n");
                    postRunSave.append("        line = line + (', %f'%c if len(line)>0 else '%f'%c)\n");
                    postRunSave.append("    file_"+outComp.id+".write(line+'\\n')\n");
                    postRunSave.append("file_"+outComp.id+".close()\n");
                }
            }
            
            
            
            
            StringBuilder preRunPlot = new StringBuilder();
            StringBuilder postRunPlot = new StringBuilder();

            for (Component dispComp : simCpt.getAllChildren()) {
                if (dispComp.getTypeName().equals("Display")) {
                    String dispId = "display_"+dispComp.getID();
                    preRunPlot.append("\n    # Display: " + dispComp + "\n");
                    postRunPlot.append("\n    # Display: " + dispComp + "\n    "+dispId+" = plt.figure(\""+ dispComp.getTextParam("title") + "\")\n");
                    for (Component lineComp : dispComp.getAllChildren()) {
                        if (lineComp.getTypeName().equals("Line")) {
                            String trace = "trace_" + dispComp.getID() + "_"
                                    + lineComp.getID();
                            String ref = lineComp.getStringValue("quantity");

                            String pop, num, var;
                            if (ref.indexOf("/")>0) {
                                String[] splitSlash = ref.split("/");
                                pop = splitSlash[0].split("\\[")[0];
                                num = ref.split("\\[")[1].split("\\]")[0];
                                var = "";
                                for (int i=1;i<splitSlash.length;i++) {
                                    if (var.length()>0)
                                        var += "_";
                                    var += splitSlash[i];
                                }

                            } else {
                                pop = DEFAULT_POP;
                                num = "0";
                                var = ref;
                            }
                            preRunPlot.append("    "+trace + " = StateMonitor(" + pop + ",'" + var + "',record=[" + num + "]) # " + lineComp.summary() + "\n");
                            String times = brian2 ? "t " : "times";
                            
                            String plotId = "plot_"+lineComp.getID();
                    
                            postRunPlot.append("    "+plotId+" = "+dispId+".add_subplot(111, autoscale_on=True)\n");
                            postRunPlot.append("    "+plotId+".plot(" + trace + "."+times+"/second,"
                                    + trace + "[" + num + "], color=\""
                                    + lineComp.getStringValue("color") + "\", label=\""+lineComp.getID()+"\")\n");
                            postRunPlot.append("    "+plotId+".legend()\n");
                        }
                    }
                }
                
            }
            
            
            sb.append("\nif show_gui:\n");
            sb.append(preRunPlot);
            
            sb.append(preRunSave);

            String len = simCpt.getStringValue("length");
            String dt = simCpt.getStringValue("step");

            len = len.replaceAll("ms", "*msecond");
            len = len.replaceAll("0s", "0*second"); // TODO: Fix!!!
            dt = dt.replaceAll("ms", "*msecond");

            if (dt.endsWith("s"))
                dt = dt.substring(0, dt.length() - 1) + "*second"; // TODO: Fix!!!

            sb.append("\ndefaultclock.dt = " + dt + "\n");
            sb.append("run(" + len + ")\n");
            
            sb.append(postRunSave);
            
            sb.append("\nif show_gui:\n\n");
            
            sb.append("    import matplotlib.pyplot as plt\n");

            sb.append(postRunPlot);

            sb.append("    plt.show()\n");

            System.out.println(sb);
            
        } catch (ContentError e) {
            throw new GenerationException("Error with LEMS content", e);
        } catch (ParseError ex) {
            throw new GenerationException("Error parsing LEMS content", ex);
        } 
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
		if (dim.getName().equals("current"))
			return "amp";
		if (dim.getName().equals("volume"))
			return "meter3";
		if (dim.getName().equals("concentration"))
			return "mmole";
        
        StringBuilder unitInfo = new StringBuilder();
        appendSIUnit(unitInfo, "kilogram", dim.getM());
        appendSIUnit(unitInfo, "meter", dim.getL());
        appendSIUnit(unitInfo, "second", dim.getT());
        appendSIUnit(unitInfo, "amp", dim.getI());
        appendSIUnit(unitInfo, "kelvin", dim.getK());
        appendSIUnit(unitInfo, "mole", dim.getN());
        appendSIUnit(unitInfo, "candle", dim.getJ());
        
		return unitInfo.toString()+" # Dimension: "+dim.summary();
	}
    
    private void appendSIUnit(StringBuilder base, String siVal, int power) {
        
        if (power!=0) {
            if (base.length()!=0)
                base.append("*");
            String pow = power!=1 ? "**"+power : "";
            base.append(siVal+pow);
        }
            
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
			if (units.contains(Unit.NO_UNIT))
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
			if (units.contains(Unit.NO_UNIT))
				units = " 1";
			String expr = td.getValueExpression();
			expr = expr.replace("^", "**");
			compInfo.eqns.append("    d" + localName + "/dt = " + expr
					+ " : "+units+"\n");
		}

		for (StateVariable svar : dyn.getStateVariables()) {
			String localName = prefix + svar.getName();
			String units = " "+getBrianSIUnits(svar.getDimension());
			if (units.contains(Unit.NO_UNIT))
				units = " 1";
			if (!stateVars.contains(localName)) // i.e. no TimeDerivative of
												// StateVariable
			{
				stateVars.add(localName);
				compInfo.eqns.append("    d" + localName + "/dt = 0 * 1/second : "+units+"\n");
			}
		}


		LemsCollection<DerivedVariable> expDevVar = dyn.getDerivedVariables();
		for (DerivedVariable edv : expDevVar) {
			// String expr = ((DVal)edv.getRateexp().getRoot()).toString(prefix,
			// stateVars);
			String units = " "+getBrianSIUnits(edv.getDimension());
			if (units.contains(Unit.NO_UNIT))
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

	}

    public static void main(String[] args) throws Exception {

    	
        File exampleFile = new File("../lemspaper/tidyExamples/test/Fig_HH.xml");
        exampleFile = new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex9_FN.xml");
        
		Lems lems = Utils.readLemsNeuroMLFile(exampleFile).getLems();
        System.out.println("Loaded: "+exampleFile.getAbsolutePath());

        BrianWriter bw = new BrianWriter(lems);

        String br = bw.getMainScript();


        File brFile = new File(exampleFile.getAbsolutePath().replaceAll(".xml", "_brian.py"));
        System.out.println("Writing to: "+brFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(br, brFile);

      }

}
