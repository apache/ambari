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

package org.apache.hms.agent.rest;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.apache.hms.agent.dispatcher.PackageRunner;
import org.apache.hms.common.entity.PackageCommand;
import org.apache.hms.common.entity.RestSource;
import org.apache.hms.common.rest.Response;

@Path("package")
public class PackageManager extends RestSource {
  
  @POST
  @Path("install")
  public Response install(PackageCommand pc) {
    PackageRunner pr = new PackageRunner();
    return pr.install(pc);
  }
  
  @POST
  @Path("remove")
  public Response remove(PackageCommand pc) {
    PackageRunner pr = new PackageRunner();
    return pr.remove(pc);
  }
  
  @GET
  @Path("info")
  public Response info(PackageCommand pc) {
    PackageRunner pr = new PackageRunner();
    return pr.query(pc);
  }
  
}
