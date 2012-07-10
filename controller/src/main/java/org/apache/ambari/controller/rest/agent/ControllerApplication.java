package org.apache.ambari.controller.rest.agent;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.ambari.controller.rest.config.ExtendedWadlGeneratorConfig;

public class ControllerApplication extends Application {
  @Override
  public Set<Class<?>> getClasses() {
      final Set<Class<?>> classes = new HashSet<Class<?>>();
      // register root resources/providers
      classes.add(ControllerResource.class);
      classes.add(ExtendedWadlGeneratorConfig.class);
      return classes;
  }
}
