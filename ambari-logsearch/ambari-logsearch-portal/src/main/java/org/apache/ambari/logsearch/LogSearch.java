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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.EnumSet;

import org.apache.ambari.logsearch.common.ManageStartEndTime;
import org.apache.ambari.logsearch.common.PropertiesHelper;
import org.apache.ambari.logsearch.conf.ApplicationConfig;
import org.apache.ambari.logsearch.util.SSLUtil;
import org.apache.ambari.logsearch.web.listener.LogSearchSessionListener;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Chmod;
import org.apache.tools.ant.types.FileSet;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
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
  private static final String LOGSEARCH_CERT_FOLDER_LOCATION = "logsearch.cert.folder.location";
  private static final String LOGSEARCH_CERT_ALGORITHM = "logsearch.cert.algorithm";
  private static final String HTTPS_PROTOCOL = "https";
  private static final String HTTP_PROTOCOL = "http";
  private static final String HTTPS_PORT = "61889";
  private static final String HTTP_PORT = "61888";

  private static final String WEB_RESOURCE_FOLDER = "webapps/app";
  private static final String ROOT_CONTEXT = "/";
  private static final Integer SESSION_TIMEOUT = 60 * 30;

  private static final String LOGSEARCH_CERT_FILENAME = "logsearch.crt";
  private static final String LOGSEARCH_KEYSTORE_FILENAME = "logsearch.jks";
  private static final String LOGSEARCH_KEYSTORE_PRIVATE_KEY = "logsearch.private.key";
  private static final String LOGSEARCH_KEYSTORE_PUBLIC_KEY = "logsearch.public.key";
  private static final String LOGSEARCH_CERT_DEFAULT_ALGORITHM = "sha256WithRSAEncryption";

  public static final String LOGSEARCH_CERT_DEFAULT_FOLDER = "/etc/ambari-logsearch-portal/conf/keys";
  public static final String LOGSEARCH_KEYSTORE_DEFAULT_PASSWORD = "bigdata";

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
    loadKeystore();
    Server server = buildSever(argv);
    HandlerList handlers = new HandlerList();
    handlers.addHandler(createSwaggerContext());
    handlers.addHandler(createBaseWebappContext());

    server.setHandler(handlers);
    server.start();

    LOG
        .debug("============================Server Dump=======================================");
    LOG.debug(server.dump());
    LOG
        .debug("==============================================================================");
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
      checkPort(Integer.parseInt(port));
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
      checkPort(Integer.parseInt(port));
      ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfiguration));
      connector.setPort(Integer.parseInt(port));
      server.setConnectors(new Connector[] { connector });
    }
    URI logsearchURI = URI.create(String.format("%s://0.0.0.0:%s", protcolProperty, port));
    LOG.info("Starting logsearch server URI=" + logsearchURI);
    return server;
  }

  private WebAppContext createBaseWebappContext() throws MalformedURLException {
    URI webResourceBase = findWebResourceBase();
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

  private URI findWebResourceBase() {
    URL fileCompleteUrl = Thread.currentThread().getContextClassLoader()
        .getResource(WEB_RESOURCE_FOLDER);
    String errorMessage = "Web Resource Folder " + WEB_RESOURCE_FOLDER+ " not found in classpath";
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

  private void checkPort(int port) {
    ServerSocket serverSocket = null;
    boolean portBusy = false;
    try {
      serverSocket = new ServerSocket(port);
    } catch (IOException ex) {
      portBusy = true;
      LOG.error(ex.getLocalizedMessage() + " PORT :" + port);
    } finally {
      if (serverSocket != null) {
        try {
          serverSocket.close();
        } catch (Exception exception) {
          // ignore
        }
      }
      if (portBusy) {
        System.exit(1);
      }
    }
  }

  /**
   * Create keystore with keys and certificate (only if the keystore does not exist or if you have no permissions on the keystore file)
   */
  void loadKeystore() {
    try {
      String certFolder = PropertiesHelper.getProperty(LOGSEARCH_CERT_FOLDER_LOCATION, LOGSEARCH_CERT_DEFAULT_FOLDER);
      String certAlgorithm = PropertiesHelper.getProperty(LOGSEARCH_CERT_ALGORITHM, LOGSEARCH_CERT_DEFAULT_ALGORITHM);
      String certLocation = String.format("%s/%s", LOGSEARCH_CERT_DEFAULT_FOLDER, LOGSEARCH_CERT_FILENAME);
      String keyStoreLocation = StringUtils.isNotEmpty(SSLUtil.getKeyStoreLocation()) ? SSLUtil.getKeyStoreLocation()
        : String.format("%s/%s", LOGSEARCH_CERT_DEFAULT_FOLDER, LOGSEARCH_KEYSTORE_FILENAME);
      char[] password = StringUtils.isNotEmpty(SSLUtil.getKeyStorePassword()) ?
        SSLUtil.getKeyStorePassword().toCharArray() : LOGSEARCH_KEYSTORE_DEFAULT_PASSWORD.toCharArray();
      boolean keyStoreFileExists = new File(keyStoreLocation).exists();
      if (!keyStoreFileExists) {
        createDefaultKeyFolder(certFolder);
        LOG.warn("Keystore file ('{}') does not exist, creating new one. " +
          "If the file exists, make sure you have proper permissions on that.", keyStoreLocation);
        if (SSLUtil.isKeyStoreSpecified() && !"JKS".equalsIgnoreCase(SSLUtil.getKeyStoreType())) {
          throw new RuntimeException(String.format("Keystore does not exist. Only JKS keystore can be auto generated. (%s)", keyStoreLocation));
        }
        LOG.info("SSL keystore is not specified. Generating it with certificate ... (using default format: JKS)");
        Security.addProvider(new BouncyCastleProvider());
        KeyPair keyPair = SSLUtil.createKeyPair("RSA", 2048);
        File privateKeyFile = new File(String.format("%s/%s", certFolder, LOGSEARCH_KEYSTORE_PRIVATE_KEY));
        if (!privateKeyFile.exists()) {
          FileUtils.writeByteArrayToFile(privateKeyFile, keyPair.getPrivate().getEncoded());
        }
        File file = new File(String.format("%s/%s", certFolder, LOGSEARCH_KEYSTORE_PUBLIC_KEY));
        if (!file.exists()) {
          FileUtils.writeByteArrayToFile(file, keyPair.getPublic().getEncoded());
        }
        X509Certificate cert = SSLUtil.generateCertificate(certLocation, keyPair, certAlgorithm);
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, password);
        SSLUtil.setKeyAndCertInKeystore(cert, keyPair, keyStore, keyStoreLocation, password);
        setPermissionOnCertFolder(certFolder);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void createDefaultKeyFolder(String certFolder) {
    File keyFolderDirectory = new File(certFolder);
    if (!keyFolderDirectory.exists()) {
      LOG.info("Default key dir does not exist ({}). Creating ...", certFolder);
      boolean mkDirSuccess = keyFolderDirectory.mkdirs();
      if (!mkDirSuccess) {
        String errorMessage = String.format("Could not create directory %s", certFolder);
        LOG.error(errorMessage);
        throw new RuntimeException(errorMessage);
      }
    }
  }

  private void setPermissionOnCertFolder(String certFolder) {
    Chmod chmod = new Chmod();
    chmod.setProject(new Project());
    FileSet fileSet = new FileSet();
    fileSet.setDir(new File(certFolder));
    fileSet.setIncludes("**");
    chmod.addFileset(fileSet);
    chmod.setPerm("600");
    chmod.execute();
  }
}
