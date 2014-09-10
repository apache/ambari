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

package org.apache.ambari.view.slider.clients;

import java.util.Map;

public interface AmbariClient {
	/**
	 * Provides the first cluster defined on this Ambari server.
	 * 
	 * @return
	 */
	public AmbariClusterInfo getClusterInfo();

	/**
	 * Provides detailed information about the given cluster.
	 * 
	 * @param clusterInfo
	 * @return
	 */
	public AmbariCluster getCluster(AmbariClusterInfo clusterInfo);

	/**
	 * Provides configs identified by type and tag on given cluster.
	 * 
	 * @param cluster
	 * @param configType
	 * @param configTag
	 * @return
	 */
	public Map<String, String> getConfiguration(AmbariClusterInfo cluster,
	    String configType, String configTag);

	/**
	 * Provides detailed information about the given service.
	 * 
	 * @param cluster
	 * @param serviceId
	 * @return
	 */
	public AmbariService getService(AmbariClusterInfo cluster, String serviceId);
}
