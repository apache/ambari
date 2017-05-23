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
package org.apache.ambari.server.state.stack.upgrade;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.UpgradeContext;

import com.google.common.base.Objects;

/**
 * The {@link SecurityCondition} class is used to represent that the cluster has
 * been configured for a specific type of security model.
 *
 * @see SecurityType
 */
@XmlType(name = "security")
@XmlAccessorType(XmlAccessType.FIELD)
public final class SecurityCondition extends Condition {

  /**
   * The type of security which much be enabled.
   */
  @XmlAttribute(name = "type")
  public SecurityType securityType;

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return Objects.toStringHelper(this).add("type", securityType).omitNullValues().toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSatisfied(UpgradeContext upgradeContext) {
    Cluster cluster = upgradeContext.getCluster();
    return cluster.getSecurityType() == securityType;
  }
}

