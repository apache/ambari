/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.apache.ambari.view.hive20.internal.parsers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParserUtils {

  public static final String DATA_TYPE_REGEX = "\\s*([^() ]+)\\s*(\\s*\\(\\s*([0-9]+)\\s*(\\s*,\\s*([0-9]+))?\\s*\\)\\s*)?\\s*";

  /**
   * @param columnDataTypeString : the string that needs to be parsed as a datatype example : decimal(10,3)
   * @return a list of string containing type, precision and scale in that order if present, null otherwise
   */
  public static List<String> parseColumnDataType(String columnDataTypeString) {
    List<String> typePrecisionScale = new ArrayList<>(3);

    Pattern pattern = Pattern.compile(DATA_TYPE_REGEX);
    Matcher matcher = pattern.matcher(columnDataTypeString);

    if (matcher.find()) {
      typePrecisionScale.add(matcher.group(1));
      typePrecisionScale.add(matcher.group(3));
      typePrecisionScale.add(matcher.group(5));
    }else{
      typePrecisionScale.add(null);
      typePrecisionScale.add(null);
      typePrecisionScale.add(null);
    }

    return typePrecisionScale;
  }
}
