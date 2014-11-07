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
package org.apache.ambari.server.state.stack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.ambari.server.state.stack.upgrade.Batch;
import org.apache.ambari.server.state.stack.upgrade.Task;

/**
 * Represents an upgrade pack.
 */
@XmlRootElement(name="upgrade")
@XmlAccessorType(XmlAccessType.FIELD)
public class UpgradePack {

  @XmlElement(name="target")
  private String target;


  @XmlElementWrapper(name="order")
  @XmlElement(name="service")
  private List<OrderService> services;

  @XmlElementWrapper(name="processing")
  @XmlElement(name="service")
  private List<ProcessingService> processing;

  @XmlTransient
  private Map<String, List<String>> m_orders = null;
  @XmlTransient
  private Map<String, Map<String, ProcessingComponent>> m_process = null;

  /**
   * @return the target version for the upgrade pack
   */
  public String getTarget() {
    return target;
  }

  /**
   * Gets the order by which services and components should be upgraded.
   * @return a map of service_name -> list of component_name.
   */
  public Map<String, List<String>> getOrder() {

    if (null == m_orders) {
      m_orders = new LinkedHashMap<String, List<String>>();

      for (OrderService order : services) {
        m_orders.put(order.name, order.components);
      }
    }

    return m_orders;
  }

  /**
   * Gets the tasks by which services and components should be upgraded.
   * @return a map of service_name -> map(component_name -> process).
   */
  public Map<String, Map<String, ProcessingComponent>> getTasks() {

    if (null == m_process) {
      m_process = new LinkedHashMap<String, Map<String, ProcessingComponent>>();

      for (ProcessingService svc : processing) {
        if (!m_process.containsKey(svc.name)) {
          m_process.put(svc.name, new LinkedHashMap<String, ProcessingComponent>());
        }

        Map<String, ProcessingComponent> componentMap = m_process.get(svc.name);

        for (ProcessingComponent pc : svc.components) {
          componentMap.put(pc.name, pc);
        }
      }
    }

    return m_process;
  }


  /**
   * A service definition that holds a list of componenents in the 'order' element.
   */
  public static class OrderService {

    @XmlAttribute
    public String name;

    @XmlElement(name="component")
    public List<String> components;
  }

  /**
   * A service definition in the 'processing' element.
   */
  public static class ProcessingService {

    @XmlAttribute
    public String name;

    @XmlElement(name="component")
    public List<ProcessingComponent> components;
  }

  /**
   * A component definition in the 'processing/service' path.
   */
  public static class ProcessingComponent {

    @XmlAttribute
    public String name;

    @XmlElementWrapper(name="pre-upgrade")
    @XmlElement(name="task")
    public List<Task> preTasks;

    @XmlElementWrapper(name="upgrade")
    @XmlElement(name="task")
    public List<Task> tasks;

    @XmlElementWrapper(name="post-upgrade")
    @XmlElement(name="task")
    public List<Task> postTasks;

    @XmlElement(name="batch")
    public Batch batch;

  }

}
