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

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A lifecycle is used to delineate specific portions of an upgrade.  Orchestration
 * will occur based on the lifecycle phases in order they are declared in {@link LifecycleType},
 * namely:
 *
 * <ol>
 *   <li>{@link LifecycleType#INSTALL}</li>
 *   <li>{@link LifecycleType#QUIET}</li>
 *   <li>{@link LifecycleType#SNAPSHOT}</li>
 *   <li>{@link LifecycleType#PREPARE}</li>
 *   <li>{@link LifecycleType#STOP}</li>
 *   <li>{@link LifecycleType#UPGRADE}</li>
 *   <li>{@link LifecycleType#START}</li>
 *   <li>{@link LifecycleType#FINALIZE}</li>
 * </ol>
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
   * Post-processes the groups for their enclosing lifecycle type
   */
  protected void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
    if (null == groups) {
      return;
    }

    groups.stream().forEach(group -> group.lifecycle = type);
  }

}
