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
package org.apache.ambari.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.common.rest.entities.Node;


public class Nodes {
	
	public static final String AGENT_DEPLOYMENT_STATE_TOBE_INSTALLED = "AGENT_TOBE_INSTALLED";
	public static final String AGENT_DEPLOYMENT_STATE_INSTALLED = "AGENT_INSTALLED";
	
	
	public static final short NODE_HEARTBEAT_INTERVAL_IN_MINUTES = 5;
	public static final short NODE_MAX_MISSING_HEARBEAT_INTERVALS = 3;
	
	// One node name to Node hashmap
	protected ConcurrentHashMap<String, Node> nodes = new ConcurrentHashMap<String, Node>();
	
	// Cluster name to Node names hash map
	protected ConcurrentHashMap<String, ConcurrentHashMap<String, String>> cluster_to_nodes = new ConcurrentHashMap<String, ConcurrentHashMap<String, String>>();
	
	/**
	 * @return the cluster_to_nodes
	 */
	public ConcurrentHashMap<String, ConcurrentHashMap<String, String>> getCluster_to_nodes() {
		return cluster_to_nodes;
	}

	private static Nodes NodesTypeRef=null;
	
	private Nodes() {}
	    
	public static synchronized Nodes getInstance() {
		if(NodesTypeRef == null) {
			NodesTypeRef = new Nodes();
		}
	    return NodesTypeRef;
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
	   
	public ConcurrentHashMap<String, Node> getNodes () {
		return nodes;
	}
	
	public List<Node> getNodes (String clusterName, String roleName) {
		
		return null;
	}
}
