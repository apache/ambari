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
package org.apache.ambari.server.agent;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.UnitOfWork;

@Singleton
public class AgentReportsProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(AgentReportsProcessor.class);

  private ScheduledExecutorService executor;

  private ConcurrentLinkedQueue<AgentReport> agentReportsQueue = new ConcurrentLinkedQueue<>();

  public void addAgentReport(AgentReport agentReport) {
    agentReportsQueue.add(agentReport);
  }

  @Inject
  private HeartBeatHandler hh;

  @Inject
  private UnitOfWork unitOfWork;

  @Inject
  public AgentReportsProcessor(Configuration configuration) {

    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("agent-report-processor-%d").build();
    int poolSize = configuration.getAgentsReportThreadPoolSize();
    executor = Executors.newScheduledThreadPool(poolSize, threadFactory);
    for (int i=0; i< poolSize; i++) {
      executor.scheduleAtFixedRate(new AgentReportProcessingTask(),
          configuration.getAgentsReportProcessingStartTimeout(),
          configuration.getAgentsReportProcessingPeriod(), TimeUnit.SECONDS);
    }
  }

  private class AgentReportProcessingTask implements Runnable {

    @Override
    public void run() {
      try {
        unitOfWork.begin();
        while (true) {
          AgentReport agentReport = agentReportsQueue.poll();
          if (agentReport == null) {
            break;
          }
          String hostName = agentReport.getHostName();
          try {

            //TODO rewrite with polymorphism usage.
            if (agentReport.getCommandReports() != null) {
              hh.handleCommandReportStatus(agentReport.getCommandReports(), hostName);
            } else if (agentReport.getComponentStatuses() != null) {
              hh.handleComponentReportStatus(agentReport.getComponentStatuses(), hostName);
            } else if (agentReport.getHostStatusReport() != null) {
              hh.handleHostReportStatus(agentReport.getHostStatusReport(), hostName);
            }
          } catch (AmbariException e) {
            LOG.error("Error processing agent reports", e);
          }
        }
      } finally {
        unitOfWork.end();
      }
    }
  }
}
