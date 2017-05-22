/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.infra;

import org.apache.ambari.infra.conf.InfraManagerConfig;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.apache.ambari.infra.common.InfraManagerConstants.DATA_FOLDER_LOCATION_PARAM;
import static org.apache.ambari.infra.common.InfraManagerConstants.DEFAULT_DATA_FOLDER_LOCATION;
import static org.apache.ambari.infra.common.InfraManagerConstants.DEFAULT_PORT;
import static org.apache.ambari.infra.common.InfraManagerConstants.DEFAULT_PROTOCOL;
import static org.apache.ambari.infra.common.InfraManagerConstants.INFRA_MANAGER_SESSION_ID;
import static org.apache.ambari.infra.common.InfraManagerConstants.PROTOCOL_SSL;
import static org.apache.ambari.infra.common.InfraManagerConstants.ROOT_CONTEXT;
import static org.apache.ambari.infra.common.InfraManagerConstants.SESSION_TIMEOUT;
import static org.apache.ambari.infra.common.InfraManagerConstants.WEB_RESOURCE_FOLDER;

public class InfraManager {

  private static final Logger LOG = LoggerFactory.getLogger(InfraManager.class);

  public static void main(String[] args) {
    Options options = new Options();
    HelpFormatter helpFormatter = new HelpFormatter();
    helpFormatter.setDescPadding(10);
    helpFormatter.setWidth(200);

    final Option helpOption = Option.builder("h")
      .longOpt("help")
      .desc("Print commands")
      .build();

    final Option portOption = Option.builder("p")
      .longOpt("port")
      .desc("Infra Manager port")
      .numberOfArgs(1)
      .argName("port_number")
      .build();

    final Option dataFolderOption = Option.builder("df")
      .longOpt("data-folder")
      .desc("Infra Manager data folder location")
      .numberOfArgs(1)
      .argName("data_folder")
      .build();

    final Option protocolOption = Option.builder("t")
      .longOpt("tls-enabled")
      .desc("TLS enabled for Infra Manager")
      .build();

    options.addOption(helpOption);
    options.addOption(portOption);
    options.addOption(protocolOption);
    options.addOption(dataFolderOption);

    try {
      CommandLineParser cmdLineParser = new DefaultParser();
      CommandLine cli = cmdLineParser.parse(options, args);
      int port = cli.hasOption('p') ? Integer.parseInt(cli.getOptionValue('p')) : DEFAULT_PORT;
      String protocol = cli.hasOption("t") ? PROTOCOL_SSL : DEFAULT_PROTOCOL;
      String dataFolder = cli.hasOption("df") ? cli.getOptionValue("df"): DEFAULT_DATA_FOLDER_LOCATION;

      System.setProperty(DATA_FOLDER_LOCATION_PARAM, dataFolder); // be able to access it from jobs

      Server server = buildServer(port, protocol);
      HandlerList handlers = new HandlerList();
      handlers.addHandler(createSwaggerContext());
      handlers.addHandler(createBaseWebappContext(dataFolder));

      server.setHandler(handlers);
      server.start();

      LOG.debug("============================Server Dump=======================================");
      LOG.debug(server.dump());
      LOG.debug("==============================================================================");
      server.join();
    } catch (Exception e) {
      // TODO
      e.printStackTrace();
    }
  }

  private static Server buildServer(int port, String protocol) {
    Server server = new Server();
    HttpConfiguration httpConfiguration = new HttpConfiguration();
    httpConfiguration.setRequestHeaderSize(65535);
    // TODO: tls
    ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfiguration));
    connector.setPort(port);
    server.setConnectors(new Connector[]{connector});
    URI infraManagerURI = URI.create(String.format("%s://0.0.0.0:%s", protocol, String.valueOf(port)));
    LOG.info("Starting infra manager URI=" + infraManagerURI);
    return server;
  }

  private static WebAppContext createBaseWebappContext(String dataFolder) throws MalformedURLException {
    URI webResourceBase = findWebResourceBase();
    WebAppContext context = new WebAppContext();
    ResourceCollection resources = new ResourceCollection(Resource.newResource(webResourceBase));
    context.setBaseResource(resources);
    context.setContextPath(ROOT_CONTEXT);
    context.setParentLoaderPriority(true);

    // Data folder servlet
    ServletHolder dataServlet = new ServletHolder("static-data", DefaultServlet.class);
    dataServlet.setInitParameter("dirAllowed","true");
    dataServlet.setInitParameter("pathInfoOnly","true");
    dataServlet.setInitParameter("resourceBase", dataFolder);

    context.addServlet(dataServlet,"/files/*");

    // Configure Spring
    context.addEventListener(new ContextLoaderListener());
    context.addEventListener(new RequestContextListener());
    // TODO: security, add: context.addFilter(new FilterHolder(new DelegatingFilterProxy("springSecurityFilterChain")), "/*", EnumSet.allOf(DispatcherType.class));
    context.setInitParameter("contextClass", AnnotationConfigWebApplicationContext.class.getName());
    context.setInitParameter("contextConfigLocation", InfraManagerConfig.class.getName());

    // Configure Jersey
    ServletHolder jerseyServlet = context.addServlet(org.glassfish.jersey.servlet.ServletContainer.class, "/api/v1/*");
    jerseyServlet.setInitOrder(1);
    jerseyServlet.setInitParameter("jersey.config.server.provider.packages","org.apache.ambari.infra.rest,io.swagger.jaxrs.listing");

    context.getSessionHandler().getSessionManager().setMaxInactiveInterval(SESSION_TIMEOUT);
    context.getSessionHandler().getSessionManager().getSessionCookieConfig().setName(INFRA_MANAGER_SESSION_ID);

    return context;
  }

  private static URI findWebResourceBase() {
    URL fileCompleteUrl = Thread.currentThread().getContextClassLoader().getResource(WEB_RESOURCE_FOLDER);
    String errorMessage = "Web Resource Folder " + WEB_RESOURCE_FOLDER + " not found in classpath";
    if (fileCompleteUrl != null) {
      try {
        return fileCompleteUrl.toURI().normalize();
      } catch (URISyntaxException e) {
        LOG.error(errorMessage, e);
        System.exit(1);
      }
    } else {
      LOG.error(errorMessage);
      System.exit(1);
    }
    throw new IllegalStateException(errorMessage);
  }

  private static ServletContextHandler createSwaggerContext() throws URISyntaxException {
    ResourceHandler resourceHandler = new ResourceHandler();
    ResourceCollection resources = new ResourceCollection(new String[] {
      InfraManager.class.getClassLoader()
        .getResource("META-INF/resources/webjars/swagger-ui/2.1.0")
        .toURI().toString(),
      InfraManager.class.getClassLoader()
        .getResource("swagger")
        .toURI().toString()
    });
    resourceHandler.setBaseResource(resources);
    resourceHandler.setWelcomeFiles(new String[]{"swagger.html"}); // rewrite index.html from swagger-ui webjar
    ServletContextHandler context = new ServletContextHandler();
    context.setContextPath("/docs/");
    context.setHandler(resourceHandler);
    return context;
  }
}
