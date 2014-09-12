package org.neuroml.export;

import java.io.File;
import java.io.IOException;
import java.util.AbstractList;
import java.util.LinkedHashMap;

import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.sim.Sim;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Dimension;
import org.lemsml.jlems.core.type.DimensionalQuantity;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.QuantityReader;
import org.lemsml.jlems.core.type.Unit;
import org.lemsml.jlems.io.IOUtil;
import org.lemsml.jlems.io.reader.JarResourceInclusionReader;
import org.lemsml.jlems.io.util.FileUtil;
import org.lemsml.jlems.io.util.JUtil;
import org.lemsml.jlems.io.xmlio.XMLSerializer;
import org.neuroml.model.Cell;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.Standalone;
import org.neuroml.model.util.NeuroML2Validator;
import org.neuroml.model.util.NeuroMLConverter;
import org.neuroml.model.util.NeuroMLElements;
import org.neuroml.model.util.NeuroMLException;

public class Utils {
	
	private static Lems lemsWithNML2CompTypes;
    
    public static final String ARCH_I686 = "i686";
    public static final String ARCH_I386 = "i386";
    public static final String ARCH_64BIT = "amd64";
    public static final String ARCH_POWERPC = "ppc";
    public static final String ARCH_UMAC = "umac";

    public static final String DIR_I386 = "i386";
    public static final String DIR_I686 = "i686";
    public static final String DIR_64BIT = "x86_64";
    public static final String DIR_POWERPC = "powerpc";
    public static final String DIR_UMAC = "umac";
    
    private static Lems getLemsWithNML2CompTypes() throws LEMSException {
        if (lemsWithNML2CompTypes==null) {

            NeuroML2Validator nmlv = new NeuroML2Validator();
            String content = JUtil.getRelativeResource(nmlv.getClass(),
                    Main.getNeuroMLExamplesResourcesDir()
                            + "/NML2_AbstractCells.nml");
            String lemsVer = NeuroMLConverter.convertNeuroML2ToLems(content);
            lemsWithNML2CompTypes = readLemsNeuroMLFile(lemsVer).getLems();

   
        }
		return lemsWithNML2CompTypes;
    }
	
	
	public static String getHeaderComment(String format) {
		String commentString = "    This "+format+" file has been generated by org.neuroml.export (see https://github.com/NeuroML/org.neuroml.export)\n" +
    			"         org.neuroml.export  v"+Main.ORG_NEUROML_EXPORT_VERSION+"\n" +
                "         org.neuroml.model   v"+NeuroMLElements.ORG_NEUROML_MODEL_VERSION+"\n" +
                "         jLEMS               v"+org.lemsml.jlems.io.Main.VERSION;
		return commentString;
	}
	
	/*
	 * Gets the magnitude of a NeuroML 2 quantity string in SI units (e.g. -60mV -> -0.06)
	 */
	public static float getMagnitudeInSI(String nml2Quantity) throws NeuroMLException {
		
        try {
            DimensionalQuantity dq = QuantityReader.parseValue(nml2Quantity, getLemsWithNML2CompTypes().getUnits());
            float val = (float)dq.getValue();

            return val;
        } catch (LEMSException ex) {
            throw new NeuroMLException("Problem getting magnitude in SI for: "+nml2Quantity, ex);
        }
		
	}
    
	/*
	 * Gets the Dimension of a NeuroML 2 quantity string in SI units (e.g. -60mV returns Dimension with values for Voltage)
	 */
	public static Dimension getDimension(String nml2Quantity) throws NeuroMLException {
		
        try {
            DimensionalQuantity dq = QuantityReader.parseValue(nml2Quantity, getLemsWithNML2CompTypes().getUnits());

            return dq.getDimension();
        } catch (LEMSException ex) {
            throw new NeuroMLException("Problem getting magnitude in SI for: "+nml2Quantity, ex);
        }
		
	}
    
    /*
         For example, ../Pop0[0] returns Pop0; ../Gran/0/Granule_98 returns Gran
    */
    public static String parseCellRefStringForPopulation(String cellRef) {
        System.out.println("Parsing for population: "+cellRef);
        int loc = cellRef.startsWith("../") ? 1 : 0;
        String ref = cellRef.indexOf("/")>=0 ? cellRef.split("/")[loc] : cellRef;
        if (ref.indexOf("[")>=0)
            return ref.substring(0, ref.indexOf("["));
        else
            return ref;
    }
    
    /*
         For example, ../Pop0[0] returns 0; ../Gran/0/Granule_98 returns 0; Gran/1/Granule_98 returns 0
    */
    public static int parseCellRefStringForCellNum(String cellRef) {
        //System.out.println("Parsing for cell num: "+cellRef);
        if (cellRef.indexOf("[")>=0) {
            return Integer.parseInt(cellRef.substring(cellRef.indexOf("[")+1, cellRef.indexOf("]")));
        } else {
            int loc = cellRef.startsWith("../") ? 2 : 1;
            String ref = cellRef.split("/")[loc];
            return Integer.parseInt(ref);
        }
    }
    
    public static Unit getSIUnitInNeuroML(Dimension dim) throws NeuroMLException
    {
        try {
            for (Unit unit: getLemsWithNML2CompTypes().getUnits()) {
                if (unit.getDimension().getName().equals(dim.getName()) &&
                    unit.scale==1 && unit.power==0 && unit.offset==0)
                    return unit;
            }
        } catch (LEMSException ex) {
            throw new NeuroMLException("Problem finding SI unit for dimension: "+dim, ex);
        }
        
        return null;
    }

	public static Sim readLemsNeuroMLFile(String contents) throws LEMSException {

		JarResourceInclusionReader.addSearchPathInJar("/NeuroML2CoreTypes");
		JarResourceInclusionReader.addSearchPathInJar("/examples");
		JarResourceInclusionReader.addSearchPathInJar("/");
		
		JarResourceInclusionReader jrir = new JarResourceInclusionReader(contents);
		JUtil.setResourceRoot(NeuroMLConverter.class);
        Sim sim = new Sim(jrir.read());
            
        sim.readModel();
    	return sim;
		
	}

	public static Sim readNeuroMLFile(File f) throws LEMSException, IOException {

		JarResourceInclusionReader.addSearchPathInJar("/NeuroML2CoreTypes");
		JarResourceInclusionReader.addSearchPath(f.getParentFile());
		
		E.info("Reading from: "+f.getAbsolutePath());

    	String nml = FileUtil.readStringFromFile(f);
    	
    	String nmlLems = NeuroMLConverter.convertNeuroML2ToLems(nml);
		
		JarResourceInclusionReader jrir = new JarResourceInclusionReader(nmlLems);
		
        Sim sim = new Sim(jrir.read());
            
        sim.readModel();
    	return sim;
		
	}
	public static Sim readLemsNeuroMLFile(File f) throws LEMSException {

		JarResourceInclusionReader.addSearchPathInJar("/NeuroML2CoreTypes");
		JarResourceInclusionReader.addSearchPath(f.getParentFile());
		
		E.info("Reading from: "+f.getAbsolutePath());
		
		JarResourceInclusionReader jrir = new JarResourceInclusionReader(f);
		
        Sim sim = new Sim(jrir.read());
            
        sim.readModel();
    	return sim;
		
	}

    public static String replaceInExpression(String expression, String oldVal, String newVal) {
    	expression = " "+expression+" ";
    	String[] pres = new String[]{"\\(","\\+","-","\\*","/","\\^", " ", "="};
        String[] posts = new String[]{"\\)","\\+","-","\\*","/","\\^", " ", "="};

        for(String pre: pres){
            for(String post: posts){
                String o = pre+oldVal+post;
                String n = pre+" "+newVal+" "+post;
                if (expression.contains(o))
	                E.info("Replacing {"+o+"} with {"+n+"} in {"+expression+"}");
                expression = expression.replaceAll(o, n);
            }
        }
        return expression.trim();
    }

    
    
    public static LinkedHashMap<String,Standalone> convertLemsComponentToNeuroML(Component comp) throws LEMSException, NeuroMLException 
    {
        XMLSerializer xmlSer = XMLSerializer.newInstance();
        String compString = xmlSer.writeObject(comp);
        
        NeuroMLConverter nmlc = new NeuroMLConverter();
    	NeuroMLDocument nmlDocument = nmlc.loadNeuroML("<neuroml xmlns=\"http://www.neuroml.org/schema/neuroml2\"\n" +
"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
"      xsi:schemaLocation=\"http://www.neuroml.org/schema/neuroml2 "+NeuroMLElements.LATEST_SCHEMA_LOCATION+"\">"+compString+"</neuroml>");
        LinkedHashMap<String,Standalone> els = NeuroMLConverter.getAllStandaloneElements(nmlDocument);
        return els;
    }
    
    
    public static Component convertNeuroMLToComponent(Standalone nmlElement) throws NeuroMLException, LEMSException
    {
        Lems lems = convertNeuroMLToSim(nmlElement).getLems();
        
        try {
			return lems.getComponent(nmlElement.getId());
		} catch (ContentError e) {
			throw new NeuroMLException(e);
		}
    }
    
	public static Cell getCellFromComponent(Component comp) throws LEMSException, NeuroMLException {
		LinkedHashMap<String, Standalone> els = Utils.convertLemsComponentToNeuroML(comp);
		Cell cell = (Cell) els.values().iterator().next();
		return cell;
	}

    public static Sim convertNeuroMLToSim(Standalone nmlElement) throws NeuroMLException, LEMSException
    {
        NeuroMLDocument nml2 = new NeuroMLDocument();
        nml2.setId(nmlElement.getId());
        NeuroMLConverter.addElementToDocument(nml2, nmlElement);
        NeuroMLConverter nmlc = new NeuroMLConverter();
		
        String nml2String = nmlc.neuroml2ToXml(nml2);
        String lemsString = NeuroMLConverter.convertNeuroML2ToLems(nml2String);
        Sim sim = Utils.readLemsNeuroMLFile(lemsString);
		
        
        return sim;
    }
    
    public static AbstractList reorderAlphabetically(AbstractList list, boolean ascending)
    {
        if (list.size() > 1)
        {
            for (int j = 1; j < list.size(); j++)
            {

                for (int k = 0; k < j; k++)
                {
                    if (ascending)
                    {
                        if (list.get(j).toString().compareToIgnoreCase(list.get(k).toString()) < 0)
                        {
                            Object earlier = list.get(j);
                            Object later = list.get(k);
                            list.set(j, later);
                            list.set(k, earlier);
                        }
                    }
                    else
                    {
                        if (list.get(j).toString().compareToIgnoreCase(list.get(k).toString()) > 0)
                        {
                            Object earlier = list.get(j);
                            Object later = list.get(k);
                            list.set(j, later);
                            list.set(k, earlier);
                        }
                    }
                }
            }
        }
        return list;
    }
    
    public static void runLemsFile(File f) throws LEMSException, ModelFeatureSupportException, NeuroMLException {
        loadLemsFile(f, true);
    }

    public static void loadLemsFile(File f, boolean run) throws LEMSException, ModelFeatureSupportException, NeuroMLException {

        Sim sim = Utils.readLemsNeuroMLFile(f);
        sim.build();

        if (run) {
            SupportLevelInfo sli = SupportLevelInfo.getSupportLevelInfo();
            sli.checkAllFeaturesSupported(SupportLevelInfo.LEMS_NATIVE_EXECUTION, sim.getLems());
            sim.run();
            IOUtil.saveReportAndTimesFile(sim);
            E.info("Finished reading, building, running and displaying LEMS model");
        } else {
            E.info("Finished reading and building LEMS model");
        }

    }
    
    public static boolean isWindowsBasedPlatform()
    {
        return System.getProperty("os.name").toLowerCase().indexOf("indows") > 0;
    }
    
    public static boolean isLinuxBasedPlatform()
    {
        ///if (true) return false;
        /** @todo See if this is general enough */
        return System.getProperty("os.name").toLowerCase().indexOf("nix") >= 0 ||
            System.getProperty("os.name").toLowerCase().indexOf("linux") >= 0;
    }


    public static boolean isMacBasedPlatform()
    {
        ///if (true) return true;
        /** @todo See if this is general enough */
        if (isWindowsBasedPlatform()) return false;
        if (isLinuxBasedPlatform()) return false;

        return System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0;
    }
        
    /**
     * @return i686 for most, x86_64 if "64" present in system properties os.arch, 
     * e.g. amd64. Will need updating as Neuron tested on more platforms...
     *
     */
    public static String getArchSpecificDir()
    {
        if (!isMacBasedPlatform() &&
            (System.getProperty("os.arch").equals(ARCH_64BIT) ||
            System.getProperty("os.arch").indexOf("64")>=0))
        {
            return DIR_64BIT;
        }
        else if (isMacBasedPlatform() && System.getProperty("os.arch").indexOf(ARCH_POWERPC)>=0)
        {
            return DIR_POWERPC;
        }
        else if (isMacBasedPlatform() && System.getProperty("os.arch").indexOf(ARCH_I386)>=0)
        {
            return DIR_I686;
        }
        else
        {
            return DIR_I686;
        }
    }

    public static boolean is64bitPlatform()
    {
        return System.getProperty("os.arch").indexOf("64")>=0; // should be enough in most cases
    }
    

    public static void main(String[] args) throws Exception {
    	
    	String expr = "q+instances";
    	E.info("Replaced "+expr+" with "+replaceInExpression(expr, "q", "gg"));
    }

}
