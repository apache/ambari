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

package org.apache.ambari.view.slider;

import java.util.Map;

public class SliderApp {
	public static final String STATE_FROZEN = "FROZEN";

	private String id;
	private String yarnId;
	private String name;
	private String type;
	private String user;
	private String state;
	private String diagnostics;
	private long startTime;
	private long endTime;
	private Map<String, String> jmx;
	private Map<String, String> urls;
	private Map<String, Map<String, String>> configs;
	private Map<String, SliderAppComponent> components;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getYarnId() {
		return yarnId;
	}

	public void setYarnId(String yarnId) {
		this.yarnId = yarnId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getDiagnostics() {
		return diagnostics;
	}

	public void setDiagnostics(String diagnostics) {
		this.diagnostics = diagnostics;
	}

	public Map<String, String> getJmx() {
		return jmx;
	}

	public void setJmx(Map<String, String> jmx) {
		this.jmx = jmx;
	}

	public Map<String, String> getUrls() {
		return urls;
	}

	public void setUrls(Map<String, String> urls) {
		this.urls = urls;
	}

	public Map<String, Map<String, String>> getConfigs() {
		return configs;
	}

	public void setConfigs(Map<String, Map<String, String>> configs) {
		this.configs = configs;
	}

	public Map<String, SliderAppComponent> getComponents() {
		return components;
	}

	public void setComponents(Map<String, SliderAppComponent> components) {
		this.components = components;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}
}
