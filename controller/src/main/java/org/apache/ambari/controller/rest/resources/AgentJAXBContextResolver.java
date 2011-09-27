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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import com.sun.jersey.api.json.JSONJAXBContext;
import org.apache.ambari.common.rest.entities.agent.Command;
import org.apache.ambari.common.rest.entities.agent.HeartBeat;
import org.apache.ambari.common.rest.entities.agent.ControllerResponse;
import org.apache.ambari.common.rest.entities.agent.Action;
import org.apache.ambari.common.rest.entities.agent.ActionResult;

@Provider
public class AgentJAXBContextResolver implements ContextResolver<JAXBContext> {
  private JAXBContext context;
  private Set<Class<?>> types;
  private Class<?>[] classTypes = {
      Command.class, 
      HeartBeat.class, 
      ControllerResponse.class, 
      ActionResult.class,
      Action.class };
  protected Set<String> jsonArray = new HashSet<String>() {
    {
      add("commandResults");
      add("cleanUpCommandResults");
      add("actionResults");
      add("serversStatus");
      add("cmd");
      add("commands");
      add("cleanUpCommands");
    }
  };
  
  public AgentJAXBContextResolver() throws Exception {
    Map<String, Object> props = new HashMap<String, Object>();
    props.put(JSONJAXBContext.JSON_NOTATION, JSONJAXBContext.JSONNotation.MAPPED);
    props.put(JSONJAXBContext.JSON_ROOT_UNWRAPPING, Boolean.TRUE);
    props.put(JSONJAXBContext.JSON_ARRAYS, jsonArray);
    this.types = new HashSet<Class<?>>(Arrays.asList(classTypes));
    this.context = new JSONJAXBContext(classTypes, props);
  }

  public JAXBContext getContext(Class<?> objectType) {
    for(Class<?> c : types) {
      if(c==objectType) {
        return context;
      }
    }
    return null;
  }
}
