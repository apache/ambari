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
package org.apache.ambari.server.state.stack.upgrade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link ConfigUpgradeChangeDefinition} represents a configuration change. This change can be
 * defined with conditional statements that will only set values if a condition
 * passes:
 * <p/>
 *
 * <pre>
 * {@code
 * <definition>
 *   <condition type="hive-site" key="hive.server2.transport.mode" value="binary">
 *     <type>hive-site</type>
 *     <key>hive.server2.thrift.port</key>
 *     <value>10010</value>
 *   </condition>
 *   <condition type="hive-site" key="hive.server2.transport.mode" value="http">
 *     <type>hive-site</type>
 *     <key>hive.server2.http.port</key>
 *     <value>10011</value>
 *   </condition>
 * </definition>
 * }
 * </pre>
 *
 * It's also possible to simple set values directly without a precondition
 * check.
 *
 * <pre>
 * {@code
 * <definition xsi:type="configure">
 *   <type>hive-site</type>
 *   <set key="hive.server2.thrift.port" value="10010"/>
 *   <set key="foo" value="bar"/>
 *   <set key="foobar" value="baz"/>
 * </definition>
 * }
 * </pre>
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ConfigUpgradeChangeDefinition {

  private static Logger LOG = LoggerFactory.getLogger(ConfigUpgradeChangeDefinition.class);

  /**
   * The key that represents the configuration type to change (ie hdfs-site).
   */
  public static final String PARAMETER_CONFIG_TYPE = "configure-task-config-type";

  /**
   * Setting key/value pairs can be several per task, so they're passed in as a
   * json-ified list of objects.
   */
  public static final String PARAMETER_KEY_VALUE_PAIRS = "configure-task-key-value-pairs";

  /**
   * Transfers can be several per task, so they're passed in as a json-ified
   * list of objects.
   */
  public static final String PARAMETER_TRANSFERS = "configure-task-transfers";

  /**
   * Replacements can be several per task, so they're passed in as a json-ified list of
   * objects.
   */
  public static final String PARAMETER_REPLACEMENTS = "configure-task-replacements";

  public static final String actionVerb = "Configuring";

  public static final Float DEFAULT_PRIORITY = 1.0f;

  /**
   * Gson
   */
  private Gson m_gson = new Gson();

  /**
   * An optional brief description of config changes.
   */
  @XmlAttribute(name = "summary")
  public String summary;

  @XmlAttribute(name = "id", required = true)
  public String id;

  @XmlElement(name="type")
  private String configType;

  @XmlElement(name = "set")
  private List<ConfigurationKeyValue> keyValuePairs;

  @XmlElement(name = "transfer")
  private List<Transfer> transfers;

  @XmlElement(name="replace")
  private List<Replace> replacements;

  /**
   * @return the config type
   */
  public String getConfigType() {
    return configType;
  }

  /**
   * @return the list of <set key=foo value=bar/> items
   */
  public List<ConfigurationKeyValue> getKeyValuePairs() {
    return keyValuePairs;
  }

  /**
   * @return the list of transfers, checking for appropriate null fields.
   */
  public List<Transfer> getTransfers() {
    if (null == transfers) {
      return Collections.emptyList();
    }

    List<Transfer> list = new ArrayList<>();
    for (Transfer t : transfers) {
      switch (t.operation) {
        case COPY:
        case MOVE:
          if (null != t.fromKey && null != t.toKey) {
            list.add(t);
          } else {
            LOG.warn(String.format("Transfer %s is invalid", t));
          }
          break;
        case DELETE:
          if (null != t.deleteKey) {
            list.add(t);
          } else {
            LOG.warn(String.format("Transfer %s is invalid", t));
          }

          break;
      }
    }

    return list;
  }

  /**
   * @return the replacement tokens, never {@code null}
   */
  public List<Replace> getReplacements() {
    if (null == replacements) {
      return Collections.emptyList();
    }

    List<Replace> list = new ArrayList<>();
    for (Replace r : replacements) {
      if (null == r.key || null == r.find || null == r.replaceWith) {
        LOG.warn(String.format("Replacement %s is invalid", r));
        continue;
      }
      list.add(r);
    }

    return list;
  }

  /**
   * Used for configuration updates that should mask their values from being
   * printed in plain text.
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Masked {
    @XmlAttribute(name = "mask")
    public boolean mask = false;

    /**
     * The key to read for the if condition.
     */
    @XmlAttribute(name = "if-key")
    public String ifKey;

    /**
     * The config type to read for the if condition.
     */
    @XmlAttribute(name = "if-type")
    public String ifType;

    /**
     * The property value to compare against for the if condition.
     */
    @XmlAttribute(name = "if-value")
    public String ifValue;

    /**
     * The property key state for the if condition
     */
    @XmlAttribute(name = "if-key-state")
    public PropertyKeyState ifKeyState;
  }


  /**
   * A key/value pair to set in the type specified by {@link ConfigUpgradeChangeDefinition#configType}
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "set")
  public static class ConfigurationKeyValue extends Masked {
    @XmlAttribute(name = "key")
    public String key;

    @XmlAttribute(name = "value")
    public String value;

    @Override
    public String toString() {
      return "Set{" +
              ", key='" + key + '\'' +
              ", value='" + value + '\'' +
              ", ifKey='" + ifKey + '\'' +
              ", ifType='" + ifType + '\'' +
              ", ifValue='" + ifValue + '\'' +
              ", ifKeyState='" + ifKeyState + '\'' +
              '}';
    }
  }

  /**
   * A {@code transfer} element will copy, move, or delete the value of one type/key to another type/key.
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "transfer")
  public static class Transfer extends Masked {
    /**
     * The type of operation, such as COPY or DELETE.
     */
    @XmlAttribute(name = "operation")
    public TransferOperation operation;

    /**
     * The configuration type to copy or move from.
     */
    @XmlAttribute(name = "from-type")
    public String fromType;

    /**
     * The key to copy or move the configuration from.
     */
    @XmlAttribute(name = "from-key")
    public String fromKey;

    /**
     * The key to copy the configuration value to.
     */
    @XmlAttribute(name = "to-key")
    public String toKey;

    /**
     * The configuration key to delete, or "*" for all.
     */
    @XmlAttribute(name = "delete-key")
    public String deleteKey;

    /**
     * If {@code true}, this will ensure that any changed properties are not
     * removed during a {@link TransferOperation#DELETE}.
     */
    @XmlAttribute(name = "preserve-edits")
    public boolean preserveEdits = false;

    /**
     * A default value to use when the configurations don't contain the
     * {@link #fromKey}.
     */
    @XmlAttribute(name = "default-value")
    public String defaultValue;

    /**
     * A data type to convert the configuration value to when the action is
     * {@link TransferOperation#COPY}.
     */
    @XmlAttribute(name = "coerce-to")
    public TransferCoercionType coerceTo;

    /**
     * The keys to keep when the action is {@link TransferOperation#DELETE}.
     */
    @XmlElement(name = "keep-key")
    public List<String> keepKeys = new ArrayList<String>();


    @Override
    public String toString() {
      return "Transfer{" +
              "operation=" + operation +
              ", fromType='" + fromType + '\'' +
              ", fromKey='" + fromKey + '\'' +
              ", toKey='" + toKey + '\'' +
              ", deleteKey='" + deleteKey + '\'' +
              ", preserveEdits=" + preserveEdits +
              ", defaultValue='" + defaultValue + '\'' +
              ", coerceTo=" + coerceTo +
              ", ifKey='" + ifKey + '\'' +
              ", ifType='" + ifType + '\'' +
              ", ifValue='" + ifValue + '\'' +
              ", ifKeyState='" + ifKeyState + '\'' +
              ", keepKeys=" + keepKeys +
              '}';
    }
  }

  /**
   * Used to replace strings in a key with other strings.  More complex
   * scenarios will be possible with regex (when needed)
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "replace")
  public static class Replace extends Masked {
    /**
     * The key name
     */
    @XmlAttribute(name="key")
    public String key;

    /**
     * The string to find
     */
    @XmlAttribute(name="find")
    public String find;

    /**
     * The string to replace
     */
    @XmlAttribute(name="replace-with")
    public String replaceWith;

    @Override
    public String toString() {
      return "Replace{" +
              "key='" + key + '\'' +
              ", find='" + find + '\'' +
              ", replaceWith='" + replaceWith + '\'' +
              ", ifKey='" + ifKey + '\'' +
              ", ifType='" + ifType + '\'' +
              ", ifValue='" + ifValue + '\'' +
              ", ifKeyState='" + ifKeyState + '\'' +
              '}';
    }
  }
}