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

package org.apache.ambari.metrics.core.timeline.query;

import junit.framework.Assert;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class DefaultConditionTest {

  @Test
  @Ignore ("")
  public void testMetricNameWhereCondition() {
    //EMPTY
    List<byte[]> uuids = new ArrayList<>();
    DefaultCondition condition = new DefaultCondition(uuids,null,null,null,null,null,null,null,null,true);
    StringBuilder sb = new StringBuilder();
    condition.appendUuidClause(sb);
    Assert.assertEquals(sb.toString(), "");
    Assert.assertTrue(CollectionUtils.isEqualCollection(uuids, condition.getUuids()));

    //Metric uuid
    uuids.add(new byte[16]);
    condition = new DefaultCondition(uuids,null,null,null,null,null,null,null,null,true);
    sb = new StringBuilder();
    condition.appendUuidClause(sb);
    Assert.assertEquals(sb.toString(), "(UUID LIKE ?)");
    Assert.assertEquals(uuids.size(), condition.getUuids().size());
    Assert.assertTrue(new String(condition.getUuids().get(0)).endsWith("%"));

    //metric uuid + Host uuid
    uuids.add(new byte[4]);
    condition = new DefaultCondition(uuids,null,null,null,null,null,null,null,null,true);
    sb = new StringBuilder();
    condition.appendUuidClause(sb);
    Assert.assertEquals(sb.toString(), "(UUID LIKE ? AND UUID LIKE ?)");
    Assert.assertEquals(uuids.size(), condition.getUuids().size());
    Assert.assertTrue(new String(condition.getUuids().get(1)).startsWith("%"));

    //metric + host + full
    uuids.add(new byte[20]);
    uuids.add(new byte[20]);
    condition = new DefaultCondition(uuids,null,null,null,null,null,null,null,null,true);
    sb = new StringBuilder();
    condition.appendUuidClause(sb);
    Assert.assertEquals(sb.toString(), "(UUID IN (?, ?) AND UUID LIKE ? AND UUID LIKE ?)");
    Assert.assertEquals(uuids.size(), condition.getUuids().size());

    //Only IN clause.
    uuids.clear();
    uuids.add(new byte[20]);
    condition = new DefaultCondition(uuids,null,null,null,null,null,null,null,null,true);
    sb = new StringBuilder();
    condition.appendUuidClause(sb);
    Assert.assertEquals(sb.toString(), "(UUID IN (?))");
    Assert.assertEquals(uuids.size(), condition.getUuids().size());

    //metric NOT LIKE
    uuids.clear();
    uuids.add(new byte[16]);
    condition = new DefaultCondition(uuids,null,null,null,null,null,null,null,null,true);
    sb = new StringBuilder();
    condition.setMetricNamesNotCondition(true);
    condition.appendUuidClause(sb);
    Assert.assertEquals(sb.toString(), "(UUID NOT LIKE ?)");
    Assert.assertEquals(uuids.size(), condition.getUuids().size());

    //metric NOT LIKE host LIKE
    uuids.clear();
    uuids.add(new byte[16]);
    uuids.add(new byte[4]);
    condition = new DefaultCondition(uuids,null,null,null,null,null,null,null,null,true);
    sb = new StringBuilder();
    condition.setMetricNamesNotCondition(true);
    condition.appendUuidClause(sb);
    Assert.assertEquals(sb.toString(), "(UUID NOT LIKE ? AND UUID LIKE ?)");
    Assert.assertEquals(uuids.size(), condition.getUuids().size());
    Assert.assertTrue(new String(condition.getUuids().get(0)).endsWith("%"));
    Assert.assertTrue(new String(condition.getUuids().get(1)).startsWith("%"));

    //metric LIKE host NOT LIKE
    uuids.clear();
    uuids.add(new byte[16]);
    uuids.add(new byte[4]);
    condition = new DefaultCondition(uuids,null,null,null,null,null,null,null,null,true);
    sb = new StringBuilder();
    condition.setHostnamesNotCondition(true);
    condition.appendUuidClause(sb);
    Assert.assertEquals(sb.toString(), "(UUID LIKE ? AND UUID NOT LIKE ?)");
    Assert.assertEquals(uuids.size(), condition.getUuids().size());
    Assert.assertTrue(new String(condition.getUuids().get(0)).endsWith("%"));
    Assert.assertTrue(new String(condition.getUuids().get(1)).startsWith("%"));

    //metric LIKE or LIKE host LIKE
    uuids.clear();
    uuids.add(new byte[4]);
    uuids.add(new byte[16]);
    uuids.add(new byte[16]);
    condition = new DefaultCondition(uuids,null,null,null,null,null,null,null,null,true);
    sb = new StringBuilder();
    condition.appendUuidClause(sb);
    Assert.assertEquals(sb.toString(), "((UUID LIKE ? OR UUID LIKE ?) AND UUID LIKE ?)");
    Assert.assertEquals(uuids.size(), condition.getUuids().size());
    Assert.assertTrue(new String(condition.getUuids().get(0)).endsWith("%"));
    Assert.assertTrue(new String(condition.getUuids().get(1)).endsWith("%"));
    Assert.assertTrue(new String(condition.getUuids().get(2)).startsWith("%"));

    //UUID in metric LIKE or LIKE host LIKE
    uuids.clear();
    uuids.add(new byte[16]);
    uuids.add(new byte[16]);
    uuids.add(new byte[20]);
    uuids.add(new byte[4]);
    condition = new DefaultCondition(uuids,null,null,null,null,null,null,null,null,true);
    sb = new StringBuilder();
    condition.appendUuidClause(sb);
    Assert.assertEquals(sb.toString(), "(UUID IN (?) AND (UUID LIKE ? OR UUID LIKE ?) AND UUID LIKE ?)");
    Assert.assertEquals(uuids.size(), condition.getUuids().size());
    Assert.assertTrue(new String(condition.getUuids().get(1)).endsWith("%"));
    Assert.assertTrue(new String(condition.getUuids().get(2)).endsWith("%"));
    Assert.assertTrue(new String(condition.getUuids().get(3)).startsWith("%"));

    //metric LIKE host LIKE or LIKE
    uuids.clear();
    uuids.add(new byte[16]);
    uuids.add(new byte[4]);
    uuids.add(new byte[4]);
    condition = new DefaultCondition(uuids,null,null,null,null,null,null,null,null,true);
    sb = new StringBuilder();
    condition.appendUuidClause(sb);
    Assert.assertEquals(sb.toString(), "(UUID LIKE ? AND (UUID LIKE ? OR UUID LIKE ?))");
    Assert.assertEquals(uuids.size(), condition.getUuids().size());
    Assert.assertTrue(new String(condition.getUuids().get(0)).endsWith("%"));
    Assert.assertTrue(new String(condition.getUuids().get(1)).startsWith("%"));
    Assert.assertTrue(new String(condition.getUuids().get(2)).startsWith("%"));

    //UUID NOT IN metric LIKE host LIKE
    uuids.clear();
    uuids.add(new byte[20]);
    uuids.add(new byte[16]);
    uuids.add(new byte[4]);
    condition = new DefaultCondition(uuids,null,null,null,null,null,null,null,null,true);
    sb = new StringBuilder();
    condition.setUuidNotCondition(true);
    condition.appendUuidClause(sb);
    Assert.assertEquals(sb.toString(), "(UUID NOT IN (?) AND UUID LIKE ? AND UUID LIKE ?)");
    Assert.assertEquals(uuids.size(), condition.getUuids().size());
    Assert.assertTrue(new String(condition.getUuids().get(1)).endsWith("%"));
    Assert.assertTrue(new String(condition.getUuids().get(2)).startsWith("%"));
  }
}

