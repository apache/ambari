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
package org.apache.ambari.server.state;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.stack.MasterHostResolver;
import org.apache.ambari.server.state.stack.upgrade.Direction;

/**
 * Used to hold various helper objects required to process an upgrade pack.
 */
public class UpgradeContext {

  private String m_version;
  private Direction m_direction;
  private MasterHostResolver m_resolver;
  private AmbariMetaInfo m_metaInfo;

  /**
   * Constructor.
   * @param resolver  the resolver that also references the required cluster
   * @param version   the target version to upgrade to
   * @param direction the direction for the upgrade
   */
  public UpgradeContext(MasterHostResolver resolver, String version,
      Direction direction) {
    m_version = version;
    m_direction = direction;
    m_resolver = resolver;
  }

  /**
   * @return the cluster from the {@link MasterHostResolver}
   */
  public Cluster getCluster() {
    return m_resolver.getCluster();
  }

  /**
   * @return the target version for the upgrade
   */
  public String getVersion() {
    return m_version;
  }

  /**
   * @return the direction of the upgrade
   */
  public Direction getDirection() {
    return m_direction;
  }

  /**
   * @return the resolver
   */
  public MasterHostResolver getResolver() {
    return m_resolver;
  }

  /**
   * @return the metainfo for access to service definitions
   */
  public AmbariMetaInfo getAmbariMetaInfo() {
    return m_metaInfo;
  }

  /**
   * @param metaInfo the metainfo for access to service definitions
   */
  public void setAmbariMetaInfo(AmbariMetaInfo metaInfo) {
    m_metaInfo = metaInfo;
  }

}
