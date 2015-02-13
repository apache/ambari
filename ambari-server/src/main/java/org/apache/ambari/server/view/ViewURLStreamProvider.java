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

package org.apache.ambari.server.view;

import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.proxy.ProxyService;
import org.apache.ambari.view.ViewContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper around an internal URL stream provider.
 */
public class ViewURLStreamProvider implements org.apache.ambari.view.URLStreamProvider {

  /**
   * The key for the "doAs" header.
   */
  private static final String DO_AS_PARAM = "doAs";

  /**
   * The view context.
   */
  private final ViewContext viewContext;

  /**
   * Internal stream provider.
   */
  private final URLStreamProvider streamProvider;


  // ----- Constructor -----------------------------------------------------

  /**
   * Construct a view URL stream provider.
   *
   * @param viewContext     the associated view context
   * @param streamProvider  the underlying stream provider
   */
  protected ViewURLStreamProvider(ViewContext viewContext, URLStreamProvider streamProvider) {
    this.viewContext    = viewContext;
    this.streamProvider = streamProvider;
  }


  // ----- URLStreamProvider -----------------------------------------------

  @Override
  public InputStream readFrom(String spec, String requestMethod, String body, Map<String, String> headers)
      throws IOException {
    // adapt the headers to the internal URLStreamProvider processURL signature
    Map<String, List<String>> headerMap = new HashMap<String, List<String>>();
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      headerMap.put(entry.getKey(), Collections.singletonList(entry.getValue()));
    }

    HttpURLConnection connection = streamProvider.processURL(spec, requestMethod, body, headerMap);

    int responseCode = connection.getResponseCode();

    return responseCode >= ProxyService.HTTP_ERROR_RANGE_START ?
        connection.getErrorStream() : connection.getInputStream();
  }

  @Override
  public InputStream readAs(String spec, String requestMethod, String body, Map<String, String> headers,
                            String userName)
      throws IOException {

    if (spec.toLowerCase().contains(DO_AS_PARAM)) {
      throw new IllegalArgumentException("URL cannot contain \"" + DO_AS_PARAM + "\" parameter.");
    }

    if (headers == null) {
      headers = new HashMap<String, String>();
    } else {
      headers = new HashMap<String, String>(headers);
    }

    headers.put(DO_AS_PARAM, userName);

    return readFrom(spec, requestMethod, body, headers);
  }

  @Override
  public InputStream readAsCurrent(String spec, String requestMethod, String body, Map<String, String> headers)
      throws IOException {

    return readAs(spec, requestMethod, body, headers, viewContext.getUsername());
  }
}

