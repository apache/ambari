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

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.ambari.infra.manager.JobManager;
import org.apache.ambari.infra.model.ExecutionContextResponse;
import org.apache.ambari.infra.model.JobExecutionDetailsResponse;
import org.apache.ambari.infra.model.JobExecutionInfoResponse;
import org.apache.ambari.infra.model.JobExecutionRequest;
import org.apache.ambari.infra.model.JobExecutionRestartRequest;
import org.apache.ambari.infra.model.JobExecutionStopRequest;
import org.apache.ambari.infra.model.JobInstanceDetailsResponse;
import org.apache.ambari.infra.model.JobInstanceStartRequest;
import org.apache.ambari.infra.model.JobRequest;
import org.apache.ambari.infra.model.PageRequest;
import org.apache.ambari.infra.model.StepExecutionContextResponse;
import org.apache.ambari.infra.model.StepExecutionInfoResponse;
import org.apache.ambari.infra.model.StepExecutionProgressResponse;
import org.apache.ambari.infra.model.StepExecutionRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.admin.service.NoSuchStepExecutionException;
import org.springframework.batch.admin.web.JobInfo;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.context.annotation.Scope;

import com.google.common.base.Splitter;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

@Api(value = "jobs", description = "Job operations", authorizations = {@Authorization(value = "basicAuth")})
@Path("jobs")
@Named
@Scope("request")
public class JobResource {
  private static final Logger logger = LogManager.getLogger(JobResource.class);

  @Inject
  private JobManager jobManager;

  @GET
  @Produces({"application/json"})
  @ApiOperation("Get all jobs")
  public List<JobInfo> getAllJobs(@BeanParam @Valid PageRequest request) {
    return jobManager.getAllJobs(request.getPage(), request.getSize());
  }

  @POST
  @Produces({"application/json"})
  @Path("{jobName}")
  @ApiOperation("Start a new job instance by job name.")
  public JobExecutionInfoResponse startJob(@BeanParam @Valid JobInstanceStartRequest request)
    throws JobParametersInvalidException, NoSuchJobException, JobExecutionAlreadyRunningException,
    JobRestartException, JobInstanceAlreadyCompleteException {

    String jobName = request.getJobName();
    String params = request.getParams();
    JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();
    if (params != null) {
      logger.info("Parsing parameters of job {} '{}'", jobName, params);
      Splitter.on(',')
              .trimResults()
              .withKeyValueSeparator(Splitter.on('=').limit(2).trimResults())
              .split(params).forEach(jobParametersBuilder::addString);
    }

    return jobManager.launchJob(jobName, jobParametersBuilder.toJobParameters());
  }

  @GET
  @Produces({"application/json"})
  @Path("/info/names")
  @ApiOperation("Get all job names")
  public Set<String> getAllJobNames() {
    return jobManager.getAllJobNames();
  }

  @GET
  @Produces({"application/json"})
  @Path("{jobName}/info")
  @ApiOperation("Get job details by job name.")
  public List<JobInstanceDetailsResponse> getJobDetails(@BeanParam @Valid JobRequest jobRequest) throws NoSuchJobException {
    return jobManager.getJobDetails(jobRequest.getJobName(), jobRequest.getPage(), jobRequest.getSize());
  }

  @GET
  @Path("{jobName}/executions")
  @Produces({"application/json"})
  @ApiOperation("Get the id values of all the running job instances.")
  public Set<Long> getExecutionIdsByJobName(@PathParam("jobName") @NotNull @Valid String jobName) throws NoSuchJobException {
    return jobManager.getExecutionIdsByJobName(jobName);
  }

  @GET
  @Produces({"application/json"})
  @Path("/executions/{jobExecutionId}")
  @ApiOperation("Get job and step details for job execution instance.")
  public JobExecutionDetailsResponse getExecutionInfo(@PathParam("jobExecutionId") @Valid Long jobExecutionId) throws NoSuchJobExecutionException {
    return jobManager.getExecutionInfo(jobExecutionId);
  }

  @GET
  @Produces({"application/json"})
  @Path("/executions/{jobExecutionId}/context")
  @ApiOperation("Get execution context for specific job.")
  public ExecutionContextResponse getExecutionContextByJobExecId(@PathParam("jobExecutionId") Long executionId) throws NoSuchJobExecutionException {
    return jobManager.getExecutionContextByJobExecutionId(executionId);
  }


  @DELETE
  @Produces({"application/json"})
  @Path("/executions/{jobExecutionId}")
  @ApiOperation("Stop or abandon a running job execution.")
  public JobExecutionInfoResponse stopOrAbandonJobExecution(@BeanParam @Valid JobExecutionStopRequest request)
    throws NoSuchJobExecutionException, JobExecutionNotRunningException, JobExecutionAlreadyRunningException {
    return jobManager.stopOrAbandonJobByExecutionId(request.getJobExecutionId(), request.getOperation());
  }

  @DELETE
  @Produces({"application/json"})
  @Path("/executions")
  @ApiOperation("Stop all job executions.")
  public Integer stopAll() {
    return jobManager.stopAllJobs();
  }

  @GET
  @Produces({"application/json"})
  @Path("/{jobName}/{jobInstanceId}/executions")
  @ApiOperation("Get execution of job instance.")
  public List<JobExecutionInfoResponse> getExecutionsOfInstance(@BeanParam @Valid JobExecutionRequest request) throws
          NoSuchJobException, NoSuchJobInstanceException {
    return jobManager.getExecutionsForJobInstance(request.getJobName(), request.getJobInstanceId());
  }

  @POST
  @Produces({"application/json"})
  @Path("/{jobName}/{jobInstanceId}/executions")
  @ApiOperation("Restart job instance.")
  public JobExecutionInfoResponse restartJobInstance(@BeanParam @Valid JobExecutionRestartRequest request) throws JobInstanceAlreadyCompleteException,
    NoSuchJobExecutionException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException, NoSuchJobException {
    return jobManager.restart(request.getJobInstanceId(), request.getJobName(), request.getOperation());
  }

  @GET
  @Produces({"application/json"})
  @Path("/executions/{jobExecutionId}/steps/{stepExecutionId}")
  @ApiOperation("Get step execution details.")
  public StepExecutionInfoResponse getStepExecution(@BeanParam @Valid StepExecutionRequest request) throws NoSuchStepExecutionException, NoSuchJobExecutionException {
    return jobManager.getStepExecution(request.getJobExecutionId(), request.getStepExecutionId());
  }

  @GET
  @Produces({"application/json"})
  @Path("/executions/{jobExecutionId}/steps/{stepExecutionId}/execution-context")
  @ApiOperation("Get the execution context of step execution.")
  public StepExecutionContextResponse getStepExecutionContext(@BeanParam @Valid StepExecutionRequest request) throws NoSuchStepExecutionException, NoSuchJobExecutionException {
    return jobManager.getStepExecutionContext(request.getJobExecutionId(), request.getStepExecutionId());
  }

  @GET
  @Produces({"application/json"})
  @Path("/executions/{jobExecutionId}/steps/{stepExecutionId}/progress")
  @ApiOperation("Get progress of step execution.")
  public StepExecutionProgressResponse getStepExecutionProgress(@BeanParam @Valid StepExecutionRequest request) throws NoSuchStepExecutionException, NoSuchJobExecutionException {
    return jobManager.getStepExecutionProgress(request.getJobExecutionId(), request.getStepExecutionId());
  }

}
