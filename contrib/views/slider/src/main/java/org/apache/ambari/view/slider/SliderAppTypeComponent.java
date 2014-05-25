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

public class SliderAppTypeComponent {
	private String id;
	private String name;
	private String category;
	private String displayName;
	private int priority;
	private int instanceCount;
	private int maxInstanceCount;
	private int yarnMemory;
	private int yarnCpuCores;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public int getInstanceCount() {
		return instanceCount;
	}

	public void setInstanceCount(int instanceCount) {
		this.instanceCount = instanceCount;
	}

	public int getYarnMemory() {
		return yarnMemory;
	}

	public void setYarnMemory(int yarnMemory) {
		this.yarnMemory = yarnMemory;
	}

	public int getYarnCpuCores() {
		return yarnCpuCores;
	}

	public void setYarnCpuCores(int yarnCpuCores) {
		this.yarnCpuCores = yarnCpuCores;
	}

	public int getMaxInstanceCount() {
		return maxInstanceCount;
	}

	public void setMaxInstanceCount(int maxInstanceCount) {
		this.maxInstanceCount = maxInstanceCount;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}
}
