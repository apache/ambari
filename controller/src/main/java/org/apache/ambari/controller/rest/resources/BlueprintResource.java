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
import org.apache.ambari.controller.Stacks;

/** BlueprintResource represents a Hadoop blueprint to be installed on a 
 *  cluster. Blueprints define a collection of Hadoop components that are
 *  installed together on a cluster and their configuration.
 */
@Path(value = "/blueprints/{blueprintName}")
public class BlueprintResource {
        
    /** Get a blueprint
     * 
     *  <p>
     *  REST:<br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;URL Path                                    : /blueprints/{blueprintName}<br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;HTTP Method                                 : GET <br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;HTTP Request Header                         : <br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Content-type        = application/json <br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Accept              = application/json <br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;HTTP Response Header                        : <br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Content-type        = application/json <br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Accept              = application/json <br>
     *  <p> 
     *  
     *      @param  blueprintName   Name of the blueprint
     *      @param  revision        The optional blueprint revision to get
     *      @return                 blueprint definition
     *      @throws Exception       throws Exception (TBD)
     */
    @GET
    @Produces({"application/json", "application/xml"})
    public Blueprint getBlueprint(@PathParam("blueprintName") String blueprintName, 
                                  @DefaultValue("") @QueryParam("revision") String revision) throws Exception {     
        try {
            if (revision == null || revision.equals("")) {
                String msg = "Revision number not specified";
                throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
            }
            return Blueprints.getInstance().getBlueprint(blueprintName, Integer.parseInt(revision));
        }catch (WebApplicationException we) {
            throw we;
        }catch (Exception e) {
            throw new WebApplicationException((new ExceptionResponse(e)).get());
        }      
    }
    
    /** Delete the blueprint
     * 
     *  <p>
     *  REST:<br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;URL Path                                    : /blueprints/{blueprintName}<br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;HTTP Method                                 : DELETE <br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;HTTP Request Header                         : <br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Content-type        = application/json <br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Accept              = application/json <br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;HTTP Response Header                        : <br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Content-type        = application/json <br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Accept              = application/json <br>
     *  <p> 
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
    
    /** Update a current blueprint.
     *  <p>
     *  Updates a current blueprint to update some of its fields.
     *  <p>
     *  REST:<br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;URL Path                                    : /blueprints/{blueprintName}<br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;HTTP Method                                 : PUT <br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;HTTP Request Header                         : <br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Content-type        = application/json <br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Accept              = application/json <br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;HTTP Response Header                        : <br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Content-type        = application/json <br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Accept              = application/json <br>
     *  <p> 
     * 
     * @param blueprintName             Name of the blueprint
     * @param blueprint                 Input blueprint object specifying the blueprint definition
     * @return                                  Returns the new revision of the blueprint
     * @throws Exception                throws Exception
     */
    @Path(value = "/blueprints/{blueprintName}")
    @PUT
    @Consumes
    public Blueprint updateBlueprint(@PathParam("blueprintName") String blueprintName, Blueprint blueprint) throws Exception {
        return null;
    }
    
}
