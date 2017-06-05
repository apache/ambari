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
package org.apache.ambari.server.state;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Represents the state of an mpack.
 */
public class Mpacks {

    private Long mpackId;

    private String registryId;

    @SerializedName("name")
    private String name;

    @SerializedName("version")
    private String version;

    @SerializedName("description")
    private String description;

    @SerializedName("prerequisites")
    private HashMap<String,String> prerequisites;

    @SerializedName("packlets")
    private ArrayList<Packlet> packlets;

    private String mpacksUrl;

    public Long getMpackId() {
        return mpackId;
    }

    public void setMpackId(Long mpackId) {
        this.mpackId = mpackId;
    }

    public String getRegistryId() {
        return registryId;
    }

    public void setRegistryId(String registryId) {
        this.registryId = registryId;
    }

    public String getMpacksUrl() {
        return mpacksUrl;
    }

    public void setMpacksUrl(String mpacksUrl) {
        this.mpacksUrl = mpacksUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public HashMap<String, String> getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(HashMap<String, String> prerequisites) {
        this.prerequisites = prerequisites;
    }

    public ArrayList<Packlet> getPacklets() {
        return packlets;
    }

    public void setPacklets(ArrayList<Packlet> packlets) {
        this.packlets = packlets;
    }
}
