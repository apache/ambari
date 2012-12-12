/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.eventdb.webservice;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.ambari.eventdb.db.PostgresConnector;
import org.apache.ambari.eventdb.model.Jobs;
import org.apache.ambari.eventdb.model.Jobs.JobDBEntry;
import org.apache.ambari.eventdb.model.Workflows;
import org.apache.ambari.eventdb.model.Workflows.WorkflowDBEntry;

@Path("/json")
public class WorkflowJsonService {
  private static final String PREFIX = "eventdb.";
  private static final String HOSTNAME = PREFIX + "db.hostname";
  private static final String DBNAME = PREFIX + "db.name";
  private static final String USERNAME = PREFIX + "db.user";
  private static final String PASSWORD = PREFIX + "db.password";
  
  private static final List<WorkflowDBEntry> EMPTY_WORKFLOWS = Collections.emptyList();
  private static final List<JobDBEntry> EMPTY_JOBS = Collections.emptyList();
  
  @Context
  ServletContext servletContext;
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/workflow")
  public Workflows getWorkflows() {
    Workflows workflows = new Workflows();
    try {
      PostgresConnector conn = new PostgresConnector(servletContext.getInitParameter(HOSTNAME), servletContext.getInitParameter(DBNAME),
          servletContext.getInitParameter(USERNAME), servletContext.getInitParameter(PASSWORD));
      workflows.setWorkflows(conn.fetchWorkflows());
      conn.close();
    } catch (IOException e) {
      e.printStackTrace();
      workflows.setWorkflows(EMPTY_WORKFLOWS);
    }
    return workflows;
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/job")
  public Jobs getJobs(@QueryParam("workflowId") String workflowId) {
    Jobs jobs = new Jobs();
    try {
      PostgresConnector conn = new PostgresConnector(servletContext.getInitParameter(HOSTNAME), servletContext.getInitParameter(DBNAME),
          servletContext.getInitParameter(USERNAME), servletContext.getInitParameter(PASSWORD));
      jobs.setJobs(conn.fetchJobDetails(workflowId));
      conn.close();
    } catch (IOException e) {
      e.printStackTrace();
      jobs.setJobs(EMPTY_JOBS);
    }
    return jobs;
  }
}
