/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.eventdb.webservice;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ContextResolver;
import javax.xml.bind.JAXBContext;

import org.apache.ambari.eventdb.model.TaskLocalityData;
import org.apache.ambari.eventdb.model.Jobs;
import org.apache.ambari.eventdb.model.TaskData;
import org.apache.ambari.eventdb.model.WorkflowContext;
import org.apache.ambari.eventdb.model.WorkflowDag;
import org.apache.ambari.eventdb.model.Workflows;

@Provider
public class JAXBContextResolver implements ContextResolver<JAXBContext> {

  /* NOTE: Remember to add any new Model classes to this list. */
  private static final Class[] classes = {
    WorkflowContext.class,
    WorkflowDag.class,
    WorkflowDag.WorkflowDagEntry.class,
    Jobs.class,
    Jobs.JobDBEntry.class,
    Workflows.class,
    Workflows.WorkflowDBEntry.class,
    TaskData.class,
    TaskData.Point.class,
    TaskLocalityData.class,
    TaskLocalityData.DataPoint.class
  };

  private static final Set<Class> types = 
    new HashSet<Class>(Arrays.asList(classes));

  private static final JAXBContext context;

  static {
    JAXBContext tmpContext;

    try {
      tmpContext = new JSONJAXBContext(JSONConfiguration.natural().build(), classes);
    } catch (Exception e) {
      /* Do Nothing (with the exception). */
      tmpContext = null;
    }

    context = tmpContext;
  }

  @Override
  public JAXBContext getContext(Class<?> classType) {
    return (types.contains(classType)) ? context : null;
  }
}
