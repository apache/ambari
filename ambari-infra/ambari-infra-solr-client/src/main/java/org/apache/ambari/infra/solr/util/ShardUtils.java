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
package org.apache.ambari.infra.solr.util;

import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.cloud.Slice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class ShardUtils {

  private static final Logger LOG = LoggerFactory.getLogger(ShardUtils.class);

  public static String generateShardListStr(int maxShardsPerNode) {
    String shardsListStr = "";
    for (int i = 0; i < maxShardsPerNode; i++) {
      if (i != 0) {
        shardsListStr += ",";
      }
      String shard = "shard" + i;
      shardsListStr += shard;
    }
    return shardsListStr;
  }

  public static List<String> generateShardList(int maxShardsPerNode) {
    List<String> shardsList = new ArrayList<>();
    for (int i = 0; i < maxShardsPerNode; i++) {
      shardsList.add("shard" + i);
    }
    return shardsList;
  }

  public static Collection<String> getShardNamesFromSlices(Collection<Slice> slices, String collection) {
    Collection<String> result = new HashSet<String>();
    Iterator<Slice> iter = slices.iterator();
    while (iter.hasNext()) {
      Slice slice = iter.next();
      for (Replica replica : slice.getReplicas()) {
        LOG.info("collectionName=" + collection + ", slice.name="
          + slice.getName() + ", slice.state=" + slice.getState()
          + ", replica.core=" + replica.getStr("core")
          + ", replica.state=" + replica.getStr("state"));
        result.add(slice.getName());
      }
    }
    return result;
  }
}
