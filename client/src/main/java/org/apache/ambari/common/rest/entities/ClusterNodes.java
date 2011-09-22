package org.apache.ambari.common.rest.entities;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ClusterNodesType", propOrder = {
    "nodeRange",
    "roleToNodesMap"
})
@XmlRootElement(name = "ClusterNodes")
public class ClusterNodes {
	
    @XmlElement(name = "NodeRange", required = true)
    protected String nodeRange;
    @XmlElement(name = "RoleToNodesMap")
    protected RoleToNodesMap roleToNodesMap;
    
    /**
	 * @return the nodeRange
	 */
	public String getNodeRange() {
		return nodeRange;
	}
	/**
	 * @param nodeRange the nodeRange to set
	 */
	public void setNodeRange(String nodeRange) {
		this.nodeRange = nodeRange;
	}
	/**
	 * @return the roleToNodesMap
	 */
	public RoleToNodesMap getRoleToNodesMap() {
		return roleToNodesMap;
	}
	/**
	 * @param roleToNodesMap the roleToNodesMap to set
	 */
	public void setRoleToNodesMap(RoleToNodesMap roleToNodesMap) {
		this.roleToNodesMap = roleToNodesMap;
	}
}