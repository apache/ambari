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
@Singleton
@Path(value = "/clusters")
public class ClustersResource {
	
    public ClustersResource() throws Exception {	
        ClusterDefinition cluster123 = new ClusterDefinition();
        ClusterDefinition cluster124 = new ClusterDefinition();
        cluster123.setName("blue.dev.Cluster123");
       //cluster123.setBlueprintURI("http://localhost:123/blueprint");
       // cluster123.setDescription("test cluster");
       // cluster124.setName("blue.research.Cluster124");
       //cluster124.setBlueprintURI("http://localhost:124/blueprint");
       // cluster124.setDescription("production cluster");
       // Clusters.getInstance().addCluster(cluster123, Clusters.GOAL_STATE_ATTIC);
       // Clusters.getInstance().addCluster(cluster124, Clusters.GOAL_STATE_ATTIC);
    }  
    
    /** Get the list of clusters.
     *  <p>
     *  State: <br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;"ALL"		: All the clusters (irrespective of their state), 
     *  &nbsp;&nbsp;&nbsp;&nbsp;"ACTIVE"	: All the active state clusters
     *  &nbsp;&nbsp;&nbsp;&nbsp;"INACTIVE"	: All the inactive state clusters
     *  &nbsp;&nbsp;&nbsp;&nbsp;"ATTIC"		: All the retired i.e. ATTIC state clusters
     *  
     *  <p>
	 *  REST:<br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;URL Path                                    : /clusters<br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;HTTP Method                                 : GET <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;HTTP Request Header	                        : <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Content-type        = application/json <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Accept              = application/json <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;HTTP Response Header                        : <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Content-type        = application/json <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Accept              = application/json <br>
	 *  <p> 
	 *  
     *  @param	state			The state of the cluster
     *  @param	search   		Optional search expression to return list of matching clusters
     *  @return					Returns the list of clusters based on specified state and optional search criteria.
     *  @throws	Exception		throws Exception (TBD)
     */
    @GET
    @Produces({"application/json", "application/xml"})
    public List<ClusterDefinition> getClusterList (
    						 @DefaultValue("ALL") @QueryParam("state") String state,
    		                 @DefaultValue("") @QueryParam("search") String search) throws Exception {
    	List<ClusterDefinition> searchResults = null;
    	if (!search.equals("")) {
    		/*
    		 * TODO: Implement search 
    		searchResults = new ArrayList<Cluster>();
    		for (Cluster cls : Clusters.getInstance().getClusterList(state)) {
    			if (cls.getName().matches("^.*"+search+".*$")) {
    				searchResults.add(cls);
    			}
    		}
    		*/
    	} else {
    		//searchResults = Clusters.getInstance().getClusterList(state);
    	}
    
    	if (searchResults.isEmpty()) {
    		throw new WebApplicationException(Response.Status.NOT_FOUND);
    	}
    	
    	return searchResults;
    }
    
    /** Add new cluster definition.
	 *  <p>
	 *  Cluster goal state can be either "ACTIVE" or "INACTIVE". In the "INACTIVE" state, nodes specified in the 
	 *  cluster definition will be reserved for the cluster. Although the actual deployment and starting of services 
	 *  would begin when cluster definition is updated to be "ACTIVE"
     *  <p>   
     *  For cluster to be in active state cluster definition needs to be complete & valid 
     *  e.g. number of nodes associated are sufficient for each role, specified blueprint for cluster configuration
     *  should exist etc. 
     *  
     *  <p>
	 *  REST:<br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;URL Path                                    : /clusters/<br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;HTTP Method                                 : POST <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;HTTP Request Header	                        : <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Content-type        = application/json <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Accept              = application/json <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;HTTP Response Header                        : <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Content-type        = application/json <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Accept              = application/json <br>
	 *  <p> 
     *  
     *   @param		cluster			Definition of the cluster to be created 
     *   @return					Returns the cluster definition 
     *   @throws	Exception		Throws exception (TBD)
     */
    @POST
	@Consumes({"application/json", "application/xml"})
	public ClusterDefinition addCluster(ClusterDefinition cluster) throws Exception {
		Clusters.getInstance().addCluster(cluster);
		return null;
	}
}
