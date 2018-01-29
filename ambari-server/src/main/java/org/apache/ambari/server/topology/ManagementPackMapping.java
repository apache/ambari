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

package org.apache.ambari.server.topology;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Iterator;

import com.google.common.base.Splitter;

/**
 * Represents a component -> mpack mapping in the multi-everything cluster topology template.
 */
public class ManagementPackMapping {
  private String componentName;
  private String mpackName;
  private String mpackVersion;

  public ManagementPackMapping(String componentName, String mpackNameAndVersion) {
    this.componentName = componentName;
    checkArgument(mpackNameAndVersion.contains("-"),
      "Management pack must be specified in {name}-{version} format. Received %s", mpackNameAndVersion);
    Iterator<String> mpackNameIterator = Splitter.on('-').split(mpackNameAndVersion).iterator();
    this.mpackName = mpackNameIterator.next();
    this.mpackVersion = mpackNameIterator.next();
  }

  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public String getMpackName() {
    return mpackName;
  }

  public void setMpackName(String mpackName) {
    this.mpackName = mpackName;
  }

  public String getMpackVersion() {
    return mpackVersion;
  }

  public void setMpackVersion(String mpackVersion) {
    this.mpackVersion = mpackVersion;
  }
}
