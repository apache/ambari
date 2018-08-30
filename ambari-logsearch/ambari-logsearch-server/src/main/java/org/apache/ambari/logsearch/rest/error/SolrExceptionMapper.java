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

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import javax.inject.Named;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.solr.common.SolrException;

@Named
@Provider
public class SolrExceptionMapper implements ExceptionMapper<SolrException> {
  @Override
  public Response toResponse(SolrException exception) {
    Response.Status status = Response.Status.fromStatusCode(exception.code());
    if (status == null)
      status = INTERNAL_SERVER_ERROR;

    return GeneralExceptionMapper.toResponse(exception, status);
  }
}
