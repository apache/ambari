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
package org.apache.ambari.server.checks;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Singleton;

/**
 * Checks that namenode high availability is enabled.
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.MULTIPLE_COMPONENT_WARNING, order = 16.0f)
public class DruidHighAvailabilityCheck extends AbstractCheckDescriptor
{

  public static final String DRUID_SERVICE_NAME = "DRUID";
  public static final String[] DRUID_COMPONENT_NAMES = new String[]{
      "DRUID_BROKER",
      "DRUID_COORDINATOR",
      "DRUID_HISTORICAL",
      "DRUID_OVERLORD",
      "DRUID_MIDDLEMANAGER",
      "DRUID_ROUTER"
  };

  /**
   * Constructor.
   */
  public DruidHighAvailabilityCheck()
  {
    super(CheckDescription.DRUID_HA_WARNING);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getApplicableServices()
  {
    return Sets.newHashSet(DRUID_SERVICE_NAME);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<CheckQualification> getQualifications()
  {
    return Arrays.asList(
        new PriorCheckQualification(CheckDescription.DRUID_HA_WARNING));
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException
  {
    List<String> haNotEnabledComponents = Lists.newArrayList();
    for (String component : DRUID_COMPONENT_NAMES) {
      Set<String> hosts = getHostsForComponent(request, component);
      if (hosts.size() == 1) {
        // This component is installed on only 1 host, HA is not enabled for it.
        haNotEnabledComponents.add(component);
      }
    }
    if (!haNotEnabledComponents.isEmpty()) {
      prerequisiteCheck.getFailedOn().add(DRUID_SERVICE_NAME);
      prerequisiteCheck.setStatus(PrereqCheckStatus.WARNING);
      String failReason = getFailReason(prerequisiteCheck, request);
      prerequisiteCheck.setFailReason(String.format(failReason, StringUtils.join(haNotEnabledComponents.toArray(), ", ")));
    }

  }

  private Set<String> getHostsForComponent(PrereqCheckRequest request, String componentName)
      throws AmbariException
  {
    Set<String> hosts = new HashSet<>();
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    try {
      ServiceComponent serviceComponent = cluster.getService(DRUID_SERVICE_NAME).getServiceComponent(componentName);
      if (serviceComponent != null) {
        hosts = serviceComponent.getServiceComponentHosts().keySet();
      }
    }
    catch (ServiceComponentNotFoundException err) {
      // This exception can be ignored if the component doesn't exist because it is a best-attempt at finding it.
    }

    return hosts;
  }
}
