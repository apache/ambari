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

package org.apache.hms.controller.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hms.common.entity.manifest.PackageInfo;
import org.apache.hms.common.entity.manifest.Role;
import org.apache.hms.common.entity.manifest.SoftwareManifest;

@Path("software")
public class SoftwareManager {
  private static Log LOG = LogFactory.getLog(SoftwareManager.class);
  
  @GET
  @Path("manifest/sample")
  public SoftwareManifest getSample() {
    SoftwareManifest sm = new SoftwareManifest();
    sm.setName("hadoop");
    sm.setVersion("0.23");
    PackageInfo[] packages = new PackageInfo[1];
    packages[0]= new PackageInfo();
    packages[0].setName("http://hrt8n36.cc1.ygridcore.net/hadoop-0.23.torrent");

    List<Role> roles = new ArrayList<Role>();
    Role role = new Role();
    role.setName("namenode");
    role.setPackages(packages);
    roles.add(role);
    
    role = new Role();
    role.setName("datanode");
    role.setPackages(packages);
    roles.add(role);
    
    role = new Role();
    role.setName("jobtracker");
    role.setPackages(packages);
    roles.add(role);
    
    role = new Role();
    role.setName("tasktracker");
    role.setPackages(packages);
    roles.add(role);

    sm.setRoles(roles);
    return sm;
  }
}
