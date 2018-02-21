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
package org.apache.ambari.server.state.stack.upgrade;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A lifecycle is used to delineate specific portions of an upgrade.  Orchestration
 * will occur based on the lifecycle phases in order they are declared in {@link LifecycleType}
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class Lifecycle {

  @XmlAttribute
  public LifecycleType type;

  @XmlElementWrapper(name="order")
  @XmlElement(name="group")
  public List<Grouping> groups;

  /**
   * Identifies the lifecycle types
   */
  @XmlEnum
  public enum LifecycleType {

    /**
     * Work required that can be classified as installation.  Normally installation of
     * bits occurs outside the scope of upgrade orchestration.
     */
    @XmlEnumValue("install")
    INSTALL,

    /**
     * Work to stop and wait on, for example, queues or topologies.
     */
    @XmlEnumValue("quiet")
    QUIET,

    /**
     * Work required to snapshot or other backup.
     */
    @XmlEnumValue("snapshot")
    SNAPSHOT,

    /**
     * Work required to prepare to upgrade.
     */
    @XmlEnumValue("prepare")
    PREPARE,

    /**
     * Work required to stop a service.
     */
    @XmlEnumValue("stop")
    STOP,

    /**
     * For a Rolling upgrade, work required to restart and upgrade the service.
     */
    @XmlEnumValue("upgrade")
    UPGRADE,

    /**
     * Work required to start a service.
     */
    @XmlEnumValue("start")
    START,

    /**
     * Work required to finalize.  Will not happen until the end of the upgrade.
     */
    @XmlEnumValue("finalize")
    FINALIZE

  }

}
