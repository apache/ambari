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

package org.apache.ambari.server.controller.internal;


import org.apache.ambari.server.controller.ivory.Feed;
import org.apache.ambari.server.controller.ivory.IvoryService;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * Tests for FeedResourceProvider.
 */
public class FeedResourceProviderTest {
  @Test
  public void testCreateResources() throws Exception {
    IvoryService service = createMock(IvoryService.class);

    Set<Map<String, Object>> propertySet = new HashSet<Map<String, Object>>();

    Map<String, Object> properties = new HashMap<String, Object>();

    properties.put(FeedResourceProvider.FEED_NAME_PROPERTY_ID, "Feed1");
    properties.put(FeedResourceProvider.FEED_DESCRIPTION_PROPERTY_ID, "desc");
    properties.put(FeedResourceProvider.FEED_SCHEDULE_PROPERTY_ID, "sched");
    properties.put(FeedResourceProvider.FEED_STATUS_PROPERTY_ID, "SUBMITTED");
    properties.put(FeedResourceProvider.FEED_SOURCE_CLUSTER_NAME_PROPERTY_ID, "source");
    properties.put(FeedResourceProvider.FEED_TARGET_CLUSTER_NAME_PROPERTY_ID, "target");

    // set expectations
    service.submitFeed(FeedResourceProvider.getFeed("Feed1", properties));

    // replay
    replay(service);

    propertySet.add(properties);

    Request request = PropertyHelper.getCreateRequest(propertySet, Collections.<String,String>emptyMap());

    FeedResourceProvider provider = new FeedResourceProvider(service,
        PropertyHelper.getPropertyIds(Resource.Type.DRFeed),
        PropertyHelper.getKeyPropertyIds(Resource.Type.DRFeed));

    provider.createResources(request);

    // verify
    verify(service);
  }

  @Test
  public void testGetResources() throws Exception {
    IvoryService service = createMock(IvoryService.class);

    Set<Map<String, Object>> propertySet = new HashSet<Map<String, Object>>();

    Map<String, Object> properties = new HashMap<String, Object>();

    List<String> feedNames = new LinkedList<String>();
    feedNames.add("Feed1");
    feedNames.add("Feed2");
    feedNames.add("Feed3");

    Map<String,String> props = new HashMap<String, String>();

    Feed feed1 = new Feed("Feed1", "d", "s", "sch", "source", "st", "end", "l", "a", "target", "st", "end", "l", "a", props);
    Feed feed2 = new Feed("Feed2", "d", "s", "sch", "source", "st", "end", "l", "a", "target", "st", "end", "l", "a", props);
    Feed feed3 = new Feed("Feed3", "d", "s", "sch", "source", "st", "end", "l", "a", "target", "st", "end", "l", "a", props);

    // set expectations
    expect(service.getFeedNames()).andReturn(feedNames);

    expect(service.getFeed("Feed1")).andReturn(feed1);
    expect(service.getFeed("Feed2")).andReturn(feed2);
    expect(service.getFeed("Feed3")).andReturn(feed3);

    // replay
    replay(service);

    propertySet.add(properties);

    Request request = PropertyHelper.getCreateRequest(propertySet, Collections.<String,String>emptyMap());

    FeedResourceProvider provider = new FeedResourceProvider(service,
        PropertyHelper.getPropertyIds(Resource.Type.DRFeed),
        PropertyHelper.getKeyPropertyIds(Resource.Type.DRFeed));

    Set<Resource> resources = provider.getResources(request, null);

    Assert.assertEquals(3, resources.size());

    // verify
    verify(service);
  }

  @Test
  public void testUpdateResources() throws Exception {
    IvoryService service = createMock(IvoryService.class);

    Set<Map<String, Object>> propertySet = new HashSet<Map<String, Object>>();

    Map<String, Object> properties = new HashMap<String, Object>();

    properties.put(FeedResourceProvider.FEED_NAME_PROPERTY_ID, "Feed1");
    properties.put(FeedResourceProvider.FEED_DESCRIPTION_PROPERTY_ID, "desc");
    properties.put(FeedResourceProvider.FEED_SCHEDULE_PROPERTY_ID, "sched");
    properties.put(FeedResourceProvider.FEED_STATUS_PROPERTY_ID, "WAITING");
    properties.put(FeedResourceProvider.FEED_SOURCE_CLUSTER_NAME_PROPERTY_ID, "source");
    properties.put(FeedResourceProvider.FEED_TARGET_CLUSTER_NAME_PROPERTY_ID, "target");

    List<String> feedNames = new LinkedList<String>();
    feedNames.add("Feed1");

    Map<String,String> props = new HashMap<String, String>();

    Feed feed1 = new Feed("Feed1", "desc", "WAITING", "sched", "source", "st", "end", "l", "a", "target", "st", "end", "l", "a", props);

    // set expectations
    expect(service.getFeedNames()).andReturn(feedNames);

    expect(service.getFeed("Feed1")).andReturn(feed1);

    service.updateFeed(feed1);

    // replay
    replay(service);

    propertySet.add(properties);

    Request request = PropertyHelper.getCreateRequest(propertySet, Collections.<String,String>emptyMap());

    FeedResourceProvider provider = new FeedResourceProvider(service,
        PropertyHelper.getPropertyIds(Resource.Type.DRFeed),
        PropertyHelper.getKeyPropertyIds(Resource.Type.DRFeed));

    provider.updateResources(request, null);

    // verify
    verify(service);
  }

  @Test
  public void testDeleteResources() throws Exception {
    IvoryService service = createMock(IvoryService.class);

    List<String> feedNames = new LinkedList<String>();
    feedNames.add("Feed1");

    Map<String,String> props = new HashMap<String, String>();

    Feed feed1 = new Feed("Feed1", "d", "s", "sch", "source", "st", "end", "l", "a", "target", "st", "end", "l", "a", props);

    // set expectations
    expect(service.getFeedNames()).andReturn(feedNames);

    expect(service.getFeed("Feed1")).andReturn(feed1);

    service.deleteFeed("Feed1");

    // replay
    replay(service);

    FeedResourceProvider provider = new FeedResourceProvider(service,
        PropertyHelper.getPropertyIds(Resource.Type.DRFeed),
        PropertyHelper.getKeyPropertyIds(Resource.Type.DRFeed));

    Predicate predicate = new PredicateBuilder().property(FeedResourceProvider.FEED_NAME_PROPERTY_ID).equals("Feed1").toPredicate();

    provider.deleteResources(predicate);

    // verify
    verify(service);
  }

  @Test
  public void testGetKeyPropertyIds() throws Exception {
    IvoryService service = createMock(IvoryService.class);

    Map<Resource.Type, String> keyPropertyIds = PropertyHelper.getKeyPropertyIds(Resource.Type.DRFeed);

    FeedResourceProvider provider = new FeedResourceProvider(service,
        PropertyHelper.getPropertyIds(Resource.Type.DRFeed),
        keyPropertyIds);

    Assert.assertEquals(keyPropertyIds, provider.getKeyPropertyIds());
  }
}
