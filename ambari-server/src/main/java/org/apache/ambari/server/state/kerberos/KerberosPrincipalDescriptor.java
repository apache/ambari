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

import java.util.HashMap;
import java.util.Map;

/**
 * KerberosPrincipalDescriptor is an implementation of an AbstractKerberosDescriptor that
 * encapsulates data related to a Kerberos principal.  This class is typically associated with the
 * KerberosKeytabDescriptor via a KerberosIdentityDescriptor.
 * <p/>
 * A KerberosPrincipalDescriptor has the following properties:
 * <ul>
 * <li>value</li>
 * <li>configuration</li>
 * </ul>
 * <p/>
 * The following JSON Schema will yield a valid KerberosPrincipalDescriptor
 * <pre>
 *   {
 *      "$schema": "http://json-schema.org/draft-04/schema#",
 *      "title": "KerberosIdentityDescriptor",
 *      "description": "Describes a Kerberos principal and associated details",
 *      "type": "object",
 *      "properties": {
 *        "value": {
 *          "description": "The pattern to use to generate the principal",
 *          "type": "string"
 *        },
 *        "configuration": {
 *          "description": "The configuration type and property name indicating the property to be
 *                          updated with the generated principal - format: config-type/property.name",
 *          "type": "string"
 *        }
 *      }
 *   }
 * </pre>
 * <p/>
 * In this implementation,
 * {@link org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptor#name} will hold the
 * KerberosPrincipalDescriptor#value value
 */
public class KerberosPrincipalDescriptor extends AbstractKerberosDescriptor {

  /**
   * A string declaring configuration type and property name indicating the property to be updated
   * with the generated principal
   * <p/>
   * This String is expected to be in the following format: configuration-type/property.name, where
   * <ul>
   * <li>configuration-type is the configuration file type where the property exists</li>
   * <li>property.value is the name of the relevant property within the configuration</li>
   * </ul>
   * <p/>
   * Example: hdfs-site/dfs.namenode.kerberos.principal
   */
  private String configuration;

  /**
   * Creates a new KerberosPrincipalDescriptor
   * <p/>
   * See {@link org.apache.ambari.server.state.kerberos.KerberosPrincipalDescriptor} for the JSON
   * Schema that may be used to generate this map.
   *
   * @param data a Map of values use to populate the data for the new instance
   * @see org.apache.ambari.server.state.kerberos.KerberosPrincipalDescriptor
   */
  public KerberosPrincipalDescriptor(Map<?, ?> data) {
    // The name for this KerberosPrincipalDescriptor is stored in the "value" entry in the map
    // This is not automatically set by the super classes.
    setName(getStringValue(data, "value"));

    setConfiguration(getStringValue(data, "configuration"));
  }

  /**
   * Gets the value (or principal name pattern) for this KerberosPrincipalDescriptor
   * <p/>
   * The value may include variable placeholders to be replaced as needed
   * <ul>
   * <li>
   * ${variable} placeholders are replaced on the server - see
   * {@link org.apache.ambari.server.state.kerberos.KerberosDescriptor#replaceVariables(String, java.util.Map)}
   * </li>
   * <li>the _HOST placeholder is replaced on the hosts to dynamically populate the relevant hostname</li>
   * </ul>
   *
   * @return a String declaring this principal's value
   * @see org.apache.ambari.server.state.kerberos.KerberosDescriptor#replaceVariables(String, java.util.Map)
   */
  public String getValue() {
    return getName();
  }

  /**
   * Sets the value (or principal name pattern) for this KerberosPrincipalDescriptor
   *
   * @param value a String declaring this principal's value
   * @see #getValue()
   */
  public void setValue(String value) {
    setName(value);
  }

  /**
   * Gets the configuration type and property name indicating the property to be updated with the
   * generated principal
   *
   * @return a String declaring the configuration type and property name indicating the property to
   * be updated with the generated principal
   * #see #configuration
   */
  public String getConfiguration() {
    return configuration;
  }

  /**
   * Sets the configuration type and property name indicating the property to be updated with the
   * generated principal
   *
   * @param configuration a String declaring the configuration type and property name indicating the
   *                      property to be updated with the generated principal
   * @see #configuration
   */
  public void setConfiguration(String configuration) {
    this.configuration = configuration;
  }

  /**
   * Updates this KerberosPrincipalDescriptor with data from another KerberosPrincipalDescriptor
   * <p/>
   * Properties will be updated if the relevant updated values are not null.
   *
   * @param updates the KerberosPrincipalDescriptor containing the updated values
   */
  public void update(KerberosPrincipalDescriptor updates) {
    if (updates != null) {
      String updatedValue;

      updatedValue = updates.getValue();
      if (updatedValue != null) {
        setValue(updatedValue);
      }

      updatedValue = updates.getConfiguration();
      if (updatedValue != null) {
        setConfiguration(updatedValue);
      }
    }
  }

  /**
   * Creates a Map of values that can be used to create a copy of this KerberosPrincipalDescriptor
   * or generate the JSON structure described in
   * {@link org.apache.ambari.server.state.kerberos.KerberosPrincipalDescriptor}
   *
   * @return a Map of values for this KerberosPrincipalDescriptor
   * @see org.apache.ambari.server.state.kerberos.KerberosPrincipalDescriptor
   */
  @Override
  public Map<String, Object> toMap() {
    Map<String, Object> map = new HashMap<String, Object>();

    map.put("value", getValue());
    map.put("configuration", getConfiguration());

    return map;
  }

  @Override
  public int hashCode() {
    return super.hashCode() +
        ((getConfiguration() == null)
            ? 0
            : getConfiguration().hashCode());
  }

  @Override
  public boolean equals(Object object) {
    if (object == null) {
      return false;
    } else if (object == this) {
      return true;
    } else if (object.getClass() == KerberosPrincipalDescriptor.class) {
      KerberosPrincipalDescriptor descriptor = (KerberosPrincipalDescriptor) object;
      return super.equals(object) &&
          (
              (getConfiguration() == null)
                  ? (descriptor.getConfiguration() == null)
                  : getConfiguration().equals(descriptor.getConfiguration())
          );
    } else {
      return false;
    }
  }
}
