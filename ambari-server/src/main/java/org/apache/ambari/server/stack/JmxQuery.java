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

package org.apache.ambari.server.stack;

import com.google.common.reflect.TypeToken;
import org.apache.ambari.server.utils.HTTPUtils;
import org.apache.ambari.server.utils.StageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * Class with methods to query JMX.
 * This is its own class with non-static methods because it is mocked during unit tests.
 */
public class JmxQuery {

  private static Logger LOG = LoggerFactory.getLogger(JmxQuery.class);

  public String queryJmxBeanValue(String hostname, int port, String beanName, String attributeName,
                                   boolean asQuery) {
    return queryJmxBeanValue(hostname, port, beanName, attributeName, asQuery, false);
  }

  /**
   * Query the JMX attribute at http(s)://$server:$port/jmx?qry=$query or http(s)://$server:$port/jmx?get=$bean::$attribute
   * @param hostname host name
   * @param port port number
   * @param beanName if asQuery is false, then search for this bean name
   * @param attributeName if asQuery is false, then search for this attribute name
   * @param asQuery whether to search bean or query
   * @param encrypted true if using https instead of http.
   * @return The jmx value.
   */
  public String queryJmxBeanValue(String hostname, int port, String beanName, String attributeName,
      boolean asQuery, boolean encrypted) {

    String protocol = encrypted ? "https://" : "http://";
    String endPoint = protocol + (asQuery ?
        String.format("%s:%s/jmx?qry=%s", hostname, port, beanName) :
        String.format("%s:%s/jmx?get=%s::%s", hostname, port, beanName, attributeName));

    String response = HTTPUtils.requestURL(endPoint);

    if (null == response || response.isEmpty()) {
      return null;
    }

    Type type = new TypeToken<Map<String, ArrayList<HashMap<String, String>>>>() {}.getType();

    try {
      Map<String, ArrayList<HashMap<String, String>>> jmxBeans =
          StageUtils.getGson().fromJson(response, type);

      return jmxBeans.get("beans").get(0).get(attributeName);
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.info("Could not load JMX from {}/{} from {}", beanName, attributeName, hostname, e);
      } else {
        LOG.info("Could not load JMX from {}/{} from {}", beanName, attributeName, hostname);
      }
    }

    return null;
  }
}
