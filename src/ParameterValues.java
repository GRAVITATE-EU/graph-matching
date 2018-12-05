/////////////////////////////////////////////////////////////////////////
//
// (c) Copyright University of Southampton IT Innovation, 2015
//
// Copyright in this software belongs to IT Innovation Centre of
// Gamma House, Enterprise Road, Southampton SO16 7NS, UK.
//
// This software may not be used, sold, licensed, transferred, copied
// or reproduced in whole or in part in any manner or form or in or
// on any media by any person other than in accordance with the terms
// of the Licence Agreement supplied with the software, or otherwise
// without the prior written consent of the copyright owners.
//
// This software is distributed WITHOUT ANY WARRANTY, without even the
// implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
// PURPOSE, except where stated in the Licence Agreement supplied with
// the software.
//
// Created By : Jessica Rosati
// Created Date : 2017/06/23
// Created for Project : GRAVITATE
//
/////////////////////////////////////////////////////////////////////////
//
// Dependencies : Source code derived from ITINNO copyright code in TRIDEC
//
/////////////////////////////////////////////////////////////////////////
package ITinnov.semantic_matching;

import java.io.*;
import java.util.Properties;

public class ParameterValues {

    /*
    Load parameters values from configuration file and call function to check integers values
    Input: a string representing the path of file with parameters
    */
    Properties loadParamsFromFile(String strFileParamsPath) {
        File configFile = new File(strFileParamsPath);
        try {
            InputStream inputStream = new FileInputStream(configFile);
            Properties props = new Properties();
            props.load(inputStream);
            inputStream.close();
            checkIntegerValues(props);
            return props;
        } catch (FileNotFoundException ex) {
            // file does not exist
            System.out.println("Configuration File not found");
            return null;
        } catch (IOException ex) {
            // I/O error
            System.out.println("I/O error while reading Configuration File");
            return null;
        }
    }

    /*
    Check integers values
    Input: java.util.Properties specification
    */
    private void checkIntegerValues(Properties props) {
    	String[] arrayOfIntegerProperties = {"timeLimitforImportQuery", "depthWalk", "numberWalks"};
        for (String strPropToCheck: arrayOfIntegerProperties) {
            try {
                Integer.valueOf(props.getProperty(strPropToCheck));
            }catch (NumberFormatException e){
                System.out.println("Parameter "+strPropToCheck + " is not a valid integer");
                System.out.println(e.getMessage());
                throw(e);
            }
        }
    }
}
