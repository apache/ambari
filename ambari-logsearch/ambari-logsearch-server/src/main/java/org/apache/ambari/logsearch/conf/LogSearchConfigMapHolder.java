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
package org.apache.ambari.logsearch.conf;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.web.context.support.StandardServletEnvironment;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.ambari.logsearch.common.LogSearchConstants.LOGSEARCH_PROPERTIES_FILE;

@Configuration
public class LogSearchConfigMapHolder {

  @Inject
  private Environment environment;

  private Map<String, String> logsearchProperties = new HashMap<>();

  public Map<String, String> getLogsearchProperties() {
    if (logsearchProperties.isEmpty()) {
      PropertySource propertySource = ((StandardServletEnvironment) environment)
        .getPropertySources().get("class path resource [" + LOGSEARCH_PROPERTIES_FILE + "]");
      setLogsearchProperties(stringifyValues(((MapPropertySource) propertySource).getSource()));
    }
    return logsearchProperties;
  }

  public void setLogsearchProperties(Map<String, String> logsearchProperties) {
    this.logsearchProperties = logsearchProperties;
  }

  private Map<String, String> stringifyValues(Map<String, Object> vars) {
    return vars.entrySet()
      .stream()
      .collect(Collectors.toMap(Map.Entry::getKey, e -> (String) e.getValue()));
  }

}
