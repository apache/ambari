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
package org.apache.ambari.logsearch.solr.util;

import org.apache.zookeeper.data.ACL;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AclUtils {

  public static Set<String> getUsersFromAclData(List<ACL> acls) {
    Set<String> result = new HashSet<>();
    if (!acls.isEmpty()) {
      for (ACL acl : acls) {
        String username = "";
        String id = acl.getId().getId();
        String[] splitted = id.split(":");
        if (splitted.length > 1) {
          username = splitted[0];
        } else {
          username = id;
        }
        result.add(username);
      }
    }
    return result;
  }

  public static List<ACL> updatePermissionForScheme(List<ACL> acls, String scheme, int permission) {
    List<ACL> aclResult = new ArrayList<>();
    if (!acls.isEmpty()) {
      for (ACL acl : acls) {
        int permissionToAdd = scheme.equals(acl.getId().getScheme()) ? permission : acl.getPerms();
        acl.setPerms(permissionToAdd);
        aclResult.add(acl);
      }
    }
    return aclResult;
  }

  public static boolean isPermissionDiffersForScheme(List<ACL> acls, String scheme, int permission) {
    boolean result = false;
    if (!acls.isEmpty()) {
      for (ACL acl : acls) {
        if (scheme.equals(acl.getId().getScheme()) && acl.getPerms() == permission) {
          result = true;
        }
      }
    }
    return result;
  }
}
