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
package org.apache.oozie.ambari.view.workflowmanager;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.ambari.view.ViewContext;
import org.apache.oozie.ambari.view.exception.WfmWebException;

public class WorkflowsManagerResource {
	private final WorkflowManagerService workflowManagerService;
	private final ViewContext viewContext;
	public WorkflowsManagerResource(ViewContext viewContext) {
		super();
		this.viewContext=viewContext;
		this.workflowManagerService=new WorkflowManagerService(viewContext);
	}

  @GET
  public Response getWorkflows() {
    try {
      HashMap<String, Object> result = new HashMap<>();
      result.put("wfprojects", workflowManagerService.getAllWorkflows(viewContext.getUsername()));
      return Response.ok(result).build();
    } catch (Exception ex) {
      throw new WfmWebException(ex);
    }
  }


  @DELETE
	@Path("/{projectId}")
	public Response deleteWorkflow(@PathParam("projectId") String id,
                                 @DefaultValue("false") @QueryParam("deleteDefinition") Boolean deleteDefinition){
	  try{
      workflowManagerService.deleteWorkflow(id,deleteDefinition);
      return Response.ok().build();
    }catch (Exception ex) {
      throw new WfmWebException(ex);
    }
	}
}
