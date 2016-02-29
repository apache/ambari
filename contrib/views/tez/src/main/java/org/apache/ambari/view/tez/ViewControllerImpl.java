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

package org.apache.ambari.view.tez;

import java.lang.String;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.cluster.Cluster;
import org.apache.ambari.view.tez.exceptions.ATSUrlFetchException;
import org.apache.ambari.view.tez.exceptions.ActiveRMFetchException;
import org.apache.ambari.view.utils.ambari.AmbariApi;
import org.apache.ambari.view.utils.ambari.AmbariApiException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Implementation of controller to handle requests for the tez View.
 */
@Singleton
public class ViewControllerImpl implements ViewController {

  private AmbariApi ambariApi;

  @Inject
  public ViewControllerImpl(ViewContext viewContext) {
    this.ambariApi = new AmbariApi(viewContext);
  }

  /**
   * Because only an admin user is allowed to see the properties in
   * api/v1/views/tez/versions/{version_number}/instances/{instance_name}/ ,
   * we need a way for all users to see the basic properties of the tez View, primarily
   * the YARN ATS URL and YARN Resource Manager URL
   * @return Get the properties that any user is allowed to see, even non-admin users.
   */
  @Override
  public ViewStatus getViewStatus() {

    ViewStatus status = new ViewStatus();
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put(ViewController.PARAM_YARN_ATS_URL, getActiveATSUrl());
    parameters.put(ViewController.PARAM_YARN_RESOURCEMANAGER_URL, getActiveRMUrl());
    parameters.put(ViewController.PARAM_YARN_PROTOCOL, getYARNProtocol());
    status.setParameters(parameters);
    return status;
  }

  @Override
  public String getActiveATSUrl() {
    try {
      return ambariApi.getServices().getTimelineServerUrl();
    } catch (AmbariApiException ex) {
      throw new ATSUrlFetchException(ex);
    }
  }

  @Override
  public String getActiveRMUrl() {
    try {
      return ambariApi.getServices().getRMUrl();
    } catch (AmbariApiException ex) {
      throw new ActiveRMFetchException(ex);
    }
  }

  @Override
  public String getYARNProtocol() {
    return ambariApi.getServices().getYARNProtocol();
  }
}

