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
package org.apache.ambari.logfeeder.util;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class PlaceholderUtil {
  private PlaceholderUtil() {
    throw new UnsupportedOperationException();
  }
  
  private static final Pattern placeHolderPattern = Pattern.compile("\\$\\s*(\\w+)");

  public static String replaceVariables(String inputStr, HashMap<String, String> contextParam) {
    Matcher m = placeHolderPattern.matcher(inputStr);
    String output = new String(inputStr);
    while (m.find()) {
      String placeholder = m.group();
      if (placeholder != null && !placeholder.isEmpty()) {
        String key = placeholder.replace("$","").toLowerCase();// remove brace
        String replacement = getFromContext(contextParam, placeholder, key);
        output = output.replace(placeholder, replacement);
      }
    }
    return output;
  }

  private static String getFromContext(HashMap<String, String> contextParam, String defaultValue, String key) {
    String returnValue = defaultValue; // by default set default value as a return
    if (contextParam != null) {
      String value = contextParam.get(key);
      if (StringUtils.isNotBlank(value)) {
        returnValue = value;
      }
    }
    return returnValue;
  }
}
