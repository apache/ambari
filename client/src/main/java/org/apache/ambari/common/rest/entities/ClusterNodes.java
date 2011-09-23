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