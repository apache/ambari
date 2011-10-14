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
package org.apache.ambari.controller.rest.resources;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.ambari.common.rest.entities.Blueprint;
import org.apache.ambari.controller.Blueprints;
import org.apache.ambari.controller.ExceptionResponse;
import org.codehaus.jettison.json.JSONArray;

/** 
 * BlueprintResource represents a Hadoop blueprint to be installed on a 
 * cluster. Blueprints define a collection of Hadoop components that are
 * installed together on a cluster and their configuration.
 */
@Path(value = "/blueprints")
public class BlueprintsResource {
 
    /** 
     * Creates a new blueprint.
     *
     *  If named blueprint does not exists already, then it creates new one i.e. revision zero.
     * 
     * @param blueprint  Input blueprint object specifying the blueprint definition
     * @return Returns the newly created revision of the blueprint
     * @throws Exception throws Exception
     */
    @POST
    @Consumes ({"application/xml", "application/json"})
    public Blueprint createBlueprint(@DefaultValue("") @QueryParam("url") String locationURL, Blueprint blueprint) throws Exception {  
        try {
            if (locationURL == null || locationURL.equals("")) {
                return Blueprints.getInstance().addBlueprint(blueprint);
            } else {
                return Blueprints.getInstance().importDefaultBlueprint (locationURL);
            }
        }catch (WebApplicationException we) {
            throw we;
        }catch (Exception e) {
            throw new WebApplicationException((new ExceptionResponse(e)).get());
        } 
    }

    /** 
     * Get the list of blueprint names
     * 
     * @return Returns the list of blueprint names
     * @throws Exception throws Exception
     */
    @GET
    @Produces({"application/json", "application/xml"})
    public JSONArray listBlueprints() throws Exception {
        JSONArray list;
        try {
            list = Blueprints.getInstance().getBlueprintList();
            if (list.length() == 0) {
                throw new WebApplicationException(Response.Status.NO_CONTENT);
            } 
            return list;
        }catch (WebApplicationException we) {
            throw we;
        }catch (Exception e) {
            throw new WebApplicationException((new ExceptionResponse(e)).get());
        } 
    }
    
}
