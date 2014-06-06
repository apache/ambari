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
package org.apache.ambari.server.upgrade;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author root
 */
public class StackUpgradeHelperTest {
  
  public StackUpgradeHelperTest() {
  }

  @Test
  public void testUpdateStackVersion() {
    System.out.println("updateStackVersion");
    Map<String, String> stackInfo = new HashMap<String, String>();
    stackInfo.put("repo_url", "http://foo.bar");
    stackInfo.put("repo_url_os", "centos6");
    stackInfo.put("HDP", "1.3.0");
    StackUpgradeHelper instance = new StackUpgradeHelper();
    try {
      instance.updateStackVersion(stackInfo);
    } catch (Exception ex) {
      assertEquals("UnknownHostException, Responce: 0, "
       + "during check URL: http://foo.bar/repodata/repomd.xml", ex.getMessage());
    }
  }

  
}
