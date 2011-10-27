package org.apache.ambari.controller.rest.config;

import org.apache.ambari.common.rest.entities.ClusterDefinition;
import org.apache.ambari.common.rest.entities.ClusterInformation;

public class Examples {
	public static final ClusterInformation CLUSTER_INFORMATION = new ClusterInformation();
	public static final ClusterDefinition CLUSTER_DEFINITION = new ClusterDefinition();
	
	static {
		CLUSTER_DEFINITION.setName("example-definition");
		CLUSTER_INFORMATION.setDefinition(CLUSTER_DEFINITION);
	}
}
