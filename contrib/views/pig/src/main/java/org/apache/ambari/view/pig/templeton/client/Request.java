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

package org.apache.ambari.view.pig.templeton.client;

import com.google.gson.Gson;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.ambari.view.ViewContext;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Request handler, supports GET, POST, PUT, DELETE methods
 * @param <RESPONSE> data type to deserialize response from JSON
 */
public class Request<RESPONSE> {
  protected final Class<RESPONSE> responseClass;
  protected final ViewContext context;
  protected final WebResource resource;

  protected final Gson gson = new Gson();

  protected final static Logger LOG =
      LoggerFactory.getLogger(Request.class);

  /**
   * Constructor
   * @param resource object that represents resource
   * @param responseClass model class
   * @param context View Context instance
   */
  public Request(WebResource resource, Class<RESPONSE> responseClass, ViewContext context) {
    this.resource = resource;
    this.responseClass = responseClass;
    this.context = context;
  }

  /**
   * Main implementation of GET request
   * @param resource resource
   * @return unmarshalled response data
   */
  public RESPONSE get(WebResource resource) throws IOException {
    LOG.debug("GET " + resource.toString());

    InputStream inputStream = context.getURLStreamProvider().readFrom(resource.toString(), "GET",
        null, new HashMap<String, String>());

    String responseJson = IOUtils.toString(inputStream);
    LOG.debug(String.format("RESPONSE => %s", responseJson));
    return gson.fromJson(responseJson, responseClass);
  }

  /**
   * Make GET request
   * @see #get(WebResource)
   */
  public RESPONSE get() throws IOException {
    return get(this.resource);
  }

  /**
   * Make GET request
   * @see #get(WebResource)
   */
  public RESPONSE get(MultivaluedMapImpl params) throws IOException {
    return get(this.resource.queryParams(params));
  }

  /**
   * Main implementation of POST request
   * @param resource resource
   * @param data post body
   * @return unmarshalled response data
   */
  public RESPONSE post(WebResource resource, MultivaluedMapImpl data) throws IOException {
    LOG.debug("POST " + resource.toString());
    LOG.debug("data: " + data.toString());

    UriBuilder builder = UriBuilder.fromPath("host/");
    for(String key : data.keySet()) {
      for(String value : data.get(key))
        builder.queryParam(key, value);
    }

    if (data != null)
      LOG.debug("... data: " + builder.build().getRawQuery());

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("Content-Type", "application/x-www-form-urlencoded");

    InputStream inputStream = context.getURLStreamProvider().readFrom(resource.toString(),
        "POST", builder.build().getRawQuery(), headers);
    String responseJson = IOUtils.toString(inputStream);

    LOG.debug(String.format("RESPONSE => %s", responseJson));
    return gson.fromJson(responseJson, responseClass);
  }

  /**
   * @see #post(WebResource, MultivaluedMapImpl)
   */
  public RESPONSE post(MultivaluedMapImpl data) throws IOException {
    return post(resource, data);
  }

  /**
   * @see #post(WebResource, MultivaluedMapImpl)
   */
  public RESPONSE post() throws IOException {
    return post(resource, new MultivaluedMapImpl());
  }

  /**
   * @see #post(WebResource, MultivaluedMapImpl)
   */
  public RESPONSE post(MultivaluedMapImpl params, MultivaluedMapImpl data) throws IOException {
    return post(resource.queryParams(params), data);
  }

  /**
   * Main implementation of PUT request
   * @param resource resource
   * @param data put body
   * @return unmarshalled response data
   */
  public RESPONSE put(WebResource resource, MultivaluedMapImpl data) throws IOException {
    LOG.debug("PUT " + resource.toString());

    UriBuilder builder = UriBuilder.fromPath("host/");
    for(String key : data.keySet()) {
      for(String value : data.get(key))
        builder.queryParam(key, value);
    }

    if (data != null)
      LOG.debug("... data: " + builder.build().getRawQuery());

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("Content-Type", "application/x-www-form-urlencoded");

    InputStream inputStream = context.getURLStreamProvider().readFrom(resource.toString(),
        "PUT", builder.build().getRawQuery(), headers);
    String responseJson = IOUtils.toString(inputStream);

    LOG.debug(String.format("RESPONSE => %s", responseJson));
    return gson.fromJson(responseJson, responseClass);
  }

  /**
   * @see #put(WebResource, MultivaluedMapImpl)
   */
  public RESPONSE put(MultivaluedMapImpl data) throws IOException {
    return put(resource, data);
  }

  /**
   * @see #put(WebResource, MultivaluedMapImpl)
   */
  public RESPONSE put() throws IOException {
    return put(resource, new MultivaluedMapImpl());
  }

  /**
   * @see #put(WebResource, MultivaluedMapImpl)
   */
  public RESPONSE put(MultivaluedMapImpl params, MultivaluedMapImpl data) throws IOException {
    return put(resource.queryParams(params), data);
  }

  /**
   * Main implementation of DELETE request
   * @param resource resource
   * @param data delete body
   * @return unmarshalled response data
   */
  public RESPONSE delete(WebResource resource, MultivaluedMapImpl data) throws IOException {
    LOG.debug("DELETE " + resource.toString());

    UriBuilder builder = UriBuilder.fromPath("host/");
    for(String key : data.keySet()) {
      for(String value : data.get(key))
        builder.queryParam(key, value);
    }

    if (data != null)
      LOG.debug("... data: " + builder.build().getRawQuery());

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("Content-Type", "application/x-www-form-urlencoded");

    InputStream inputStream = context.getURLStreamProvider().readFrom(resource.toString(),
        "DELETE", builder.build().getRawQuery(), headers);
    String responseJson = IOUtils.toString(inputStream);

    LOG.debug(String.format("RESPONSE => %s", responseJson));
    return gson.fromJson(responseJson, responseClass);
  }

  /**
   * @see #delete(WebResource, MultivaluedMapImpl)
   */
  public RESPONSE delete(MultivaluedMapImpl data) throws IOException {
    return delete(resource, data);
  }

  /**
   * @see #delete(WebResource, MultivaluedMapImpl)
   */
  public RESPONSE delete() throws IOException {
    return delete(resource, new MultivaluedMapImpl());
  }

  /**
   * @see #delete(WebResource, MultivaluedMapImpl)
   */
  public RESPONSE delete(MultivaluedMapImpl params, MultivaluedMapImpl data) throws IOException {
    return delete(resource.queryParams(params), data);
  }
}
