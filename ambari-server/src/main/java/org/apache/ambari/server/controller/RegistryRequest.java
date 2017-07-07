/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller;

import org.apache.ambari.server.registry.RegistryType;

public class RegistryRequest {
    private Long registryId;
    private String registryName;
    private RegistryType registryType;
    private String registryUri;

    public RegistryRequest(Long registryId, String registryName, RegistryType registryType, String registryUri) {
        this.registryId = registryId;
        this.registryName = registryName;
        this.registryType = registryType;
        this.registryUri = registryUri;
    }

    public Long getRegistryId() {
        return registryId;
    }

    public void setRegistryId(Long registryId) {
        this.registryId = registryId;
    }

    public String getRegistryName() {
        return registryName;
    }

    public void setRegistryName(String registryName) {
        this.registryName = registryName;
    }

    public RegistryType getRegistryType() {
        return registryType;
    }

    public void setRegistryType(RegistryType registryType) {
        this.registryType = registryType;
    }

    public String getRegistryUri() {
        return registryUri;
    }

    public void setRegistryUri(String registryUri) {
        this.registryUri = registryUri;
    }

}
