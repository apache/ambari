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

package org.apache.ambari.server.utils;

import org.junit.Assert;
import org.junit.Test;

public class URLCredentialsHiderTest {

  @Test
  public void testHideUserInfo() {

    String testURL1 = "http://user01:pass@host:8443/api/v1";
    Assert.assertEquals(String.format("http://%s@host:8443/api/v1", URLCredentialsHider.HIDDEN_CREDENTIALS),
                        URLCredentialsHider.hideCredentials(testURL1));

    String testURL2 = "http://user01@host:8443/api/v1";
    Assert.assertEquals(String.format("http://%s@host:8443/api/v1",
                                      URLCredentialsHider.HIDDEN_USER),
                        URLCredentialsHider.hideCredentials(testURL2));

    String invalidURL = "htt://user01:pass@host:8443/api/v1";
    Assert.assertEquals(URLCredentialsHider.INVALID_URL, URLCredentialsHider.hideCredentials(invalidURL));
  }
}
