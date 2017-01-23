
package org.neuroml.export.utils;

import java.io.File;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.io.reader.JarResourceInclusionReader;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.util.NeuroMLConverter;
import org.neuroml.model.util.hdf5.NeuroMLHDF5Reader;


public class NeuroMLInclusionReader extends JarResourceInclusionReader
{
    boolean includeConnectionsFromHDF5 = true;
    
    public NeuroMLInclusionReader(String content)
    {
        super(content);
    }
    
    public NeuroMLInclusionReader(File f)
    {
        super(f);
    }

    public void setIncludeConnectionsFromHDF5(boolean includeConnections)
    {
        this.includeConnectionsFromHDF5 = includeConnections;
    }
    
    
    
    @Override
    public String getRelativeContent(String attribute, String s) throws ContentError {
        
        if (attribute.equals(FILE) && s.endsWith(".h5"))
        {
            File hdf5File = getPossibleFile(s);
            try {
                if (fullFilePathsIncluded.contains(hdf5File.getCanonicalPath()))
                {
                    return "";
                }
                NeuroMLConverter nmlCon = new NeuroMLConverter();
                NeuroMLHDF5Reader h5Reader;
                h5Reader = new NeuroMLHDF5Reader();
                h5Reader.parse(hdf5File, this.includeConnectionsFromHDF5);
                
                NeuroMLDocument nmlDoc = h5Reader.getNeuroMLDocument();
                String xml = nmlCon.neuroml2ToXml(nmlDoc);
                fullFilePathsIncluded.add(hdf5File.getCanonicalPath());
                return xml;
            }
            catch (Exception ex) {
                throw new ContentError("Error reading HDF5 file: "+s+" ("+hdf5File.getAbsolutePath()+")",ex);
            }
        }
        else
        {
            String ss = super.getRelativeContent(attribute,s);
            return ss;
        }
    }
    
    
    @Override
	protected String insertIncludes(String stxta) throws ContentError {
		String stxt = removeXMLComments(stxta);
        stxt = stxt.replaceAll("></include>", "/>");
        stxt = stxt.replaceAll("<include  href", "<include href");
        
		StringBuilder sfullSB = new StringBuilder();
		String sinc = "<include href";
		while (true) {	
			int iinc = stxt.indexOf(sinc);
			if (iinc < 0) {
				break;
			} else {
				sfullSB.append(stxt.substring(0, iinc));
				int icb = stxt.indexOf("/>", iinc);
				String fullInclusion=stxt.substring(iinc + sinc.length(), icb);
				
		        if (fullInclusion.startsWith("xmlns=\"")){
		        	fullInclusion = fullInclusion.substring(fullInclusion.indexOf(" ")+1);
		        }

		        fullInclusion = fullInclusion.replace(" ", "");
				
				String attribute=fullInclusion.substring(0,fullInclusion.indexOf("="));
				String value = fullInclusion.substring(fullInclusion.indexOf("=")+1).replace("\"", "");

				sfullSB.append(getIncludeContent(attribute,value));
				stxt = stxt.substring(icb + 2, stxt.length());
			}
		
		}
        
		sfullSB.append(stxt);
		String interString = sfullSB.toString();
        
        return super.insertIncludes(interString);
	}
    
}
