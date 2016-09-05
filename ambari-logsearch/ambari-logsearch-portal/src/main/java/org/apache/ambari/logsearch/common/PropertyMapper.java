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
package org.apache.ambari.logsearch.common;

import com.google.common.base.Splitter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("propertyMapper")
public class PropertyMapper {

  public Map<String, String> map(String property) {
    return this.map(property, ",");
  }

  public List<String> list(String property) {
    return this.list(property, ",");
  }

  public Map<String, String> solrUiMap(String property) { return this.solrUiMap(property, ","); }

  private List<String> list(String property, String splitter) {
    return Splitter.on(splitter).omitEmptyStrings().trimResults().splitToList(property);
  }

  private Map<String, String> map(String property, String splitter) {
    return Splitter.on(splitter).omitEmptyStrings().trimResults().withKeyValueSeparator(":").split(property);
  }

  private Map<String, String> solrUiMap(String property, String splitter) {
    Map<String, String> result = new HashMap<>();
    Map<String, String> map = this.map(property, splitter);
    for (Map.Entry<String, String> propEntry : map.entrySet()) {
      result.put(propEntry.getKey() + LogSearchConstants.SOLR_SUFFIX, propEntry.getValue());
      result.put(propEntry.getValue() + LogSearchConstants.UI_SUFFIX, propEntry.getKey());
    }
    return result;
  }

}
