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
import javax.xml.bind.annotation.XmlAttribute;


/**
 * The nodes associated with a role.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class RoleToNodes {

    @XmlAttribute(required = true)
    protected String roleName;
    @XmlAttribute
    protected String nodes;
    
        /**
         * @return the roleName
         */
        public String getRoleName() {
                return roleName;
        }
        /**
         * @param roleName the roleName to set
         */
        public void setRoleName(String roleName) {
                this.roleName = roleName;
        }
        /**
         * @return the nodes
         */
        public String getNodes() {
                return nodes;
        }
        /**
         * @param nodes the nodeRangeExpressions to set
         */
        public void setNodes(String nodes) {
                this.nodes = nodes;
        }
}
