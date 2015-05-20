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

package org.apache.ambari.server.state.kerberos;

import com.google.inject.Singleton;
import org.apache.ambari.server.AmbariException;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class to provide variable replacement services
 */
@Singleton
public class VariableReplacementHelper {

  /**
   * a regular expression Pattern used to find "variable" placeholders in strings
   */
  private static final Pattern PATTERN_VARIABLE = Pattern.compile("\\$\\{(?:([\\w\\-\\.]+)/)?([\\w\\-\\.]+)(?:\\s*\\|\\s*(.+?))?\\}");

  /**
   * a regular expression Pattern used to parse "function" declarations: name(arg1, arg2, ...)
   */
  private static final Pattern PATTERN_FUNCTION = Pattern.compile("(\\w+)\\((.*?)\\)");

  /**
   * A map of "registered" functions
   */
  private static final Map<String, Function> FUNCTIONS = new HashMap<String, Function>() {
    {
      put("each", new EachFunction());
    }
  };

  /**
   * Performs variable replacement on the supplied String value using values from the replacementsMap.
   * <p/>
   * The value is a String containing one or more "variables" in the form of ${variable_name}, such
   * that "variable_name" may indicate a group identifier; else "" is used as the group.
   * For example:
   * <p/>
   * variable_name:  group: ""; property: "variable_name"
   * group1/variable_name:  group: "group1"; property: "variable_name"
   * root/group1/variable_name:  Not Supported
   * <p/>
   * The replacementsMap is a Map of Maps creating a (small) hierarchy of data to traverse in order
   * to resolve the variable.
   * <p/>
   * If a variable resolves to one or more variables, that new variable(s) will be processed and replaced.
   * If variable exists after a set number of iterations it is assumed that a cycle has been created
   * and the process will abort returning a String in a possibly unexpected state.
   *
   * @param value           a String containing zero or more variables to be replaced
   * @param replacementsMap a Map of data used to perform the variable replacements
   * @return a new String
   */
  public String replaceVariables(String value, Map<String, Map<String, String>> replacementsMap) throws AmbariException {
    if ((value != null) && (replacementsMap != null) && !replacementsMap.isEmpty()) {
      int count = 0; // Used to help prevent an infinite loop...
      boolean replacementPerformed;

      do {
        if (++count > 1000) {
          throw new AmbariException(String.format("Circular reference found while replacing variables in %s", value));
        }

        Matcher matcher = PATTERN_VARIABLE.matcher(value);
        StringBuffer sb = new StringBuffer();

        replacementPerformed = false;

        while (matcher.find()) {
          String type = matcher.group(1);
          String name = matcher.group(2);
          String function = matcher.group(3);

          Map<String, String> replacements;

          if ((name != null) && !name.isEmpty()) {
            if (type == null) {
              replacements = replacementsMap.get("");
            } else {
              replacements = replacementsMap.get(type);
            }

            if (replacements != null) {
              String replacement = replacements.get(name);

              if (replacement != null) {
                if (function != null) {
                  replacement = applyReplacementFunction(function, replacement);
                }

                // Escape '$' and '\' so they don't cause any issues.
                matcher.appendReplacement(sb, replacement.replace("\\", "\\\\").replace("$", "\\$"));
                replacementPerformed = true;
              }
            }
          }
        }

        matcher.appendTail(sb);
        value = sb.toString();
      }
      while (replacementPerformed); // Process the string again to make sure new variables were not introduced
    }

    return value;
  }

  /**
   * Applies the specified replacement function to the supplied data.
   * <p/>
   * The function must be in the following format:
   * <code>
   * name(arg1,arg2,arg3...)
   * </code>
   * <p/>
   * Commas in arguments should be escaped with a '/'.
   *
   * @param function    the name and arguments of the function
   * @param replacement the data to use in the function
   * @return a new string generated by appling the function
   */
  private String applyReplacementFunction(String function, String replacement) {
    if (function != null) {
      Matcher matcher = PATTERN_FUNCTION.matcher(function);

      if (matcher.matches()) {
        String name = matcher.group(1);

        if (name != null) {
          Function f = FUNCTIONS.get(name);

          if (f != null) {
            String args = matcher.group(2);
            String[] argsList = args.split("(?<!\\\\),");

            // Remove escape character from '\,'
            for (int i = 0; i < argsList.length; i++) {
              argsList[i] = argsList[i].trim().replace("\\,", ",");
            }

            return f.perform(argsList, replacement);
          }
        }
      }
    }

    return replacement;
  }

  /**
   * Function is the interface to be implemented by replacement functions.
   */
  private interface Function {
    /**
     * Perform the function to generate a new string by applying the logic of this function to the
     * supplied data.
     *
     * @param args an array of arguments, specific to the function
     * @param data the data to apply the function logic to
     * @return the resulting string
     */
    String perform(String[] args, String data);
  }

  /**
   * EachFunction is a Function implementation that iterates over a list of values pulled from a
   * delimited string to yield a new string.
   * <p/>
   * This function expects the following arguments (in order) within the args array:
   * <ol>
   * <li>pattern to use for each item, see {@link String#format(String, Object...)}</li>
   * <li>delimiter to use when concatenating the resolved pattern per item</li>
   * <li>regular expression used to split the original value</li>
   * </ol>
   */
  private static class EachFunction implements Function {
    @Override
    public String perform(String[] args, String data) {
      if ((args == null) || (args.length != 3)) {
        throw new IllegalArgumentException("Invalid number of arguments encountered");
      }

      if (data != null) {
        StringBuilder builder = new StringBuilder();

        String pattern = args[0];
        String concatDelimiter = args[1];
        String dataDelimiter = args[2];

        String[] items = data.split(dataDelimiter);

        for (String item : items) {
          if (builder.length() > 0) {
            builder.append(concatDelimiter);
          }

          builder.append(String.format(pattern, item));
        }

        return builder.toString();
      }

      return "";
    }
  }
}
