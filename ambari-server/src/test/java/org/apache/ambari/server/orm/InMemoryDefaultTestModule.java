package org.apache.ambari.server.orm;

import com.google.inject.AbstractModule;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ControllerModule;

import java.util.Properties;

public class InMemoryDefaultTestModule extends AbstractModule {
  @Override
  protected void configure() {
    Properties properties = new Properties();
    properties.setProperty(Configuration.PERSISTENCE_IN_MEMORY_KEY, "true");
    install(new ControllerModule(properties));
  }
}
