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
public interface UnitConverter
{
    double convert(double siValue, String dimensionName);
}
