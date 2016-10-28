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

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.UpgradeContext;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.base.Objects;

/**
 * The {@link ConfigurationCondition} class is used to represent a condition on
 * a property.
 */
@XmlType(name = "config")
@XmlAccessorType(XmlAccessType.FIELD)
public final class ConfigurationCondition extends Condition {

  /**
   * The type of comparison to make.
   */
  @XmlEnum
  public enum ComparisonType {

    /**
     * Equals comparison.
     */
    @XmlEnumValue("equals")
    EQUALS;
  }

  /**
   * The configuration type, such as {@code hdfs-site}.
   */
  @XmlAttribute(name = "type")
  public String type;

  /**
   * The configuration property key.
   */
  @XmlAttribute(name = "property")
  public String property;

  /**
   * The value to compare against.
   */
  @XmlAttribute(name = "value")
  public String value;

  /**
   * The type of comparison to make.
   */
  @XmlAttribute(name = "comparison")
  public ComparisonType comparisonType;

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return Objects.toStringHelper(this).add("type", type).add("property", property).add(value,
        value).add("comparison", comparisonType).omitNullValues().toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSatisfied(UpgradeContext upgradeContext) {
    Cluster cluster = upgradeContext.getCluster();
    Config config = cluster.getDesiredConfigByType(type);
    if (null == config) {
      return false;
    }

    Map<String, String> properties = config.getProperties();
    if (MapUtils.isEmpty(properties)) {
      return false;
    }

    String propertyValue = properties.get(property);
    switch (comparisonType) {
      case EQUALS:
        return StringUtils.equals(propertyValue, value);
      default:
        return false;
    }
  }
}

