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

import org.apache.ambari.server.registry.Registry;
import org.apache.ambari.server.registry.RegistryType;

/**
 * Represents a software registry response.
 */
public class RegistryResponse {
    private Long registryId;
    private String registryName;
    private RegistryType registryType;
    private String registryUri;

    public RegistryResponse(Registry registry) {
        this.registryId = registry.getRegistryId();
        this.registryName = registry.getRegistryName();
        this.registryType = registry.getRegistryType();
        this.registryUri = registry.getRegistryUri();
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

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 + getRegistryId().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RegistryResponse)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        RegistryResponse registryResponse = (RegistryResponse) obj;
        return getRegistryId().equals(registryResponse.getRegistryId());
    }
}
