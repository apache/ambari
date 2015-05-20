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
 * KerberosKeytabDescriptor is an implementation of an AbstractKerberosDescriptor that
 * encapsulates data related to a Kerberos keytab file.  This class is typically associated with a
 * KerberosPrincipalDescriptor via a KerberosIdentityDescriptor.
 * <p/>
 * A KerberosKeytabDescriptor has the following properties:
 * <ul>
 * <li>file</li>
 * <li>owner {name, access}</li>
 * <li>group {name, access}</li>
 * <li>configuration</li>
 * <li>cachable</li>
 * </ul>
 * <p/>
 * The following JSON Schema will yield a valid KerberosPrincipalDescriptor
 * <pre>
 *   {
 *      "$schema": "http://json-schema.org/draft-04/schema#",
 *      "title": "KerberosKeytabDescriptor",
 *      "description": "Describes a Kerberos keytab file and associated details",
 *      "type": "object",
 *      "properties": {
 *        "file": {
 *          "description": "The absolute path for the keytab file",
 *          "type": "string"
 *        },
 *        "owner": {
 *          "description": "Details about the file's user ownership",
 *          "type": "object",
 *          "properties": {
 *            "name": {
 *              "description": "The local username that should be set as the owner of this file",
 *              "type": "string"
 *            },
 *            "access": {
 *              "description": "The relevant access permissions that should be set for the owner of
 *                              this file. Expected values are 'rw', 'r', ''"
 *              "type": "string"
 *            }
 *          }
 *        }
 *        "group": {
 *          "description": "Details about the file's group ownership",
 *          "type": "object",
 *          "properties": {
 *            "name": {
 *              "description": "The local group name that should be set as the group owner of this file",
 *              "type": "string"
 *            },
 *            "access": {
 *              "description": "The relevant access permissions that should be set for the group
 *                              owner of this file. Expected values are 'rw', 'r', ''"
 *              "type": "string"
 *            }
 *          }
 *        }
 *        "configuration": {
 *          "description": "The configuration type and property name indicating the property to be
 *                          updated with the generated absolute path to the keytab file
 *                          - format: config-type/property.name",
 *          "type": "string"
 *        }
 *        "cachable" : {
 *          "description": "Indicates whether the generated keytab is allowed to be cached by the
 *                          Ambari server (true) or not (false)",
 *          "type": "boolean"
 *        }
 *      }
 *   }
 * </pre>
 * <p/>
 * In this implementation,
 * {@link org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptor#name} will hold the
 * KerberosKeytabDescriptor#file value
 */
public class KerberosKeytabDescriptor extends AbstractKerberosDescriptor {

  /**
   * A String declaring the local username that should be set as the owner of the keytab file
   */
  private String ownerName = null;

  /**
   * A String declaring the access permissions that should be set on the keytab file related to the
   * owner.
   * <p/>
   * Expected values are:
   * <ul>
   * <li>"rw" - read/write</li>
   * <li>"r" - read-only</li>
   * <li>"" - no access</li>
   * </ul>
   */
  private String ownerAccess = null;

  /**
   * A String declaring the local groip name that should be set as the group owner of the keytab file
   */
  private String groupName = null;

  /**
   * A String declaring the access permissions that should be set on the keytab file related to the
   * group.
   * <p/>
   * Expected values are:
   * <ul>
   * <li>"rw" - read/write</li>
   * <li>"r" - read-only</li>
   * <li>"" - no access</li>
   * </ul>
   */
  private String groupAccess = null;

  /**
   * A string declaring configuration type and property name indicating the property to be updated
   * with the absolute path to the keytab file
   * <p/>
   * This String is expected to be in the following format: configuration-type/property.name, where
   * <ul>
   * <li>configuration-type is the configuration file type where the property exists</li>
   * <li>property.value is the name of the relevant property within the configuration</li>
   * </ul>
   * <p/>
   * Example: hdfs-site/dfs.namenode.keytab.file
   */
  private String configuration = null;


  /**
   * A boolean value indicating whether the generated keytab is allowed to be cached by the Ambari
   * server or not.
   */
  private boolean cachable = true;

  /**
   * Creates a new KerberosKeytabDescriptor
   *
   * @param file the path to the keytab file
   * @param ownerName the local username of the file owner
   * @param ownerAccess the file access privileges for the file owner ("r", "rw", "")
   * @param groupName the local group name with privileges to access the file
   * @param groupAccess the file access privileges for the group ("r", "rw", "")
   * @param configuration the configuration used to store the principal name
   * @param cachable true if the keytab may be cached by Ambari; otherwise false
   */
  public KerberosKeytabDescriptor(String file, String ownerName, String ownerAccess, String groupName,
                                  String groupAccess, String configuration, boolean cachable) {
    setName(file);
    setOwnerName(ownerName);
    setOwnerAccess(ownerAccess);
    setGroupName(groupName);
    setGroupAccess(groupAccess);
    setConfiguration(configuration);
    setCachable(cachable);
  }
  /**
   * Creates a new KerberosKeytabDescriptor
   * <p/>
   * See {@link org.apache.ambari.server.state.kerberos.KerberosKeytabDescriptor} for the JSON
   * Schema that may be used to generate this map.
   *
   * @param data a Map of values use to populate the data for the new instance
   * @see org.apache.ambari.server.state.kerberos.KerberosKeytabDescriptor
   */
  public KerberosKeytabDescriptor(Map<?, ?> data) {
    // The name for this KerberosKeytabDescriptor is stored in the "file" entry in the map
    // This is not automatically set by the super classes.
    setName(getStringValue(data, "file"));

    if (data != null) {
      Object object;

      object = data.get("owner");
      if (object instanceof Map) {
        Map<?, ?> map = (Map<?, ?>) object;
        setOwnerName(getStringValue(map, "name"));
        setOwnerAccess(getStringValue(map, "access"));
      }

      object = data.get("group");
      if (object instanceof Map) {
        Map<?, ?> map = (Map<?, ?>) object;
        setGroupName(getStringValue(map, "name"));
        setGroupAccess(getStringValue(map, "access"));
      }

      setConfiguration(getStringValue(data, "configuration"));

      // If the "cachable" value is anything but false, set it to true
      setCachable(!"false".equalsIgnoreCase(getStringValue(data, "cachable")));
    }
  }

  /**
   * Gets the path to the keytab file
   * <p/>
   * The value may include variable placeholders to be replaced as needed
   * <ul>
   * <li>
   * ${variable} placeholders are replaced on the server - see
   * {@link VariableReplacementHelper#replaceVariables(String, Map)}
   * </li>
   * </ul>
   *
   * @return a String declaring the keytab file's absolute path
   * @see VariableReplacementHelper#replaceVariables(String, Map)
   */
  public String getFile() {
    return getName();
  }

  /**
   * Sets the path to the keytab file
   *
   * @param file a String declaring this keytab's file path
   * @see #getFile()
   */
  public void setFile(String file) {
    setName(file);
  }

  /**
   * Gets the local username to set as the owner of the keytab file
   *
   * @return a String declaring the name of the user to own the keytab file
   */
  public String getOwnerName() {
    return ownerName;
  }

  /**
   * Sets the local username to set as the owner of the keytab file
   *
   * @param name a String declaring the name of the user to own the keytab file
   */
  public void setOwnerName(String name) {
    this.ownerName = name;
  }

  /**
   * Gets the access permissions that should be set on the keytab file related to the file's owner
   *
   * @return a String declaring the access permissions that should be set on the keytab file related
   * to the file's owner
   * @see #ownerAccess
   */
  public String getOwnerAccess() {
    return ownerAccess;
  }

  /**
   * Sets the access permissions that should be set on the keytab file related to the file's owner
   *
   * @param access a String declaring the access permissions that should be set on the keytab file
   *               related to the file's owner
   * @see #ownerAccess
   */
  public void setOwnerAccess(String access) {
    this.ownerAccess = access;
  }

  /**
   * Gets the local group name to set as the group owner of the keytab file
   *
   * @return a String declaring the name of the group to own the keytab file
   */
  public String getGroupName() {
    return groupName;
  }

  /**
   * Sets the local group name to set as the group owner of the keytab file
   *
   * @param name a String declaring the name of the group to own the keytab file
   */
  public void setGroupName(String name) {
    this.groupName = name;
  }

  /**
   * Gets the access permissions that should be set on the keytab file related to the file's group
   *
   * @return a String declaring the access permissions that should be set on the keytab file related
   * to the file's group
   * @see #groupAccess
   */
  public String getGroupAccess() {
    return groupAccess;
  }

  /**
   * Sets the access permissions that should be set on the keytab file related to the file's group
   *
   * @param access a String declaring the access permissions that should be set on the keytab file
   *               related to the file's group
   * @see #groupAccess
   */
  public void setGroupAccess(String access) {
    this.groupAccess = access;
  }

  /**
   * Gets the configuration type and property name indicating the property to be updated with the
   * keytab's file path
   *
   * @return a String declaring the configuration type and property name indicating the property to
   * be updated with the keytab's file path
   * #see #configuration
   */
  public String getConfiguration() {
    return configuration;
  }

  /**
   * Sets the configuration type and property name indicating the property to be updated with the
   * keytab's file path
   *
   * @param configuration a String declaring the configuration type and property name indicating the
   *                      property to be updated with the keytab's file path
   * @see #configuration
   */
  public void setConfiguration(String configuration) {
    this.configuration = configuration;
  }

  /**
   * Indicates whether the generated keytab is allowed to be cached by the Ambari server or not
   *
   * @return true if allowed to be cached; false otherwise
   */
  public boolean isCachable() {
    return cachable;
  }

  /**
   * Sets whether the generated keytab is allowed to be cached by the Ambari server or not
   *
   * @param cachable true if allowed to be cached; false otherwise
   */
  public void setCachable(boolean cachable) {
    this.cachable = cachable;
  }

  /**
   * Updates this KerberosKeytabDescriptor with data from another KerberosKeytabDescriptor
   * <p/>
   * Properties will be updated if the relevant updated values are not null.
   *
   * @param updates the KerberosKeytabDescriptor containing the updated values
   */
  public void update(KerberosKeytabDescriptor updates) {
    if (updates != null) {
      String updatedValue;

      updatedValue = updates.getFile();
      if (updatedValue != null) {
        setFile(updatedValue);
      }

      updatedValue = updates.getConfiguration();
      if (updatedValue != null) {
        setConfiguration(updatedValue);
      }

      updatedValue = updates.getOwnerName();
      if (updatedValue != null) {
        setOwnerName(updatedValue);
      }

      updatedValue = updates.getOwnerAccess();
      if (updatedValue != null) {
        setOwnerAccess(updatedValue);
      }

      updatedValue = updates.getGroupName();
      if (updatedValue != null) {
        setGroupName(updatedValue);
      }

      updatedValue = updates.getGroupAccess();
      if (updatedValue != null) {
        setGroupAccess(updatedValue);
      }
    }
  }

  /**
   * Creates a Map of values that can be used to create a copy of this KerberosKeytabDescriptor
   * or generate the JSON structure described in
   * {@link org.apache.ambari.server.state.kerberos.KerberosKeytabDescriptor}
   *
   * @return a Map of values for this KerberosKeytabDescriptor
   * @see org.apache.ambari.server.state.kerberos.KerberosKeytabDescriptor
   */
  @Override
  public Map<String, Object> toMap() {
    Map<String, Object> map = new HashMap<String, Object>();

    map.put("file", getFile());

    map.put("owner", new HashMap<String, Object>() {{
      put("name", getOwnerName());
      put("access", getOwnerAccess());
    }});

    map.put("group", new HashMap<String, Object>() {{
      put("name", getGroupName());
      put("access", getGroupAccess());
    }});

    map.put("configuration", getConfiguration());

    return map;
  }

  @Override
  public int hashCode() {
    return super.hashCode() +
        ((getConfiguration() == null)
            ? 0
            : getConfiguration().hashCode()) +
        ((getOwnerName() == null)
            ? 0
            : getOwnerName().hashCode()) +
        ((getOwnerAccess() == null)
            ? 0
            : getOwnerAccess().hashCode()) +
        ((getGroupName() == null)
            ? 0
            : getGroupName().hashCode()) +
        ((getGroupAccess() == null)
            ? 0
            : getGroupAccess().hashCode()) +
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
    } else if (object.getClass() == KerberosKeytabDescriptor.class) {
      KerberosKeytabDescriptor descriptor = (KerberosKeytabDescriptor) object;
      return super.equals(object) &&
          (
              (getConfiguration() == null)
                  ? (descriptor.getConfiguration() == null)
                  : getConfiguration().equals(descriptor.getConfiguration())
          ) &&
          (
              (getOwnerName() == null)
                  ? (descriptor.getOwnerName() == null)
                  : getOwnerName().equals(descriptor.getOwnerName())
          ) &&
          (
              (getOwnerAccess() == null)
                  ? (descriptor.getOwnerAccess() == null)
                  : getOwnerAccess().equals(descriptor.getOwnerAccess())
          ) &&
          (
              (getGroupName() == null)
                  ? (descriptor.getGroupName() == null)
                  : getGroupName().equals(descriptor.getGroupName())
          ) &&
          (
              (getGroupAccess() == null)
                  ? (descriptor.getGroupAccess() == null)
                  : getGroupAccess().equals(descriptor.getGroupAccess())
          );
    } else {
      return false;
    }
  }
}
