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

import org.apache.ambari.common.rest.entities.Stack;
import org.apache.ambari.common.rest.entities.StackInformation;
import org.apache.ambari.controller.Stacks;
import org.apache.ambari.controller.ExceptionResponse;
import org.codehaus.jettison.json.JSONArray;

/** 
 * StackResource represents a Hadoop blueprint to be installed on a 
 * cluster. Stacks define a collection of Hadoop components that are
 * installed together on a cluster and their configuration.
 */
@Path(value = "/stacks")
public class StacksResource {
 
    /** 
     * Get the list of stacks
     * 
     * @response.representation.200.doc         Successful
     * @response.representation.200.mediaType   application/json
     * @response.representation.204.doc         List is empty.
     *  
     * @return Returns the list of StackInformation items
     * @throws Exception
     */
    @GET
    @Produces({"application/json", "application/xml"})
    public List<StackInformation> listBlueprints() throws Exception {
        List<StackInformation> list;
        try {
            list = Stacks.getInstance().getStackList();
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
    
}
