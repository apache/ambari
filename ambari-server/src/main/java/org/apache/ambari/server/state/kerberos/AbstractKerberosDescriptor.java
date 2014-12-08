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

package org.apache.ambari.server.state.kerberos;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.ambari.server.AmbariException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AbstractKerberosDescriptor is the base class for all Kerberos*Descriptor and associated classes.
 * <p/>
 * It provides storage and management for the parent and name values on behalf of implementing classes.
 * It also provides utility and helper methods.
 */
public abstract class AbstractKerberosDescriptor {

  /**
   * a regular expression Pattern used to find "variable" placeholders in strings
   */
  private static final Pattern PATTERN_VARIABLE = Pattern.compile("\\$\\{(?:(.+?)/)?(.+?)\\}");

  /**
   * An AbstractKerberosDescriptor serving as the parent (or container) for this
   * AbstractKerberosDescriptor.
   * <p/>
   * This value may be null in the event no parent has been identified.
   */
  private AbstractKerberosDescriptor parent = null;

  /**
   * A String declaring the name of this AbstractKerberosDescriptor.
   * <p/>
   * This value may be null in the event a name (or identifier) is not relevant.
   */
  private String name = null;

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
  public static String replaceVariables(String value, Map<String, Map<String, String>> replacementsMap) throws AmbariException {
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
   * Generates a Map of data that represents this AbstractKerberosDescriptor implementation.
   * <p/>
   * This method should be overwritten by AbstractKerberosDescriptor implementations to generate a
   * Map of data specific to it.
   * <p/>
   * It is not necessary to call this method from the overriding method.
   * <p/>
   * The map of data generated should be setup so that it can be fed back into the implementation
   * class to build a copy of it. For example:
   * <p/>
   * <pre>
   *  descriptor1 = AbstractKerberosDescriptorImpl(...)
   *  map = descriptor1.toMap()
   *  descriptor2 = AbstractKerberosDescriptor(map)
   *  descriptor1 should have the same data as descriptor2
   * </pre>
   *
   * @return a Map of date representing this AbstractKerberosDescriptor implementation
   */
  public Map<String, Object> toMap() {
    HashMap<String, Object> dataMap = new HashMap<String, Object>();
    String name = getName();

    if (name != null) {
      dataMap.put("name", name);
    }

    return dataMap;
  }

  /**
   * Returns the name of this descriptor
   *
   * @return a String indicating the name of this descriptor
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of this descriptor
   *
   * @param name a String indicating the name of this descriptor
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the parent (or container) of this descriptor
   *
   * @return an AbstractKerberosDescriptor representing the parent (or container) of this descriptor
   * or null if no parent was set
   */
  public AbstractKerberosDescriptor getParent() {
    return parent;
  }

  /**
   * Sets the parent (or container) of this descriptor
   *
   * @param parent an AbstractKerberosDescriptor representing the parent (or container) of this
   *               descriptor or null to clear the value
   */
  public void setParent(AbstractKerberosDescriptor parent) {
    this.parent = parent;
  }

  /**
   * Test this AbstractKerberosDescriptor to see if it is a container.
   * <p/>
   * The default implementation always returns false.  Implementing classes should override this
   * method to return something different, like true.
   *
   * @return true if this AbstractKerberosDescriptor is a container, false otherwise
   */
  public boolean isContainer() {
    return false;
  }

  /**
   * Parses a file containing JSON-formatted text into a (generic) Map.
   *
   * @param file a File containing the JSON-formatted text to parse
   * @return a Map of the data
   * @throws FileNotFoundException if the specified File does not point to a valid file
   * @throws IOException           if the specified File is not a readable file
   */
  protected static Map<String, Object> parseFile(File file) throws IOException {
    if (file == null) {
      return Collections.emptyMap();
    } else if (!file.isFile() || !file.canRead()) {
      throw new IOException(String.format("%s is not a readable file", file.getAbsolutePath()));
    } else {
      return new Gson().fromJson(new FileReader(file),
          new TypeToken<Map<String, Object>>() {
          }.getType());
    }
  }

  /**
   * Parses a JSON-formatted String into a (generic) Map.
   *
   * @param json a String containing the JSON-formatted text to parse
   * @return a Map of the data
   */
  protected static Map<String, Object> parseJSON(String json) {
    if ((json == null) || json.isEmpty()) {
      return Collections.emptyMap();
    } else {
      return new Gson().fromJson(json,
          new TypeToken<Map<String, Object>>() {
          }.getType());
    }
  }

  /**
   * Safely retrieves the requested value from the supplied Map
   *
   * @param map a Map containing the relevant data
   * @param key a String declaring the item to retrieve
   * @return an Object representing the requested data; or null if not found
   */
  protected static Object getValue(Map<?, ?> map, String key) {
    return ((map == null) || key == null) ? null : map.get(key);
  }

  /**
   * Safely retrieves the requested value (converted to a String) from the supplied Map
   * <p/>
   * The found value will be converted to a String using the {@link Object#toString()} method.
   *
   * @param map a Map containing the relevant data
   * @param key a String declaring the item to retrieve
   * @return a String representing the requested data; or null if not found
   */
  protected static String getStringValue(Map<?, ?> map, String key) {
    Object value = getValue(map, key);
    return (value == null) ? null : value.toString();
  }

  /**
   * Gets the requested AbstractKerberosDescriptor implementation using a type name and a relevant
   * descriptor name.
   * <p/>
   * Implementation classes should override this method to handle relevant descriptor types.
   *
   * @param type a String indicating the type of the requested descriptor
   * @param name a String indicating the name of the requested descriptor
   * @return a AbstractKerberosDescriptor representing the requested descriptor or null if not found
   */
  protected AbstractKerberosDescriptor getDescriptor(KerberosDescriptorType type, String name) {
    return null;
  }

  /**
   * Traverses up the hierarchy to find the "root" or "parent" container.
   * <p/>
   * The root AbstractKerberosDescriptor is the first descriptor in the hierarchy with a null parent.
   *
   * @return the AbstractKerberosDescriptor implementation that is found to be the root of the hierarchy.
   */
  protected AbstractKerberosDescriptor getRoot() {
    AbstractKerberosDescriptor root = this;

    while (root.getParent() != null) {
      root = root.getParent();
    }

    return root;
  }

  @Override
  public int hashCode() {
    return 37 *
        ((getName() == null)
            ? 0
            : getName().hashCode());
  }

  @Override
  public boolean equals(Object object) {
    if (object == null) {
      return false;
    } else if (object == this) {
      return true;
    } else if (object instanceof AbstractKerberosDescriptor) {
      AbstractKerberosDescriptor descriptor = (AbstractKerberosDescriptor) object;
      return (
          (getName() == null)
              ? (descriptor.getName() == null)
              : getName().equals(descriptor.getName())
      );
    } else {
      return false;
    }
  }
}
