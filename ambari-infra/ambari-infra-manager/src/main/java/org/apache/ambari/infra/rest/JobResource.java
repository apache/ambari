/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.infra.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobInstanceAlreadyExistsException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.context.annotation.Scope;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.Date;
import java.util.Set;

@Api(value = "jobs", description = "Job operations")
@Path("jobs")
@Named
@Scope("request")
public class JobResource {

  @Inject
  private JobOperator jobOperator;

  @Inject
  private JobExplorer jobExplorer;

  @GET
  @Produces({"application/json"})
  @ApiOperation("Get all job names")
  public Set<String> getAllJobNames() {
    return jobOperator.getJobNames();
  }

  @GET
  @Path("executions/{jobName}")
  @Produces({"application/json"})
  @ApiOperation("Get the id values of all the running job instances by job name")
  public Set<Long> getExecutionIdsByJobName(
    @PathParam("jobName") String jobName) throws NoSuchJobException {
    return jobOperator.getRunningExecutions(jobName);
  }

  @POST
  @Produces({"application/json"})
  @Path("start/{jobName}")
  public Long startJob(@PathParam("jobName") String jobName, @QueryParam("params") String params)
    throws JobParametersInvalidException, JobInstanceAlreadyExistsException, NoSuchJobException {
    JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();
    jobParametersBuilder.addDate("date", new Date());
    return jobOperator.start(jobName, jobParametersBuilder.toJobParameters() + "," + params);
  }

}
