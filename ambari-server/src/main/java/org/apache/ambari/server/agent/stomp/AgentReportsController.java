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
package org.apache.ambari.server.agent.stomp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.AgentSessionManager;
import org.apache.ambari.server.agent.ComponentStatus;
import org.apache.ambari.server.agent.HeartBeatHandler;
import org.apache.ambari.server.agent.stomp.dto.ComponentStatusReport;
import org.apache.ambari.server.agent.stomp.dto.ComponentStatusReports;
import org.apache.ambari.server.state.cluster.ClustersImpl;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import com.google.inject.Injector;

@Controller
@SendToUser("/")
@MessageMapping("/reports")
public class AgentReportsController {
  private static Log LOG = LogFactory.getLog(AgentReportsController.class);
  private final HeartBeatHandler hh;
  private final ClustersImpl clusters;
  private final AgentSessionManager agentSessionManager;

  public AgentReportsController(Injector injector) {
    hh = injector.getInstance(HeartBeatHandler.class);
    clusters = injector.getInstance(ClustersImpl.class);
    agentSessionManager = injector.getInstance(AgentSessionManager.class);
  }

  @SubscribeMapping("/component_status")
  public void handleComponentReportStatus(@Header String simpSessionId, ComponentStatusReports message)
      throws WebApplicationException, InvalidStateTransitionException, AmbariException {
    List<ComponentStatus> statuses = new ArrayList<>();
    for (Map.Entry<String, List<ComponentStatusReport>> clusterReport : message.getComponentStatusReports().entrySet()) {
      for (ComponentStatusReport report : clusterReport.getValue()) {
        ComponentStatus componentStatus = new ComponentStatus();
        componentStatus.setClusterName(clusters.getCluster(report.getClusterId()).getClusterName());
        componentStatus.setComponentName(report.getComponentName());
        componentStatus.setServiceName(report.getServiceName());
        componentStatus.setStatus(report.getStatus().toString());
        statuses.add(componentStatus);
      }
    }

    hh.handleComponentReportStatus(statuses,
        agentSessionManager.getHost(simpSessionId).getHostName());
  }

}
