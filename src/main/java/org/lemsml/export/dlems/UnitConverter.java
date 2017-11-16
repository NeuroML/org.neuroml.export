/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lemsml.export.dlems;

import org.lemsml.jlems.core.sim.LEMSException;

/**
 *
 * @author padraig
 */
public interface UnitConverter
{
    float convert(float siValue, String dimensionName) throws LEMSException;
}
