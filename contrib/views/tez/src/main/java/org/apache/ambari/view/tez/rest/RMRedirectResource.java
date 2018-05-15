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

package org.apache.ambari.view.tez.rest;

import com.google.inject.Inject;
import org.apache.ambari.view.tez.ViewController;
import org.apache.ambari.view.tez.utils.ProxyHelper;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Proxy class to redirect call to Active RM server
 */
public class RMRedirectResource extends BaseRedirectionResource {

  private ViewController viewController;
  private ProxyHelper proxyHelper;

  @Inject
  public RMRedirectResource(ViewController viewController, ProxyHelper proxyHelper) {
    this.viewController = viewController;
    this.proxyHelper = proxyHelper;
  }

  @Override
  public String getProxyUrl(String endpoint, MultivaluedMap<String, String> queryParams) {
    String activeRMUrl = viewController.getActiveRMUrl();
    return proxyHelper.getProxyUrl(activeRMUrl, endpoint, queryParams, getAuthType());
  }

  @Override
  public String getAuthType() {
    return viewController.getRMAuthenticationType();
  }
}
