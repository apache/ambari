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

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * The configuration included in a Stack. Configurations are a set of categories
 * that correspond to the different configuration files necessary for running 
 * Hadoop. The categories other than Ambari come from the components. The
 * categories for Hadoop, HDFS, MapReduce and Pig are:
 * <ul>
 * <li> <b>ambari</b> - the generic properties that affect multiple components
 * <li> Categories for Hadoop:
 *    <ul>
 *    <li> hadoop-env
 *    <li> common-site
 *    <li> log4j
 *    <li> metrics2
 *    </ul>
 * <li> Categories for HDFS:
 *    <ul>
 *    <li> hdfs-site
 *    </ul>
 * <li> Categories for MapReduce:
 *    <ul>
 *    <li> mapred-site
 *    <li> mapred-queue-acl
 *    <li> task-controller
 *    <li> capacity-scheduler
 *    </ul>
 * <li> Categories for Pig:
 *    <ul>
 *    <li> pig-env
 *    <li> pig-site
 *    </ul>
 * </ul>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Configuration", propOrder = {
    "category"
})
@XmlRootElement
public class Configuration {

    @XmlElements({@XmlElement})
    protected List<ConfigurationCategory> category;

    /**
     * Gets the value of the category property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the category property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCategory().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ConfigurationCategory }
     * 
     * 
     */
    public List<ConfigurationCategory> getCategory() {
        if (category == null) {
            category = new ArrayList<ConfigurationCategory>();
        }
        return this.category;
    }

}
