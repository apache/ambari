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

package org.apache.ambari.server.controller;

/**
 * An extension version is like a stack version but it contains custom services.  Linking an extension
 * version to the current stack version allows the cluster to install the custom services contained in
 * the extension version.
 */
public class ExtensionResponse {

  private String extensionName;

  public ExtensionResponse(String extensionName) {
    setExtensionName(extensionName);
  }

  public String getExtensionName() {
    return extensionName;
  }

  public void setExtensionName(String extensionName) {
    this.extensionName = extensionName;
  }

  @Override
  public int hashCode() {
    int result = 1;
    result = 31 + getExtensionName().hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ExtensionResponse)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    ExtensionResponse extensionResponse = (ExtensionResponse) obj;
    return getExtensionName().equals(extensionResponse.getExtensionName());
  }

}
