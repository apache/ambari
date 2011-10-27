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

import org.apache.ambari.common.rest.entities.ClusterDefinition;
import org.apache.ambari.common.rest.entities.ClusterInformation;
import org.apache.ambari.common.rest.entities.ClusterState;
import org.apache.ambari.common.rest.entities.Node;
import org.apache.ambari.common.rest.entities.Stack;
import org.apache.ambari.controller.Clusters;
import org.apache.ambari.controller.ExceptionResponse;
import org.apache.ambari.controller.Nodes;
import org.apache.ambari.controller.rest.config.Examples;

import com.sun.jersey.spi.resource.Singleton;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;


/**
 * Clusters Resource represents the collection of Hadoop clusters in a data center
 */
@Path(value = "/clusters")
public class ClustersResource {
    
    /** 
     * Get the list of clusters.
     *
     *  State: "ALL"           : All the clusters (irrespective of their state), 
     *         "ACTIVE"        : All the active state clusters
     *         "INACTIVE"      : All the inactive state clusters
     *         "ATTIC"         : All the retired i.e. ATTIC state clusters
     *  @response.representation.200.doc Return ClusterInformation
     *  @response.representation.200.mediaType application/json
     *  @response.representation.200.example
     *  @response.representation.204.doc No cluster defined
     *  @response.representation.500.doc Internal Server Error
     *  @param  state      The state of the cluster
     *  @param  search     Optional search expression to return list of matching 
     *                     clusters
     *  @return            Returns the list of clusters based on specified state 
     *                     and optional search criteria.
     *  @throws Exception  throws Exception (TBD)
     */
    @GET
    @Produces({"application/json", "application/xml"})
    public List<ClusterInformation> getClusterList (
                                 @DefaultValue("ALL") @QueryParam("state") String state,
                                 @DefaultValue("") @QueryParam("search") String search) throws Exception {
        List<ClusterInformation> searchResults = null;
        try {
            searchResults = Clusters.getInstance().getClusterInformationList(state);
            if (searchResults.isEmpty()) {
                throw new WebApplicationException(Response.Status.NO_CONTENT);
            }   
        }catch (WebApplicationException we) {
            throw we;
        }catch (Exception e) {
            throw new WebApplicationException((new ExceptionResponse(e)).get());
        } 
        return searchResults;
    }
    
    
    /** 
     * Get the information of a specified Hadoop Cluster. Information includes Cluster definition 
     * and the cluster state.
     * 
     *  @response.representation.200.doc        Get the definition & current state of the specified Hadoop cluster
     *  @response.representation.200.mediaType  application/json
     *  @response.representation.200.example    {@link Examples#CLUSTER_INFORMATION}
     *  @response.representation.404.doc        Specified cluster does not exist
     *  
     *  @param      clusterName                 Name of the cluster AAAA
     *  @return                                 Returns the Cluster Information
     *  @throws     WebApplicationException     Throws exception (TBD)
     */
    @Path(value = "/{clusterNamez}")
    @GET
    @Produces({"application/json", "application/xml"})
    public ClusterInformation getClusterDefinition(@PathParam("clusterNamez") String clusterName) throws WebApplicationException {
        try {
            return Clusters.getInstance().getClusterInformation(clusterName);
        }catch (WebApplicationException we) {
            throw we;
        }catch (Exception e) {
            throw new WebApplicationException((new ExceptionResponse(e)).get());
        }       
    }
    
    /** 
     * Add/Update cluster definition. If cluster does not exist it will created.
     *  
     *  While creating new cluster, cluster definition must specify name, stack name 
     *  and nodes associated with the cluster. 
     *  Default values for new cluster definition parameters, if not specified
     *    -- goalstate          = "INACTIVE"  (optionally, it can be set to ACTIVE)
     *    -- stack revision = latest revision
     *    -- RoleToNodes        = If explicit association is not specified then Ambari
     *                            will determine the optimal role to nodes association. 
     *                            User can view it by running the command in dry_run.
     *    -- active services    = "ALL" i.e. if not specified all the configured 
     *                            services will be activated
     *    -- description        = Default description will be associated
     *    -- dry_run            = false
     *  
     *  
     *  For new cluster to be in active state cluster definition needs to be 
     *  complete & valid e.g. number of nodes associated are sufficient for 
     *  each role, specified stack for cluster configuration should exist 
     *  etc. 
     *  
     *  Updating existing cluster definition would require only specific sub elements
     *  of cluster definition to be specified. 
     * 
     * @response.representation.200.doc         Returns new or updated cluster definition.
     * @response.representation.200.mediaType   application/json
     * @response.representation.200.example     {@link Examples#CLUSTER_DEFINITION}
     * @response.representation.400.doc         Bad request (See "ErrorMessage" in the response
     *                                          http header describing specific error condition).
     * 
     * @param   clusterName                     Name of the cluster
     * @param   dry_run                         Run without actual execution
     * @param   cluster                         Cluster definition to be created new or updated existing one
     *                                          Cluster name can not be updated through this API.
     * @return                                  Returns updated cluster definition
     * @throws  Exception                       throws Exception
     */ 
    @Path(value = "/{clusterNamey}")
    @PUT
    @Consumes({"application/json", "application/xml"})
    @Produces({"application/json", "application/xml"})
    public ClusterDefinition updateClusterDefinition(
           @PathParam("clusterNamey") String clusterName,
           @DefaultValue("false") @QueryParam("dry_run") boolean dry_run,
           ClusterDefinition cluster) throws Exception {    
        try {
            return Clusters.getInstance().updateCluster(clusterName, cluster, dry_run);
        }catch (WebApplicationException we) {
            throw we;
        }catch (Exception e) {
            throw new WebApplicationException((new ExceptionResponse(e)).get());
        }     
    }
     
    /** 
     * Rename the cluster.
     * 
     * @response.representation.200.doc         Rename the cluster. This operation is allowed only
     *                                          when cluster is in ATTIC state
     * @response.representation.200.mediaType   application/json
     * @response.representation.400.doc         Bad request (See "ErrorMessage" in the response
     *                                          http header describing specific error condition).
     * @response.representation.406.doc         Not Acceptable. Cluster is not in ATTIC state.
     * @response.representation.404.doc         Cluster does not exist
     * 
     * @param   clusterName                     Existing name of the cluster  FFFFF
     * @param   new_name                        New name of the cluster
     * @throws  Exception                       throws Exception (TBD)
     */ 
    @PUT
    @Consumes({"application/json", "application/xml"})
    @Path(value = "/clusters/{clusterName}/rename")
    public void renameCluster(
           @PathParam("clusterName") String clusterName,
           @DefaultValue("") @QueryParam("new_name") String new_name) throws Exception {    
        try {
            Clusters.getInstance().renameCluster(clusterName, new_name);
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
     *                                  repository when all the nodes are released 
     *                                  from the cluster. It is asynchronous operation.
     *                                  In "ATTIC" state all the 
     *                                  cluster services would be stopped and 
     *                                  nodes are released. All the cluster data 
     *                                  will be lost.
     *  
     *  @param  clusterName             Name of the cluster BBBB
     *  @throws Exception               throws Exception (TBD)
     */
    @Path(value = "/{clusterNamex}")
    @DELETE
    @Consumes({"application/json", "application/xml"})
    public void deleteCluster(@PathParam("clusterNamex") String clusterName) throws Exception {
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
     *  @response.representation.200.doc            This provides the run time state of the 
     *                                              cluster. Cluster state is derived based 
     *                                              on the state of various services running on the cluster.
     *                                              Representative cluster states:
     *                                                  "ACTIVE"  : Hadoop stack is deployed on cluster nodes and 
     *                                                              required cluster services are running
     *                                                  "INACTIVE : No cluster services are running. Hadoop stack 
     *                                                              may or may not be deployed on the cluster nodes
     *                                                  "ATTIC"   : Only cluster definition is available. No nodes are 
     *                                                              reserved for the cluster in this state.
     *  @response.representation.200.mediaType   application/json
     *  @response.representation.200.example     {@link Examples#CLUSTER_STATE}
     *  @response.representation.404.doc         Cluster does not exist
     *  
     *  @param  clusterName             Name of the cluster EEEE
     *  @return                         Returns cluster state object.
     *  @throws Exception               throws Exception (TBD)  
     */
    @Path(value = "/{clusterName}/state")
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
     *  @response.representation.200.doc Get list of nodes associated with the cluster.
     *  The "alive" is a boolean variable that 
     *  specify the type of nodes to return based on their state i.e. live or 
     *  dead. Live nodes are the ones that are consistently heart beating with 
     *  the controller. If both live and dead nodes need to be returned 
     *  then do not specify the alive query parameter
     *  @response.representation.200.mediaType application/json
     *  @response.representation.200.example {@link Examples#NODES}
     *  @response.representation.204.doc    No content; No nodes are associated with the cluster
     *  @response.representation.500.doc    Internal Server Error; No nodes are associated with the cluster
     *                                      (See "ErrorMessage" in the response http header describing specific error condition).
     *  
     *  @param  clusterName Name of the cluster CCCC
     *  @param  role        Optionally specify the role name to get the nodes 
     *                      associated with the service role
     *  @param  alive       Boolean value (true/false) to specify, if nodes to be 
     *                      returned are alive or dead. if this query parameter is 
     *                      is not specified them all the nodes associated with cluster
     *                      are returned) 
     *  @return             List of nodes
     *  @throws Exception   throws Exception
     */
    @Path(value = "/{clusterName}/nodes")
    @GET
    @Produces({"application/json", "application/xml"})
    public List<Node> getNodes (@PathParam("clusterName") String clusterName,
                                @DefaultValue("") @QueryParam("role") String role,
                                @DefaultValue("") @QueryParam("alive") String alive) throws Exception {    
        try {
            List<Node> list = Nodes.getInstance().getClusterNodes(clusterName, role, alive);
            
            if (list.isEmpty()) {
                String msg = "No nodes found!";
                throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.NO_CONTENT)).get());
            }
            return list;
        }catch (WebApplicationException we) {
            throw we;
        }catch (Exception e) {
            throw new WebApplicationException((new ExceptionResponse(e)).get());
        }   
    }
    
    /** 
     * Get the stack associated with cluster
     *  
     *  @response.representation.200.doc Get the stack associated with cluster
     *  @response.representation.200.mediaType   application/json
     *  @response.representation.200.example {@link Examples#STACK}
     *  @response.representgation.404.doc        Cluster does not exist
     *  
     *  @param  clusterName Name of the cluster DDDD
     *  @param  expanded    Optionally specify the boolean value to indicate if 
     *                      to retrieved the cluster level stack or the fully
     *                      derived stack in-lining the parent stacks 
     *                      associated with the service role
     *  @return             Stack
     *  @throws Exception   throws Exception
     */
    @Path(value = "/{clusterName}/stack")
    @GET
    @Produces({"application/json", "application/xml"})
    public Stack getClusterStack (@PathParam("clusterName") String clusterName,
                                @DefaultValue("true") @QueryParam("expanded") boolean expanded) throws Exception {    
        try {
            return Clusters.getInstance().getClusterStack(clusterName, expanded);
        }catch (WebApplicationException we) {
            throw we;
        }catch (Exception e) {
            throw new WebApplicationException((new ExceptionResponse(e)).get());
        }   
    }
}
