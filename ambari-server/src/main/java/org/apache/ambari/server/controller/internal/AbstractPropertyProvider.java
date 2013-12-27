/**
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

package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Abstract property provider implementation.
 */
public abstract class AbstractPropertyProvider extends BaseProvider implements PropertyProvider {

  /**
   * The property/metric information for this provider keyed by component name / property id.
   */
  private final Map<String, Map<String, PropertyInfo>> componentMetrics;

  /**
   * Regular expression for checking a property id for a metric argument with methods.
   * (e.g. metrics/yarn/Queue$1.replaceAll(\",q(\\d+)=\",\"/\")/AppsRunning)
   */
  private static final Pattern CHECK_FOR_METRIC_ARGUMENT_METHODS_REGEX = Pattern.compile("(\\$\\d\\.[^\\$]+\\))+");

  /**
   * Regular expression for extracting the methods from a metric argument.
   * (e.g. $1.replaceAll(\",q(\\d+)=\",\"/\"))
   */
  private static final Pattern FIND_ARGUMENT_METHOD_REGEX = Pattern.compile(".\\w+\\(.*?\\)");

  /**
   * Regular expression for extracting the arguments for methods from a metric argument.
   * Only strings and integers are supported.
   */
  private static final Pattern FIND_ARGUMENT_METHOD_ARGUMENTS_REGEX = Pattern.compile("\".*?\"|[0-9]+");

  /**
   * Supported any regex inside ()
   */
  private static final String FIND_REGEX_IN_METRIC_REGEX = "\\([^)]+\\)";

  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a provider.
   *
   * @param componentMetrics map of metrics for this provider
   */
  public AbstractPropertyProvider(Map<String, Map<String, PropertyInfo>> componentMetrics) {
    super(PropertyHelper.getPropertyIds(componentMetrics));
    this.componentMetrics = componentMetrics;
  }


  // ----- accessors ---------------------------------------------------------

  /**
   * Get the map of metrics for this provider.
   *
   * @return the map of metric / property info.
   */
  public Map<String, Map<String, PropertyInfo>> getComponentMetrics() {
    return componentMetrics;
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Get a map of metric / property info based on the given component name and property id.
   * Note that the property id may map to multiple metrics if the property id is a category.
   *
   * @param componentName  the component name
   * @param propertyId     the property id; may be a category
   *
   * @return a map of metrics
   */
  protected Map<String, PropertyInfo> getPropertyInfoMap(String componentName, String propertyId) {
    Map<String, PropertyInfo> propertyInfoMap = new HashMap<String, PropertyInfo>();

    getPropertyInfoMap(componentName, propertyId, propertyInfoMap);

    return propertyInfoMap;
  }

  protected void getPropertyInfoMap(String componentName, String propertyId, Map<String, PropertyInfo> propertyInfoMap) {
    Map<String, PropertyInfo> componentMetricMap = componentMetrics.get(componentName);

    propertyInfoMap.clear();

    if (componentMetricMap == null) {
      return;
    }

    PropertyInfo propertyInfo = componentMetricMap.get(propertyId);
    if (propertyInfo != null) {
      propertyInfoMap.put(propertyId, propertyInfo);
      return;
    }

    String regExpKey = getRegExpKey(propertyId);

    if (regExpKey != null) {
      propertyInfo = componentMetricMap.get(regExpKey);
      if (propertyInfo != null) {
        propertyInfoMap.put(regExpKey, propertyInfo);
        return;
      }
    }

    if (!propertyId.endsWith("/")){
      propertyId += "/";
    }

    for (Map.Entry<String, PropertyInfo> entry : componentMetricMap.entrySet()) {
      if (entry.getKey().startsWith(propertyId)) {
        String key = entry.getKey();
        propertyInfoMap.put(key, entry.getValue());
      }
    }

    if (regExpKey != null) {
      if (!regExpKey.endsWith("/")){
        regExpKey += "/";
      }

      
      for (Map.Entry<String, PropertyInfo> entry : componentMetricMap.entrySet()) {
        if (entry.getKey().startsWith(regExpKey)) {
          propertyInfoMap.put(entry.getKey(), entry.getValue());
        }
      }
    }

    return;
  }

  /**
   * Substitute the given value into the argument in the given property id.  If there are methods attached
   * to the argument then execute them for the given value.
   *
   * @param propertyId  the property id
   * @param argName     the argument name
   * @param val         the value to substitute
   *
   * @return the modified property id
   */
  protected static String substituteArgument(String propertyId, String argName, String val) {

    // find the argument in the property id
    int argStart = propertyId.indexOf(argName);

    if (argStart > -1) {
      // get the string segment starting with the given argument
      String argSegment = propertyId.substring(argStart);

      // check to see if there are any methods attached to the argument
      Matcher matcher = CHECK_FOR_METRIC_ARGUMENT_METHODS_REGEX.matcher(argSegment);
      if (matcher.find()) {

        // expand the argument name to include its methods
        argName = argSegment.substring(matcher.start(), matcher.end());

        // for each method attached to the argument ...
        matcher = FIND_ARGUMENT_METHOD_REGEX.matcher(argName);
        while (matcher.find()) {
          // find the end of the method
          int openParenIndex  = argName.indexOf('(', matcher.start());
          int closeParenIndex = indexOfClosingParenthesis(argName, openParenIndex);

          String methodName = argName.substring(matcher.start() + 1, openParenIndex);
          String args       = argName.substring(openParenIndex + 1, closeParenIndex);

          List<Object>   argList    = new LinkedList<Object>();
          List<Class<?>> paramTypes = new LinkedList<Class<?>>();

          // for each argument of the method ...
          Matcher argMatcher = FIND_ARGUMENT_METHOD_ARGUMENTS_REGEX.matcher(args);
          while (argMatcher.find()) {
            addArgument(args, argMatcher.start(), argMatcher.end(), argList, paramTypes);
          }

          try {
            val = invokeArgumentMethod(val, methodName, argList, paramTypes);
          } catch (Exception e) {
            throw new IllegalArgumentException("Can't apply method " + methodName + " for argument " +
                argName + " in " + propertyId, e);
          }
        }
      }
      // Do the substitution
      return propertyId.replace(argName, val);
    }
    throw new IllegalArgumentException("Can't substitute " + val + "  for argument " + argName + " in " + propertyId);
  }

  /**
   * Find the index of the closing parenthesis in the given string.
   */
  private static int indexOfClosingParenthesis(String s, int index) {
    int depth  = 0;
    int length = s.length();

    while (index < length) {
      char c = s.charAt(index++);
      if (c == '(') {
        ++depth;
      } else if (c == ')') {
        if (--depth ==0 ){
         return index;
        }
      }
    }
    return -1;
  }

  /**
   * Extract an argument from the given string and add it to the given arg and param type collections.
   */
  private static void addArgument(String args, int start, int end, List<Object> argList, List<Class<?>> paramTypes) {
    String arg = args.substring(start, end);

    // only supports strings and integers
    if (arg.contains("\"")) {
      argList.add(arg.substring(1, arg.length() -1));
      paramTypes.add(String.class);
    } else {
      Integer number = Integer.parseInt(arg);
      argList.add(number);
      paramTypes.add(Integer.TYPE);
    }
  }

  /**
   * Invoke a method on the given argument string with the given parameters.
   */
  private static String invokeArgumentMethod(String argValue, String methodName, List<Object> argList,
                                             List<Class<?>> paramTypes)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    // invoke the method through reflection
    Method method = String.class.getMethod(methodName, paramTypes.toArray(new Class<?>[paramTypes.size()]));

    return (String) method.invoke(argValue, argList.toArray(new Object[argList.size()]));
  }

  /**
   * Adds to the componentMetricMap a specific(not regexp)
   * metric for the propertyId
   *
   * @param componentMetricMap
   * @param propertyId
   */
  protected void updateComponentMetricMap(
    Map<String, PropertyInfo> componentMetricMap, String propertyId) {

    String regExpKey = getRegExpKey(propertyId);


    if (!componentMetricMap.containsKey(propertyId) && regExpKey != null &&
      !regExpKey.equals(propertyId)) {

      PropertyInfo propertyInfo = componentMetricMap.get(regExpKey);
      if (propertyInfo != null) {
        List<String> regexGroups = getRegexGroups(regExpKey, propertyId);
        String key = propertyInfo.getPropertyId();
        for (String regexGroup : regexGroups) {
          regexGroup = regexGroup.replace("/", ".");
          key = key.replaceFirst(FIND_REGEX_IN_METRIC_REGEX, regexGroup);
        }
        componentMetricMap.put(propertyId, new PropertyInfo(key,
          propertyInfo.isTemporal(), propertyInfo.isPointInTime()));
      }

    }
  }
}
