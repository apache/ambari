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
import javax.ws.rs.core.Response.ResponseBuilder;


import org.apache.ambari.common.rest.entities.Cluster;
import org.apache.ambari.common.rest.entities.ClusterDefinition;
import org.apache.ambari.common.rest.entities.Node;
import org.apache.ambari.common.rest.entities.ClusterState;
import org.apache.ambari.controller.Clusters;
import org.apache.ambari.controller.ExceptionResponse;
import org.apache.ambari.controller.Nodes;

/** 
 * ClusterResource represents a Hadoop Cluster in a data center.
 *  
 */
@Path(value = "/clusters/{clusterName}")
public class ClusterResource {
        
    /** 
     * Get the definition of specified Hadoop cluster.
     * 
     *  @response.representation.200.doc       Get the definition of specified
     *                                         Hadoop cluster
     *  @response.representation.200.mediaType application/json
     *  
     *  @param      clusterName             Name of the cluster; Each cluster is identified w/ unique name
     *  @return                             Returns the Cluster definition
     *  @throws     Exception               Throws exception (TBD)
     */
    @GET
    @Produces({"application/json", "application/xml"})
    public ClusterDefinition getClusterDefinition(@PathParam("clusterName") String clusterName) throws WebApplicationException {
        try {
            return Clusters.getInstance().getClusterDefinition(clusterName);
        }catch (WebApplicationException we) {
            throw we;
        }catch (Exception e) {
            throw new WebApplicationException((new ExceptionResponse(e)).get());
        }       
    }
    
    /** 
     * Update cluster definition.
     * 
     * @request.representation.example { 'abc' : 'abc' }
     * @response.representation.200.doc Returns cluster definition.
     * 
     * @param   clusterName             Name of the cluster; Each cluster is identified w/ unique name
     * @param   cluster                 Cluster definition with specific sub-elements to be updated
     * @return                          Returns updated cluster definition
     * @throws  Exception               throws Exception (TBD)
     */ 
    @PUT
    @Consumes({"application/json", "application/xml"})
    public ClusterDefinition updateClusterDefinition(@PathParam("clusterName") String clusterName, ClusterDefinition cluster) throws Exception {    
        try {
            return Clusters.getInstance().updateCluster(clusterName, cluster);
        }catch (WebApplicationException we) {
            throw we;
        }catch (Exception e) {
            throw new WebApplicationException((new ExceptionResponse(e)).get());
        }     
    }
     
    /** 
     * Delete the the cluster.
     * 
     * @response.representation.200.doc Delete operation will lead the cluster 
     *                                  to "ATTIC" state and then the cluster 
     *                                  definition is purged from the controller 
     *                                  repository. In "ATTIC" state all the 
     *                                  cluster services would be stopped and 
     *                                  nodes are released. All the data on 
     *                                  the cluster will be lost.
     *  
     *  @param  clusterName             Name of the cluster; Each cluster is identified w/ unique name
     *  @throws Exception               throws Exception (TBD)
     */
    @DELETE
    @Consumes({"application/json", "application/xml"})
    public void deleteCluster(@PathParam("clusterName") String clusterName) throws Exception {
        try {
            Clusters.getInstance().deleteCluster(clusterName);
        }catch (WebApplicationException we) {
            throw we;
        }catch (Exception e) {
            throw new WebApplicationException((new ExceptionResponse(e)).get());
        }    
    }
    
    /** 
     * Get the cluster state.
     *  
     *  @response.representation.200.doc This provides the run time state of the 
     *                                   cluster. Representative cluster state 
     *                                   is based on the state of various 
     *                                   services running on the cluster.
     *  Representative cluster states:
     *    "ACTIVE"  : Hadoop stack is deployed on cluster nodes and 
     *                required cluster services are running
     *    "INACTIVE : No cluster services are running. Hadoop stack 
     *                may or may not be deployed on the cluster nodes
     *    "ATTIC"   : Only cluster definition is available. No nodes are 
     *                reserved for the cluster in this state.
     *  
     *  @param  clusterName             Name of the cluster; Each cluster is 
     *                                  identified w/ unique name
     *  @return                         Returns cluster state object.
     *  @throws Exception               throws Exception (TBD)  
     */
    @Path(value = "/state")
    @GET
    @Produces({"application/json", "application/xml"})
    public ClusterState getClusterState(@PathParam("clusterName") String clusterName) throws Exception {
        try {
            return Clusters.getInstance().getClusterState(clusterName);
        }catch (WebApplicationException we) {
            throw we;
        }catch (Exception e) {
            throw new WebApplicationException((new ExceptionResponse(e)).get());
        }    
    }
    
    /** 
     * Get list of nodes associated with the cluster.
     *  
     *  @response.representation.200.doc The "alive" is a boolean variable that 
     *  specify the type of nodes to return based on their state i.e. live or 
     *  dead. Live nodes are the ones that are consistently heart beating with 
     *  the controller. If both live and dead nodes are need to be returned 
     *  then specify the alive parameter as null.  
     *  
     *  @param  clusterName Name of the cluster; Each cluster is identified w/ 
     *                      unique name
     *  @param  roleName    Optionally specify the role name to get the nodes 
     *                      associated with the service role
     *  @param  alive       Boolean value to specify, if nodes to be returned 
     *                       are alive or dead or both (if alive is set to null) 
     *  @return             List of nodes
     *  @throws Exception   throws Exception
     */
    @Path(value = "/nodes")
    @GET
    @Produces({"application/json", "application/xml"})
    public List<Node> getNodes (@PathParam("clusterName") String clusterName,
                                @DefaultValue("") @QueryParam("role") String role,
                                @DefaultValue("") @QueryParam("alive") boolean alive) throws Exception {    
        try {
            return Nodes.getInstance().getClusterNodes(clusterName, role, alive);
        }catch (WebApplicationException we) {
            throw we;
        }catch (Exception e) {
            throw new WebApplicationException((new ExceptionResponse(e)).get());
        }   
    }
}
