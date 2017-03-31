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
package org.apache.ambari.logsearch;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;

import org.apache.ambari.logsearch.common.ManageStartEndTime;
import org.apache.ambari.logsearch.common.PropertiesHelper;
import org.apache.ambari.logsearch.conf.ApplicationConfig;
import org.apache.ambari.logsearch.util.SSLUtil;
import org.apache.ambari.logsearch.util.WebUtil;
import org.apache.ambari.logsearch.web.listener.LogSearchSessionListener;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import javax.servlet.DispatcherType;

import static org.apache.ambari.logsearch.common.LogSearchConstants.LOGSEARCH_SESSION_ID;

public class LogSearch {
  private static final Logger LOG = LoggerFactory.getLogger(LogSearch.class);

  private static final String LOGSEARCH_PROTOCOL_PROP = "logsearch.protocol";
  private static final String HTTPS_PROTOCOL = "https";
  private static final String HTTP_PROTOCOL = "http";
  private static final String HTTPS_PORT = "61889";
  private static final String HTTP_PORT = "61888";

  private static final String ROOT_CONTEXT = "/";
  private static final Integer SESSION_TIMEOUT = 60 * 30;

  public static void main(String[] argv) {
    LogSearch logSearch = new LogSearch();
    ManageStartEndTime.manage();
    try {
      logSearch.run(argv);
    } catch (Throwable e) {
      LOG.error("Error running logsearch server", e);
    }
  }

  public void run(String[] argv) throws Exception {
    SSLUtil.ensureStorePasswords();
    SSLUtil.loadKeystore();
    Server server = buildSever(argv);
    HandlerList handlers = new HandlerList();
    handlers.addHandler(createSwaggerContext());
    handlers.addHandler(createBaseWebappContext());

    server.setHandler(handlers);
    server.start();

    LOG.debug("============================Server Dump=======================================");
    LOG.debug(server.dump());
    LOG.debug("==============================================================================");
    server.join();
  }

  public Server buildSever(String argv[]) {
    Server server = new Server();
    boolean portSpecified = argv.length > 0;
    String protcolProperty = PropertiesHelper.getProperty(LOGSEARCH_PROTOCOL_PROP,HTTP_PROTOCOL);
    HttpConfiguration httpConfiguration = new HttpConfiguration();
    httpConfiguration.setRequestHeaderSize(65535);
    if (StringUtils.isEmpty(protcolProperty)) {
      protcolProperty = HTTP_PROTOCOL;
    }
    String port = null;
    if (HTTPS_PROTOCOL.equals(protcolProperty) && SSLUtil.isKeyStoreSpecified()) {
      LOG.info("Building https server...........");
      port = portSpecified ? argv[0] : HTTPS_PORT;
      WebUtil.checkPort(Integer.parseInt(port));
      httpConfiguration.addCustomizer(new SecureRequestCustomizer());
      SslContextFactory sslContextFactory = SSLUtil.getSslContextFactory();
      ServerConnector sslConnector = new ServerConnector(server,
          new SslConnectionFactory(sslContextFactory, "http/1.1"),
          new HttpConnectionFactory(httpConfiguration));
      sslConnector.setPort(Integer.parseInt(port));
      server.setConnectors(new Connector[] { sslConnector });
    } else {
      LOG.info("Building http server...........");
      port = portSpecified ? argv[0] : HTTP_PORT;
      WebUtil.checkPort(Integer.parseInt(port));
      ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfiguration));
      connector.setPort(Integer.parseInt(port));
      server.setConnectors(new Connector[] { connector });
    }
    URI logsearchURI = URI.create(String.format("%s://0.0.0.0:%s", protcolProperty, port));
    LOG.info("Starting logsearch server URI=" + logsearchURI);
    return server;
  }

  private WebAppContext createBaseWebappContext() throws MalformedURLException {
    URI webResourceBase = WebUtil.findWebResourceBase();
    WebAppContext context = new WebAppContext();
    context.setBaseResource(Resource.newResource(webResourceBase));
    context.setContextPath(ROOT_CONTEXT);
    context.setParentLoaderPriority(true);
    context.addEventListener(new LogSearchSessionListener());

    // Configure Spring
    context.addEventListener(new ContextLoaderListener());
    context.addEventListener(new RequestContextListener());
    context.addFilter(new FilterHolder(new DelegatingFilterProxy("springSecurityFilterChain")), "/*", EnumSet.allOf(DispatcherType.class));
    context.setInitParameter("contextClass", AnnotationConfigWebApplicationContext.class.getName());
    context.setInitParameter("contextConfigLocation", ApplicationConfig.class.getName());

    // Configure Jersey
    ServletHolder jerseyServlet = context.addServlet(org.glassfish.jersey.servlet.ServletContainer.class, "/api/v1/*");
    jerseyServlet.setInitOrder(1);
    jerseyServlet.setInitParameter("jersey.config.server.provider.packages","org.apache.ambari.logsearch.rest,io.swagger.jaxrs.listing");

    context.getSessionHandler().getSessionManager().setMaxInactiveInterval(SESSION_TIMEOUT);
    context.getSessionHandler().getSessionManager().getSessionCookieConfig().setName(LOGSEARCH_SESSION_ID);

    return context;
  }

  private ServletContextHandler createSwaggerContext() throws URISyntaxException {
    ResourceHandler resourceHandler = new ResourceHandler();
    ResourceCollection resources = new ResourceCollection(new String[] {
      LogSearch.class.getClassLoader()
        .getResource("META-INF/resources/webjars/swagger-ui/2.1.0")
        .toURI().toString(),
      LogSearch.class.getClassLoader()
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
