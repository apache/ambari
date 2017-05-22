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
package org.apache.ambari.logfeeder.output;

import org.apache.ambari.logfeeder.common.LogFeederConstants;
import org.apache.ambari.logfeeder.input.Input;
import org.apache.ambari.logfeeder.input.cache.LRUCache;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputDescriptor;
import org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl.InputDescriptorImpl;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OutputLineFilterTest {

  private static final String CACHE_KEY_FIELD = "log_message";
  private static final String DEFAULT_DUMMY_MESSAGE = "myMessage";

  private OutputLineFilter underTest;
  private Input inputMock;

  @Before
  public void setUp() {
    underTest = new OutputLineFilter();
    inputMock = EasyMock.mock(Input.class);
  }

  @Test
  public void testApplyWithFilterOutByDedupInterval() {
    // GIVEN
    EasyMock.expect(inputMock.getCache()).andReturn(createLruCache(DEFAULT_DUMMY_MESSAGE, 100L, false));
    EasyMock.expect(inputMock.getInputDescriptor()).andReturn(generateInputDescriptor());
    EasyMock.expect(inputMock.getCacheKeyField()).andReturn(CACHE_KEY_FIELD);
    EasyMock.replay(inputMock);
    // WHEN
    boolean result = underTest.apply(generateLineMap(), inputMock);
    // THEN
    EasyMock.verify(inputMock);
    assertTrue(result);
  }

  @Test
  public void testApplyDoNotFilterOutDataByDedupInterval() {
    // GIVEN
    EasyMock.expect(inputMock.getCache()).andReturn(createLruCache(DEFAULT_DUMMY_MESSAGE, 10L, false));
    EasyMock.expect(inputMock.getInputDescriptor()).andReturn(generateInputDescriptor());
    EasyMock.expect(inputMock.getCacheKeyField()).andReturn(CACHE_KEY_FIELD);
    EasyMock.replay(inputMock);
    // WHEN
    boolean result = underTest.apply(generateLineMap(), inputMock);
    // THEN
    EasyMock.verify(inputMock);
    assertFalse(result);
  }

  @Test
  public void testApplyWithFilterOutByDedupLast() {
    // GIVEN
    EasyMock.expect(inputMock.getCache()).andReturn(createLruCache(DEFAULT_DUMMY_MESSAGE, 10L, true));
    EasyMock.expect(inputMock.getInputDescriptor()).andReturn(generateInputDescriptor());
    EasyMock.expect(inputMock.getCacheKeyField()).andReturn(CACHE_KEY_FIELD);
    EasyMock.replay(inputMock);
    // WHEN
    boolean result = underTest.apply(generateLineMap(), inputMock);
    // THEN
    EasyMock.verify(inputMock);
    assertTrue(result);
  }

  @Test
  public void testApplyDoNotFilterOutDataByDedupLast() {
    // GIVEN
    EasyMock.expect(inputMock.getCache()).andReturn(createLruCache("myMessage2", 10L, true));
    EasyMock.expect(inputMock.getInputDescriptor()).andReturn(generateInputDescriptor());
    EasyMock.expect(inputMock.getCacheKeyField()).andReturn(CACHE_KEY_FIELD);
    EasyMock.replay(inputMock);
    // WHEN
    boolean result = underTest.apply(generateLineMap(), inputMock);
    // THEN
    EasyMock.verify(inputMock);
    assertFalse(result);
  }

  @Test
  public void testApplyWithoutLruCache() {
    // GIVEN
    EasyMock.expect(inputMock.getCache()).andReturn(null);
    EasyMock.replay(inputMock);
    // WHEN
    boolean result = underTest.apply(generateLineMap(), inputMock);
    // THEN
    EasyMock.verify(inputMock);
    assertFalse(result);
  }

  @Test
  public void testApplyWithoutInMemoryTimestamp() {
    // GIVEN
    EasyMock.expect(inputMock.getCache()).andReturn(createLruCache(DEFAULT_DUMMY_MESSAGE, 100L, true));
    EasyMock.expect(inputMock.getInputDescriptor()).andReturn(generateInputDescriptor());
    EasyMock.expect(inputMock.getCacheKeyField()).andReturn(CACHE_KEY_FIELD);
    EasyMock.replay(inputMock);
    Map<String, Object> lineMap = generateLineMap();
    lineMap.remove(LogFeederConstants.IN_MEMORY_TIMESTAMP);
    // WHEN
    boolean result = underTest.apply(lineMap, inputMock);
    // THEN
    EasyMock.verify(inputMock);
    assertFalse(result);
  }

  @Test
  public void testApplyWithoutLogMessage() {
    // GIVEN
    EasyMock.expect(inputMock.getCache()).andReturn(createLruCache(DEFAULT_DUMMY_MESSAGE, 100L, true));
    EasyMock.expect(inputMock.getInputDescriptor()).andReturn(generateInputDescriptor());
    EasyMock.expect(inputMock.getCacheKeyField()).andReturn(CACHE_KEY_FIELD);
    EasyMock.replay(inputMock);
    Map<String, Object> lineMap = generateLineMap();
    lineMap.remove(CACHE_KEY_FIELD);
    // WHEN
    boolean result = underTest.apply(lineMap, inputMock);
    // THEN
    EasyMock.verify(inputMock);
    assertFalse(result);
  }

  private Map<String, Object> generateLineMap() {
    Map<String, Object> lineMap = new HashMap<>();
    lineMap.put(CACHE_KEY_FIELD, "myMessage");
    lineMap.put(LogFeederConstants.IN_MEMORY_TIMESTAMP, 150L);
    return lineMap;
  }

  private InputDescriptor generateInputDescriptor() {
    InputDescriptorImpl inputDescriptor = new InputDescriptorImpl() {};
    inputDescriptor.setRowtype("service");
    return inputDescriptor;
  }

  private LRUCache createLruCache(String defaultKey, long defaultValue, boolean lastDedupEanabled) {
    LRUCache lruCache = new LRUCache(4, "myfilepath", 100, lastDedupEanabled);
    lruCache.put(defaultKey, defaultValue);
    return lruCache;
  }

}
