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

public class SliderAppComponent {
	private String componentName;
	private int instanceCount;
	private Map<String, Map<String, String>> activeContainers;
	private Map<String, Map<String, String>> completedContainers;

	public String getComponentName() {
		return componentName;
	}

	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}

	public int getInstanceCount() {
		return instanceCount;
	}

	public void setInstanceCount(int instanceCount) {
		this.instanceCount = instanceCount;
	}

	public Map<String, Map<String, String>> getActiveContainers() {
		return activeContainers;
	}

	public void setActiveContainers(
	    Map<String, Map<String, String>> activeContainers) {
		this.activeContainers = activeContainers;
	}

	public Map<String, Map<String, String>> getCompletedContainers() {
		return completedContainers;
	}

	public void setCompletedContainers(
	    Map<String, Map<String, String>> completedContainers) {
		this.completedContainers = completedContainers;
	}
}
