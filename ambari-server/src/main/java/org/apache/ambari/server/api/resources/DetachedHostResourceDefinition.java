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


package org.apache.ambari.server.api.resources;

import org.apache.ambari.server.api.query.render.HostSummaryRenderer;
import org.apache.ambari.server.api.query.render.Renderer;
import org.apache.ambari.server.controller.internal.HostResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;


/**
 * Resource definition for /hosts resources.
 */
public class DetachedHostResourceDefinition extends BaseResourceDefinition {

  public DetachedHostResourceDefinition() {
    super(Resource.Type.Host);
  }

  @Override
  public String getPluralName() {
    return "hosts";
  }

  @Override
  public String getSingularName() {
    return "host";
  }

  @Override
  public Renderer getRenderer(String name) {
    if (HostResourceProvider.SUMMARY_PROPERTY_ID.equals(name)) {
      return new HostSummaryRenderer();
    }
    return super.getRenderer(name);
  }

}
