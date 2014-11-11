/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.neuroml.export.geppetto;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lemsml.export.base.XMLWriter;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.Utils;

/**
 *
 * @author padraig
 */
public class GeppettoWriter extends XMLWriter {
    
    private final File lemsFile;
    
    public GeppettoWriter(Lems l, File lemsFile) {
        super(l, "Geppetto");
        this.lemsFile = lemsFile;
    }
    
	@Override
    public String getMainScript() throws ContentError {
            
        StringBuilder main = new StringBuilder();
        main.append("<?xml version='1.0' encoding='UTF-8'?>\n");

        String[] attrs = new String[]{"xmlns=http://www.openworm.org/simulationSchema",
            "xmlns:xsi=http://www.w3.org/2001/XMLSchema-instance",
            "xsi:schemaLocation=http://www.openworm.org/simulationSchema https://raw.githubusercontent.com/openworm/org.geppetto.core/master/src/main/resources/schema/simulation/simulationSchema.xsd"};
        

        startElement(main, "simulation", attrs);
        
        startElement(main, "entity");
        
        Target target = lems.getTarget();
        
        Component simCpt = target.getComponent();
        E.info("simCpt: "+simCpt);
        String simId = simCpt.getID();
        startEndTextElement(main, "id", simId);
        
        startElement(main, "aspect");
        
        startEndTextElement(main, "id", "electrical");
        
        startElement(main, "simulator");
        startEndTextElement(main, "simulatorId", "jLemsSimulator");
        endElement(main, "simulator");
        
        startElement(main, "model");
        startEndTextElement(main, "modelInterpreterId", "lemsModelInterpreter");
        startEndTextElement(main, "modelURL", "file:///"+lemsFile.getAbsolutePath());
        endElement(main, "model");
        
        
        endElement(main, "aspect");
        endElement(main, "entity");
        
        File geppettoScript = new File(lemsFile.getParentFile(), lemsFile.getName().replace(".xml", ".js"));
        
        StringBuilder gScript = new StringBuilder();
        

        ArrayList<String> watchVars = new ArrayList<String>();
        
        gScript.append("Simulation.addWatchLists([{name:\"variables\", variablePaths:[ ");
        
        int dispIndex = 1;
        for (Component dispComp : simCpt.getAllChildren()) {
            if (dispComp.getTypeName().equals("Display")) {
                
                gScript.append("\nG.addWidget(GEPPETTO.Widgets.PLOT);\n");
                String plot = "Plot"+dispIndex;
                gScript.append(plot+".setOptions({yaxis:{min:-.08,max:-.04},xaxis:{min:0,max:400,show:false}});\n");
                gScript.append(plot+".setSize(400, 600);\n");
                gScript.append(plot+".setPosition("+(dispIndex*200)+","+(dispIndex*200)+");\n");
                gScript.append(plot+".setName(\""+dispComp.getStringValue("title")+"\");\n");
                
                dispIndex++;
                    for (Component lineComp : dispComp.getAllChildren()) {
                        if (lineComp.getTypeName().equals("Line")) {
                           
                            String ref = lineComp.getStringValue("quantity");
                            String gepRef = simId+".electrical.SimulationTree."+ref.replaceAll("/", ".");
                            gScript.append(plot+".plotData("+gepRef+");\n");
                            watchVars.add(gepRef);
                        }
                    }
            }
        }
        
        for (int ii = 0; ii<watchVars.size(); ii++) {
            if (ii>0) gScript.append(", ");
            gScript.append("\""+watchVars.get(ii)+"\"");
        }
        gScript.append(" ]}]);\n\n");
        
        gScript.append("Simulation.startWatch();\n");
        gScript.append("Simulation.start();\n");
        
        try {
            FileUtil.writeStringToFile(gScript.toString(), geppettoScript);
            
        } catch (IOException ex) {
            throw new ContentError("Error saving Geppetto script", ex);
        }
        
        startEndTextElement(main, "script", "file:///"+geppettoScript.getAbsolutePath());
        
        endElement(main, "simulation");
        
        return main.toString();
    }
    
    
    public static void main(String[] args) throws Exception {

    	
        File exampleFile = new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex5_DetCell.xml");
        
		Lems lems = Utils.readLemsNeuroMLFile(exampleFile).getLems();
        System.out.println("Loaded: "+exampleFile.getAbsolutePath());

        GeppettoWriter gw = new GeppettoWriter(lems, exampleFile);

        String g = gw.getMainScript();


        File grFile = new File(exampleFile.getAbsolutePath().replaceAll(".xml", ".g.xml"));
        System.out.println("Writing to: "+grFile.getAbsolutePath());
        
        FileUtil.writeStringToFile(g, grFile);

      }
    
}
