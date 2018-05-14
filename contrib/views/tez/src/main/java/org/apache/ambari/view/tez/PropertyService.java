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
package org.apache.ambari.view.tez;

import org.apache.ambari.view.ViewContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

/**
 * The base property service.
 */
public abstract class PropertyService {

  private static final Logger LOG = LoggerFactory.getLogger(PropertyService.class);


  /**
 * The view context.
 */
  @Inject
  protected ViewContext context;

  protected String getResponse(String ... propertyNames) {

    if (LOG.isDebugEnabled()) {
      LOG.debug("PropertyService getResponse for view: " + context.getInstanceName());
    }

    Map<String, String> properties = context.getProperties();

    StringBuffer buffer = new StringBuffer();
    int count = 0;

    buffer.append("[");
    for (String propertyName : propertyNames) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("PropertyService, instanceName=" + context.getInstanceName()
            + ", propName=" + propertyName
            + ", val=" + properties.get(propertyName));
      }

      if (count++ > 0) {
        buffer.append(",\n");
      }
      buffer.append(getPropertyResponse(properties, propertyName));
    }
    buffer.append("]");

    return buffer.toString();
  }

  private String getPropertyResponse(Map<String, String> properties, String key) {
    StringBuffer buffer = new StringBuffer();

    String value = properties.get(key);

    buffer.append("{\"");
    buffer.append(key);
    buffer.append("\" : \"");
    buffer.append(value);
    buffer.append("\"}");

    return buffer.toString();
  }
} // end PropertyService
