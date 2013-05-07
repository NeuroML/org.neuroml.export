package org.neuroml.export.graph;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.type.*;
import org.lemsml.jlems.core.type.dynamics.*;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.Utils;
import org.neuroml.export.base.BaseWriter;

public class GraphWriter extends BaseWriter {

    String netShape = "rectangle";
    String popShape = "diamond";
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

    private static ArrayList<String> suppressChildren = new ArrayList<String>();


    public GraphWriter(Lems lems) {
        super(lems, "GraphViz");
    }

    protected void addComment(StringBuilder sb, String comment) {

        String comm = "# ";
        sb.append(comm + comment + "\n");
    }



	public String getMainScript() throws ContentError {

        main = new StringBuilder();
        net = new StringBuilder();
        comps = new StringBuilder();
        compTypes = new StringBuilder();
        extern = new StringBuilder();


        if (!compTypesOnly) {
            Target target = lems.getTarget();

            Component simCpt = target.getComponent();
            E.info("simCpt: "+simCpt);

            
            String targetId = simCpt.getStringValue("target");

            Component tgtNet = lems.getComponent(targetId);

            addComment(main, "GraphViz compliant export for:" + tgtNet.summary()+"\n");

            main.append("digraph " + simCpt.getID() + " {\n");
            main.append("fontsize=10;\n\n");
            if (rankdirLR) main.append("rankdir=\"LR\"\n");

            net.append("node [shape=" + netShape + "]; " + tgtNet.getID() + ";\n");

            ArrayList<Component> pops = tgtNet.getChildrenAL("populations");

            for (Component pop : pops) {
                String compRef = pop.getStringValue("component");
                Component popComp = lems.getComponent(compRef);

                addComment(net, "   Population " + pop.getID() + " contains components of: " + popComp + " ");
                net.append("node [shape=" + popShape + "]; " + pop.getID() + ";\n");

                net.append(tgtNet.getID() + " -> " + pop.getID() + " [len=1.00, arrowhead=" + childLink + "]\n");

                addCompAndChildren(popComp, pop.getID(), pop.getStringValue("size"));

            }
            if (pops.isEmpty()){  // i.e. simulate 1 component
                addCompAndChildren(tgtNet, tgtNet.getID(), null);
            }

            main.append("\nsubgraph cluster_network {\n");
            main.append("    style=filled;\n");
            main.append("    color=\"#D6eeEA\";\n");
            main.append("    node [style=filled,color=white];\n");
            main.append("    label = \"Network to be simulated\";\n\n");

            main.append(net.toString());
            main.append("   }\n\n");

            main.append("subgraph cluster_comps {\n");
            main.append("    style=filled;\n");
            main.append("    color=\"#CCFFCC\";\n");
            main.append("    node [style=filled,color=white];\n");
            main.append("    label = \"Components\";\n\n");
            main.append(comps.toString());

            main.append("   }\n\n");
        }
        else
        {
            main.append("digraph " + rootCompType.getName() + " {\n");
            main.append("fontsize=10;\n");
            main.append("bgcolor=\"#D6E0EA\";\n");
            //main.append("size=\"29,5\"\n");

            addCompTypeAndChildrenAndExtends(rootCompType, null, null, 0);
        }

        if (!compTypesOnly) {
            main.append("subgraph cluster_compTypes {\n");
            main.append("   style=filled;\n");
            main.append("   color=\""+compTypeCol+"\";\n");
            main.append("   node [style="+compTypeStyle+",color=white];\n");
            main.append("   label = \"Component Types\";\n");
        }

        int maxDepth = 30;

        for (int depth: compTypeSubnets.keySet()){
            if (depth<=maxDepth){
                main.append("    subgraph cluster_"+depth+" {\n");
                main.append("        node[style=filled];\n");
                main.append("        color=\""+compTypeCol+"\";\n");
                //main.append("        color=black;\n");
                main.append("        node [style="+compTypeStyle+",color=white];\n");
                main.append("        "+compTypeSubnets.get(depth).toString().replaceAll("\n", "\n        "));
                main.append("\n    }\n\n");
            }
            else
            {
                compTypes.append("    "+compTypeSubnets.get(depth).toString().replaceAll("\n", "\n    "));

            }
        }

        main.append("   node [style="+compTypeStyle+",color=white];\n");
        main.append(compTypes.toString());
        
        if (!compTypesOnly) {
            main.append("   }\n\n");
        }

        main.append(extern.toString());

        main.append("}\n");


        //System.out.println(main);
        return main.toString();
    }

    HashMap<String, Integer> noIdComps = new HashMap<String, Integer>();

    protected String getCompTypeInfo(ComponentType compType) throws ContentError{

        String label= " label=<<table border=\"0\" cellborder=\"0\"><tr><td>" + compType.getName() + "" + "</td></tr>";
            ArrayList<String> expAdded = new ArrayList<String>();
            for(Exposure e: compType.getExposures()){
                    label= label+"<tr><td><font color=\""+paramsColour+"\">" + e.getName() + " (" + e.getDimension().getName() + ")</font></td></tr>";
                    expAdded.add(e.getName());
            }
            int count = 0;
            int maxLine = 3;

            if (compType.getParameters().size()>0) label= label+"<tr><td><font color=\"#669999\">";

            for (Parameter p: compType.getParameters()){
                if (count >0) label= label+", ";
                if (!expAdded.contains(p.getName())){
                    if (count == maxLine){
                        label= label+"<br/>";
                        count = 0;
                    }
                    label= label+"" + p.getName() + " (" + p.getDimension().getName() + ")";

                    count++;
                }
            }
            if (compType.getParameters().size()>0) label= label+" </font></td></tr>";

            for (Constant c: compType.getConstants()){
                    label= label+"<tr><td><font color=\"#662211\">" + c.getName() + " (" + c.getDimension().getName() + ") == "+c.getValue()+"</font></td></tr>";
            }

            for (Requirement r: compType.getRequirements()){
                    label= label+"<tr><td><font color=\"#666699\">REQUIRES: " + r.getName() + " (" + r.getDimension().getName() + ")</font></td></tr>";
            }
            for (Text t: compType.getTexts()){
                    label= label+"<tr><td><font color=\"#B2C0D9\">" + t.getName() + "</font></td></tr>";
            }
            if (compType.getDynamics()!=null){

                for (StateVariable sv: compType.getDynamics().getStateVariables()){
                        if (!expAdded.contains(sv.getName()))
                            label= label+"<tr><td><font color=\"#FF9966\">" + sv.getName() + " ("+sv.getDimension().getName()+")</font></td></tr>";
                }
                for (DerivedVariable dv: compType.getDynamics().getDerivedVariables()){
                        label= label+"<tr><td><font color=\"#99CC00\">" + dv.getName() + " = "+dv.getValueExpression()+"</font></td></tr>";
                }
                for (TimeDerivative td: compType.getDynamics().getTimeDerivatives()){
                        label= label+"<tr><td><font color=\"#666633\">" + td.getStateVariable().getName() + "' = "+td.value+"</font></td></tr>";
                }
                for (OnCondition oc: compType.getDynamics().getOnConditions()){
                        label= label+"<tr><td><font color=\"#996633\">IF " 
                                + oc.test.replaceAll("<=", "lte").replaceAll(">=", "gte").replaceAll("less_than", "&lt;").replaceAll("greater_than", "&gt;").replaceAll(".and.", "AND")
                                + " THEN </font></td></tr><tr><td><font color=\"#996633\">";
                        int c = 0;
                        for(StateAssignment sa: oc.getStateAssignments()){
                            if(c>=1) label= label+"  AND  ";
                            label= label+"(" + sa.getStateVariable().getName() + " = "
                                    +sa.value
                                    +")";
                            c++;
                        }
                        label= label+"</font></td></tr>";
                }

                for (Regime r: compType.getDynamics().getRegimes()){
                    label= label+"<tr><td><font color=\"#555555\">-------- REGIME: " + r.getName() + ""+(r.isInitial()?" (Initial)":"")+" --------</font></td></tr>";
                    for (OnEntry oe: r.getOnEntrys()){
                            label= label+"<tr><td><font color=\"#229933\">ON ENTRY: ";
                        int c = 0;
                        for(StateAssignment sa: oe.getStateAssignments()){
                            if(c>=1) label= label+"  AND  ";
                            label= label+"(" + sa.getStateVariable().getName() + " = "
                                    +sa.value
                                    +")";
                            c++;
                        }
                        label= label+"</font></td></tr>";
                    }

                    for (TimeDerivative td: r.getTimeDerivatives()){
                            label= label+"<tr><td><font color=\"#666633\">" + td.getStateVariable().getName() + "' = "+td.value+"</font></td></tr>";
                    }

                    for (OnCondition oc: r.getOnConditions()){
                        label= label+"<tr><td><font color=\"#996633\">IF "
                                + oc.test.toString().replaceAll("<=", "lte").replaceAll(">=", "gte").replaceAll("less_than", "&lt;").replaceAll("greater_than", "&gt;").replaceAll(".and.", "AND")
                                + " THEN </font></td></tr><tr><td><font color=\"#996633\">";
                        int c = 0;
                        for(StateAssignment sa: oc.getStateAssignments()){
                            if(c>=1) label= label+"  AND  ";
                            label= label+"(" + sa.getStateVariable().getName() + " = "
                                    +sa.value
                                    +")";
                            c++;
                        }
                        for(Transition t: oc.getTransitions()){
                            if(c>=1) label= label+"  AND  ";
                            label= label+" GOTO " + t.getRegime()+"";
                            c++;
                        }
                        label= label+"</font></td></tr>";
                    }


                    label= label+"<tr><td><font color=\"#555555\">---------------------------------------</font></td></tr>";

                }


            }
            label= label+"</table>>";
            return label;
    }

    protected void addCompTypeAndChildrenAndExtends(ComponentType compType, String parent, String extendedType, int depth) throws ContentError{

        if (compTypeSubnets.get(depth)==null) compTypeSubnets.put(depth, new StringBuilder());

        StringBuilder compTypeSub = compTypeSubnets.get(depth);

        String label  =", label=\""+compType.getName()+"\"";

        if (paramInfo) label= getCompTypeInfo(compType);

        compTypeSub.append("node [shape=" + compTypeShape + label + "]; " + compType.getName() + ";\n");

        if (suppressChildren.contains(compType.getName())){
            String dummy = "ChildrenRemoved_"+compType.getName();
            String nodeInfo = "node [shape=" + compTypeShape + ", label=\"...\"]; "+dummy+";\n";


            StringBuilder compTypeSubChild = compTypeSubnets.get(depth+1);
            compTypeSubChild.append(nodeInfo);

            compTypes.append(compType.getName() + " -> " + dummy + " [len=1.00, arrowhead=" + childLink + "]\n");

        }
        else {

            for (Child c: compType.childs){
                E.info("+++ ComponentType "+compType.getName() +" has child "+ c+" +++");
                addCompTypeAndChildrenAndExtends(c.getComponentType(), compType.getName(), null, depth+1);

                String edge = compType.getName() + " -> " + c.getComponentType().getName() + " [len=1.00, arrowhead=" + childLink + "]\n";

                if (!edgesMade.contains(edge)) {
                    compTypes.append(edge);
                    edgesMade.add(edge);
                }
            }
            for (Children c: compType.childrens){
                addCompTypeAndChildrenAndExtends(c.getComponentType(), compType.getName(), null, depth+1);
                String edge = compType.getName() + " -> " + c.getComponentType().getName() + " [len=1.00, arrowhead=" + childLink + "]\n";

                if (!edgesMade.contains(edge)) {
                    compTypes.append(edge);
                    edgesMade.add(edge);
                }
            }

            LemsCollection<ComponentType> extendingTypes = lems.getComponentTypesExtending(compType.getName());

            String extended = compType.getName();

            for (ComponentType extType: extendingTypes){
                    E.info("... ComponentType "+extType.getName() +" extends "+ extended+"...");

                //while (extType != null) {
                    addCompTypeAndChildrenAndExtends(extType, null, extended, depth+1);
                    String edge = extType.getName() + " -> " + extended + " [len=1.00, arrowhead=" + extendsLink + "]\n";

                    if (!edgesMade.contains(edge)) {
                        compTypes.append(edge);
                        edgesMade.add(edge);
                    }
               // }
            }
        }

    }

    protected void addCompAndChildren(Component comp, String parent, String arrowLabel) throws ContentError {

        String ref = "" + comp.getName() + " (id = " + comp.getID() + ")" + "";
        

        if (comp.getID() == null) {

            if (!noIdComps.containsKey(comp.getName())) {
                noIdComps.put(comp.getName(), 0);
            }
            int numSoFar = noIdComps.get(comp.getName());


            ref = "" + comp.getName() + " (" + numSoFar + ")" + "";
            numSoFar++;
            noIdComps.put(comp.getName(), numSoFar);

        }
        String label  ="";

        if (paramInfo){
            label= " label=<<table border=\"0\" cellborder=\"0\"><tr><td>" + ref + "</td></tr><tr><td><font color=\""+paramsColour+"\">";
            int count = 0;
            int maxLine = 3;

            for(ParamValue pv: comp.getParamValues()){
                if (!comp.getComponentType().constants.hasName(pv.getName())){
                    if (count>0) label= label+", ";
                    if (count == maxLine){
                        label= label+"<br/>";
                        count = 0;
                    }
                    String unit = "";
                    Dimension d = lems.getDimensions().getByName(pv.getDimensionName());
                    if (d!=null && Dimension.getSIUnit(d).length()>0)
                        unit = " "+Dimension.getSIUnit(d);

                    String val = (float)pv.getDoubleValue()+"";

                    if (val.endsWith(".0")) val = val.substring(0,val.length()-2);

                    label= label+"" + pv.getName() + " = " + val + unit;
                    count++;
                }
            }

            label= label+"</font></td></tr></table>>";
        }


        comps.append("node [shape=" + compShape + label+"]; \"" + ref + "\";\n\n");
        String al = "";
        if (arrowLabel!=null)
            al = "label=\"" + arrowLabel + "\",";
        
        String edge = "\""+parent+"\"" + " -> \"" + ref + "\" ["+al+"len=1.00, arrowhead=" + childLink + "]\n";
        if (!edgesMade.contains(edge)) {
            comps.append(edge);
            edgesMade.add(edge);
        }
        
        ComponentType compType = comp.getComponentType();

        label  ="";

        if (paramInfo) label= getCompTypeInfo(compType);

        compTypes.append("node [shape=" + compTypeShape + label + "]; " + compType.getName() + ";\n");

        if (!compTypesOnly) extern.append("\""+ref + "\" -> " + compType.getName() + " [len=1.00]\n");


        ComponentType extType = compType.getExtends();
        String par = compType.getName();

        while (extType != null) {

            label  ="";

            if (paramInfo) label= getCompTypeInfo(extType);

            compTypes.append("node [shape=" + compTypeShape + label+"]; " + extType.getName() + ";\n");
            edge = par + " -> " + extType.getName() + " [len=1.00, arrowhead=" + extendsLink + "]\n";

            if (!edgesMade.contains(edge)) {
                compTypes.append(edge);
                edgesMade.add(edge);
            }
            par = extType.getName();
            extType = extType.getExtends();
        }

        for (Component c : comp.getAllChildren()) {
            addCompAndChildren(c, ref, null);
        }
    }


    public static void main(String[] args) throws Exception
    {
        compTypesOnly = true;
        File xml = new File("../NeuroML2/NeuroML2CoreTypes/LEMS_NML2_Ex0_IaF.xml");
        File targetDir = new File(".");


        rootCompType = new ComponentType("neuroml");

		Lems lems = Utils.readLemsNeuroMLFile(xml).getLems();

        lems.addComponentType(rootCompType);

        paramInfo = false;
        //paramInfo = true;

        rootCompType.childs.add(new Child("network", lems.getComponentTypeByName("network")));
        rootCompType.childs.add(new Child("basePointCurrent", lems.getComponentTypeByName("basePointCurrent")));
        rootCompType.childs.add(new Child("baseCell", lems.getComponentTypeByName("baseCell")));
        rootCompType.childs.add(new Child("baseIonChannel", lems.getComponentTypeByName("baseIonChannel")));
        rootCompType.childs.add(new Child("extracellularProperties", lems.getComponentTypeByName("extracellularProperties")));
        rootCompType.childs.add(new Child("intracellularProperties", lems.getComponentTypeByName("intracellularProperties")));
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

        //rootCompType.childs.add(new Child("network", lems.getComponentTypeByName("network")));
        rootCompType.childs.add(new Child("basePointCurrent", lems.getComponentTypeByName("basePointCurrent")));
        //rootCompType.childs.add(new Child("baseCell", lems.getComponentTypeByName("baseCell")));
//        rootCompType.childs.add(new Child("baseIonChannel", lems.getComponentTypeByName("baseIonChannel")));
//        rootCompType.childs.add(new Child("extracellularProperties", lems.getComponentTypeByName("extracellularProperties")));
//        rootCompType.childs.add(new Child("intracellularProperties", lems.getComponentTypeByName("intracellularProperties")));
//        rootCompType.childs.add(new Child("morphology", lems.getComponentTypeByName("morphology")));

        String synapses = "NML2_Synapses";
        generatePng(lems, targetDir, synapses);


        rootCompType = new ComponentType("neuroml");
        lems.addComponentType(rootCompType);

        paramInfo = true;

        //rootCompType.childs.add(new Child("network", lems.getComponentTypeByName("network")));
        //rootCompType.childs.add(new Child("pointCurrent", lems.getComponentTypeByName("pointCurrent")));
        //rootCompType.childs.add(new Child("baseCell", lems.getComponentTypeByName("baseCell")));
        rootCompType.childs.add(new Child("baseIonChannel", lems.getComponentTypeByName("baseIonChannel")));
//        rootCompType.childs.add(new Child("extracellularProperties", lems.getComponentTypeByName("extracellularProperties")));
//        rootCompType.childs.add(new Child("intracellularProperties", lems.getComponentTypeByName("intracellularProperties")));
//        rootCompType.childs.add(new Child("morphology", lems.getComponentTypeByName("morphology")));

        String channels = "NML2_Channels";
        generatePng(lems, targetDir, channels);


        rootCompType = new ComponentType("neuroml");
        lems.addComponentType(rootCompType);

        paramInfo = true;

        //rootCompType.childs.add(new Child("network", lems.getComponentTypeByName("network")));
        //rootCompType.childs.add(new Child("pointCurrent", lems.getComponentTypeByName("pointCurrent")));
        rootCompType.childs.add(new Child("baseCell", lems.getComponentTypeByName("baseCell")));
        //rootCompType.childs.add(new Child("baseIonChannel", lems.getComponentTypeByName("baseIonChannel")));
//        rootCompType.childs.add(new Child("extracellularProperties", lems.getComponentTypeByName("extracellularProperties")));
//        rootCompType.childs.add(new Child("intracellularProperties", lems.getComponentTypeByName("intracellularProperties")));
//        rootCompType.childs.add(new Child("morphology", lems.getComponentTypeByName("morphology")));

        String cells = "NML2_Cells";
        generatePng(lems, targetDir, cells);


    }

    private static void generatePng(Lems lems, File targetDir, String name) throws ContentError, IOException, InterruptedException {

        GraphWriter gw = new GraphWriter(lems);

        File imgFile = new File(targetDir, name+".png");
        File gv = new File(targetDir, name+".gv");

        FileUtil.writeStringToFile(gw.getMainScript(), gv);

        E.info("Graph details written to file: "+gv);


        String cmd = "dot -Tpng  " + gv.getAbsolutePath() + " -o " + imgFile.getAbsolutePath();
        String[] env = new String[]{};
        Runtime run = Runtime.getRuntime();
        Process pr = run.exec(cmd, env, gv.getParentFile());

        int ret = pr.waitFor();

        E.info("Written out to image file: "+imgFile+"\nUsed: "+cmd+"\nReturn value: "+ret);
    }
}
