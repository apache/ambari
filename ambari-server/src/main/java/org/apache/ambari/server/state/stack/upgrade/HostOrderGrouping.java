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

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.state.UpgradeContext;
import org.apache.ambari.server.state.stack.UpgradePack.ProcessingComponent;

/**
 * Marker group for Host-Ordered upgrades.
 */
@XmlType(name="host-order")
public class HostOrderGrouping extends Grouping {

  /**
   * Contains the ordered actions to schedule for this grouping.
   */
  private List<HostOrderItem> m_hostOrderItems;

  /**
   * Constructor
   */
  public HostOrderGrouping() {
  }

  /**
   * Sets the {@link HostOrderItem}s on this grouping.
   *
   * @param hostOrderItems
   */
  public void setHostOrderItems(List<HostOrderItem> hostOrderItems) {
    m_hostOrderItems = hostOrderItems;
  }

  @Override
  public StageWrapperBuilder getBuilder() {
    return new HostBuilder(this);
  }

  /**
   * Builder for host upgrades.
   */
  private static class HostBuilder extends StageWrapperBuilder {
    /**
     * @param grouping the grouping
     */
    protected HostBuilder(HostOrderGrouping grouping) {
      super(grouping);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(UpgradeContext upgradeContext, HostsType hostsType, String service,
        boolean clientOnly, ProcessingComponent pc, Map<String, String> params) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StageWrapper> build(UpgradeContext upgradeContext,
        List<StageWrapper> stageWrappers) {
      return null;
    }
  }

}
