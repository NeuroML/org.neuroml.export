/**
 * A writer for the various variants of NineML/SpineML
 */
package org.neuroml.export.xineml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.lemsml.export.base.GenerationException;

import org.lemsml.jlems.core.expression.Parser;
import org.lemsml.jlems.core.flatten.ComponentFlattener;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.ComponentType;
import org.lemsml.jlems.core.type.Constant;
import org.lemsml.jlems.core.type.EventPort;
import org.lemsml.jlems.core.type.Exposure;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.Parameter;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.core.type.dynamics.DerivedVariable;
import org.lemsml.jlems.core.type.dynamics.Dynamics;
import org.lemsml.jlems.core.type.dynamics.EventOut;
import org.lemsml.jlems.core.type.dynamics.OnCondition;
import org.lemsml.jlems.core.type.dynamics.StateAssignment;
import org.lemsml.jlems.core.type.dynamics.StateVariable;
import org.lemsml.jlems.core.type.dynamics.TimeDerivative;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.Utils;
import org.neuroml.export.base.XMLWriter;

public class XineMLWriter extends XMLWriter {

    public enum Variant {

        NineML, SpineML
    };

    Variant variant = null;

    public static final String SCHEMA_9ML = "https://raw.github.com/OpenSourceBrain/NineMLShowcase/master/Schemas/NineML/NineML_v0.3.xsd";
    public static final String NAMESPACE_9ML = "http://nineml.incf.org/9ML/0.3";

    public static final String SCHEMA_SPINEML_COMP_LAYER = "http://bimpa.group.shef.ac.uk/SpineML/schemas/SpineMLComponentLayer.xsd";
    public static final String SCHEMA_SPINEML_NET_LAYER = "http://bimpa.group.shef.ac.uk/SpineML/schemas/SpineMLNetworkLayer.xsd";
    public static final String SCHEMA_SPINEML_EXP_LAYER = "http://bimpa.group.shef.ac.uk/SpineML/schemas/SpineMLExperimentLayer.xsd";

    public static final String NAMESPACE_SPINEML_COMP_LAYER = "http://www.shef.ac.uk/SpineMLComponentLayer";
    public static final String NAMESPACE_SPINEML_NET_LAYER = "http://www.shef.ac.uk/SpineMLNetworkLayer";
    public static final String NAMESPACE_SPINEML_EXP_LAYER = "http://www.shef.ac.uk/SpineMLExperimentLayer";

    ArrayList<File> allGeneratedFiles = new ArrayList<File>();

    //public static final String LOCAL_9ML_SCHEMA = "src/test/resources/Schemas/sbml-l2v2-schema/sbml.xsd";
    public XineMLWriter(Lems l, Variant v) {
        super(l, v.toString());
        this.variant = v;
    }

    public ArrayList<File> getFilesGenerated() {
        return allGeneratedFiles;
    }

    public ArrayList<File> generateAllFiles(File mainFile) throws ContentError, GenerationException, IOException {

        String main = generate(mainFile.getParentFile(), mainFile.getName());
        try {
            FileUtil.writeStringToFile(main, mainFile);
            allGeneratedFiles.add(mainFile);
        } catch (IOException ex) {
            throw new ContentError("Error writing to file: " + mainFile.getAbsolutePath(), ex);
        }
        return allGeneratedFiles;
    }

    @Override
    public String getMainScript() throws GenerationException {
        try {
            return generate(null, "Test");
        } catch (ContentError e) {
            throw new GenerationException("Error with LEMS content", e);
        } catch (IOException ex) {
            throw new GenerationException("Error with file I/O", e);
        }

    }

    public String generate(File dirForFiles, String reference) throws GenerationException, IOException, ContentError {

        Parser p = new Parser();

        StringBuilder mainFile = new StringBuilder();

        StringBuilder abstLayer = new StringBuilder();
        StringBuilder userLayer = new StringBuilder();
        StringBuilder expLayer = new StringBuilder();
        int expLayerFlag = 3;

        mainFile.append("<?xml version='1.0' encoding='UTF-8'?>\n");

        String namespace = null;
        String schema = null;
        String extraAttr = "";

        String defaultDimension = null;

        switch (variant) {
            case NineML:
                namespace = NAMESPACE_9ML;
                schema = SCHEMA_9ML;
                defaultDimension = "none";
                break;
            case SpineML:
                namespace = NAMESPACE_SPINEML_COMP_LAYER;
                schema = SCHEMA_SPINEML_COMP_LAYER;
                defaultDimension = "?";
                break;
        }

        String[] attrs = new String[]{"xmlns=" + namespace,
            extraAttr,
            "xmlns:xsi=http://www.w3.org/2001/XMLSchema-instance",
            "xsi:schemaLocation=" + namespace + " " + schema};

        String root = variant.toString();

        startElement(mainFile, root, attrs);

        if (variant.equals(variant.SpineML)) {
            String[] attrs_exp = new String[]{"xmlns=" + NAMESPACE_SPINEML_EXP_LAYER,
                extraAttr,
                "xmlns:xsi=http://www.w3.org/2001/XMLSchema-instance",
                "xsi:schemaLocation=" + NAMESPACE_SPINEML_EXP_LAYER + " " + SCHEMA_SPINEML_EXP_LAYER};

            startElement(expLayer, root, attrs_exp, expLayerFlag);
        }

        String info = "\n" + Utils.getHeaderComment(format) + "\n"
                + "\nExport of model:\n" + lems.textSummary(false, false) + "\n";

        addComment(mainFile, info);

        Target target = lems.getTarget();
        Component simCpt = target.getComponent();

        String targetId = simCpt.getStringValue("target");

        Component tgtNet = lems.getComponent(targetId);

        try {

            ////addComment(main, "Adding simulation " + simCpt + " of network: " + tgtNet.summary() + "", true);
            if (variant.equals(Variant.SpineML)) {
                startElement(expLayer, "Experiment", "name=" + simCpt.getID() + "description=Export from LEMS", expLayerFlag);
                startElement(expLayer, "Model", "network_layer_url=network_" + tgtNet.id + "/>", expLayerFlag);

                //<Simulation duration="1" preferred_simulator="BRAHMS"><EulerIntegration dt="0.1"/></Simulation>
                startElement(expLayer, "Simulation", "duration=" + simCpt.getStringValue("length"), expLayerFlag);
                startEndElement(expLayer, "EulerIntegration", "dt=" + simCpt.getStringValue("step"), expLayerFlag);
                endElement(expLayer, "Simulation", expLayerFlag);

            }

            ////addComment(main, "Adding simulation " + simCpt + " of network: " + tgtNet.summary() + "", true);
            String netId = tgtNet.getID();
            ///startElement(userLayer, "network", "id=" + netId, "name=" + netId);
            userLayer.append("\n");

            //indent="";
            ArrayList<Component> pops = tgtNet.getChildrenAL("populations");

            int initAssNum = 0;
            int onCondNum = 0;

            ///startElement(userLayer, "groups?");
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

            ///endElement(userLayer, "groups?");
            userLayer.append("\n");

            ArrayList<String> compTypesAdded = new ArrayList<String>();

            for (Component pop : pops) {
                String compRef = pop.getStringValue("component");
                Component popCompFull = lems.getComponent(compRef);
                ComponentType ctFull = popCompFull.getComponentType();

                if (!compTypesAdded.contains(ctFull.getName())) {

                    compTypesAdded.add(ctFull.getName());

                    ComponentType ctFlat;
                    Component cpFlat;

                    boolean flatten = false;

                    if (flatten) {
                        try {
                            ComponentFlattener cf = new ComponentFlattener(lems, popCompFull);
                            ctFlat = cf.getFlatType();
                            cpFlat = cf.getFlatComponent();

                            lems.addComponentType(ctFlat);
                            lems.addComponent(cpFlat);
                            /*
                             String typeOut = XMLSerializer.serialize(ctFlat);
                             String cptOut = XMLSerializer.serialize(cpFlat);

                             E.info("Flat type: \n" + typeOut);
                             E.info("Flat cpt: \n" + cptOut);

                             lems.resolve(ctFlat);
                             lems.resolve(cpFlat);*/

                        } catch (Exception e) {
                            throw new GenerationException("Error when flattening component: " + popCompFull, e);
                        }
                    } else {
                        ctFlat = ctFull;
                        cpFlat = popCompFull;
                    }

                    String dynInitRegInfo = "";
                    String ocTargetRegInfo = "";

                    String defaultRegime = "defaultRegime";

                    switch (variant) {
                        case NineML:
                            break;
                        case SpineML:
                            dynInitRegInfo = "initial_regime=" + defaultRegime;
                            ocTargetRegInfo = "target_regime=" + defaultRegime;
                            break;
                    }

                    StringBuilder params = new StringBuilder();
                    StringBuilder ports = new StringBuilder();
                    StringBuilder dynamics = new StringBuilder();
                    StringBuilder stateVars = new StringBuilder();
                    StringBuilder regimes = new StringBuilder();

                    startElement(abstLayer, "ComponentClass", "name=" + ctFlat.getName());

                    for (Parameter param : ctFlat.getParameters()) {
                        startEndElement(params,
                                "Parameter",
                                "name=" + param.getName(),
                                "dimension=" + defaultDimension);
                    }
                    for (Constant constant : ctFlat.getConstants()) {
                        startEndElement(params,
                                "Parameter",
                                "name=" + constant.getName(),
                                "dimension=" + defaultDimension);
                    }
                    for (Exposure exp : ctFlat.getExposures()) {
                        switch (variant) {
                            case NineML:
                                startEndElement(ports,
                                        "AnalogPort",
                                        "name=" + exp.getName(),
                                        "mode=send",
                                        "dimension=" + defaultDimension);
                                break;
                            case SpineML:
                                startEndElement(ports,
                                        "AnalogSendPort",
                                        "name=" + exp.getName());
                                break;
                        }
                    }
                    for (EventPort port : ctFlat.getEventPorts()) {

                        switch (variant) {
                            case NineML:
                                startEndElement(ports,
                                        "EventPort",
                                        "name=" + port.getName(),
                                        "mode=" + (port.direction.equals("out") ? "send" : "receive"));
                                break;
                            case SpineML:
                                startEndElement(ports,
                                        (port.direction.equals("out") ? "EventSendPort" : "EventReceivePort"),
                                        "name=" + port.getName());
                                break;
                        }
                    }

                    Dynamics dyn = ctFlat.getDynamics();

                    if (dyn.getRegimes().isEmpty()) {
                        startElement(dynamics, "Dynamics", dynInitRegInfo);

                        startElement(regimes, "Regime", "name=" + defaultRegime);

                        for (TimeDerivative td : dyn.getTimeDerivatives()) {
                            startElement(regimes, "TimeDerivative", "variable=" + td.getVariable());
                            startEndTextElement(regimes, "MathInline", td.getValueExpression());
                            endElement(regimes, "TimeDerivative");
                        }
                        for (DerivedVariable dv : dyn.getDerivedVariables()) {
                            if (dv.getReduce() == null || dv.getReduce().equals("null")) {
                                startElement(regimes, "Alias", "name=" + dv.getName());
                                startEndTextElement(regimes, "MathInline", dv.getValueExpression());
                                endElement(regimes, "Alias");
                            }
                        }

                        for (OnCondition oc : dyn.getOnConditions()) {

                            startElement(regimes, "OnCondition", ocTargetRegInfo);

                            StringBuilder trigger = new StringBuilder();

                            startElement(trigger, "Trigger");
                            if (oc.test != null) {
                                startEndTextElement(trigger, "MathInline", oc.test);
                            }
                            endElement(trigger, "Trigger");
                            if (variant.equals(Variant.NineML)) {
                                regimes.append(trigger);
                            }

                            for (StateAssignment sa : oc.getStateAssignments()) {

                                startElement(regimes, "StateAssignment", "variable=" + sa.getVariable());

                                startEndTextElement(regimes, "MathInline", sa.getValueExpression());

                                endElement(regimes, "StateAssignment");
                            }
                            for (EventOut eo : oc.getEventOuts()) {
                                startEndElement(regimes, "EventOut", "port=" + eo.port);
                            }

                            if (variant.equals(Variant.SpineML)) {
                                regimes.append(trigger);
                            }

                            endElement(regimes, "OnCondition");
                        }
                        endElement(regimes, "Regime");

                    } else {
                        startElement(dynamics, "Dynamics-MultiRegimesNotYetImpl", dynInitRegInfo);
                    }

                    for (StateVariable sv : dyn.getStateVariables()) {
                        startEndElement(stateVars,
                                "StateVariable",
                                "name=" + sv.getName(),
                                "dimension=" + defaultDimension);

                    }

                    switch (variant) {
                        case NineML:
                            abstLayer.append(params);
                            abstLayer.append(ports);
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
                            abstLayer.append(ports);
                            abstLayer.append(params);
                            break;
                    }

                    endElement(abstLayer, "ComponentClass");
                }
            }

            mainFile.append(abstLayer);
            mainFile.append("\n");

            endElement(mainFile, root);
        } catch (ContentError e) {
            throw new GenerationException("Error with LEMS content", e);

     
        }

        if (variant.equals(Variant.SpineML)) {
            endElement(expLayer, "Model", expLayerFlag);
            endElement(expLayer, "Experiment", expLayerFlag);
            endElement(expLayer, root, expLayerFlag);

            String expFilename = "Experiment_" + reference;
            File expFile = new File(dirForFiles, expFilename);

            FileUtil.writeStringToFile(expLayer.toString(), expFile);
            allGeneratedFiles.add(expFile);
        }

        return mainFile.toString();
    }

    private String getSuitableId(String str) {
        return str.replace(" ", "_").replace(".", "").replace("(", "_").replace(")", "_").replace("*", "mult").replace("+", "plus").replace("/", "div");
    }

    public static void main(String[] args) throws Exception {

        E.setDebug(false);

        ArrayList<File> lemsFiles = new ArrayList<File>();
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex0_IaF.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex5_DetCell.xml"));
        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex9_FN.xml"));
        //lemsFiles.add(new File("../git/HindmarshRose1984/NeuroML2/Run_Regular_HindmarshRose.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/cerebellum/cerebellar_granule_cell/GranuleCell/neuroConstruct/generatedNeuroML2/LEMS_GranuleCell.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/lobster/PyloricNetwork/neuroConstruct/generatedNeuroML2/LEMS_PyloricPacemakerNetwork.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/celegans/CElegansNeuroML/CElegans/pythonScripts/c302/LEMS_c302_A.xml"));
        //lemsFiles.add(new File("../git/GPUShowcase/NeuroML2/LEMS_simplenet.xml"));

        for (File lemsFile : lemsFiles) {
            Lems lems = Utils.readLemsNeuroMLFile(lemsFile).getLems();
            File mainFile = new File(lemsFile.getParentFile(), lemsFile.getName().replaceAll(".xml", ".9ml"));

            XineMLWriter cw = new XineMLWriter(lems, Variant.NineML);
            ArrayList<File> sr = cw.generateAllFiles(mainFile);

            System.out.println("Generated: " + sr);

            for (File f : cw.getFilesGenerated()) {
                System.out.println("Generated file: " + f.getAbsolutePath());
            }
        }

    }

}
