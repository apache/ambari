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
package org.apache.ambari.logfeeder.input.cache;

import org.apache.ambari.logfeeder.plugin.input.cache.LRUCache;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class LRUCacheTest {

  private LRUCache underTest;

  @Before
  public void setUp() {
    underTest = new LRUCache(4, "/mypath", Long.parseLong("1000"), true);
  }

  @Test
  public void testLruCachePut() {
    // GIVEN
    // WHEN
    underTest.put("mymessage1", 1000L);
    underTest.put("mymessage2", 1000L);
    underTest.put("mymessage3", 1000L);
    underTest.put("mymessage4", 1000L);
    underTest.put("mymessage5", 1000L);
    underTest.put("mymessage1", 1500L);
    underTest.put("mymessage1", 3500L);
    underTest.put("mymessage5", 1700L);
    // THEN
    assertEquals((Long) 1500L, underTest.get("mymessage1"));
    assertEquals((Long) 1000L, underTest.get("mymessage5"));
    assertEquals(underTest.getMRUKey(), "mymessage5");
    assertEquals(4, underTest.size());
    assertFalse(underTest.containsKey("mymessage2"));
  }

  @Test
  public void testLruCacheFilterMruKeys() {
    // GIVEN
    // WHEN
    underTest.put("mymessage1", 1000L);
    underTest.put("mymessage1", 3000L);
    underTest.put("mymessage1", 5000L);
    underTest.put("mymessage1", 7000L);
    // THEN
    assertEquals((Long) 1000L, underTest.get("mymessage1"));
  }

  @Test
  public void testLruCacheDoNotFilterMruKeysIfLastDedupDisabled() {
    // GIVEN
    underTest = new LRUCache(4, "/mypath", 1000, false);
    // WHEN
    underTest.put("mymessage1", 1000L);
    underTest.put("mymessage1", 3000L);
    // THEN
    assertEquals((Long) 3000L, underTest.get("mymessage1"));
  }

  @Test
  public void testLruCacheFilterByDedupInterval() {
    // GIVEN
    // WHEN
    underTest.put("mymessage1", 1000L);
    underTest.put("mymessage2", 1000L);
    underTest.put("mymessage1", 1250L);
    underTest.put("mymessage2", 1500L);
    underTest.put("mymessage1", 1500L);
    underTest.put("mymessage2", 2100L);
    // THEN
    assertEquals((Long) 1000L, underTest.get("mymessage1"));
    assertEquals((Long) 2100L, underTest.get("mymessage2"));
  }

  @Test
  public void testLruCacheWithDates() {
    // GIVEN
    DateTime firstDate = DateTime.now();
    DateTime secondDate = firstDate.plusMillis(500);
    // WHEN
    underTest.put("mymessage1", firstDate.toDate().getTime());
    underTest.put("mymessage2", firstDate.toDate().getTime());
    underTest.put("mymessage1", secondDate.toDate().getTime());
    // THEN
    assertEquals((Long) firstDate.toDate().getTime(), underTest.get("mymessage1"));
    assertEquals((Long) firstDate.toDate().getTime(), underTest.get("mymessage2"));
  }

  @Test
  public void testLruCacheWithDatesReachDedupInterval() {
    // GIVEN
    DateTime firstDate = DateTime.now();
    DateTime secondDate = firstDate.plusMillis(1500);
    // WHEN
    underTest.put("mymessage1", firstDate.toDate().getTime());
    underTest.put("mymessage2", firstDate.toDate().getTime());
    underTest.put("mymessage1", secondDate.toDate().getTime());
    // THEN
    assertEquals((Long) secondDate.toDate().getTime(), underTest.get("mymessage1"));
    assertEquals((Long) firstDate.toDate().getTime(), underTest.get("mymessage2"));
  }

}
