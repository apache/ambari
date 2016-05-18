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

package org.apache.ambari.view.hive.resources.uploads.parsers;

import org.apache.ambari.view.hive.client.ColumnDescription;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ParseUtils {

  final public static String[] DATE_FORMATS = {"mm/dd/yyyy", "dd/mm/yyyy", "mm-dd-yyyy" /*add more formatss*/};

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

  public static ColumnDescription.DataTypes detectHiveDataType(Object object) {
    // detect Integer
    if (isInteger(object)) return ColumnDescription.DataTypes.INT;
    if (isLong(object)) return ColumnDescription.DataTypes.BIGINT;
    if (isBoolean(object)) return ColumnDescription.DataTypes.BOOLEAN;
    if (isDouble(object)) return ColumnDescription.DataTypes.DOUBLE;
    if (isDate(object)) return ColumnDescription.DataTypes.DATE;
    if (isChar(object)) return ColumnDescription.DataTypes.CHAR;

    return ColumnDescription.DataTypes.STRING;
  }
}
