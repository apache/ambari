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
package org.apache.ambari.logfeeder.logconfig.filter;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Default filter to allow everything
 */
public class DefaultDataFilter {
  private static Logger logger = Logger.getLogger(DefaultDataFilter.class);

  protected static final boolean CASE_SENSITIVE = false;

  public boolean applyFilter(Map<String, Object> outputJsonObj, boolean defaultValue) {
    return defaultValue;
  }

  public boolean isEmpty(Map<String, Object> map) {
    if (map == null || map.isEmpty()) {
      return true;
    }
    return false;
  }

  public boolean isEmpty(String str) {
    if (str == null || str.trim().isEmpty()) {
      return true;
    }
    return false;
  }

  public boolean isNotEmpty(String str) {
    return !isEmpty(str);
  }

}
