package org.neuroml.export.utils;

import java.io.File;
import java.io.IOException;
import java.util.AbstractList;
import java.util.LinkedHashMap;

import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.sim.Sim;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.ComponentType;
import org.lemsml.jlems.core.type.Dimension;
import org.lemsml.jlems.core.type.DimensionalQuantity;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.QuantityReader;
import org.lemsml.jlems.core.type.Unit;
import org.lemsml.jlems.io.IOUtil;
import org.lemsml.jlems.io.util.FileUtil;
import org.lemsml.jlems.io.util.JUtil;
import org.lemsml.jlems.io.xmlio.XMLSerializer;
import org.lemsml.jlems.viz.datadisplay.ControlPanel;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.support.SupportLevelInfo;
import org.neuroml.model.Cell;
import org.neuroml.model.Cell2CaPools;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.Standalone;
import org.neuroml.model.util.NeuroML2Validator;
import org.neuroml.model.util.NeuroMLConverter;
import org.neuroml.model.util.NeuroMLElements;
import org.neuroml.model.util.NeuroMLException;

public class Utils
{

	private static Lems lemsWithNML2CompTypes;

	public static String ORG_NEUROML_EXPORT_VERSION = "1.6.1";

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

	public static final String LEMS_EXAMPLES_RESOURCES_DIR = "/LEMSexamples";
	public static final String NEUROML_COMPTYPR_RESOURCES_DIR = "/NeuroML2CoreTypes";
	public static final String NEUROML_EXAMPLES_RESOURCES_DIR = "/examples";

	private static Lems getLemsWithNML2CompTypes() throws LEMSException
	{
		if(lemsWithNML2CompTypes == null)
		{

			NeuroML2Validator nmlv = new NeuroML2Validator();
			String content = JUtil.getRelativeResource(nmlv.getClass(), NEUROML_EXAMPLES_RESOURCES_DIR + "/NML2_AbstractCells.nml");
			String lemsVer = NeuroMLConverter.convertNeuroML2ToLems(content);
			lemsWithNML2CompTypes = readLemsNeuroMLFile(lemsVer).getLems();

		}
		return lemsWithNML2CompTypes;
	}

	public static String getHeaderComment(Format format)
	{
		String commentString = "    This " + format.getLabel() + " file has been generated by org.neuroml.export (see https://github.com/NeuroML/org.neuroml.export)\n" + "         org.neuroml.export  v"
				+ Utils.ORG_NEUROML_EXPORT_VERSION + "\n" + "         org.neuroml.model   v" + NeuroMLElements.ORG_NEUROML_MODEL_VERSION + "\n" + "         jLEMS               v"
				+ org.lemsml.jlems.io.Main.VERSION;
		return commentString;
	}

	/*
	 * Gets the magnitude of a NeuroML 2 quantity string in SI units (e.g. -60mV -> -0.06)
	 */
	public static float getMagnitudeInSI(String nml2Quantity) throws NeuroMLException
	{

		try
		{
			DimensionalQuantity dq = QuantityReader.parseValue(nml2Quantity, getLemsWithNML2CompTypes().getUnits());
			float val = (float) dq.getValue();

			return val;
		}
		catch(LEMSException ex)
		{
			throw new NeuroMLException("Problem getting magnitude in SI for: " + nml2Quantity, ex);
		}

	}

	/*
	 * Gets the Dimension of a NeuroML 2 quantity string in SI units (e.g. -60mV returns Dimension with values for Voltage)
	 */
	public static Dimension getDimension(String nml2Quantity) throws NeuroMLException
	{

		try
		{
			DimensionalQuantity dq = QuantityReader.parseValue(nml2Quantity, getLemsWithNML2CompTypes().getUnits());

			return dq.getDimension();
		}
		catch(LEMSException ex)
		{
			throw new NeuroMLException("Problem getting magnitude in SI for: " + nml2Quantity, ex);
		}

	}

	/*
	 * For example, ../Pop0[0] returns Pop0; ../Gran/0/Granule_98 returns Gran
	 */
	public static String parseCellRefStringForPopulation(String cellRef)
	{
		//System.out.println("Parsing for population: " + cellRef);
		int loc = cellRef.startsWith("../") ? 1 : 0;
		String ref = cellRef.contains("/") ? cellRef.split("/")[loc] : cellRef;
		if(ref.contains("[")) return ref.substring(0, ref.indexOf("["));
		else return ref;
	}

	/*
	 * For example, ../Pop0[0] returns 0; ../Gran/0/Granule_98 returns 0; Gran/1/Granule_98 returns 0
	 */
	public static int parseCellRefStringForCellNum(String cellRef)
	{
		// System.out.println("Parsing for cell num: "+cellRef);
		if(cellRef.contains("["))
		{
			return Integer.parseInt(cellRef.substring(cellRef.indexOf("[") + 1, cellRef.indexOf("]")));
		}
		else
		{
			int loc = cellRef.startsWith("../") ? 2 : 1;
			String ref = cellRef.split("/")[loc];
			return Integer.parseInt(ref);
		}
	}

	public static Unit getSIUnitInNeuroML(Dimension dim) throws NeuroMLException
	{
		try
		{
			for(Unit unit : getLemsWithNML2CompTypes().getUnits())
			{
				if(unit.getDimension().getName().equals(dim.getName()) && unit.scale == 1 && unit.power == 0 && unit.offset == 0) return unit;
			}
		}
		catch(LEMSException ex)
		{
			throw new NeuroMLException("Problem finding SI unit for dimension: " + dim, ex);
		}

		return null;
	}

	public static Sim readLemsNeuroMLFile(String contents) throws LEMSException
	{

		NeuroMLInclusionReader.addSearchPathInJar("/NeuroML2CoreTypes");
		NeuroMLInclusionReader.addSearchPathInJar("/examples");
		NeuroMLInclusionReader.addSearchPathInJar("/");

		NeuroMLInclusionReader nmlIr = new NeuroMLInclusionReader(contents);
		JUtil.setResourceRoot(NeuroMLConverter.class);
		Sim sim = new Sim(nmlIr.read());

		sim.readModel();
		return sim;

	}

	public static Sim readNeuroMLFile(File f) throws LEMSException, IOException
	{
        return readNeuroMLFile(f, true);
    }

	public static Sim readNeuroMLFile(File f, boolean includeConnectionsFromHDF5) throws LEMSException, IOException
	{
        
		NeuroMLInclusionReader.addSearchPathInJar("/NeuroML2CoreTypes");
		NeuroMLInclusionReader.addSearchPath(f.getParentFile());

		E.info("Reading from: " + f.getAbsolutePath());

		String nml = FileUtil.readStringFromFile(f);

		String nmlLems = NeuroMLConverter.convertNeuroML2ToLems(nml);

		NeuroMLInclusionReader nmlIr = new NeuroMLInclusionReader(nmlLems);
        nmlIr.setIncludeConnectionsFromHDF5(includeConnectionsFromHDF5);

		Sim sim = new Sim(nmlIr.read());

		sim.readModel();
		return sim;
	}
    
	public static Sim readNeuroMLFile(String contents) throws LEMSException
	{

		NeuroMLInclusionReader.addSearchPathInJar("/NeuroML2CoreTypes");

		String nmlLems = NeuroMLConverter.convertNeuroML2ToLems(contents);

		NeuroMLInclusionReader nmlIr = new NeuroMLInclusionReader(nmlLems);

		Sim sim = new Sim(nmlIr.read());

		sim.readModel();
		return sim;
	}

    public static File copyFromJarToTempLocation(String filename) throws ContentError, IOException
    {
        NeuroML2Validator nmlv = new NeuroML2Validator();
        String content = JUtil.getRelativeResource(nmlv.getClass(), filename);
        
        //File tempDir = Files.createTempDirectory("jNeuroML").toFile();
        //File newFile = new File(tempDir,(new File(filename)).getName());
        
        File newFile = File.createTempFile("jNeuroML", "_"+(new File(filename)).getName());
        
        FileUtil.writeStringToFile(content, newFile);

        return newFile;
    }

	public static Sim readLemsNeuroMLFile(File f) throws LEMSException, NeuroMLException
    {
        return readLemsNeuroMLFile(f,true);
    }

	public static Sim readLemsNeuroMLFile(File f, boolean includeConnectionsFromHDF5) throws LEMSException, NeuroMLException
	{
        
		NeuroMLInclusionReader.addSearchPathInJar("/NeuroML2CoreTypes");
		NeuroMLInclusionReader.addSearchPath(f.getParentFile());

		E.info("Reading from: " + f.getAbsolutePath());

		NeuroMLInclusionReader nmlIr = new NeuroMLInclusionReader(f);
        nmlIr.setIncludeConnectionsFromHDF5(includeConnectionsFromHDF5);

		Sim sim = new Sim(nmlIr.read());

		sim.readModel();
        sim.getLems().setAllIncludedFiles(nmlIr.getAllIncludedFiles());
		return sim;

	}

	public static String replaceInExpression(String expression, String oldVal, String newVal)
	{
		expression = " " + expression + " ";
		String[] pres = new String[] { "\\(", "\\+", "-", "\\*", "/", "\\^", " ", "=" };
		String[] posts = new String[] { "\\)", "\\+", "-", "\\*", "/", "\\^", " ", "=" };

		for(String pre : pres)
		{
			for(String post : posts)
			{
				String o = pre + oldVal + post;
				String n = pre + " " + newVal + " " + post;
				// if (expression.contains(o))
				// E.info("Replacing {"+o+"} with {"+n+"} in {"+expression+"}");
				expression = expression.replaceAll(o, n);
			}
		}
		return expression.trim();
	}

	public static String convertLemsToNeuroMLLikeXml(Lems lems, String onlyNetwork) throws LEMSException, NeuroMLException
	{
        StringBuilder compString = new StringBuilder();
        
		XMLSerializer xmlSer = XMLSerializer.newInstance();
        
        for (Component comp: lems.getComponents()) 
        {
            if (!comp.getTypeName().equals("Simulation") && !comp.getTypeName().equals("include")) 
            {
                String knownAs = null;
                String typeAttribute = null;
                
                String xml = xmlSer.writeObject(comp,knownAs,typeAttribute)+"\n";
                xml = xml.replaceAll("<populationList ", "<population type=\"populationList\" ");
                xml = xml.replaceAll("</populationList>", "</population>");
                
                xml = xml.replaceAll("<networkWithTemperature ", "<network type=\"networkWithTemperature\" ");
                xml = xml.replaceAll("</networkWithTemperature>", "</network>");
                
                boolean include = true;
                
                if (comp.getTypeName().equals("network") || comp.getTypeName().equals("networkWithTemperature"))
                {
                    if (comp.getID().equals(onlyNetwork)) 
                        include = true;
                    else
                        include = false;
                }
                if (include) compString.append(xml);
                
            }
        }
        
        Lems standardNml2Lems = getLemsWithNML2CompTypes();
        
        for (ComponentType ct: lems.getComponentTypes())
        {
            try
            {
                standardNml2Lems.getComponentTypeByName(ct.getName());
            }
            catch(ContentError ce)  // not a standard nml2 comp type...
            {
                String xml = xmlSer.writeObject(ct);
                compString.append(xml);
                
            }
        }
        
        String nmlString = "<neuroml xmlns=\"http://www.neuroml.org/schema/neuroml2\"\n" 
                + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
				+ "    xsi:schemaLocation=\"http://www.neuroml.org/schema/neuroml2 " + NeuroMLElements.LATEST_SCHEMA_LOCATION + "\"\n"
                + "    id=\"Exported_from_LEMS\">\n\n" + compString + "</neuroml>";
        
        
		return nmlString;
	}

	public static String extractLemsSimulationXml(Lems lems, String externalFiletoInclude) throws LEMSException, NeuroMLException
    {
        return extractLemsSimulationXml(lems, externalFiletoInclude, null);
    }

	public static String extractLemsSimulationXml(Lems lems, String externalFiletoInclude, String reportFile) throws LEMSException, NeuroMLException
	{
        StringBuilder compString = new StringBuilder();
        
		XMLSerializer xmlSer = XMLSerializer.newInstance();
        
        String target = null;
        for (Component comp: lems.getComponents()) 
        {
            if (comp.getTypeName().equals("Simulation")) 
            {
                target = comp.getID();
                String xml = xmlSer.writeObject(comp,null,null)+"\n";
                
                compString.append(xml);
                
            }
        }
        
        String lemsString = "<Lems> \n";
        String report = reportFile==null ? "" : " reportFile=\""+reportFile+"\"";
        
        lemsString += "<!-- Specify which component to run -->\n" +
"    <Target component=\""+target+"\""+report+"/>\n" +
"    \n" +
"    <!-- Include core NeuroML2 ComponentType definitions -->\n" +
"    <Include file=\"Cells.xml\"/>\n" +
"    <Include file=\"PyNN.xml\"/>\n" +
"    <Include file=\"Networks.xml\"/>\n" +
"    <Include file=\"Simulation.xml\"/>\n\n";
        
        if (externalFiletoInclude!=null)
            lemsString += "    <Include file=\""+externalFiletoInclude+"\"/>\n\n";
            
        lemsString += compString + "</Lems>";
        
        
		return lemsString;
	}

	public static NeuroMLDocument convertLemsComponentToNeuroMLDocument(Component comp) throws LEMSException, NeuroMLException
	{
		XMLSerializer xmlSer = XMLSerializer.newInstance();
		String compString = xmlSer.writeObject(comp);

		NeuroMLConverter nmlc = new NeuroMLConverter();
        String nmlString = "<neuroml xmlns=\"http://www.neuroml.org/schema/neuroml2\"\n" + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
				+ "      xsi:schemaLocation=\"http://www.neuroml.org/schema/neuroml2 " + NeuroMLElements.LATEST_SCHEMA_LOCATION + "\">" + compString + "</neuroml>";
		NeuroMLDocument nmlDocument = nmlc.loadNeuroML(nmlString);
        
		return nmlDocument;
	}

	public static LinkedHashMap<String, Standalone> convertLemsComponentToNeuroML(Component comp) throws LEMSException, NeuroMLException
	{
		NeuroMLDocument nmlDocument = convertLemsComponentToNeuroMLDocument(comp);
        
		LinkedHashMap<String, Standalone> els = NeuroMLConverter.getAllStandaloneElements(nmlDocument);
		return els;
	}

	public static Component convertNeuroMLToComponent(Standalone nmlElement) throws NeuroMLException, LEMSException
	{
		Lems lems = convertNeuroMLToSim(nmlElement).getLems();

		try
		{
			return lems.getComponent(nmlElement.getId());
		}
		catch(ContentError e)
		{
			throw new NeuroMLException(e);
		}
	}

	public static Cell getCellFromComponent(Component comp) throws LEMSException, NeuroMLException
	{
		LinkedHashMap<String, Standalone> els = Utils.convertLemsComponentToNeuroML(comp);
		Cell cell = (Cell) els.values().iterator().next();
        if (cell == null)
        {
            Cell2CaPools cell2 = (Cell2CaPools) els.values().iterator().next();
            cell = cell2;
        }
        if (cell==null) 
        {
            throw new NeuroMLException("Problem finding cell element in component: "+comp+" and converting to NeuroML");
        }
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
		if(list.size() > 1)
		{
			for(int j = 1; j < list.size(); j++)
			{

				for(int k = 0; k < j; k++)
				{
					if(ascending)
					{
						if(list.get(j).toString().compareToIgnoreCase(list.get(k).toString()) < 0)
						{
							Object earlier = list.get(j);
							Object later = list.get(k);
							list.set(j, later);
							list.set(k, earlier);
						}
					}
					else
					{
						if(list.get(j).toString().compareToIgnoreCase(list.get(k).toString()) > 0)
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

	public static void runLemsFile(File f, boolean showGui) throws LEMSException, ModelFeatureSupportException, NeuroMLException
	{
		loadLemsFile(f, true, showGui);
	}

	public static void loadLemsFile(File lemsFile, boolean run, boolean showGui) throws LEMSException, ModelFeatureSupportException, NeuroMLException
	{
		ControlPanel cp = new ControlPanel("jNeuroML", showGui) {

			@Override
			public Sim importFile(File simFile) throws LEMSException {
                try 
                {
                    Sim sim;
                    sim = Utils.readLemsNeuroMLFile(simFile);
                    sim.build();
                    return sim;  
                }
                catch (NeuroMLException e)
                {
                    throw new LEMSException(e);
                }
            }
        };
        
        Sim sim = cp.initialise(lemsFile);
        
        if(sim == null) {
        	E.info(String.format("Control Panel Initialisation : Failed to read and build simulation from file %s", lemsFile.getName()));
        	return;
        }

		if(run)
		{
			SupportLevelInfo sli = SupportLevelInfo.getSupportLevelInfo();
			sli.checkConversionSupported(Format.LEMS, sim.getLems());
			sim.run();
			IOUtil.saveReportAndTimesFile(sim, lemsFile);
			E.info("Finished reading, building, running and displaying LEMS model");
		}
		else
		{
			E.info("Finished reading and building LEMS model");
		}

	}

	public static boolean isWindowsBasedPlatform()
	{
		return System.getProperty("os.name").toLowerCase().indexOf("indows") > 0;
	}

	public static boolean isLinuxBasedPlatform()
	{
		// /if (true) return false;
		/** @todo See if this is general enough */
		return System.getProperty("os.name").toLowerCase().contains("nix") || System.getProperty("os.name").toLowerCase().contains("linux");
	}

	public static boolean isMacBasedPlatform()
	{
		// /if (true) return true;
		/** @todo See if this is general enough */
		if(isWindowsBasedPlatform()) return false;
		if(isLinuxBasedPlatform()) return false;

		return System.getProperty("os.name").toLowerCase().contains("mac");
	}

	/**
	 * @return i686 for most, x86_64 if "64" present in system properties os.arch, e.g. amd64. Will need updating as Neuron tested on more platforms...
	 * 
	 */
	public static String getArchSpecificDir()
	{
		if(!isMacBasedPlatform() && (System.getProperty("os.arch").equals(ARCH_64BIT) || System.getProperty("os.arch").contains("64")))
		{
			return DIR_64BIT;
		}
		else if(isMacBasedPlatform() && System.getProperty("os.arch").contains(ARCH_POWERPC))
		{
			return DIR_POWERPC;
		}
		else if(isMacBasedPlatform() && System.getProperty("os.arch").contains(ARCH_I386))
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
		return System.getProperty("os.arch").contains("64"); // should be
																	// cases
	}
    
    public static boolean isVersionControlDir(String dirname)
    {
        return dirname.equals("CVS") ||  dirname.equals(".svn") ||  dirname.equals("_svn")||  dirname.equals(".git")||  dirname.equals(".hg");
    }

    public static boolean isVersionControlDir(File dir)
    {
        return isVersionControlDir(dir.getName());
    }
    
    public static void removeAllFiles(File directory, boolean removeDirToo, boolean removeVC)
    {
        File[] allFiles = directory.listFiles();

        boolean underVersionControl  = false;
        if (allFiles!=null)
        {
            for (File f : allFiles)
            {
                if (f.isDirectory())
                {
                    underVersionControl = underVersionControl || isVersionControlDir(f.getName());
                    if (!(isVersionControlDir(f.getName()) && !removeVC))
                    {
                        removeAllFiles(f, true, removeVC);
                    }
                }
                else
                {
                    try
                    {
                        boolean res = f.delete();
                        //System.out.println("Deleting: "+ f+": "+ res);
                        if (!res)
                        {
                            f.deleteOnExit();
                        }
                    }catch(SecurityException se)
                    {
                        se.printStackTrace();;
                    }
                }
            }
        }
        if (removeDirToo)
        {
            if (! (!removeVC && underVersionControl) ){
                boolean res = directory.delete();
                //System.out.println("Deleted: "+ directory+": "+ res);
                if (!res) directory.deleteOnExit();
            }
        }

    }
        

    public static void main(String args[]) throws ContentError, IOException, LEMSException, NeuroMLException
    {
        File f = new File("../neuroConstruct/osb/showcase/NetPyNEShowcase/NeuroML2/scaling/LEMS_Balanced.xml");
        //f = new File("../neuroConstruct/osb/showcase/NetPyNEShowcase/NeuroML2/scaling/LEMS_Balanced_0.2_hdf5.xml");
        
        f = new File("../git/ca1/NeuroML2/network/LEMS_PINGNet_0.xml");
        //f = new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex20a_AnalogSynapsesHH.xml");
        //Sim sim = Utils.readLemsNeuroMLFile(f);
        //Lems lems = sim.getLems();
        //System.out.println("-----------------------\nLEMS:\n"+lems.components.toString());
        f = new File("/tmp/tt/NETMORPH2NeuroML/");
        removeAllFiles(f, true, true);
    }

}
