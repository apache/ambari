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

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

import org.apache.ambari.common.rest.entities.Node;
import org.apache.ambari.controller.ExceptionResponse;
import org.apache.ambari.controller.Nodes;


/** Nodes Resource represents collection of cluster nodes.
 */
@Path(value = "/nodes")
public class NodesResource {
            
    /** Get list of nodes
     *
     *  The "allocated and "alive" are the boolean variables that specify the type of nodes to return based on their state i.e. if they are already allocated to any cluster and live or dead. 
     *  Live nodes are the ones that are consistently heart beating with the controller. If both "allocated" and "alive" are set to NULL then all the nodes are returned.  
     *  
     *  @param  allocated               Boolean value to specify, if nodes to be returned are allocated/reserved for some cluster (specify null to return both allocated and unallocated nodes)
     *  @param  alive                   Boolean value to specify, if nodes to be returned are alive or dead or both (specify null to return both live and dead nodes) 
     *  @return                         List of nodes
     *  @throws Exception               throws Exception
     */
    @GET
    @Produces({"application/json", "application/xml"})
    public List<Node> getNodesList (@DefaultValue("") @QueryParam("allocated") String allocated,
                                @DefaultValue("") @QueryParam("alive") String alive) throws Exception {
        try {
            return Nodes.getInstance().getNodesByState(allocated, alive);
        }catch (WebApplicationException we) {
            throw we;
        }catch (Exception e) {
            throw new WebApplicationException((new ExceptionResponse(e)).get());
        } 
    }

    /*
     * Get specified Node information
     */
    /** 
     * Get the node information that includes, service states, node attributes etc.
     * 
     * @param hostname          Fully qualified hostname
     * @return                  Returns the node information
     * @throws Exception        throws Exception
     */
    @Path(value = "/{hostname}")
    @GET
    @Produces({"application/json", "application/xml"})
    public Node getNode (@PathParam("hostname") String hostname) throws Exception {
        try {
            return Nodes.getInstance().getNode(hostname);
        }catch (WebApplicationException we) {
            throw we;
        }catch (Exception e) {
            throw new WebApplicationException((new ExceptionResponse(e)).get());
        } 
    }
}
