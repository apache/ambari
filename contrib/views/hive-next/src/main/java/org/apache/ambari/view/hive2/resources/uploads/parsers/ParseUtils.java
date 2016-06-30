/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive2.resources.uploads.parsers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.apache.ambari.view.hive2.client.ColumnDescription.DataTypes;

public class ParseUtils {

  protected final static Logger LOG =
    LoggerFactory.getLogger(ParseUtils.class);

  final public static String[] DATE_FORMATS = {"mm/dd/yyyy", "dd/mm/yyyy", "mm-dd-yyyy" /*add more formatss*/};

  final public static DataTypes [] dataTypeList = {DataTypes.BOOLEAN,DataTypes.INT,DataTypes.BIGINT,DataTypes.DOUBLE,DataTypes.CHAR,DataTypes.DATE,DataTypes.STRING};

  public static boolean isInteger(Object object) {
    if (object == null)
      return false;

    if (object instanceof Integer)
      return true;

    try {
      Integer i = Integer.parseInt(object.toString());
      return true;
    } catch (NumberFormatException nfe) {
      return false;
    }
  }

  public static boolean isBoolean(Object object) {
    if (object == null)
      return false;

    if (object instanceof Boolean)
      return true;

    String strValue = object.toString();
    if (strValue.equalsIgnoreCase("true") || strValue.equalsIgnoreCase("false"))
      return true;
    else
      return false;
  }

  public static boolean isString(Object object) {
    if (object == null)
      return false;
    else return true; // any non null can always be interpreted as a string
  }

  public static boolean isLong(Object object) {
    if (object == null)
      return false;

    if (object instanceof Long)
      return true;

    try {
      Long i = Long.parseLong(object.toString());
      return true;
    } catch (Exception nfe) {
      return false;
    }
  }

  public static boolean isDouble(Object object) {
    if (object == null)
      return false;

    if (object instanceof Double)
      return true;

    try {
      Double i = Double.parseDouble(object.toString());
      return true;
    } catch (Exception nfe) {
      return false;
    }
  }

  public static boolean isChar(Object object) {
    if (object == null)
      return false;

    if (object instanceof Character)
      return true;

    String str = object.toString().trim();
    if (str.length() == 1)
      return true;

    return false;
  }

  public static boolean isDate(Object object) {
    if (object == null)
      return false;

    if (object instanceof Date)
      return true;

    String str = object.toString();
    for (String format : DATE_FORMATS) {
      try {
        Date i = new SimpleDateFormat(format).parse(str);
        return true;
      } catch (Exception e) {
      }
    }

    return false;
  }

  public static DataTypes detectHiveDataType(Object object) {
    // detect Integer
    if (isBoolean(object)) return DataTypes.BOOLEAN;
    if (isInteger(object)) return DataTypes.INT;
    if (isLong(object)) return DataTypes.BIGINT;
    if (isDouble(object)) return DataTypes.DOUBLE;
    if (isChar(object)) return DataTypes.CHAR;
    if (isDate(object)) return DataTypes.DATE;

    return DataTypes.STRING;
  }

  public static boolean checkDatatype( Object object, DataTypes datatype){
    switch(datatype){

      case BOOLEAN :
        return isBoolean(object);
      case INT :
        return isInteger(object);
      case BIGINT :
        return isLong(object);
      case DOUBLE:
        return isDouble(object);
      case CHAR:
        return isChar(object);
      case DATE:
        return isDate(object);
      case STRING:
        return isString(object);

      default:
        LOG.error("this datatype detection is not supported : {}", datatype);
        return false;
    }
  }

  public static DataTypes detectHiveColumnDataType(List<Object> colValues) {
    boolean found = true;
    for(DataTypes datatype : dataTypeList){
      found = true;
      for(Object object : colValues){
        if(!checkDatatype(object,datatype)){
          found = false;
          break;
        }
      }

      if(found) return datatype;
    }

    return DataTypes.STRING; //default
  }
}
