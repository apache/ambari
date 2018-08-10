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
package org.apache.ambari.logfeeder.common;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class to generete UUID (random or based on specific fields)
 */
public class IdGeneratorHelper {

  private IdGeneratorHelper() {
  }

  /**
   * Generate UUID based on fields (or just randomly)
   * @param data object map which can contain the field key-value pairs
   * @param idFields field names that used for generating uuid
   * @return generated UUID string
   */
  public static String generateUUID(Map<String, Object> data, List<String> idFields) {
    String uuid = null;
    if (CollectionUtils.isNotEmpty(idFields)) {
      final StringBuilder sb = new StringBuilder();
      for (String idField : idFields) {
        if (data.containsKey(idField)) {
          sb.append(data.get(idField).toString());
        }
      }
      String concatId = sb.toString();
      if (StringUtils.isNotEmpty(concatId)) {
        uuid = UUID.nameUUIDFromBytes(concatId.getBytes()).toString();
      } else {
        uuid = UUID.randomUUID().toString();
      }
    } else {
      uuid = UUID.randomUUID().toString();
    }
    return uuid;
  }
}
