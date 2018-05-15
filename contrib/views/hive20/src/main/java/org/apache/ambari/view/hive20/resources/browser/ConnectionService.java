/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive20.resources.browser;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Optional;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive20.AuthParams;
import org.apache.ambari.view.hive20.ConnectionFactory;
import org.apache.ambari.view.hive20.ConnectionSystem;
import org.apache.ambari.view.hive20.client.ConnectionConfig;
import org.apache.ambari.view.hive20.internal.ConnectionException;
import org.apache.ambari.view.hive20.internal.HiveConnectionWrapper;
import org.apache.ambari.view.hive20.utils.ServiceFormattedException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Connection verification and management controller
 */
public class ConnectionService {

    public static final String NO_PASSWORD = "";
    public static final String SUFFIX = "validating the login";
    @Inject
  protected ViewContext context;

  protected final static Logger LOG =
    LoggerFactory.getLogger(ConnectionService.class);

  /**
   * Check if LDAP is configured on Hive
   * if no password is cached , ask for one(401)
   * if yes and a password is cached, try
   * to connect, if connection succeeds
   * return OK,
   *
   * if connection fails - ask for one again(401)
   */
  @GET
  @Path("connect")
  @Produces(MediaType.APPLICATION_JSON)

  public Response attemptConnection() {
    boolean ldapEnabled = ConnectionFactory.isLdapEnabled(context);
    if(ldapEnabled) {
      ConnectionSystem instance = ConnectionSystem.getInstance();
      Optional<String> password = instance.getPassword(context);
      if (!password.isPresent()) {
        // No password cached - request for one
        JSONObject entity = new JSONObject();
        Map<String, String> errors = new HashMap<>();
        errors.put("message", "Ldap password required");
        entity.put("errors", errors);
        return Response.status(Response.Status.UNAUTHORIZED).entity(entity).build();
      }
      // if there was a password cached, make a connection attempt
      // get the password
        String pass = password.get();
      // password may be stale, try to connect to Hive
        return attemptHiveConnection(pass);
    }
      return attemptHiveConnection(NO_PASSWORD);

  }


    private Response getOKResponse() {
        JSONObject response = new JSONObject();
        response.put("message", "OK");
        response.put("trace", null);
        response.put("status", "200");
        return Response.ok().entity(response).type(MediaType.APPLICATION_JSON).build();
    }

    private Response attemptHiveConnection(String pass) {
        ConnectionConfig connectionConfig = ConnectionFactory.create(context);
        HiveConnectionWrapper hiveConnectionWrapper = new HiveConnectionWrapper(connectionConfig.getJdbcUrl(), connectionConfig.getUsername(), pass,new AuthParams(context));
        try {
          hiveConnectionWrapper.connect();
        } catch (ConnectionException e) {
          // Cannot connect with the current credentials
          // check the message to see if the cause was a login failure
          // return a 401
          // else return a 500
          if(isLoginError(e)) {
            JSONObject entity = new JSONObject();
            Map<String, String> errors = new HashMap<>();
            errors.put("message", "Authentication Exception");
            entity.put("errors", errors);
            return Response.status(Response.Status.UNAUTHORIZED).entity(entity).build();
          } else
              throw new ServiceFormattedException(e.getMessage(), e);
        } finally {
          try {
            hiveConnectionWrapper.disconnect();
          }
        catch(ConnectionException e){
           LOG.warn("Cannot close the connection");
        }
      }
        return getOKResponse()  ;
    }

    private boolean isLoginError(ConnectionException ce) {
        return ce.getCause().getMessage().toLowerCase().endsWith(SUFFIX);
    }


    /**
     * Set password
     * This just updates the caches.
     */
    @POST
    @Path("auth")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setupPassword(AuthRequest request) {
        try {
            //Cache the password for the user
            ConnectionSystem instance = ConnectionSystem.getInstance();
            instance.persistCredentials(context.getUsername(),request.password);
            return attemptHiveConnection(request.password);
        } catch (WebApplicationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceFormattedException(ex.getMessage(), ex);
        }
    }



    public static class AuthRequest {
        public String password;
    }


}
