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

import java.util.Map;

import javax.inject.Named;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.google.common.collect.Maps;

@Named
@Provider
public class GeneralExceptionMapper extends ExceptionMapperBase implements ExceptionMapper<Throwable> {
  private static final Logger LOG = LoggerFactory.getLogger(GeneralExceptionMapper.class);

  private static final Map<Class, Response.Status> exceptionStatusCodeMap = Maps.newHashMap();

  static {
    exceptionStatusCodeMap.put(MethodArgumentNotValidException.class, Response.Status.BAD_REQUEST);
    exceptionStatusCodeMap.put(JsonMappingException.class, Response.Status.BAD_REQUEST);
    exceptionStatusCodeMap.put(JsonParseException.class, Response.Status.BAD_REQUEST);
    exceptionStatusCodeMap.put(UnrecognizedPropertyException.class, Response.Status.BAD_REQUEST);
  }

  @Override
  public Response toResponse(Throwable throwable) {
    Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;

    for (Map.Entry<Class, Response.Status> entry : exceptionStatusCodeMap.entrySet()) {
      if (throwable.getClass().isAssignableFrom(entry.getKey())) {
        status = entry.getValue();
        LOG.info("Exception mapped to: {} with status code: {}", entry.getKey().getCanonicalName(), entry.getValue().getStatusCode());
        break;
      }
    }

    return toErrorResponse(status, throwable);
  }
}
