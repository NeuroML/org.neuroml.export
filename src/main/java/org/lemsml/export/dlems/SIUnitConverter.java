/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lemsml.export.dlems;

/**
 *
 * @author padraig
 */
public class SIUnitConverter implements UnitConverter
{
    
    @Override
    public float convert(float siValue, String dimensionName) 
    {
        System.out.println("in out: "+siValue);
        return siValue;
    }
}
