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
package org.apache.ambari.server.checks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The {@link StormRestAPIDeletedCheck}
 * checks that STORM_REST_API Component is deleted when upgrading from HDP 2.1 to 2.2 or higher.
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.TOPOLOGY, order = 15.0f, required= false)
public class StormRestAPIDeletedCheck extends AbstractCheckDescriptor {

  @Inject
  HostComponentStateDAO hostComponentStateDao;

  /**
   * Constructor.
   */
  public StormRestAPIDeletedCheck() {
    super(CheckDescription.STORM_REST_API_MUST_BE_DELETED);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isApplicable(PrereqCheckRequest request) throws AmbariException {
    if (!super.isApplicable(request, Arrays.asList("STORM"), true)) {
      return false;
    }

    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    Set<String> hosts = new HashSet<String>();
    final String STORM = "STORM";
    final String STORM_REST_API = "STORM_REST_API";

    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    try {
      ServiceComponent serviceComponent = cluster.getService(STORM).getServiceComponent(STORM_REST_API);
      if (serviceComponent != null) {
        hosts = serviceComponent.getServiceComponentHosts().keySet();
      }
    } catch (ServiceNotFoundException err) {
      // This exception can be ignored if the component doesn't exist because it is a best-attempt at finding it.
      ;
    } catch (ServiceComponentNotFoundException err) {
      // This exception can be ignored if the component doesn't exist because it is a best-attempt at finding it.
      ;
    }

    // Try another method to find references to STORM_REST_API
    if (hosts.isEmpty()) {
      List<HostComponentStateEntity> allHostComponents = hostComponentStateDao.findAll();
      for (HostComponentStateEntity hc : allHostComponents) {
        if (hc.getServiceName().equalsIgnoreCase(STORM) && hc.getComponentName().equalsIgnoreCase(STORM_REST_API)) {
          hosts.add(hc.getHostName());
        }
      }
    }

    if (!hosts.isEmpty()) {
      prerequisiteCheck.getFailedOn().add(STORM_REST_API);
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      prerequisiteCheck.setFailReason(getFailReason(prerequisiteCheck, request));
    }
  }
}
