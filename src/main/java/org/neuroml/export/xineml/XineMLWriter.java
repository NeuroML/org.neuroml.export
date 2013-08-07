/**
 * A writer for the various variants of NineML/SpineML
 */
package org.neuroml.export.xineml;


import java.util.ArrayList;

import org.lemsml.jlems.core.expression.Parser;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.ComponentType;
import org.lemsml.jlems.core.type.Constant;
import org.lemsml.jlems.core.type.FinalParam;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.core.type.dynamics.Dynamics;
import org.lemsml.jlems.core.type.dynamics.StateVariable;
import org.neuroml.export.Utils;
import org.neuroml.export.base.XMLWriter;

public class XineMLWriter extends XMLWriter {
	
	enum Variant {NineML, SpineML};
	
	Variant variant = null;

    public static final String SCHEMA_9ML = "https://raw.github.com/apdavison/nineml/master/catalog/sample_xml_files/NineML_v0.2.xsd";
    public static final String NAMESPACE_9ML = "http://nineml.org/9ML/0.2";

    public static final String SCHEMA_SPINEML = "http://bimpa.group.shef.ac.uk/SpineML/schemas/SpineMLComponentLayer.xsd";
    public static final String NAMESPACE_SPINEML = "http://www.shef.ac.uk/SpineMLComponentLayer";
   
    
    //public static final String LOCAL_9ML_SCHEMA = "src/test/resources/Schemas/sbml-l2v2-schema/sbml.xsd";

    public XineMLWriter(Lems l, Variant v) {
        super(l, v.toString());
        this.variant = v;
    }


    public String getMainScript() throws ContentError {

        Parser p = new Parser();

        StringBuilder mainFile = new StringBuilder();

        StringBuilder abstLayer = new StringBuilder();
        
        StringBuilder userLayer = new StringBuilder();

        mainFile.append("<?xml version='1.0' encoding='UTF-8'?>\n");
        
        String namespace = null;
        String schema = null;
        String extraAttr = "";
        
        String defaultDimension = null;
        
        switch (variant) {
	        case NineML:
	        	namespace = NAMESPACE_9ML;
	        	schema = SCHEMA_9ML;
	        	extraAttr = "xmlns:comodl=CoMoDL";
	        	defaultDimension="none";
	        	break;
	        case SpineML:
        		namespace = NAMESPACE_SPINEML;
        		schema = SCHEMA_SPINEML;
	        	defaultDimension="?";
	        	break;
        }

        String[] attrs = new String[]{"xmlns="+namespace,
            extraAttr,
            "xmlns:xsi=http://www.w3.org/2001/XMLSchema-instance",
            "xsi:schemaLocation="+namespace+" "+schema};
        
        String root = variant.toString();
        
        startElement(mainFile, root, attrs);

        String info = "\n"+Utils.getHeaderComment(format) + "\n" +
        			"\nExport of model:\n" + lems.textSummary(false, false) + "\n";
        
        addComment(mainFile, info);

        
        Target target = lems.getTarget();
        
        Component simCpt = target.getComponent();
        
        String targetId = simCpt.getStringValue("target");

        Component tgtNet = lems.getComponent(targetId);
        ////addComment(main, "Adding simulation " + simCpt + " of network: " + tgtNet.summary() + "", true);

        String netId = tgtNet.getID();
        startElement(userLayer, "network", "id=" + netId, "name=" + netId);
        userLayer.append("\n");

        //indent="";

        ArrayList<Component> pops = tgtNet.getChildrenAL("populations");

        int initAssNum = 0;
        int onCondNum = 0;

        startElement(userLayer, "groups?");

        for (Component pop : pops) {
            String compRef = pop.getStringValue("component");
            Component popComp = lems.getComponent(compRef);
            ComponentType type = popComp.getComponentType();
            
            initAssNum = initAssNum + type.getDynamics().getOnStarts().size();
            onCondNum = onCondNum + type.getDynamics().getOnConditions().size();
            
            int num = Integer.parseInt(pop.getStringValue("size"));
            addComment(userLayer, "Population " + pop.getID() + " contains " + num + " instances of components of: " + popComp, true);

            for (int i = 0; i < num; i++) {
                startEndElement(userLayer, "population", "id=" + pop.getID() + "_" + i, "size=1");
            }

        }

        endElement(userLayer, "groups?");

        userLayer.append("\n");


        for (Component pop : pops) {
            String compRef = pop.getStringValue("component");
            Component popComp = lems.getComponent(compRef);
            
            ComponentType ct = popComp.getComponentType();

            String ccAttr = "";
            String dynRedInfo = "";
            
        	String defaultRegime = "defaultRegime";
        	
            switch (variant) {
	        case NineML:
	        	ccAttr = "xmlns=CoMoDL";
	        	break;
	        case SpineML:
	        	dynRedInfo = "initial_regime="+defaultRegime;
	        	break;
            }

            StringBuilder params = new StringBuilder();
            StringBuilder dynamics = new StringBuilder();
            StringBuilder stateVars = new StringBuilder();
            StringBuilder regimes = new StringBuilder();
            
            startElement(abstLayer, "ComponentClass", "name="+ct.getName(), ccAttr);


            for (FinalParam param : ct.getFinalParams()) {
                startEndElement(params,
                        "Parameter",
                        "name=" + param.getName(),
                        "dimension="+defaultDimension);
            }
            for(Constant c: ct.getConstants())
            {
	            startEndElement(params,
		            "Parameter",
		            "name=" + c.getName(),
		            "dimension="+defaultDimension);

            }

        	
            Dynamics dyn = ct.getDynamics();
            
            if (dyn.getRegimes().size()==0) {
                startElement(dynamics, "Dynamics", dynRedInfo);

                startElement(regimes, "Regime", "name="+defaultRegime);
                endElement(regimes, "Regime");
                
            } else {
                startElement(dynamics, "Dynamics-MultiRegimesNotYetImpl", dynRedInfo);
            }
         
            
            for (StateVariable sv : dyn.getStateVariables()) {
                startEndElement(stateVars,
                        "StateVariable",
                        "name=" + sv.getName(),
                        "dimension="+defaultDimension);

            }
            
            

            switch (variant) {
		        case NineML:
		        	abstLayer.append(params);
		        	dynamics.append(stateVars);
		        	dynamics.append(regimes);
		            endElement(dynamics, "Dynamics");
		        	abstLayer.append(dynamics);

		        	break;
		        case SpineML:
		        	dynamics.append(regimes);
		        	dynamics.append(stateVars);
		            endElement(dynamics, "Dynamics");
		        	abstLayer.append(dynamics);
		            
		        	abstLayer.append(params);
		        	break;
            }
            
            

            endElement(abstLayer, "ComponentClass");
        }

        
        /*
        if (initAssNum>0)
        {
            startElement(main, "listOfInitialAssignments");

            for (Component pop : pops) {
                String compRef = pop.getStringValue("component");
                Component popComp = lems.getComponent(compRef);
                ComponentType type = popComp.getComponentType();

                for (OnStart os : type.getDynamics().getOnStarts()) {
                    for (StateAssignment sa : os.getStateAssignments()) {


                        startElement(main, "initialAssignment", "symbol=" + sa.getStateVariable().getName());
                        processMathML(main, sa.getParseTree());
                        endElement(main, "initialAssignment");



                    }
                }
            }

            endElement(main, "listOfInitialAssignments");
        }
        
        main.append("\n");

        startElement(main, "listOfRules");

        for (Component pop : pops) {
            String compRef = pop.getStringValue("component");
            Component popComp = lems.getComponent(compRef);
            ComponentType type = popComp.getComponentType();

            for (TimeDerivative td : type.getDynamics().getTimeDerivatives()) {
                startElement(main, "rateRule", "variable=" + td.getStateVariable().getName());
                //MathMLWriter mmlw = new MathMLWriter();
                //E.info("TD: "+mmlw.serialize(td.getParseTree()));
                processMathML(main, td.getParseTree());
                endElement(main, "rateRule");

            }
            
            for(OnStart os: type.getDynamics().getOnStarts()){
	            for(StateAssignment sa: os.getStateAssignments()){
	            startElement(main,"assignmentRule", "variable="+sa.getStateVariable().getName());
	            processMathML(main, sa.getParseTree());
	            endElement(main,"assignmentRule");
	
	            }
            }
            
        }


        endElement(main, "listOfRules");
        main.append("\n");


        if (onCondNum>0)
        {
            startElement(main, "listOfEvents");

            for (Component pop : pops) {
                String compRef = pop.getStringValue("component");
                Component popComp = lems.getComponent(compRef);
                ComponentType type = popComp.getComponentType();

                for (OnCondition oc : type.getDynamics().getOnConditions()) {

                    String id = "check__" + getSuitableId(oc.test);
                    startElement(main, "event", "id=" + id);
                    startElement(main, "trigger");
                    //String tempTestString = oc.test.replace(".gt.", ">").replace(".lt.", "<");

                    try {
                    	ParseTree testEval = p.parseCondition(oc.test);
                        processMathML(main, testEval);
                    } catch (ParseError ex) {
                        throw new ContentError("Problem parsing string for triggering event: " + oc.test, ex);
                    }

                    endElement(main, "trigger");
                    startElement(main, "listOfEventAssignments");

                    for (StateAssignment sa : oc.getStateAssignments()) {

                        startElement(main, "eventAssignment", "variable=" + sa.getStateVariable().getName());
                        processMathML(main, sa.getParseTree());
                        endElement(main, "eventAssignment");

                    }
                    endElement(main, "listOfEventAssignments");
                    endElement(main, "event");

                }
            }
            endElement(main, "listOfEvents");
        }

		*/
        mainFile.append(abstLayer);
        mainFile.append("\n");


        endElement(mainFile, root);
        //System.out.println(main);
        return mainFile.toString();
    }

    private String getSuitableId(String str) {
        return str.replace(" ", "_").replace(".", "").replace("(", "_").replace(")", "_").replace("*", "mult").replace("+", "plus").replace("/", "div");
    }
    

}