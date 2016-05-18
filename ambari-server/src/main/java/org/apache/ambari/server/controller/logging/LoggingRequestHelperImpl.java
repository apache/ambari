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

package org.apache.ambari.server.controller.logging;


import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.security.credential.Credential;
import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Convenience class to handle the connection details of a LogSearch query request.
 *
 */
public class LoggingRequestHelperImpl implements LoggingRequestHelper {

  private static Logger LOG = Logger.getLogger(LoggingRequestHelperImpl.class);

  private static final String LOGSEARCH_ADMIN_JSON_CONFIG_TYPE_NAME = "logsearch-admin-json";

  private static final String LOGSEARCH_ADMIN_USERNAME_PROPERTY_NAME = "logsearch_admin_username";

  private static final String LOGSEARCH_ADMIN_PASSWORD_PROPERTY_NAME = "logsearch_admin_password";

  private static final String LOGSEARCH_QUERY_PATH = "/service/dashboard/solr/logs_search";

  private static final String LOGSEARCH_GET_LOG_LEVELS_PATH = "/service/dashboard/getLogLevelCounts";

  private static final String LOGSEARCH_ADMIN_CREDENTIAL_NAME = "logsearch.admin.credential";

  private static final String COMPONENT_QUERY_PARAMETER_NAME = "component_name";

  private static final String HOST_QUERY_PARAMETER_NAME = "host_name";

  private static final String DEFAULT_PAGE_SIZE = "50";

  private static final String PAGE_SIZE_QUERY_PARAMETER_NAME = "pageSize";

  private static AtomicInteger errorLogCounterForLogSearchConnectionExceptions = new AtomicInteger(0);

  private final String hostName;

  private final String portNumber;

  private final CredentialStoreService credentialStoreService;

  private final Cluster cluster;

  private final NetworkConnection networkConnection;

  public LoggingRequestHelperImpl(String hostName, String portNumber, CredentialStoreService credentialStoreService, Cluster cluster) {
    this(hostName, portNumber, credentialStoreService, cluster, new DefaultNetworkConnection());
  }

  protected LoggingRequestHelperImpl(String hostName, String portNumber, CredentialStoreService credentialStoreService, Cluster cluster, NetworkConnection networkConnection) {
    this.hostName = hostName;
    this.portNumber = portNumber;
    this.credentialStoreService = credentialStoreService;
    this.cluster = cluster;
    this.networkConnection = networkConnection;
  }

  public LogQueryResponse sendQueryRequest(Map<String, String> queryParameters) {
    try {
      // use the Apache builder to create the correct URI
      URI logSearchURI = createLogSearchQueryURI("http", queryParameters);
      LOG.debug("Attempting to connect to LogSearch server at " + logSearchURI);

      HttpURLConnection httpURLConnection  = (HttpURLConnection)logSearchURI.toURL().openConnection();
      httpURLConnection.setRequestMethod("GET");

      setupCredentials(httpURLConnection);

      StringBuffer buffer = networkConnection.readQueryResponseFromServer(httpURLConnection);

      // setup a reader for the JSON response
      StringReader stringReader =
        new StringReader(buffer.toString());

      ObjectReader logQueryResponseReader =
        createObjectReader(LogQueryResponse.class);

      return logQueryResponseReader.readValue(stringReader);

    } catch (Exception e) {
      Utils.logErrorMessageWithThrowableWithCounter(LOG, errorLogCounterForLogSearchConnectionExceptions,
        "Error occurred while trying to connect to the LogSearch service...", e);
    }

    return null;
  }


  private void setupCredentials(HttpURLConnection httpURLConnection) {
    final String logSearchAdminUser =
      getLogSearchAdminUser();
    final String logSearchAdminPassword =
      getLogSearchAdminPassword();

    // first attempt to use the LogSearch admin configuration to
    // obtain the LogSearch server credential
    if ((logSearchAdminUser != null) && (logSearchAdminPassword != null)) {
      LOG.debug("Credential found in config, will be used to connect to LogSearch");
      networkConnection.setupBasicAuthentication(httpURLConnection, createEncodedCredentials(logSearchAdminUser, logSearchAdminPassword));
    } else {
      // if no credential found in config, attempt to locate the credential using
      // the Ambari CredentialStoreService
      PrincipalKeyCredential principalKeyCredential =
        getLogSearchCredentials();

      // determine the credential to use for connecting to LogSearch
      if (principalKeyCredential != null) {
        // setup credential stored in credential service
        LOG.debug("Credential found in CredentialStore, will be used to connect to LogSearch");
        networkConnection.setupBasicAuthentication(httpURLConnection, createEncodedCredentials(principalKeyCredential));
      } else {
        LOG.debug("No LogSearch credential could be found, this is probably an error in configuration");
      }
    }
  }

  private String getLogSearchAdminUser() {
    Config logSearchAdminConfig =
      cluster.getDesiredConfigByType(LOGSEARCH_ADMIN_JSON_CONFIG_TYPE_NAME);

    if (logSearchAdminConfig != null) {
      return logSearchAdminConfig.getProperties().get(LOGSEARCH_ADMIN_USERNAME_PROPERTY_NAME);
    }

    return null;
  }

  private String getLogSearchAdminPassword() {
    Config logSearchAdminConfig =
      cluster.getDesiredConfigByType(LOGSEARCH_ADMIN_JSON_CONFIG_TYPE_NAME);

    if (logSearchAdminConfig != null) {
      return logSearchAdminConfig.getProperties().get(LOGSEARCH_ADMIN_PASSWORD_PROPERTY_NAME);
    }

    return null;
  }

  public Set<String> sendGetLogFileNamesRequest(String componentName, String hostName) {
    Map<String, String> queryParameters =
      new HashMap<String, String>();

    // TODO, this current method will be a temporary workaround
    // TODO, until the new LogSearch API method is available to handle this request

    queryParameters.put(HOST_QUERY_PARAMETER_NAME, hostName);
    queryParameters.put(COMPONENT_QUERY_PARAMETER_NAME,componentName);
    // ask for page size of 1, since we really only want a single entry to
    // get the file path name
    queryParameters.put("pageSize", "1");

    LogQueryResponse response = sendQueryRequest(queryParameters);
    if ((response != null) && (!response.getListOfResults().isEmpty())) {
      LogLineResult lineOne = response.getListOfResults().get(0);
      // this assumes that each component has only one associated log file,
      // which may not always hold true
      LOG.debug("For componentName = " + componentName + ", log file name is = " + lineOne.getLogFilePath());
      return Collections.singleton(lineOne.getLogFilePath());

    }

    return Collections.emptySet();
  }

  @Override
  public LogLevelQueryResponse sendLogLevelQueryRequest(String componentName, String hostName) {
    try {
      // use the Apache builder to create the correct URI
      URI logLevelQueryURI = createLogLevelQueryURI("http", componentName, hostName);
      LOG.debug("Attempting to connect to LogSearch server at " + logLevelQueryURI);

      HttpURLConnection httpURLConnection = (HttpURLConnection) logLevelQueryURI.toURL().openConnection();
      httpURLConnection.setRequestMethod("GET");

      setupCredentials(httpURLConnection);

      StringBuffer buffer = networkConnection.readQueryResponseFromServer(httpURLConnection);

      // setup a reader for the JSON response
      StringReader stringReader =
        new StringReader(buffer.toString());

      ObjectReader logQueryResponseReader = createObjectReader(LogLevelQueryResponse.class);

      return logQueryResponseReader.readValue(stringReader);

    } catch (Exception e) {
      Utils.logErrorMessageWithThrowableWithCounter(LOG, errorLogCounterForLogSearchConnectionExceptions,
        "Error occurred while trying to connect to the LogSearch service...", e);
    }

    return null;
  }

  /**
   * Generates the log file tail URI, using the LogSearch server's
   * query parameters.
   *
   * @param baseURI the base URI for this request, typically the URI to the
   *                Ambari Integration searchEngine component
   *
   * @param componentName the component name
   * @param hostName the host name
   *
   * @return
   */
  @Override
  public String createLogFileTailURI(String baseURI, String componentName, String hostName) {
    return baseURI + "?" + COMPONENT_QUERY_PARAMETER_NAME + "=" + componentName + "&" + HOST_QUERY_PARAMETER_NAME + "=" + hostName
      + "&" + PAGE_SIZE_QUERY_PARAMETER_NAME + "=" + DEFAULT_PAGE_SIZE;
  }

  private static ObjectReader createObjectReader(Class type) {
    // setup the Jackson mapper/reader to read in the data structure
    ObjectMapper mapper = createJSONObjectMapper();

    return mapper.reader(type);
  }

  private URI createLogSearchQueryURI(String scheme, Map<String, String> queryParameters) throws URISyntaxException {
    URIBuilder uriBuilder = createBasicURI(scheme);
    uriBuilder.setPath(LOGSEARCH_QUERY_PATH);

    // add any query strings specified
    for (String key : queryParameters.keySet()) {
      uriBuilder.addParameter(key, queryParameters.get(key));
    }

    return uriBuilder.build();
  }

  private URIBuilder createBasicURI(String scheme) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setScheme(scheme);
    uriBuilder.setHost(hostName);
    uriBuilder.setPort(Integer.valueOf(portNumber));
    return uriBuilder;
  }

  private URI createLogLevelQueryURI(String scheme, String componentName, String hostName) throws URISyntaxException {
    URIBuilder uriBuilder = createBasicURI(scheme);
    uriBuilder.setPath(LOGSEARCH_GET_LOG_LEVELS_PATH);

    Map<String, String> queryParameters = new HashMap<String, String>();
    // set the query parameters to limit this level count
    // request to the specific component on the specified host
    queryParameters.put(HOST_QUERY_PARAMETER_NAME, hostName);
    queryParameters.put(COMPONENT_QUERY_PARAMETER_NAME,componentName);

    // add any query strings specified
    for (String key : queryParameters.keySet()) {
      uriBuilder.addParameter(key, queryParameters.get(key));
    }

    return uriBuilder.build();
  }



  protected static ObjectMapper createJSONObjectMapper() {
    ObjectMapper mapper =
      new ObjectMapper();
    AnnotationIntrospector introspector =
      new JacksonAnnotationIntrospector();
    mapper.setAnnotationIntrospector(introspector);
    mapper.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
    return mapper;
  }

  private PrincipalKeyCredential getLogSearchCredentials() {
    try {
      Credential credential =
        credentialStoreService.getCredential(cluster.getClusterName(), LOGSEARCH_ADMIN_CREDENTIAL_NAME);
      if ((credential != null)  && (credential instanceof PrincipalKeyCredential)) {
        return (PrincipalKeyCredential)credential;
      }

      if (credential == null) {
        LOG.debug("LogSearch credentials could not be obtained from store.");
      } else {
        LOG.debug("LogSearch credentials were not of the correct type, this is likely an error in configuration, credential type is = " + credential.getClass().getName());
      }
    } catch (AmbariException ambariException) {
      LOG.debug("Error encountered while trying to obtain LogSearch admin credentials.", ambariException);
    }

    return null;
  }

  private static String createEncodedCredentials(PrincipalKeyCredential principalKeyCredential) {
    return createEncodedCredentials(principalKeyCredential.getPrincipal(), new String(principalKeyCredential.getKey()));
  }

  private static String createEncodedCredentials(String userName, String password) {
    return Base64.encodeBase64String((userName + ":" + password).getBytes());
  }

  /**
   * Interface used to abstract out the network access needed to
   * connect to the LogSearch Server.
   *
   * This abstraction is useful for unit testing this class, and simulating
   * different output and error conditions.
   */
  interface NetworkConnection {
    StringBuffer readQueryResponseFromServer(HttpURLConnection httpURLConnection) throws IOException;

    void setupBasicAuthentication(HttpURLConnection httpURLConnection, String encodedCredentials);
  }

  /**
   * The default implementation of NetworkConnection, that reads
   * the InputStream associated with the HttpURL connection passed in.
   */
  private static class DefaultNetworkConnection implements NetworkConnection {
    @Override
    public StringBuffer readQueryResponseFromServer(HttpURLConnection httpURLConnection) throws IOException {
      InputStream resultStream = null;
      try {
        // read in the response from LogSearch
        resultStream = httpURLConnection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(resultStream));
        LOG.debug("Response code from LogSearch Service is = " + httpURLConnection.getResponseCode());

        String line = reader.readLine();
        StringBuffer buffer = new StringBuffer();
        while (line != null) {
          buffer.append(line);
          line = reader.readLine();
        }

        LOG.debug("Sucessfully retrieved response from server, response = " + buffer);

        return buffer;
      } finally {
        // make sure to close the stream after request is completed
        if (resultStream != null) {
          resultStream.close();
        }
      }
    }

    @Override
    public void setupBasicAuthentication(HttpURLConnection httpURLConnection, String encodedCredentials) {
      // default implementation for this method should just set the Authorization header
      // required for Basic Authentication to the LogSearch Server
      httpURLConnection.setRequestProperty("Authorization", "Basic " + encodedCredentials);
    }
  }


}
