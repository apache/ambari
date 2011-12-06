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
package org.apache.ambari.components.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.apache.ambari.common.rest.entities.ComponentDefinition;
import org.apache.ambari.common.rest.agent.Action;
import org.apache.ambari.common.rest.agent.Command;
import org.apache.ambari.components.ComponentPlugin;

class XmlComponentDefinition extends ComponentPlugin {

  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "component", propOrder = {
      "requires",
      "roles",
      "prestart",
      "start",
      "check"
  })
  @XmlRootElement
  public static class Component {
    @XmlAttribute String provides;
    @XmlElement List<Requires> requires;
    @XmlElement List<Role> roles;
    @XmlElement Start start;
    @XmlElement Check check;
    @XmlElement Prestart prestart;
  }
  
  @XmlAccessorType
  @XmlType(name = "role")
  public static class Role {
    @XmlAttribute String name;
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "requires")
  public static class Requires {
    @XmlAttribute String name;
  }

  public static class ScriptCommand {
    @XmlAttribute String user;
    @XmlValue String script;
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "install")
  public static class Install extends ScriptCommand {
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "configure")
  public static class Configure extends ScriptCommand {
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "start")
  public static class Start extends ScriptCommand {
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "check")
  public static class Check extends ScriptCommand {
    @XmlAttribute String runOn;
  }
  
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "prestart")
  public static class Prestart extends ScriptCommand {
    @XmlAttribute String runOn;
  }
  
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "uninstall")
  public static class Uninstall extends ScriptCommand {
  }

  private static final JAXBContext jaxbContext;
  static {
    try {
      jaxbContext = JAXBContext.newInstance("org.apache.ambari.components.impl");
    } catch (JAXBException e) {
      throw new RuntimeException("Can't create jaxb context", e);
    }
  }

  private final String provides;
  private final String[] roles;
  private final String[] dependencies;
  private final String startCommand;
  private final String startUser = "agent";
  private final String checkRole;
  private final String prestartRole;
  private final String prestartCommand;
  private final String prestartUser = "agent";
  private final String checkCommand;
  private final String checkUser = "agent";
  
  @Override
  public String getProvides() {
    return provides;
  }

  @Override
  public String[] getActiveRoles() throws IOException {
    return roles;
  }

  @Override
  public String[] getRequiredComponents() throws IOException {
    return dependencies;
  }

  @Override
  public Action startServer(String cluster, String role) throws IOException {
    if (startCommand == null) {
      return null;
    }
    Action result = new Action();
    result.kind = Action.Kind.START_ACTION;
    result.setUser(startUser);
    result.command = new Command(startUser, startCommand, 
                                 new String[]{cluster, role});
    return result;
  }

  @Override
  public String runCheckRole() throws IOException {
    return checkRole;
  }

  @Override
  public Action checkService(String cluster, String role) throws IOException {
    if (checkCommand == null) {
      return null;
    }
    Action result = new Action();
    result.kind = Action.Kind.RUN_ACTION;
    result.setUser(checkUser);
    result.command = new Command(checkUser, checkCommand, 
                                 new String[]{cluster, role});
    return result;
  }
  
  @Override
  public String runPreStartRole() throws IOException {
    return prestartRole;
  }

  @Override
  public Action preStartAction(String cluster, String role) throws IOException {
    if (prestartCommand == null) {
      return null;
    }
    Action result = new Action();
    result.kind = Action.Kind.RUN_ACTION;
    result.setUser(prestartUser);
    result.command = new Command(prestartUser, prestartCommand, 
                                 new String[]{cluster, role});
    return result; 
  }

  private static String getCommand(ScriptCommand cmd) {
    if (cmd == null) {
      return null;
    } else {
      return cmd.script;
    }
  }

  XmlComponentDefinition(InputStream in) throws IOException {
    Unmarshaller um;
    try {
      um = jaxbContext.createUnmarshaller();
      Component component = (Component) um.unmarshal(in);
      provides = component.provides;
      int i = 0;
      if (component.requires == null) {
        dependencies = new String[0];
      } else {
        dependencies = new String[component.requires.size()];
        for(Requires r: component.requires) {
          dependencies[i++] = r.name;
        }
      }
      i = 0;
      if (component.roles == null) {
        roles = new String[0];
      } else {
        roles = new String[component.roles.size()];
        for(Role r: component.roles) {
          roles[i++] = r.name;
        }      
      }
      startCommand = getCommand(component.start);
      checkCommand = getCommand(component.check);
      prestartCommand = getCommand(component.prestart);
      if (component.check != null) {
        checkRole = component.check.runOn;
      } else {
        checkRole = null;
      }
      if (component.prestart != null) {
        prestartRole = component.prestart.runOn;
      } else {
        prestartRole = null;
      }
    } catch (JAXBException e) {
      throw new IOException("Problem parsing component defintion", e);
    }
  }

  private static InputStream getInputStream(ComponentDefinition defn) {
    String name = defn.getProvider().replace('.', '/') + "/acd/" +
                  defn.getName() + '-' +
                  defn.getVersion() + ".acd";
    InputStream result = ClassLoader.getSystemResourceAsStream(name);
    if (result == null) {
      throw new IllegalArgumentException("Can't find resource for " + defn);
    }
    return result;
  }

  /**
   * Get a component definition based on the name.
   * @param defn the name of the definition
   * @throws IOException
   */
  XmlComponentDefinition(ComponentDefinition defn) throws IOException {
    this(getInputStream(defn));
  }
  
  public static void main(String[] args) throws Exception {
    ComponentDefinition defn = new ComponentDefinition();
    defn.setName("hadoop-hdfs");
    defn.setProvider("org.apache.ambari");
    defn.setVersion("0.1.0");
    XmlComponentDefinition comp = new XmlComponentDefinition(defn);
    System.out.println(comp.provides);
    defn.setName("hadoop-common");
    comp = new XmlComponentDefinition(defn);
    System.out.println(comp.provides);
  }
}
