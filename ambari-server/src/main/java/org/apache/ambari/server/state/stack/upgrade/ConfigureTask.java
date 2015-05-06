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

import org.apache.ambari.server.serveraction.upgrades.ConfigureAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;

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
 * It's also possible to simple set a value directly without a precondition
 * check.
 *
 * <pre>
 * {@code
 * <task xsi:type="configure">
 *   <type>hive-site</type>
 *   <key>hive.server2.thrift.port</key>
 *   <value>10010</value>
 * </task>
 * }
 * </pre>
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="configure")
public class ConfigureTask extends ServerSideActionTask {

  /**
   * The key that represents the configuration type to change (ie hdfs-site).
   */
  public static final String PARAMETER_CONFIG_TYPE = "configure-task-config-type";

  /**
   * The key that represents the configuration key to change (ie
   * hive.server2.thrift.port)
   */
  public static final String PARAMETER_KEY = "configure-task-key";

  /**
   * The key that represents the configuration value to set on
   * {@link #PARAMETER_CONFIG_TYPE}/{@link #PARAMETER_KEY}.
   */
  public static final String PARAMETER_VALUE = "configure-task-value";

  /**
   * Transfers can be several per task, so they're passed in as a json-ified list of
   * objects.
   */
  public static final String PARAMETER_TRANSFERS = "configure-task-transfers";

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

  @XmlElement(name="key")
  private String key;

  @XmlElement(name="value")
  private String value;

  @XmlElement(name="summary")
  public String summary;

  @XmlElement(name = "condition")
  private List<Condition> conditions;

  @XmlElement(name = "transfer")
  private List<Transfer> transfers;

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
  public static class Transfer {
    @XmlAttribute(name = "operation")
    public TransferOperation operation;

    @XmlAttribute(name = "from-type")
    public String fromType;

    @XmlAttribute(name = "from-key")
    public String fromKey;

    @XmlAttribute(name = "to-key")
    public String toKey;

    @XmlAttribute(name = "delete-key")
    public String deleteKey;

    @XmlAttribute(name = "preserve-edits")
    public boolean preserveEdits = false;

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
   * Gets a map containing the following properties pertaining to the
   * configuration value to change:
   * <ul>
   * <li>type - the configuration type (ie hdfs-site)</li>
   * <li>key - the lookup key for the type</li>
   * <li>value - the value to set the key to</li>
   * </ul>
   *
   * @param cluster
   *          the cluster to use when retrieving conditional properties to test
   *          against (not {@code null}).
   * @return the a map containing the configuration type, key, and value to use
   *         when updating a configuration property (never {@code null).
   *         This could potentially be an empty map if no conditions are
   *         met. Callers should decide how to handle a configuration task
   *         that is unable to set any configuration values.
   */
  public Map<String, String> getConfigurationProperties(Cluster cluster) {
    Map<String, String> configParameters = new HashMap<String, String>();

    // if there are no conditions then just take the direct values
    if (null == conditions || conditions.isEmpty()) {
      configParameters.put(PARAMETER_CONFIG_TYPE, configType);
      configParameters.put(PARAMETER_KEY, key);
      configParameters.put(PARAMETER_VALUE, value);
      return configParameters;
    }

    // the first matched condition will win; a single configuration task
    // should only ever set a single configuration property
    for (Condition condition : conditions) {
      String conditionConfigType = condition.conditionConfigType;
      String conditionKey = condition.conditionKey;
      String conditionValue = condition.conditionValue;

      // check the condition; if it passes, set the configuration properties
      // and break
      String checkValue = getDesiredConfigurationValue(cluster,
          conditionConfigType, conditionKey);

      if (conditionValue.equals(checkValue)) {
        configParameters.put(PARAMETER_CONFIG_TYPE, condition.configType);
        configParameters.put(PARAMETER_KEY, condition.key);
        configParameters.put(PARAMETER_VALUE, condition.value);
        break;
      }
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