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

package org.apache.ambari.server.controller;


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.security.CertificateManager;
import org.apache.ambari.server.security.SecurityFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppContext;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.persist.jpa.JpaPersistModule;
import com.sun.jersey.spi.container.servlet.ServletContainer;

@Singleton
public class AmbariServer {
  public static final String PERSISTENCE_PROVIDER = "ambari-postgres";
  private static Log LOG = LogFactory.getLog(AmbariServer.class);
  public static int CLIENT_ONE_WAY = 4080;
  public static int CLIENT_TWO_WAY = 8443;
  private Server server = null;
  public volatile boolean running = true; // true while controller runs

  final String WEB_APP_DIR = "webapp";
  final URL warUrl = this.getClass().getClassLoader().getResource(WEB_APP_DIR);
  final String warUrlString = warUrl.toExternalForm();
  final String CONTEXT_PATH = "/";
  final String SPRING_CONTEXT_LOCATION = 
      "classpath:/webapp/WEB-INF/spring-security.xml";

  @Inject
  Configuration configs;
  @Inject
  CertificateManager certMan;
  @Inject
  Injector injector;

  public void run() {
    server = new Server();

    try {
      ClassPathXmlApplicationContext parentSpringAppContext = 
          new ClassPathXmlApplicationContext();
      parentSpringAppContext.refresh();
      ConfigurableListableBeanFactory factory = parentSpringAppContext.
          getBeanFactory();
      factory.registerSingleton("guiceInjector", injector); 
      //Spring Security xml config depends on this Bean

      String[] contextLocations = {SPRING_CONTEXT_LOCATION};
      ClassPathXmlApplicationContext springAppContext = new 
          ClassPathXmlApplicationContext(contextLocations, parentSpringAppContext);

      WebAppContext webAppContext = new WebAppContext(warUrlString, CONTEXT_PATH);

      GenericWebApplicationContext springWebAppContext = new GenericWebApplicationContext();
      springWebAppContext.setServletContext(webAppContext.getServletContext());
      springWebAppContext.setParent(springAppContext);

      webAppContext.getServletContext().setAttribute(
          WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, 
          springWebAppContext);


      server.setHandler(webAppContext);

      certMan.initRootCert();
      Context root =
//              new Context(webAppContext, "/", Context.SESSIONS);
              webAppContext;

      ServletHolder rootServlet = root.addServlet(DefaultServlet.class, "/");
      rootServlet.setInitOrder(1);

      root.addFilter(SecurityFilter.class, "/*", 1);
      //Secured connector for 2-way auth
      SslSocketConnector sslConnectorTwoWay = new SslSocketConnector();
      sslConnectorTwoWay.setPort(CLIENT_TWO_WAY);

      Map<String, String> configsMap = configs.getConfigsMap();
      String keystore = configsMap.get(Configuration.SRVR_KSTR_DIR_KEY) + 
          File.separator + configsMap.get(Configuration.KSTR_NAME_KEY);
      String srvrCrtPass = configsMap.get(Configuration.SRVR_CRT_PASS_KEY);

      sslConnectorTwoWay.setKeystore(keystore);
      sslConnectorTwoWay.setTruststore(keystore);
      sslConnectorTwoWay.setPassword(srvrCrtPass);
      sslConnectorTwoWay.setKeyPassword(srvrCrtPass);
      sslConnectorTwoWay.setTrustPassword(srvrCrtPass);
      sslConnectorTwoWay.setKeystoreType("PKCS12");
      sslConnectorTwoWay.setTruststoreType("PKCS12");
      sslConnectorTwoWay.setNeedClientAuth(true);

      //Secured connector for 1-way auth
      SslSocketConnector sslConnectorOneWay = new SslSocketConnector();
      sslConnectorOneWay.setPort(CLIENT_ONE_WAY);

      sslConnectorOneWay.setKeystore(keystore);
      sslConnectorOneWay.setTruststore(keystore);
      sslConnectorOneWay.setPassword(srvrCrtPass);
      sslConnectorOneWay.setKeyPassword(srvrCrtPass);
      sslConnectorOneWay.setTrustPassword(srvrCrtPass);
      sslConnectorOneWay.setKeystoreType("PKCS12");
      sslConnectorOneWay.setTruststoreType("PKCS12");
      sslConnectorOneWay.setNeedClientAuth(false);



      server.addConnector(sslConnectorOneWay);
      server.addConnector(sslConnectorTwoWay);

      ServletHolder sh = new ServletHolder(ServletContainer.class);
      sh.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
              "com.sun.jersey.api.core.PackagesResourceConfig");
      sh.setInitParameter("com.sun.jersey.config.property.packages",
              "org.apache.ambari.server.api.rest");
      root.addServlet(sh, "/api/*");
      sh.setInitOrder(2);

      ServletHolder agent = new ServletHolder(ServletContainer.class);
      agent.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
              "com.sun.jersey.api.core.PackagesResourceConfig");
      agent.setInitParameter("com.sun.jersey.config.property.packages",
              "org.apache.ambari.server.agent.rest");
      root.addServlet(agent, "/agent/*");
      agent.setInitOrder(3);

      ServletHolder cert = new ServletHolder(ServletContainer.class);
      cert.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
              "com.sun.jersey.api.core.PackagesResourceConfig");
      cert.setInitParameter("com.sun.jersey.config.property.packages",
              "org.apache.ambari.server.security.unsecured.rest");
      root.addServlet(cert, "/cert/*");
      cert.setInitOrder(4);

      ServletHolder certs = new ServletHolder(ServletContainer.class);
      certs.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
        "com.sun.jersey.api.core.PackagesResourceConfig");
      certs.setInitParameter("com.sun.jersey.config.property.packages",
        "org.apache.ambari.server.security.unsecured.rest");
      root.addServlet(cert, "/certs/*");
      certs.setInitOrder(5);

      ServletHolder resources = new ServletHolder(ServletContainer.class);
      resources.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
        "com.sun.jersey.api.core.PackagesResourceConfig");
      resources.setInitParameter("com.sun.jersey.config.property.packages",
        "org.apache.ambari.server.resources.api.rest");
      root.addServlet(resources, "/resources/*");
      resources.setInitOrder(6);

      server.setStopAtShutdown(true);

      springAppContext.start();
      /*
       * Start the server after controller state is recovered.
       */
      server.start();
      LOG.info("Started Server");
      server.join();
      LOG.info("Joined the Server");
    } catch (Exception e) {
      LOG.error("Error in the server", e);
    }
  }

  public void stop() throws Exception {
    try {
      server.stop();
    } catch (Exception e) {
      LOG.error("Error stopping the server", e);
    }
  }

  public static void main(String[] args) throws IOException {
    Injector injector = Guice.createInjector(new ControllerModule(), 
        new JpaPersistModule(PERSISTENCE_PROVIDER));

    try {
      LOG.info("Getting the controller");
      AmbariServer server = injector.getInstance(AmbariServer.class);
      CertificateManager certMan = injector.getInstance(CertificateManager.class);
      injector.getInstance(GuiceJpaInitializer.class);
      certMan.initRootCert();
      if (server != null) {
        server.run();
      }
    } catch (Throwable t) {
      LOG.error("Failed to run the Ambari Server", t);
    }
  }
}
