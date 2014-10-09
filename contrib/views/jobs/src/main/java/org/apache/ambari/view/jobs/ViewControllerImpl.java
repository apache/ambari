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

package org.apache.ambari.view.jobs;

import java.lang.String;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.view.ViewContext;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Implementation of controller to handle requests for the Jobs View.
 */
@Singleton
public class ViewControllerImpl implements ViewController {

  @Inject
  private ViewContext viewContext;

  /**
   * Because only an admin user is allowed to see the properties in
   * api/v1/views/JOBS/versions/{version_number}/instances/{instance_name}/ ,
   * we need a way for all users to see the basic properties of the Jobs View, primarily
   * the YARN ATS URL and YARN Resource Manager URL
   * @return Get the properties that any user is allowed to see, even non-admin users.
   */
  @Override
  public ViewStatus getViewStatus() {
    ViewStatus status = new ViewStatus();
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put(ViewController.PARAM_YARN_ATS_URL, getViewParameterValue(ViewController.PARAM_YARN_ATS_URL));
    parameters.put(ViewController.PARAM_YARN_RESOURCEMANAGER_URL, getViewParameterValue(ViewController.PARAM_YARN_RESOURCEMANAGER_URL));
    status.setParameters(parameters);
    return status;
  }

  /**
   * @param parameterName Parameter to get the value for
   * @return Returns the value of the given parameter
   */
  private String getViewParameterValue(String parameterName) {
    String value = viewContext.getProperties().get(parameterName);
    if ("null".equals(value)) {
      return null;
    }
    return value;
  }
}
