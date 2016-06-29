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

package org.apache.ambari.view.huetoambarimigration.migration.configuration;


import com.google.inject.Inject;
import org.apache.ambari.view.ViewContext;
import org.json.simple.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Service class to fetch user detail
 */

@Path("/usersdetails")

public class UserDetailHue {

  @Inject
  ViewContext view;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response userList() throws IOException, PropertyVetoException, SQLException {

    UserDetailsUtility user=new UserDetailsUtility();

    JSONObject response = new JSONObject();
    response.put("usersdetails",user.getUserDetails(view));
    return Response.ok(response).build();
  }


}
