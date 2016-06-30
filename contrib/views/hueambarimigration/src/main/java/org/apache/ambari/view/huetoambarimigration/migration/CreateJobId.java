/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.view.huetoambarimigration.migration;

import com.google.inject.Inject;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.models.JobReturnIdModel;
import org.apache.ambari.view.huetoambarimigration.resources.PersonalCRUDResourceManager;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.MigrationResourceManager;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.models.MigrationResponse;
import org.json.simple.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

@Path("/returnjobids")

public class CreateJobId {




  @Inject
  ViewContext view;


  protected MigrationResourceManager resourceManager = null;

  public synchronized PersonalCRUDResourceManager<MigrationResponse> getResourceManager() {
    if (resourceManager == null) {
      resourceManager = new MigrationResourceManager(view);
    }
    return resourceManager;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getIdOfMigrationObject(@QueryParam("username") String username,@QueryParam("instance") String instance,@QueryParam("startdate") String startdate,@QueryParam("enddate") String enddate,@QueryParam("jobtype") String jobtype) throws IOException, InvocationTargetException, IllegalAccessException {

    System.out.println("username is "+username+ "instance is "+ instance);
    MigrationResponse migrationresult=new MigrationResponse();

    migrationresult.setIntanceName(instance);
    migrationresult.setUserNameofhue(username);
    migrationresult.setProgressPercentage(0);
    migrationresult.setJobtype(jobtype);

    getResourceManager().create(migrationresult);

    JSONObject response = new JSONObject();

    JobReturnIdModel model=new JobReturnIdModel();

    model.setIdforJob(migrationresult.getId());
    model.setId(0);

    response.put("returnjobid",model);

    return Response.ok(response).build();


  }

}
