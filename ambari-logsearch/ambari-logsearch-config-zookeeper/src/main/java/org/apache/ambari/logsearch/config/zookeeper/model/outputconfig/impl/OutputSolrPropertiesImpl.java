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

package org.apache.ambari.logsearch.config.zookeeper.model.outputconfig.impl;

import org.apache.ambari.logsearch.config.api.model.outputconfig.OutputSolrProperties;

import com.google.gson.annotations.SerializedName;

public class OutputSolrPropertiesImpl implements OutputSolrProperties {
  private final String collection;

  @SerializedName("split_interval_mins")
  private final String splitIntervalMins;

  public OutputSolrPropertiesImpl(String collection, String splitIntervalMins) {
    this.collection = collection;
    this.splitIntervalMins = splitIntervalMins;
  }

  @Override
  public String getCollection() {
    return collection;
  }

  @Override
  public String getSplitIntervalMins() {
    return splitIntervalMins;
  }
}
