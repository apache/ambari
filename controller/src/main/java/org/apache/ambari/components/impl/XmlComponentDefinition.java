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
import org.apache.ambari.common.rest.entities.agent.Action;
import org.apache.ambari.common.rest.entities.agent.Command;
import org.apache.ambari.components.ComponentPlugin;

public class XmlComponentDefinition extends ComponentPlugin {

  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "component", propOrder = {
      "requires",
      "roles",
      "preinstall",
      "install",
      "configure",
      "start",
      "check",
      "uninstall"
  })
  @XmlRootElement
  public static class Component {
    @XmlAttribute String provides;
    @XmlAttribute(name="package") String pkg;
    @XmlAttribute String user;
    @XmlElement List<Requires> requires;
    @XmlElement List<Role> roles;
    @XmlElement Install install;
    @XmlElement Configure configure;
    @XmlElement Start start;
    @XmlElement Check check;
    @XmlElement Preinstall preinstall;
    @XmlElement Uninstall uninstall;
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
  @XmlType(name = "preinstall")
  public static class Preinstall extends ScriptCommand {
    @XmlAttribute String runPreinstallOn;
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
  private final String pkg;
  private final String[] roles;
  private final String[] dependencies;
  private final String configureCommand;
  private final String configureUser;
  private final String installCommand;
  private final String installUser;
  private final String startCommand;
  private final String startUser;
  private final String uninstallCommand;
  private final String uninstallUser;
  private final String checkRole;
  private final String preinstallRole;
  private final String preinstallCommand;
  private final String preinstallUser;
  private final String checkCommand;
  private final String checkUser;
  
  @Override
  public String getProvides() {
    return provides;
  }
  
  @Override
  public String getPackage() {
    return pkg;
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
  public Action configure(String cluster, String role) throws IOException {
    if (configureCommand == null) {
      return null;
    }
    Action result = new Action();
    result.kind = Action.Kind.RUN_ACTION;
    result.command = new Command(configureUser, configureCommand, 
                                 new String[]{cluster, role});
    return result;
  }

  @Override
  public Action install(String cluster, String role) throws IOException {
    if (installCommand == null) {
      return null;
    }
    Action result = new Action();
    result.kind = Action.Kind.RUN_ACTION;
    result.command = new Command(installUser, installCommand, 
                                 new String[]{cluster, role});
    return result;
  }

  @Override
  public Action startServer(String cluster, String role) throws IOException {
    if (startCommand == null) {
      return null;
    }
    Action result = new Action();
    result.kind = Action.Kind.START_ACTION;
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
    result.command = new Command(checkUser, checkCommand, 
                                 new String[]{cluster, role});
    return result;
  }
  
  @Override
  public String runPreinstallRole() throws IOException {
    return preinstallRole;
  }

  @Override
  public Action preinstallAction(String cluster, String role) throws IOException {
    if (preinstallCommand == null) {
      return null;
    }
    Action result = new Action();
    result.kind = Action.Kind.RUN_ACTION;
    result.command = new Command(preinstallUser, preinstallCommand, 
                                 new String[]{cluster, role});
    return result; 
  }

  @Override
  public Action uninstall(String cluster, String role) throws IOException {
    if (uninstallCommand == null) {
      return null;
    }
    Action result = new Action();
    result.kind = Action.Kind.RUN_ACTION;
    result.command = new Command(uninstallUser, uninstallCommand, 
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

  private static String getUser(ScriptCommand cmd, String user) {
    if (cmd == null || cmd.user == null) {
      return user;
    } else {
      return cmd.user;
    }
  }

  XmlComponentDefinition(InputStream in) throws IOException {
    Unmarshaller um;
    try {
      um = jaxbContext.createUnmarshaller();
      Component component = (Component) um.unmarshal(in);
      provides = component.provides;
      pkg = component.pkg;
      int i = 0;
      if (component.requires == null) {
        dependencies = new String[0];
      } else {
        dependencies = new String[component.requires.size()];
        for(Requires r: component.requires) {
          dependencies[i] = r.name;
        }
      }
      i = 0;
      if (component.roles == null) {
        roles = new String[0];
      } else {
        roles = new String[component.roles.size()];
        for(Role r: component.roles) {
          roles[i] = r.name;
        }      
      }
      installCommand = getCommand(component.install);
      installUser = getUser(component.install, component.user);
      configureCommand = getCommand(component.configure);
      configureUser = getUser(component.configure, component.user);
      startCommand = getCommand(component.start);
      startUser = getUser(component.start, component.user);
      checkCommand = getCommand(component.check);
      checkUser = getUser(component.check, component.user);
      preinstallCommand = getCommand(component.preinstall);
      preinstallUser = getUser(component.preinstall, component.user);
      if (component.check != null) {
        checkRole = component.check.runOn;
      } else {
        checkRole = null;
      }
      if (component.preinstall != null) {
        preinstallRole = component.preinstall.runPreinstallOn;
      } else {
        preinstallRole = null;
      }
      uninstallCommand = getCommand(component.uninstall);
      uninstallUser = getUser(component.uninstall, component.user);
    } catch (JAXBException e) {
      throw new IOException("Problem parsing component defintion", e);
    }
  }

  private static InputStream getInputStream(ComponentDefinition defn) {
    String name = defn.getGroup().replace('.', '/') + "/acd/" +
                  defn.getDefinition() + '-' +
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
  public XmlComponentDefinition(ComponentDefinition defn) throws IOException {
    this(getInputStream(defn));
  }
  
  public static void main(String[] args) throws Exception {
    ComponentDefinition defn = new ComponentDefinition();
    defn.setDefinition("hadoop-hdfs");
    defn.setGroup("org.apache.ambari");
    defn.setVersion("0.1.0");
    XmlComponentDefinition comp = new XmlComponentDefinition(defn);
    System.out.println(comp.provides);
    defn.setDefinition("hadoop-common");
    comp = new XmlComponentDefinition(defn);
    System.out.println(comp.provides);
  }
}
