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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Named
public class PropertiesSplitter {

  public List<String> parseList(String listStr) {
    return parseList(listStr, ",");
  }

  public Map<String, String> parseMap(String mapStr) {
    return parseMap(mapStr, ",", ":");
  }

  private List<String> parseList(String listStr, String separator) {
    return StringUtils.isNotBlank(listStr) ? Splitter.on(separator).splitToList(listStr) : new ArrayList<>();
  }

  public Map<String, String> parseMap(String mapStr, String separator, String keyValueSeparator) {
    Map<String, String> resultMap = new HashMap<>();
    if (StringUtils.isNotBlank(mapStr)) {
      List<String> keyValueList = parseList(mapStr, separator);
      if (!keyValueList.isEmpty()) {
        for (String keyValueElement : keyValueList) {
          if (StringUtils.isNotEmpty(keyValueElement)) {
            List<String> keyValueElementList = parseList(keyValueElement, keyValueSeparator);
            if (!CollectionUtils.isEmpty(keyValueElementList) && keyValueElementList.size() >= 2
              && StringUtils.isNotBlank(keyValueElementList.get(0))) {
              resultMap.put(keyValueElementList.get(0), keyValueElementList.get(1));
            }
          }
        }
      }
    }
    return resultMap;
  }

  public Map<String, Map<String, String>> parseMapInMap(String mapInMapStr) {
    Map<String, Map<String, String>> mapInMap = new HashMap<>();
    Map<String, String> outerMap = parseMap(mapInMapStr, ";", "#");
    if (!outerMap.isEmpty()) {
      for (Map.Entry<String, String> entry : outerMap.entrySet()) {
        Map<String, String> keyValueMap = parseMap(entry.getValue());
        if (!keyValueMap.isEmpty()) {
          mapInMap.put(entry.getKey(), keyValueMap);
        }
      }
    }
    return mapInMap;
  }

  public Map<String, List<String>> parseListInMap(String listInMapStr) {
    return parseListInMap(listInMapStr, ";", ":", ",");
  }

  public Map<String, List<String>> parseListInMap(String listInMapStr, String mapSeparator, String keyValueSeparator, String listSeparator) {
    Map<String, List<String>> listInMap = new HashMap<>();
    Map<String, String> typeKeyValueMap = parseMap(listInMapStr, mapSeparator, keyValueSeparator);
    for (Map.Entry<String, String> entry : typeKeyValueMap.entrySet()) {
      List<String> valuesList = parseList(entry.getValue(), listSeparator);
      listInMap.put(entry.getKey(), valuesList);
    }
    return listInMap;
  }

}
