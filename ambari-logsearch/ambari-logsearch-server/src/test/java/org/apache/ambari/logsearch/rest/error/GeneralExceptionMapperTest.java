package org.apache.ambari.logsearch.rest.error;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.apache.ambari.logsearch.rest.error.GeneralExceptionMapper.INTERNAL_SERVER_ERROR_MESSAGE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.ambari.logsearch.common.StatusMessage;
import org.apache.ambari.logsearch.manager.NotFoundException;
import org.junit.Test;

import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;

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
public class GeneralExceptionMapperTest {
  @Test
  public void testToResponseAddsGeneralMessageWhenStatusIsGreaterOrEqualsTo500() {
    Response response = GeneralExceptionMapper.toResponse(new Exception("Message not added"), INTERNAL_SERVER_ERROR);
    StatusMessage statusMessage = (StatusMessage) response.getEntity();
    assertThat(statusMessage.getStatus(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
    assertThat(statusMessage.getMessage(), is(INTERNAL_SERVER_ERROR_MESSAGE));
  }

  @Test
  public void testToResponseAddsMessageFromExceptionWhenStatusIsLessThan500() {
    Response response = GeneralExceptionMapper.toResponse(new Exception("Message in exception"), BAD_REQUEST);
    StatusMessage statusMessage = (StatusMessage) response.getEntity();
    assertThat(statusMessage.getStatus(), is(BAD_REQUEST.getStatusCode()));
    assertThat(statusMessage.getMessage(), is("Message in exception"));
  }

  @Test
  public void testToResponseSetsTheGivenStatusCode() {
    Response response = GeneralExceptionMapper.toResponse(new Exception("any"), BAD_REQUEST);
    assertThat(response.getStatus(), is(BAD_REQUEST.getStatusCode()));
  }

  @Test
  public void testToResponseSetsApplicationJsonContentType() {
    Response response = GeneralExceptionMapper.toResponse(new Exception("any"), BAD_REQUEST);
    assertThat(response.getHeaders().get("Content-type").get(0), is(MediaType.APPLICATION_JSON_TYPE));
  }

  @Test
  public void testToResponseSetsStatus500WhenUnexpectedException() {
    Response response = new GeneralExceptionMapper().toResponse(new RuntimeException("Unexpected"));
    assertThat(response.getStatus(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
  }

  @Test
  public void testToResponseSetsPredefinedStatusWhenExceptionIsExpected() {
    Response response = new GeneralExceptionMapper().toResponse(new NotFoundException("Something missing!"));
    assertThat(response.getStatus(), is(NOT_FOUND.getStatusCode()));
  }

  @Test
  public void testToResponseSetsPredefinedStatusWhenExceptionIsDerivedFromExpectedException() {
    InvalidTypeIdException derivedFromJsonMappingException = new InvalidTypeIdException(null, "Invalid type", null, "any type id");
    Response response = new GeneralExceptionMapper().toResponse(derivedFromJsonMappingException);
    assertThat(response.getStatus(), is(BAD_REQUEST.getStatusCode()));
  }
}