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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.ambari.common.rest.entities.Stack;
import org.apache.ambari.common.rest.entities.StackInformation;
import org.apache.ambari.controller.Clusters;
import org.apache.ambari.controller.Stacks;
import org.apache.ambari.controller.ExceptionResponse;
import org.apache.ambari.controller.rest.config.Examples;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;

/** 
 * StackResource represents a Hadoop stack to be installed on a 
 * cluster. Stacks define a collection of Hadoop components that are
 * installed together on a cluster and their configuration.
 */
@Path("stacks")
public class StacksResource {
 
    private static Log LOG = LogFactory.getLog(StacksResource.class);
    private static Stacks stacks;
    private static Clusters clusters;
    
    @Inject
    static void init(Stacks s, Clusters c) {
      stacks = s;
      clusters = c;
    }
    
    /** 
     * Get the list of stacks
     * 
     * @response.representation.200.doc         Successful
     * @response.representation.200.mediaType   application/json application/xml
     * @response.representation.200.example     {@link Examples#STACK_INFORMATION}
     * @response.representation.204.doc         List is empty.
     *  
     * @return Returns the list of StackInformation items
     * @throws Exception
     */
    @GET
    @Path("")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<StackInformation> listStacks() throws Exception {
        List<StackInformation> list;
        try {
            list = stacks.getStackList();
            if (list.isEmpty()) {
                throw new WebApplicationException(Response.Status.NO_CONTENT);
            } 
            return list;
        }catch (WebApplicationException we) {
            throw we;
        }catch (Exception e) {
            LOG.error("Caught error in get stacks", e);
            throw new WebApplicationException((new ExceptionResponse(e)).get());
        } 
    }
    
    /** 
     * Get a stack
     * 
     * @response.representation.200.doc       Get a stack
     * @response.representation.200.mediaType application/json application/xml
     * @response.representation.200.example   {@link Examples#CLUSTER_INFORMATION}
     *  
     * @param  stackName       Name of the stack
     * @param  revision        The optional stack revision, if not specified get the latest revision
     * @return                 stack definition
     * @throws Exception       throws Exception 
     */
    @GET
    @Path("{stackName}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Stack getStack(@PathParam("stackName") String stackName, 
                                  @DefaultValue("-1") @QueryParam("revision") String revision) throws Exception {     
        try {
            return stacks.getStack(stackName, Integer.parseInt(revision));
        }catch (WebApplicationException we) {
            throw we;
        }catch (Exception e) {
            throw new WebApplicationException((new ExceptionResponse(e)).get());
        }      
    }
    
    /** 
     * Get a stack's revisions
     * 
     * @response.representation.200.doc       Get stack revisions
     * @response.representation.200.mediaType application/json application/xml
     * @response.representation.200.example   {@link Examples#STACK_INFORMATION}
     *  
     * @param  stackName       Name of the stack
     * 
     * @return                 List of stack revisions
     * @throws Exception       throws Exception
     */
    @GET
    @Path("{stackName}/revisions")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<StackInformation> getstackRevisions(@PathParam("stackName") String stackName) throws Exception {     
        try {
            List<StackInformation> list = stacks.getStackRevisions(stackName);
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
     * Delete the stack
     * 
     * @response.representation.200.doc       Delete a stack
     * @response.representation.200.mediaType application/json application/xml
     *  
     * @param  stackName        Name of the stack
     * @param  revision         Revision of the stack
     * @throws Exception        throws Exception (TBD)
     */
    @DELETE
    @Path("{stackName}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response deletestack(@PathParam("stackName") String stackName) throws Exception {     
        try {
            if (clusters.isStackUsed(stackName)) {
              throw new WebApplicationException(new ExceptionResponse(stackName+ 
                  " is still used by one or more clusters.",
                  Response.Status.BAD_REQUEST).get());
            }
            stacks.deleteStack(stackName);
            return Response.ok().build();
        }catch (WebApplicationException we) {
            throw we;
        }catch (Exception e) {
            throw new WebApplicationException((new ExceptionResponse(e)).get());
        }    
    }
    
    /** 
     * Create/Update the stack.
     *
     * If named stack does not exist already, then it is created with revision zero.
     * If named stack exists, then it is updated as new revision.
     * Optional locationURL query parameter can specify the location of the repository of
     * of stacks. If specified then stack is downloaded from the repository.
     *
     * @response.representation.200.doc         Successfully created the new or updated the existing stack.
     * @response.representation.200.mediaType   application/json application/xml
     * @response.representation.200.example     {@link Examples#STACK}
     * @response.representation.404.doc         Specified stack does not exist. In case of creating new one, 
     *                                          it is not found in repository where in case of update, it does not
     *                                          exist.    
     * 
     * @param stackName Name of the stack
     * @param locationURL   URL pointing to the location of the stack
     * @param stack         Input stack object specifying the stack definition
     * @return              Returns the new revision of the stack
     * @throws Exception    throws Exception
     */
    @PUT
    @Path("{stackName}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Stack updateStack(@PathParam("stackName") String stackName, 
                                     @DefaultValue("") @QueryParam("url") String locationURL,
                                     Stack stack) throws Exception {
        try {
            if (locationURL == null || locationURL.equals("")) {
                return stacks.addStack(stackName, stack);
            } else {
                return stacks.importDefaultStack (stackName, locationURL);
            }
        }catch (WebApplicationException we) {
            throw we;
        }catch (Exception e) {
            throw new WebApplicationException((new ExceptionResponse(e)).get());
        } 
    } 
}
