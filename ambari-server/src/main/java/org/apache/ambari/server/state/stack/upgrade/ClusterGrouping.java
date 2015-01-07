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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.stack.MasterHostResolver;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.stack.UpgradePack.ProcessingComponent;

/**
 * Used to represent cluster-based operations.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="cluster")
public class ClusterGrouping extends Grouping {

  /**
   * Stages against a Service and Component, or the Server
   */
  @XmlElement(name="execute-stage")
  private List<ExecuteStage> executionStages;

  @XmlTransient
  private ClusterBuilder m_builder = new ClusterBuilder();

  @Override
  public ClusterBuilder getBuilder() {
    return m_builder;
  }


  private static class ExecuteStage {
    @XmlAttribute(name="title")
    public String title;

    /**
     * Optional service name, can be ""
     */
    @XmlAttribute(name="service")
    public String service;

    /**
     * Optional component name, can be ""
     */
    @XmlAttribute(name="component")
    public String component;

    @XmlElement(name="task")
    public Task task;
  }

  public class ClusterBuilder extends StageWrapperBuilder {
    private MasterHostResolver m_resolver = null;

    /**
     * @param cluster the cluster to use with this builder
     */
    public void setHelpers(Cluster cluster) {
      m_resolver = new MasterHostResolver(cluster);
    }

    @Override
    public void add(HostsType hostsType, String service,
        boolean forUpgrade, boolean clientOnly, ProcessingComponent pc) {
      // !!! no-op in this case
    }

    @Override
    public List<StageWrapper> build() {
      if (null == ClusterGrouping.this.executionStages) {
        return Collections.emptyList();
      }

      List<StageWrapper> results = new ArrayList<StageWrapper>();

      if (executionStages != null) {
        for (ExecuteStage execution : executionStages) {
          Task task = execution.task;

          StageWrapper wrapper = null;

          if (null != execution.service && !execution.service.isEmpty() && null != execution.component && !execution.component.isEmpty()) {

            HostsType hosts = m_resolver.getMasterAndHosts(execution.service, execution.component);

            if (null == hosts) {
              continue;
            }

            Set<String> realHosts = new LinkedHashSet<String>(hosts.hosts);

            // !!! FIXME other types
            if (task.getType() == Task.Type.EXECUTE) {
              ExecuteTask et = (ExecuteTask) task;

              if (null != et.hosts && "master".equals(et.hosts) && null != hosts.master) {
                realHosts = Collections.singleton(hosts.master);
              }

              wrapper = new StageWrapper(
                  StageWrapper.Type.RU_TASKS,
                  execution.title,
                  new TaskWrapper(execution.service, execution.component, realHosts, task));
            }
          } else {
            switch (task.getType()) {
              case MANUAL:
              case SERVER_ACTION:
                wrapper = new StageWrapper(
                    StageWrapper.Type.SERVER_SIDE_ACTION,
                    execution.title,
                    new TaskWrapper(null, null, Collections.<String>emptySet(), task));
                break;
              default:
                break;
            }
          }

          if (null != wrapper) {
            results.add(wrapper);
          }
        }
      }

      return results;
    }
  }
}
