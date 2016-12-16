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
import javax.ws.rs.QueryParam;

import org.apache.ambari.view.ViewContext;

public class WorkflowsManagerResource {
	private WorkflowManagerService workflowManagerService;
	
	public WorkflowsManagerResource(ViewContext viewContext) {
		super();
		this.workflowManagerService=new WorkflowManagerService(viewContext);
	}

	@GET
	public Map<String,Object> getWorkflows(){
	    HashMap<String,Object> result=new HashMap<>();
	    result.put("wfprojects", workflowManagerService.getAllWorkflows());
	    return result;
	}
	
	
	@DELETE
	public void deleteWorkflow( @QueryParam("worfkflowPath") String path,
            @DefaultValue("false") @QueryParam("deleteDefinition") Boolean deleteDefinition){
	    workflowManagerService.deleteWorkflow(path,deleteDefinition);
	}
}
