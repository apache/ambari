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
import org.apache.ambari.controller.Clusters;
import org.apache.ambari.controller.ExceptionResponse;

import com.sun.jersey.spi.resource.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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
     *  
     *  @param  state      The state of the cluster
     *  @param  search     Optional search expression to return list of matching 
     *                     clusters
     *  @return            Returns the list of clusters based on specified state 
     *                     and optional search criteria.
     *  @throws Exception  throws Exception (TBD)
     */
    @GET
    @Produces({"application/json", "application/xml"})
    public List<ClusterDefinition> getClusterList (
                                 @DefaultValue("ALL") @QueryParam("state") String state,
                                 @DefaultValue("") @QueryParam("search") String search) throws Exception {
        List<ClusterDefinition> searchResults = null;
        try {
            searchResults = Clusters.getInstance().getClusterDefinitionsList(state);
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
     * Add new cluster definition.
     *
     *  Cluster goal state can be either "ACTIVE" or "INACTIVE". In the 
     *  "INACTIVE" state, nodes specified in the cluster definition will be 
     *  reserved for the cluster. Although the actual deployment and starting 
     *  of services would begin when cluster definition is updated to be "ACTIVE"
     *  
     *  For cluster to be in active state cluster definition needs to be 
     *  complete & valid e.g. number of nodes associated are sufficient for 
     *  each role, specified blueprint for cluster configuration should exist 
     *  etc. 
     *  
     *   @param  cluster   Definition of the cluster to be created 
     *   @return           Returns the cluster definition 
     *   @throws Exception Throws exception (TBD)
     */
    @POST
    @Consumes({"application/json", "application/xml"})
    public ClusterDefinition addCluster(ClusterDefinition cluster) throws Exception {
        try {
            return Clusters.getInstance().addCluster(cluster);
        }catch (WebApplicationException we) {
            throw we;
        }catch (Exception e) {
            throw new WebApplicationException((new ExceptionResponse(e)).get());
        } 
    }
}
