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

package org.apache.ambari.server.api.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.ambari.server.bootstrap.BootStrapPostStatus;
import org.apache.ambari.server.bootstrap.BootStrapStatus;
import org.apache.ambari.server.bootstrap.SshHostInfo;

@Path("/bootstrap")
public class BootStrapResource {
  
  /**
   * Run bootstrap on a list of hosts.
   * @response.representation.200.doc 
   * 
   * @response.representation.200.mediaType application/json
   * @response.representation.406.doc Error in format
   * @response.representation.408.doc Request Timed out
   * @throws Exception
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML}) 
  public BootStrapPostStatus bootStrap(SshHostInfo sshInfo) {
    
    return new BootStrapPostStatus();
  }
  
  /**
   * Current BootStrap Information thats running.
   * @response.representation.200.doc 
   * 
   * @response.representation.200.mediaType application/json
   * @response.representation.406.doc Error in format
   * @response.representation.408.doc Request Timed out
   * @throws Exception
   */
  @GET
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML}) 
  public BootStrapStatus getBootStrapStatus() {
    return new BootStrapStatus();
  }
}
