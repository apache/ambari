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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.ambari.common.rest.entities.Blueprint;

/** BlueprintResource represents a Hadoop blueprint to be installed on a 
 *  cluster. Blueprints define a collection of Hadoop components that are
 *  installed together on a cluster and their configuration.
 */
@Path(value = "/blueprints")
public class BlueprintsResource {
 
    /** Creates a new blueprint.
     *  <p>
     * 	If named blueprint does not exists already, then it creates new one i.e. revision zero.
     *  <p>
	 *  REST:<br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;URL Path                                    : /blueprints/<br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;HTTP Method                                 : POST <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;HTTP Request Header	                        : <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Content-type        = application/json <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Accept              = application/json <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;HTTP Response Header                        : <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Content-type        = application/json <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Accept              = application/json <br>
	 *  <p> 
     * 
     * @param blueprint			Input blueprint object specifying the blueprint definition
     * @return					Returns the newly created revision of the blueprint
     * @throws Exception		throws Exception
     */
    @POST
    @Consumes
    public Blueprint createBlueprint(Blueprint blueprint) throws Exception {
    	return null;
    }

    /** Get the list of blueprint names
     *  <p>
	 *  REST:<br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;URL Path                                    : /blueprints<br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;HTTP Method                                 : GET <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;HTTP Request Header	                        : <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Content-type        = application/json <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Accept              = application/json <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;HTTP Response Header                        : <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Content-type        = application/json <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Accept              = application/json <br>
	 *  <p> 
     * 
     * @return	 				Returns the list of blueprint names
     * @throws Exception		throws Exception
     */
    @GET
    @Consumes
    public List<String> listBlueprints() throws Exception {
    	return null;
    }
    
}
