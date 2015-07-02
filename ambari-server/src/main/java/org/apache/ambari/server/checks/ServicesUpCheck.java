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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.models.HostComponentSummary;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Singleton;

/**
 * Checks that services are up.
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.LIVELINESS, order = 2.0f)
public class ServicesUpCheck extends AbstractCheckDescriptor {

  private static final float SLAVE_THRESHOLD = 0.5f;

  /**
   * Constructor.
   */
  public ServicesUpCheck() {
    super(CheckDescription.SERVICES_UP);
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    List<String> errorMessages = new ArrayList<String>();
    Set<String> failedServiceNames = new HashSet<String>();

    StackId stackId = cluster.getCurrentStackVersion();

    for (Map.Entry<String, Service> serviceEntry : cluster.getServices().entrySet()) {
      final Service service = serviceEntry.getValue();

      // Ignore services like Tez that are clientOnly.
      if (!service.isClientOnlyService()) {
        Map<String, ServiceComponent> serviceComponents = service.getServiceComponents();

        for (Map.Entry<String, ServiceComponent> component : serviceComponents.entrySet()) {

          boolean ignoreComponent = false;
          boolean checkThreshold = false;

          ServiceComponent serviceComponent = component.getValue();
          // In Services like HDFS, ignore components like HDFS Client
          if (serviceComponent.isClientComponent()) {
            ignoreComponent = true;
          }

          // non-master, "true" slaves with cardinality 1+
          if (!ignoreComponent && !serviceComponent.isMasterComponent()) {
            ComponentInfo componentInfo = ambariMetaInfo.get().getComponent(
                stackId.getStackName(), stackId.getStackVersion(),
                serviceComponent.getServiceName(), serviceComponent.getName());

            String cardinality = componentInfo.getCardinality();
            // !!! check if there can be more than one. This will match, say, datanodes but not ZKFC
            if (null != cardinality &&
                (cardinality.equals("ALL") || cardinality.matches("[1-9].*"))) {
              checkThreshold = true;
            }
          }

          if (!serviceComponent.isVersionAdvertised()) {
            ignoreComponent = true;
          }

          // TODO, add more logic that checks the Upgrade Pack.
          // These components are not in the upgrade pack and do not advertise a version:
          // ZKFC, Ambari Metrics, Kerberos, Atlas (right now).
          // Generally, if it advertises a version => in the upgrade pack.
          // So it can be in the Upgrade Pack but not advertise a version.
          if (!ignoreComponent) {
            List<HostComponentSummary> hostComponentSummaries = HostComponentSummary.getHostComponentSummaries(service.getName(), serviceComponent.getName());

            if (checkThreshold && !hostComponentSummaries.isEmpty()) {
              int total = hostComponentSummaries.size();
              int up = 0;
              int down = 0;

              for(HostComponentSummary s : hostComponentSummaries) {
                if ((s.getDesiredState() == State.INSTALLED || s.getDesiredState() == State.STARTED) && State.STARTED != s.getCurrentState()) {
                  down++;
                } else {
                  up++;
                }
              }

              if ((float) down/total > SLAVE_THRESHOLD) { // arbitrary
                failedServiceNames.add(service.getName());
                String message = MessageFormat.format("{0}: {1} out of {2} {3} are started; there should be {4,number,percent} started before upgrading.",
                    service.getName(), up, total, serviceComponent.getName(), SLAVE_THRESHOLD);
                errorMessages.add(message);
              }
            } else {
              for(HostComponentSummary s : hostComponentSummaries) {
                if ((s.getDesiredState() == State.INSTALLED || s.getDesiredState() == State.STARTED) && State.STARTED != s.getCurrentState()) {
                  failedServiceNames.add(service.getName());
                  String message = MessageFormat.format("{0}: {1} (in {2} on host {3})", service.getName(), serviceComponent.getName(), s.getCurrentState(), s.getHostName());
                  errorMessages.add(message);
                  break;
                }
              }
            }
          }
        }
      }
    }

    if (!errorMessages.isEmpty()) {
      prerequisiteCheck.setFailedOn(new LinkedHashSet<String>(failedServiceNames));
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      prerequisiteCheck.setFailReason("The following Service Components should be in a started state.  Please invoke a service Stop and full Start and try again. " + StringUtils.join(errorMessages, ", "));
    }
  }

}