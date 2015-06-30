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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.StringUtils;
import org.apache.ambari.server.serveraction.upgrades.ConfigureAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ConfigureTask} represents a configuration change. This task can be
 * defined with conditional statements that will only set values if a condition
 * passes:
 * <p/>
 *
 * <pre>
 * {@code
 * <task xsi:type="configure">
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
 * </task>
 * }
 * </pre>
 *
 * It's also possible to simple set values directly without a precondition
 * check.
 *
 * <pre>
 * {@code
 * <task xsi:type="configure">
 *   <type>hive-site</type>
 *   <set key="hive.server2.thrift.port" value="10010"/>
 *   <set key="foo" value="bar"/>
 *   <set key="foobar" value="baz"/>
 * </task>
 * }
 * </pre>
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="configure")
public class ConfigureTask extends ServerSideActionTask {

  private static Logger LOG = LoggerFactory.getLogger(ConfigureTask.class);

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

  /**
   * Gson
   */
  private Gson m_gson = new Gson();

  /**
   * Constructor.
   *
   */
  public ConfigureTask() {
    implClass = ConfigureAction.class.getName();
  }

  @XmlTransient
  private Task.Type type = Task.Type.CONFIGURE;

  @XmlElement(name="type")
  private String configType;

  @XmlElement(name = "set")
  private List<ConfigurationKeyValue> keyValuePairs;

  @XmlElement(name = "condition")
  private List<Condition> conditions;

  @XmlElement(name = "transfer")
  private List<Transfer> transfers;

  @XmlElement(name="replace")
  private List<Replace> replacements;

  /**
   * {@inheritDoc}
   */
  @Override
  public Type getType() {
    return type;
  }

  /**
   * @return the config type
   */
  public String getConfigType() {
    return configType;
  }

  /**
   * Used for configuration updates that should mask their values from being
   * printed in plain text.
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Masked {
    @XmlAttribute(name = "mask")
    public boolean mask = false;
  }


  /**
   * A key/value pair to set in the type specified by {@link ConfigureTask#type}
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "set")
  public static class ConfigurationKeyValue extends Masked {
    @XmlAttribute(name = "key")
    public String key;

    @XmlAttribute(name = "value")
    public String value;
  }

  /**
   * A conditional element that will only perform the configuration if the
   * condition is met.
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "condition")
  public static class Condition {
    @XmlAttribute(name = "type")
    private String conditionConfigType;

    @XmlAttribute(name = "key")
    private String conditionKey;

    @XmlAttribute(name = "value")
    private String conditionValue;

    @XmlElement(name = "type")
    private String configType;

    @XmlElement(name = "key")
    private String key;

    @XmlElement(name = "value")
    private String value;
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

    // if the condition is true apply the transfer action
    // only supported conditional action is DELETE
    // if-type/if-key == if-value
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
     * The keys to keep when the action is {@link TransferOperation#DELETE}.
     */
    @XmlElement(name = "keep-key")
    public List<String> keepKeys = new ArrayList<String>();
  }

  /**
   * @return the list of transfers, checking for appropriate null fields.
   */
  public List<Transfer> getTransfers() {
    if (null == transfers) {
      return Collections.<Transfer>emptyList();
    }

    List<Transfer> list = new ArrayList<Transfer>();
    for (Transfer t : transfers) {
      switch (t.operation) {
        case COPY:
        case MOVE:
          if (null != t.fromKey && null != t.toKey) {
            list.add(t);
          }
          break;
        case DELETE:
          if (null != t.deleteKey) {
            list.add(t);
          }

          break;
      }
    }

    return list;
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
  }

  /**
   * @return the replacement tokens, never {@code null}
   */
  public List<Replace> getReplacements() {
    if (null == replacements) {
      return Collections.emptyList();
    }

    List<Replace> list = new ArrayList<Replace>();
    for (Replace r : replacements) {
      if (null == r.key || null == r.find || null == r.replaceWith) {
        continue;
      }
      list.add(r);
    }

    return list;
  }

  /**
   * Gets a map containing the following properties pertaining to the
   * configuration value to change:
   * <ul>
   * <li>{@link #PARAMETER_CONFIG_TYPE} - the configuration type (ie hdfs-site)</li>
   * <li>{@link #PARAMETER_KEY_VALUE_PAIRS} - key/value pairs for the
   * configurations</li>
   * <li>{@link #PARAMETER_KEY_VALUE_PAIRS} - key/value pairs for the
   * configurations</li>
   * <li>{@link #PARAMETER_TRANSFERS} - COPY/MOVE/DELETE changes</li>
   * <li>{@link #PARAMETER_REPLACEMENTS} - value replacements</li>
   * </ul>
   *
   * @param cluster
   *          the cluster to use when retrieving conditional properties to test
   *          against (not {@code null}).
   * @return the a map containing the changes to make. This could potentially be
   *         an empty map if no conditions are met. Callers should decide how to
   *         handle a configuration task that is unable to set any configuration
   *         values.
   */
  public Map<String, String> getConfigurationChanges(Cluster cluster) {
    Map<String, String> configParameters = new HashMap<String, String>();

    // the first matched condition will win; conditions make configuration tasks singular in
    // the properties that can be set - when there is a condition the task will only contain
    // conditions
    if( null != conditions && !conditions.isEmpty() ){
      for (Condition condition : conditions) {
        String conditionConfigType = condition.conditionConfigType;
        String conditionKey = condition.conditionKey;
        String conditionValue = condition.conditionValue;

        // always add the condition's target type just so that we have one to
        // return even if none of the conditions match
        configParameters.put(PARAMETER_CONFIG_TYPE, condition.configType);

        // check the condition; if it passes, set the configuration properties
        // and break
        String checkValue = getDesiredConfigurationValue(cluster,
            conditionConfigType, conditionKey);

        if (conditionValue.equals(checkValue)) {
          List<ConfigurationKeyValue> configurations = new ArrayList<ConfigurationKeyValue>(1);
          ConfigurationKeyValue keyValue = new ConfigurationKeyValue();
          keyValue.key = condition.key;
          keyValue.value = condition.value;
          configurations.add(keyValue);

          configParameters.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS,
              m_gson.toJson(configurations));

          return configParameters;
        }
      }
    }

    // this task is not a condition task, so process the other elements normally
    if (null != configType) {
      configParameters.put(PARAMETER_CONFIG_TYPE, configType);
    }

    // for every <set key=foo value=bar/> add it to this list
    if (null != keyValuePairs && !keyValuePairs.isEmpty()) {
      configParameters.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS,
          m_gson.toJson(keyValuePairs));
    }

    // transfers
    if (null != transfers && !transfers.isEmpty()) {

      List<Transfer> allowedTransfers = new ArrayList<Transfer>();
      for (Transfer transfer : transfers) {
        if (transfer.operation == TransferOperation.DELETE) {
          if (StringUtils.isNotBlank(transfer.ifKey) &&
              StringUtils.isNotBlank(transfer.ifType) &&
              transfer.ifValue != null) {

            String ifConfigType = transfer.ifType;
            String ifKey = transfer.ifKey;
            String ifValue = transfer.ifValue;

            String checkValue = getDesiredConfigurationValue(cluster, ifConfigType, ifKey);
            if (!ifValue.toLowerCase().equals(StringUtils.lowerCase(checkValue))) {
              // skip adding
              LOG.info("Skipping property delete for {}/{} as the value {} for {}/{} is not equal to {}",
                       this.getConfigType(), transfer.deleteKey, checkValue, ifConfigType, ifKey, ifValue);
              continue;
            }
          }
        }
        allowedTransfers.add(transfer);
      }
      configParameters.put(ConfigureTask.PARAMETER_TRANSFERS, m_gson.toJson(allowedTransfers));
    }

    // replacements
    if( null != replacements && !replacements.isEmpty() ){
      configParameters.put(ConfigureTask.PARAMETER_REPLACEMENTS, m_gson.toJson(replacements));
    }

    return configParameters;
  }

  /**
   * Gets the value of the specified cluster property.
   *
   * @param cluster
   *          the cluster (not {@code null}).
   * @param configType
   *          the configuration type (ie hdfs-site) (not {@code null}).
   * @param propertyKey
   *          the key to retrieve (not {@code null}).
   * @return the value or {@code null} if it does not exist.
   */
  private String getDesiredConfigurationValue(Cluster cluster,
      String configType, String propertyKey) {

    Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();
    DesiredConfig desiredConfig = desiredConfigs.get(configType);
    if (null == desiredConfig) {
      return null;
    }

    Config config = cluster.getConfig(configType, desiredConfig.getTag());
    if (null == config) {
      return null;
    }

    return config.getProperties().get(propertyKey);
  }

}