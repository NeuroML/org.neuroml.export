/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.neuroml.export.neuron;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.neuroml.model.Cell;
import org.neuroml.model.Segment;
import org.neuroml.model.SegmentGroup;
import org.neuroml.model.util.CellUtils;

/**
 *
 * @author Padraig Gleeson
 */
public class NamingHelper {
    
    private final HashMap<String, LinkedHashMap<SegmentGroup, ArrayList<Integer>>> cellIdVsSegGroupInfo = new HashMap<String, LinkedHashMap<SegmentGroup, ArrayList<Integer>>>();
    private final HashMap<String, String> cachedSectionNames = new HashMap<String, String>();
    
    private final Cell cell;
    
    public NamingHelper(Cell cell) {
        this.cell = cell;
    }

    public String getNrnSectionName(Segment seg) {

        String uniqueId = cell.getId() + ":" + seg.getId();
        //System.out.println("uniqueId: "+uniqueId);
        //System.out.println("cachedSectionNames: "+cachedSectionNames);
        
        if (cachedSectionNames.containsKey(uniqueId)) {
            return cachedSectionNames.get(uniqueId);
        }

        //System.out.println("cellIdVsSegGroupInfo: "+cellIdVsSegGroupInfo);
        if (!cellIdVsSegGroupInfo.containsKey(cell.getId())) {
            LinkedHashMap<SegmentGroup, ArrayList<Integer>> sgVsSegIds = CellUtils.getSegmentGroupsVsSegIds(cell);
            cellIdVsSegGroupInfo.put(cell.getId(), sgVsSegIds);
        }
        
        LinkedHashMap<SegmentGroup, ArrayList<Integer>> sgVsSegIds = cellIdVsSegGroupInfo.get(cell.getId());
        
        //System.out.println("sgVsSegIds: "+sgVsSegIds);
        
        /*if (sgVsSegIds.isEmpty()) {
            String secName = (seg.getName()!=null && seg.getName().length()>0) ? seg.getName() : "section_"+seg.getId();
            cachedSectionNames.put(uniqueId, secName);
            return secName;
        }*/
        for (SegmentGroup grp : sgVsSegIds.keySet()) {
            if (sgVsSegIds.get(grp).contains(seg.getId())) {
                //System.out.println("ggg " + sgVsSegIds.get(grp));
                //System.out.println("ggg " + grp);
                if (CellUtils.isUnbranchedNonOverlapping(grp)) {
                    cachedSectionNames.put(uniqueId, grp.getId());
                    return grp.getId();
                } 
            }
        }
        String secName = (seg.getName()!=null && seg.getName().length()>0) ? seg.getName() : "section_"+seg.getId();
        cachedSectionNames.put(uniqueId, secName);
        return secName;
    }
    
}
