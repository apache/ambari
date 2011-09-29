/*
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
package org.apache.ambari.controller;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

public class ExceptionResponse  {

    Response r;
    
    public ExceptionResponse (Exception e) {
        ResponseBuilder builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
        builder.header("ErrorMessage", e.getMessage());
        builder.header("ErrorCode", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        r = builder.build();
    }
    
    public ExceptionResponse (String exceptionMessage, Response.Status rs) {
        ResponseBuilder builder = Response.status(rs);
        builder.header("ErrorMessage",exceptionMessage);
        builder.header("ErrorCode", rs.getStatusCode());
        r = builder.build();
    }
    
    public Response get() {
        return this.r;
    }
}
