/**
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

package org.apache.ambari.server.stack;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.quicklinks.Check;
import org.apache.ambari.server.state.quicklinks.Link;
import org.apache.ambari.server.state.quicklinks.Port;
import org.apache.ambari.server.state.quicklinks.Protocol;
import org.apache.ambari.server.state.quicklinks.QuickLinks;
import org.apache.ambari.server.state.quicklinks.QuickLinksConfiguration;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

public class QuickLinksConfigurationModuleTest {

  @Test
  public void testResolveInherit() throws Exception{
    QuickLinks[] results = resolveQuickLinks("parent_quicklinks.json", "child_quicklinks_to_inherit.json");
    QuickLinks parentQuickLinks = results[0];
    QuickLinks childQuickLinks = results[1];

    //resolved quicklinks configuration
    QuickLinksConfiguration childQuickLinksConfig = childQuickLinks.getQuickLinksConfiguration();
    assertNotNull(childQuickLinksConfig);

    //inherit links
    List<Link> links = childQuickLinksConfig.getLinks();
    assertNotNull(links);
    assertEquals(4, links.size());
    assertEquals(4, parentQuickLinks.getQuickLinksConfiguration().getLinks().size());

    //inherit protocol
    Protocol protocol = childQuickLinksConfig.getProtocol();
    assertNotNull(protocol);
    assertEquals("https", protocol.getType());
    assertEquals(1, protocol.getChecks().size());
  }

  @Test
  public void testResolveMerge() throws Exception {
    QuickLinks[] results = resolveQuickLinks("parent_quicklinks.json", "child_quicklinks_to_merge.json");
    QuickLinks parentQuickLinks = results[0];
    QuickLinks childQuickLinks = results[1];

    //resolved quicklinks configuration
    QuickLinksConfiguration childQuickLinksConfig = childQuickLinks.getQuickLinksConfiguration();
    assertNotNull(childQuickLinksConfig);

    //merged links
    List<Link> links = childQuickLinksConfig.getLinks();
    assertNotNull(links);
    assertEquals(7, links.size());
    assertEquals(4, parentQuickLinks.getQuickLinksConfiguration().getLinks().size());
  }

  @Test
  public void testResolveOverride() throws Exception{
    QuickLinks[] results = resolveQuickLinks("parent_quicklinks.json", "child_quicklinks_to_override.json");
    QuickLinks parentQuickLinks = results[0];
    QuickLinks childQuickLinks = results[1];

    //resolved quicklinks configuration
    QuickLinksConfiguration childQuickLinksConfig = childQuickLinks.getQuickLinksConfiguration();
    assertNotNull(childQuickLinksConfig);

    //links
    List<Link> links = childQuickLinksConfig.getLinks();
    assertNotNull(links);
    assertEquals(7, links.size());
    assertEquals(4, parentQuickLinks.getQuickLinksConfiguration().getLinks().size());
    boolean hasLink = false;
    for(Link link: links){
      String name = link.getName();
      if("thread_stacks".equals(name)){
        hasLink = true;
        Port port = link.getPort();
        assertEquals("mapred-site", port.getSite());
      }
    }
    assertTrue(hasLink);

    //protocol
    Protocol protocol = childQuickLinksConfig.getProtocol();
    assertNotNull(protocol);
    assertEquals("http", protocol.getType());
    assertEquals(3, protocol.getChecks().size());
    List<Check> checks = protocol.getChecks();
    for(Check check: checks){
      assertEquals("mapred-site", check.getSite());
    }
  }

  private QuickLinks[] resolveQuickLinks(String parentJson, String childJson) throws AmbariException{
    File parentQuiclinksFile = new File(this.getClass().getClassLoader().getResource(parentJson).getFile());
    File childQuickLinksFile = new File(this.getClass().getClassLoader().getResource(childJson).getFile());

    QuickLinksConfigurationModule parentModule = new QuickLinksConfigurationModule(parentQuiclinksFile);
    QuickLinksConfigurationModule childModule = new QuickLinksConfigurationModule(childQuickLinksFile);

    childModule.resolve(parentModule, null, null);

    QuickLinks parentQuickLinks = parentModule.getModuleInfo().getQuickLinksConfigurationMap().get(QuickLinksConfigurationModule.QUICKLINKS_CONFIGURATION_KEY);
    QuickLinks childQuickLinks = childModule.getModuleInfo().getQuickLinksConfigurationMap().get(QuickLinksConfigurationModule.QUICKLINKS_CONFIGURATION_KEY);

    return new QuickLinks[]{parentQuickLinks, childQuickLinks};
  }
}