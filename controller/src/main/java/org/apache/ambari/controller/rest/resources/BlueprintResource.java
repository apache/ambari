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
import org.apache.ambari.common.rest.entities.BlueprintInformation;
import org.apache.ambari.controller.Blueprints;
import org.apache.ambari.controller.ExceptionResponse;

/** BlueprintResource represents a Hadoop blueprint to be installed on a 
 *  cluster. Blueprints define a collection of Hadoop components that are
 *  installed together on a cluster and their configuration.
 */
@Path(value = "/blueprints/{blueprintName}")
public class BlueprintResource {
        
    /** 
     * Get a blueprint
     * 
     * @response.representation.200.doc       Get a blueprint
     * @response.representation.200.mediaType application/json
     * @response.representation.200.example
     *  
     * @param  blueprintName   Name of the blueprint
     * @param  revision        The optional blueprint revision, if not specified get the latest revision
     * @return                 blueprint definition
     * @throws Exception       throws Exception (TBD)
     */
    @GET
    @Produces({"application/json", "application/xml"})
    public Blueprint getBlueprint(@PathParam("blueprintName") String blueprintName, 
                                  @DefaultValue("-1") @QueryParam("revision") String revision) throws Exception {     
        try {
            return Blueprints.getInstance().getBlueprint(blueprintName, Integer.parseInt(revision));
        }catch (WebApplicationException we) {
            throw we;
        }catch (Exception e) {
            throw new WebApplicationException((new ExceptionResponse(e)).get());
        }      
    }
    
    /** 
     * Get a blueprint revisions
     * 
     * @response.representation.200.doc       Get blueprint revisions
     * @response.representation.200.mediaType application/json
     *  
     * @param  blueprintName   Name of the blueprint
     * 
     * @return                 List of blueprint revisions
     * @throws Exception       throws Exception (TBD)
     */
    @Path(value = "/revisions")
    @GET
    @Produces({"application/json", "application/xml"})
    public List<BlueprintInformation> getBlueprintRevisions(@PathParam("blueprintName") String blueprintName) throws Exception {     
        try {
            List<BlueprintInformation> list = Blueprints.getInstance().getBlueprintRevisions(blueprintName);
            if (list.isEmpty()) {
                throw new WebApplicationException(Response.Status.NO_CONTENT);
            }
            return list;
        }catch (WebApplicationException we) {
            throw we;
        }catch (Exception e) {
            throw new WebApplicationException((new ExceptionResponse(e)).get());
        }      
    }
    
    /** 
     * Delete the blueprint
     * 
     * @response.representation.200.doc       Delete a blueprint
     * @response.representation.200.mediaType application/json
     *  
     * @param  blueprintName    Name of the blueprint
     * @throws Exception        throws Exception (TBD)
     */
    @DELETE
    @Consumes({"application/json", "application/xml"})
    public void deleteBlueprint(@PathParam("blueprintName") String blueprintName,
                                @DefaultValue("") @QueryParam("revision") String revision ) throws Exception {     
        try {
            if (revision == null || revision.equals("")) {
                String msg = "Revision number not specified";
                throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
            }
            Blueprints.getInstance().deleteBlueprint(blueprintName, Integer.parseInt(revision));
        }catch (WebApplicationException we) {
            throw we;
        }catch (Exception e) {
            throw new WebApplicationException((new ExceptionResponse(e)).get());
        }    
    }
    
    /** 
     * Update a current blueprint.
     *
     * @response.representation.200.doc       Updates a current blueprint to 
     *                                        update some of its fields.
     * @response.representation.200.mediaType application/json
     * 
     * @param blueprintName Name of the blueprint
     * @param blueprint     Input blueprint object specifying the blueprint definition
     * @return              Returns the new revision of the blueprint
     * @throws Exception    throws Exception
     */
    @PUT
    @Consumes
    public Blueprint updateBlueprint(@PathParam("blueprintName") String blueprintName, Blueprint blueprint) throws Exception {
        return null;
    }
    
}
