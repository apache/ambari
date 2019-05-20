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

package org.apache.ambari.server.agent;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ComponentsResponse {
	@JsonProperty("clusterName")
	private String clusterName;

	@JsonProperty("stackName")
	private String stackName;

	@JsonProperty("stackVersion")
	private String stackVersion;

	// <service, <component, category>>
	@JsonProperty("components")
	private Map<String, Map<String, String>> components;

	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public String getStackName() {
		return stackName;
	}

	public void setStackName(String stackName) {
		this.stackName = stackName;
	}

	public String getStackVersion() {
		return stackVersion;
	}

	public void setStackVersion(String stackVersion) {
		this.stackVersion = stackVersion;
	}

	public Map<String, Map<String, String>> getComponents() {
		return components;
	}

	public void setComponents(Map<String, Map<String, String>> components) {
		this.components = components;
	}

	@Override
	public String toString() {
		return "ComponentsResponse [clusterName=" + clusterName +
      ", stackName=" + stackName +
      ", stackVersion=" + stackVersion +
      ", components=" + components + "]";
	}

}
