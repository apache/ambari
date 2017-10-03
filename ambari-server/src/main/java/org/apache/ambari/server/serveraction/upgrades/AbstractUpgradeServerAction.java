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
package org.apache.ambari.server.serveraction.upgrades;

import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.UpgradeContext;
import org.apache.ambari.server.state.UpgradeContextFactory;
import org.apache.ambari.server.state.UpgradeHelper;

import com.google.inject.Inject;

/**
 * Abstract class that reads values from command params in a consistent way.
 */
public abstract class AbstractUpgradeServerAction extends AbstractServerAction {

  public Clusters getClusters() {
    return m_clusters;
  }

  @Inject
  private Clusters m_clusters;

  /**
   * Used to move desired repo versions forward.
   */
  @Inject
  protected UpgradeHelper m_upgradeHelper;

  /**
   * Used to lookup or update {@link UpgradeEntity} instances.
   */
  @Inject
  protected UpgradeDAO m_upgradeDAO;

  /**
   * Used to create instances of {@link UpgradeContext} with injected
   * dependencies.
   */
  @Inject
  private UpgradeContextFactory m_upgradeContextFactory;

  /**
   * Gets an initialized {@link UpgradeContext} for the in-progress upgrade.
   */
  protected UpgradeContext getUpgradeContext(Cluster cluster) {
    UpgradeEntity upgrade = cluster.getUpgradeInProgress();
    UpgradeContext upgradeContext = m_upgradeContextFactory.create(cluster, upgrade);
    return upgradeContext;
  }
}
