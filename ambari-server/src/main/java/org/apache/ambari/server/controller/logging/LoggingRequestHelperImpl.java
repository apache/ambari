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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Convenience class to handle the connection details of a LogSearch query request.
 *
 */
public class LoggingRequestHelperImpl implements LoggingRequestHelper {

  private Logger LOG = Logger.getLogger(LoggingRequestHelperImpl.class);

  //TODO, hardcoded localhost for LogSearch service for dev purposes, will switch to config after POC finished


  private static String DEFAULT_HOSTNAME = "localhost";

  private static String DEFAULT_PORT = "61888";

  private static String LOGSEARCH_QUERY_PATH = "/service/dashboard/solr/logs_search";

  private static String DEFAULT_LOGSEARCH_USER = "admin";

  private static String DEFAULT_LOGSEARCH_PWD = "admin";

  private final String hostName;

  private final String portNumber;


  public LoggingRequestHelperImpl() {
    this(DEFAULT_HOSTNAME, DEFAULT_PORT);
  }

  public LoggingRequestHelperImpl(String hostName, String portNumber) {
    this.hostName = hostName;
    this.portNumber = portNumber;
  }

  public LogQueryResponse sendQueryRequest(Map<String, String> queryParameters) {
    try {
      // use the Apache builder to create the correct URI
      URI logSearchURI = createLogSearchQueryURI(queryParameters);
      LOG.info("Attempting to connect to LogSearch server at " + logSearchURI);

      HttpURLConnection httpURLConnection  = (HttpURLConnection)logSearchURI.toURL().openConnection();
      httpURLConnection.setRequestMethod("GET");
      setupBasicAuthentication(httpURLConnection);
      StringBuffer buffer = readQueryResponseFromServer(httpURLConnection);

      // setup a reader for the JSON response
      StringReader stringReader =
        new StringReader(buffer.toString());

      // setup the Jackson mapper/reader to read in the data structure
      ObjectMapper mapper = createJSONObjectMapper();

      ObjectReader logQueryResponseReader =
        mapper.reader(LogQueryResponse.class);

      LogQueryResponse queryResult =
        logQueryResponseReader.readValue(stringReader);

      LOG.debug("DEBUG: response from LogSearch was: " + buffer);

      return queryResult;

    } catch (MalformedURLException e) {
      // TODO, need better error handling here
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    } catch (URISyntaxException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }

    return null;
  }

  public Set<String> sendGetLogFileNamesRequest(String componentName, String hostName) {
    Map<String, String> queryParameters =
      new HashMap<String, String>();

    // TODO, this current method will be a temporary workaround
    // TODO, until the new LogSearch API method is available to handle this request

    queryParameters.put("host", hostName);
    queryParameters.put("components_name",componentName);
    // ask for page size of 1, since we really only want a single entry to
    // get the file path name
    queryParameters.put("pageSize", "1");

    LogQueryResponse response = sendQueryRequest(queryParameters);
    if ((response != null) && (!response.getListOfResults().isEmpty())) {
      LogLineResult lineOne = response.getListOfResults().get(0);
      // this assumes that each component has only one associated log file,
      // which may not always hold true
      LOG.info("For componentName = " + componentName + ", log file name is = " + lineOne.getLogFilePath());
      return Collections.singleton(lineOne.getLogFilePath());

    }

    return Collections.emptySet();
  }

  private URI createLogSearchQueryURI(Map<String, String> queryParameters) throws URISyntaxException {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setScheme("http");
    uriBuilder.setHost(hostName);
    uriBuilder.setPort(Integer.valueOf(portNumber));
    uriBuilder.setPath(LOGSEARCH_QUERY_PATH);

    // add any query strings specified
    for (String key : queryParameters.keySet()) {
      uriBuilder.addParameter(key, queryParameters.get(key));
    }

    return uriBuilder.build();
  }

  protected ObjectMapper createJSONObjectMapper() {
    ObjectMapper mapper =
      new ObjectMapper();
    AnnotationIntrospector introspector =
      new JacksonAnnotationIntrospector();
    mapper.setAnnotationIntrospector(introspector);
    mapper.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
    return mapper;
  }

  private StringBuffer readQueryResponseFromServer(HttpURLConnection httpURLConnection) throws IOException {
    InputStream resultStream = null;
    try {
      // read in the response from LogSearch
      resultStream = httpURLConnection.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(resultStream));
      LOG.info("Response code from LogSearch Service is = " + httpURLConnection.getResponseCode());

      String line = reader.readLine();
      StringBuffer buffer = new StringBuffer();
      while (line != null) {
        buffer.append(line);
        line = reader.readLine();
      }
      return buffer;
    } finally {
      // make sure to close the stream after request is completed
      if (resultStream != null) {
        resultStream.close();
      }
    }
  }

  private static void setupBasicAuthentication(HttpURLConnection httpURLConnection) {
    //TODO, using hard-coded Base64 auth for now, need to revisit this
    String encodedCredentials =
      Base64.encodeBase64String((DEFAULT_LOGSEARCH_USER + ":" + DEFAULT_LOGSEARCH_PWD).getBytes());
    httpURLConnection.setRequestProperty("Authorization", "Basic " + encodedCredentials);
  }


}
