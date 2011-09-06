/*
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

package org.apache.hms.common.entity.action;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;

/**
 * Utility class to resolve the formatting style of the serialized action.
 *
 */
@Provider
public class ActionContextProvider implements ContextResolver<JAXBContext> {

  private JAXBContext context;
  private Class[] types = { Action.class, DaemonAction.class, PackageAction.class, ScriptAction.class };

  public ActionContextProvider() throws Exception {
    this.context = new JSONJAXBContext(JSONConfiguration.badgerFish().build(), types);
  }

  public JAXBContext getContext(Class<?> objectType) {
    for (Class type : types) {
      if (type.equals(objectType))
        return context;
    }
    return null;
  } 
}
