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
package org.apache.ambari.logsearch.rest.error;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.time.DateTimeException;
import java.util.Map;

import javax.inject.Named;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.ambari.logsearch.common.StatusMessage;
import org.apache.ambari.logsearch.manager.AlreadyExistsException;
import org.apache.ambari.logsearch.manager.MalformedInputException;
import org.apache.ambari.logsearch.manager.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.google.common.collect.Maps;

@Named
@Provider
public class GeneralExceptionMapper implements ExceptionMapper<Exception> {
  private static final Logger LOG = LoggerFactory.getLogger(GeneralExceptionMapper.class);
  static final String INTERNAL_SERVER_ERROR_MESSAGE = "Something went wrong, For more details check the logs or configuration.";

  private static final Map<Class<? extends Exception>, Response.Status> exceptionStatusCodeMap = Maps.newHashMap();

  static {
    exceptionStatusCodeMap.put(MethodArgumentNotValidException.class, BAD_REQUEST);
    exceptionStatusCodeMap.put(JsonMappingException.class, BAD_REQUEST);
    exceptionStatusCodeMap.put(JsonParseException.class, BAD_REQUEST);
    exceptionStatusCodeMap.put(UnrecognizedPropertyException.class, BAD_REQUEST);
    exceptionStatusCodeMap.put(MalformedInputException.class, BAD_REQUEST);
    exceptionStatusCodeMap.put(AlreadyExistsException.class, CONFLICT);
    exceptionStatusCodeMap.put(NotFoundException.class, NOT_FOUND);
    exceptionStatusCodeMap.put(DateTimeException.class, BAD_REQUEST);
  }

  @Override
  public Response toResponse(Exception exception) {
    try {
      return toResponse(exception, getStatus(exception));
    }
    catch (Exception ex) {
      LOG.error("Error while generating status message. Original Exception was", exception);
      throw ex;
    }
  }

  private Response.Status getStatus(Exception exception) {
    for (Map.Entry<Class<? extends Exception>, Response.Status> entry : exceptionStatusCodeMap.entrySet()) {
      if (entry.getKey().isAssignableFrom(exception.getClass())) {
        Response.Status status = entry.getValue();
        LOG.info("Exception mapped to: {} with status code: {}", entry.getKey().getCanonicalName(), entry.getValue().getStatusCode());
        return status;
      }
    }
    return INTERNAL_SERVER_ERROR;
  }

  static Response toResponse(Exception exception, Response.Status status) {
    String errorMessage;
    if (status.getStatusCode() < 500) {
      errorMessage = exception.getMessage();
      LOG.info("REST Exception occurred: {}", exception.getMessage());
      LOG.debug("REST Exception occurred:", exception);
    }
    else {
      errorMessage = INTERNAL_SERVER_ERROR_MESSAGE;
      LOG.error("REST Exception occurred:", exception);
    }

    return Response.status(status).entity(StatusMessage.with(status, errorMessage))
            .type(MediaType.APPLICATION_JSON_TYPE).build();
  }
}
