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


import java.util.Map;

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.admin.service.NoSuchStepExecutionException;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobExecutionNotFailedException;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobExecutionNotStoppedException;
import org.springframework.batch.core.launch.JobInstanceAlreadyExistsException;
import org.springframework.batch.core.launch.JobParametersNotFoundException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.step.NoSuchStepException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.google.common.collect.Maps;

@Named
@Provider
public class JobExceptionMapper implements ExceptionMapper<Throwable> {

  private static final Logger logger = LogManager.getLogger(JobExceptionMapper.class);

  private static final Map<Class, Response.Status> exceptionStatusCodeMap = Maps.newHashMap();

  static {
    exceptionStatusCodeMap.put(MethodArgumentNotValidException.class, Response.Status.BAD_REQUEST);
    exceptionStatusCodeMap.put(NoSuchJobException.class, Response.Status.NOT_FOUND);
    exceptionStatusCodeMap.put(NoSuchStepException.class, Response.Status.NOT_FOUND);
    exceptionStatusCodeMap.put(NoSuchStepExecutionException.class, Response.Status.NOT_FOUND);
    exceptionStatusCodeMap.put(NoSuchJobExecutionException.class, Response.Status.NOT_FOUND);
    exceptionStatusCodeMap.put(NoSuchJobInstanceException.class, Response.Status.NOT_FOUND);
    exceptionStatusCodeMap.put(JobExecutionNotRunningException.class, Response.Status.INTERNAL_SERVER_ERROR);
    exceptionStatusCodeMap.put(JobExecutionNotStoppedException.class, Response.Status.INTERNAL_SERVER_ERROR);
    exceptionStatusCodeMap.put(JobInstanceAlreadyExistsException.class, Response.Status.ACCEPTED);
    exceptionStatusCodeMap.put(JobInstanceAlreadyCompleteException.class, Response.Status.ACCEPTED);
    exceptionStatusCodeMap.put(JobExecutionAlreadyRunningException.class, Response.Status.ACCEPTED);
    exceptionStatusCodeMap.put(JobExecutionAlreadyCompleteException.class, Response.Status.ACCEPTED);
    exceptionStatusCodeMap.put(JobParametersNotFoundException.class, Response.Status.NOT_FOUND);
    exceptionStatusCodeMap.put(JobExecutionNotFailedException.class, Response.Status.INTERNAL_SERVER_ERROR);
    exceptionStatusCodeMap.put(JobRestartException.class, Response.Status.INTERNAL_SERVER_ERROR);
    exceptionStatusCodeMap.put(JobParametersInvalidException.class, Response.Status.BAD_REQUEST);
  }

  @Override
  public Response toResponse(Throwable throwable) {
    logger.error("REST Exception occurred:", throwable);
    Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;

    for (Map.Entry<Class, Response.Status> entry : exceptionStatusCodeMap.entrySet()) {
      if (throwable.getClass().isAssignableFrom(entry.getKey())) {
        status = entry.getValue();
        logger.info("Exception mapped to: {} with status code: {}", entry.getKey().getCanonicalName(), entry.getValue().getStatusCode());
        break;
      }
    }

    return Response.status(status).entity(new StatusMessage(throwable.getMessage(), status.getStatusCode()))
      .type(MediaType.APPLICATION_JSON_TYPE).build();
  }

  private class StatusMessage {
    private String message;
    private int statusCode;

    StatusMessage(String message, int statusCode) {
      this.message = message;
      this.statusCode = statusCode;
    }

    public String getMessage() {
      return message;
    }

    public int getStatusCode() {
      return statusCode;
    }
  }
}
