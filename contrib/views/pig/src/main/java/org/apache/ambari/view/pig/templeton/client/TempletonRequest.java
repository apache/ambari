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

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.ambari.view.ViewContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Request handler that adds user.name and doAs
 * GET parameters to every request
 * @param <RESPONSE> data type to deserialize response from JSON
 */
public class TempletonRequest<RESPONSE> extends Request<RESPONSE> {
  private String username;
  private String doAs;

  protected final static Logger LOG =
      LoggerFactory.getLogger(TempletonRequest.class);

  /**
   * Constructor
   * @param resource object that represents resource
   * @param responseClass model class
   * @param context View Context instance
   * @param username user.name of templeton. user.name will be equal to doAs value
   */
  public TempletonRequest(WebResource resource, Class<RESPONSE> responseClass,
                          String username, ViewContext context) {
    this(resource, responseClass, username, username, context);
  }

  /**
   * Constructor
   * @param resource object that represents resource
   * @param responseClass model class
   * @param context View Context instance
   * @param username user.name of templeton
   * @param doAs doAs user for templeton
   */
  public TempletonRequest(WebResource resource, Class<RESPONSE> responseClass,
                          String username, String doAs, ViewContext context) {
    super(resource, responseClass, context);
    this.username = username;
    this.doAs = doAs;
  }

  @Override
  public RESPONSE get(WebResource resource) throws IOException {
    MultivaluedMapImpl params = new MultivaluedMapImpl();
    params.add("user.name", username);
    params.add("doAs", doAs);
    return super.get(resource.queryParams(params));
  }

  @Override
  public RESPONSE put(WebResource resource, MultivaluedMapImpl data) throws IOException {
    MultivaluedMapImpl params = new MultivaluedMapImpl();
    params.add("user.name", username);
    params.add("doAs", doAs);
    return super.put(resource.queryParams(params), data);
  }

  @Override
  public RESPONSE delete(WebResource resource, MultivaluedMapImpl data) throws IOException {
    MultivaluedMapImpl params = new MultivaluedMapImpl();
    params.add("user.name", username);
    params.add("doAs", doAs);
    return super.delete(resource.queryParams(params), data);
  }

  @Override
  public RESPONSE post(WebResource resource, MultivaluedMapImpl data) throws IOException {
    MultivaluedMapImpl params = new MultivaluedMapImpl();
    params.add("user.name", username);
    params.add("doAs", doAs);
    return super.post(resource.queryParams(params), data);
  }
}
