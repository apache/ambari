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

package org.apache.ambari.server.controller;

import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionDBInMemoryImpl;
import org.apache.ambari.server.agent.rest.AgentResource;
import org.apache.ambari.server.resources.api.rest.GetResource;
import org.apache.ambari.server.security.unsecured.rest.CertificateDownload;
import org.apache.ambari.server.security.unsecured.rest.CertificateSign;
import org.apache.ambari.server.state.live.Clusters;
import org.apache.ambari.server.state.live.ClustersImpl;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

/**
 * Used for injection purposes.
 *
 */
public class ControllerModule extends AbstractModule {

  @Override
  protected void configure() {

    requestStaticInjection(AgentResource.class);
    requestStaticInjection(CertificateDownload.class);
    requestStaticInjection(CertificateSign.class);
    requestStaticInjection(GetResource.class);
    bind(Clusters.class).to(ClustersImpl.class);
    bind(ActionDBAccessor.class).to(ActionDBInMemoryImpl.class);
    bindConstant().annotatedWith(Names.named("schedulerSleeptime")).to(10L);
    bindConstant().annotatedWith(Names.named("actionTimeout")).to(10L);
  }
}
