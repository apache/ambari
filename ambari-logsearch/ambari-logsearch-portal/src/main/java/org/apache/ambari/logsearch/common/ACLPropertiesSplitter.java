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
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

@Named
public class ACLPropertiesSplitter {

  public List<ACL> parseAcls(String aclStr) {
    List<ACL> acls = new ArrayList<>();
    List<String> aclStrList = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(aclStr);
    for (String unparcedAcl : aclStrList) {
      String[] parts = unparcedAcl.split(":");
      if (parts.length == 3) {
        acls.add(new ACL(parsePermission(parts[2]), new Id(parts[0], parts[1])));
      }
    }
    return acls;
  }

  private Integer parsePermission(String permission) {
    int permissionCode = 0;
    for (char each : permission.toLowerCase().toCharArray()) {
      switch (each) {
        case 'r':
          permissionCode |= ZooDefs.Perms.READ;
          break;
        case 'w':
          permissionCode |= ZooDefs.Perms.WRITE;
          break;
        case 'c':
          permissionCode |= ZooDefs.Perms.CREATE;
          break;
        case 'd':
          permissionCode |= ZooDefs.Perms.DELETE;
          break;
        case 'a':
          permissionCode |= ZooDefs.Perms.ADMIN;
          break;
        default:
          throw new IllegalArgumentException("Unsupported permission: " + permission);
      }
    }
    return permissionCode;
  }
}
