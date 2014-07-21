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

package org.apache.ambari.view.capacityscheduler.proxy;

import org.apache.ambari.view.capacityscheduler.utils.ServiceFormattedException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Response translator encapsulates InputStream
 * and able to convert it to different data types
 */
public class ResponseTranslator {
  private static final Logger LOG = LoggerFactory.getLogger(RequestBuilder.class);

  private InputStream inputStream;

  /**
   * Constructor for ResponseTranslator
   * @param inputStream InputStream instance
   */
  public ResponseTranslator(InputStream inputStream) {
    this.inputStream = inputStream;
  }

  /**
   * Get InputStream of response from server for manual processing
   * @return input stream of response
   */
  public InputStream asInputStream() {
    return inputStream;
  }

  /**
   * Retrieve response as String
   * @return response as String
   */
  public String asString() {
    String response;
    try {
      response = IOUtils.toString(inputStream);
    } catch (IOException e) {
      throw new ServiceFormattedException("Can't read from target host", e);
    }
    LOG.debug(String.format("Response: %s", response));

    return response;
  }

  /**
   * Retrieve response as JSON
   * @return response as JSON
   */
  public JSONObject asJSON() {
    String jsonString = asString();
    return (JSONObject) JSONValue.parse(jsonString);
  }

  /**
   * InputStream setter
   * @param inputStream InputStream instance
   */
  public void setInputStream(InputStream inputStream) {
    this.inputStream = inputStream;
  }
}
