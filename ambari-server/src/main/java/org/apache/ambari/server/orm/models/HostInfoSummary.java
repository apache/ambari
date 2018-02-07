/*
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

package org.apache.ambari.server.orm.models;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.orm.dao.HostInfoSummaryDAO;
import org.apache.ambari.server.orm.dao.HostInfoSummaryDTO;

import com.google.inject.Inject;

@StaticallyInject
public class HostInfoSummary {

  static public String HOST_INFO_SUMMARY_OS = "operating_systems";

  @Inject
  private static HostInfoSummaryDAO hostInfoSummaryDAO;

  private List<Object> summary = new ArrayList<>();

  public  HostInfoSummary getHostInfoSummary(String cluster_name) {

    List<HostInfoSummaryDTO> summaryDTOS = hostInfoSummaryDAO.findHostInfoSummary(cluster_name);
    List<Map<String, Integer>> osSummaryList = new ArrayList<>();
    for (HostInfoSummaryDTO summaryDTO : summaryDTOS) {
      osSummaryList.add(Stream.of(new AbstractMap.SimpleImmutableEntry<>(summaryDTO.getOsType(), summaryDTO.getOsTypeCount())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }
    Map<String, Object> os = new HashMap<>();
    os.put(HOST_INFO_SUMMARY_OS, osSummaryList);
    summary.add(os);
    return this;
  }

  public List<Object> getSummary() {
    return summary;
  }

}
