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

package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query;

import junit.framework.Assert;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class DefaultConditionTest {

  @Test
  public void testMetricNameWhereCondition() {
    List<String> metricNames = new ArrayList<>();

    //Only IN clause.

    metricNames.add("M1");
    DefaultCondition condition = new DefaultCondition(metricNames,null,null,null,null,null,null,null,true);
    StringBuilder sb = new StringBuilder();
    condition.appendMetricNameClause(sb);
    Assert.assertEquals(sb.toString(), "(METRIC_NAME IN (?))");
    Assert.assertTrue(CollectionUtils.isEqualCollection(metricNames, condition.getMetricNames()));

    metricNames.add("m2");
    condition = new DefaultCondition(metricNames,null,null,null,null,null,null,null,true);
    sb = new StringBuilder();
    condition.appendMetricNameClause(sb);
    Assert.assertEquals(sb.toString(), "(METRIC_NAME IN (?, ?))");
    Assert.assertTrue(CollectionUtils.isEqualCollection(metricNames, condition.getMetricNames()));

    // Only NOT IN clause
    condition = new DefaultCondition(metricNames,null,null,null,null,null,null,null,true);
    condition.setMetricNamesNotCondition(true);
    sb = new StringBuilder();
    condition.appendMetricNameClause(sb);
    Assert.assertEquals(sb.toString(), "(METRIC_NAME NOT IN (?, ?))");
    Assert.assertTrue(CollectionUtils.isEqualCollection(metricNames, condition.getMetricNames()));

    metricNames.clear();

    //Only LIKE clause
    metricNames.add("disk%");
    condition = new DefaultCondition(metricNames,null,null,null,null,null,null,null,true);
    sb = new StringBuilder();
    condition.appendMetricNameClause(sb);
    Assert.assertEquals(sb.toString(), "(METRIC_NAME LIKE ?)");
    Assert.assertTrue(CollectionUtils.isEqualCollection(metricNames, condition.getMetricNames()));

    metricNames.add("cpu%");
    condition = new DefaultCondition(metricNames,null,null,null,null,null,null,null,true);
    sb = new StringBuilder();
    condition.appendMetricNameClause(sb);
    Assert.assertEquals(sb.toString(), "(METRIC_NAME LIKE ? OR METRIC_NAME LIKE ?)");
    Assert.assertTrue(CollectionUtils.isEqualCollection(metricNames, condition.getMetricNames()));

    //Only NOT LIKE clause
    condition = new DefaultCondition(metricNames,null,null,null,null,null,null,null,true);
    condition.setMetricNamesNotCondition(true);
    sb = new StringBuilder();
    condition.appendMetricNameClause(sb);
    Assert.assertEquals(sb.toString(), "(METRIC_NAME NOT LIKE ? AND METRIC_NAME NOT LIKE ?)");
    Assert.assertTrue(CollectionUtils.isEqualCollection(metricNames, condition.getMetricNames()));

    metricNames.clear();

    // IN followed by LIKE clause
    metricNames.add("M1");
    metricNames.add("disk%");
    metricNames.add("M2");
    condition = new DefaultCondition(metricNames,null,null,null,null,null,null,null,true);
    sb = new StringBuilder();
    condition.appendMetricNameClause(sb);
    Assert.assertEquals(sb.toString(), "(METRIC_NAME IN (?, ?) OR METRIC_NAME LIKE ?)");
    Assert.assertEquals(metricNames.get(2), "disk%");

    metricNames.clear();
    //NOT IN followed by NOT LIKE clause
    metricNames.add("disk%");
    metricNames.add("metric1");
    metricNames.add("cpu%");
    condition = new DefaultCondition(metricNames,null,null,null,null,null,null,null,true);
    sb = new StringBuilder();
    condition.setMetricNamesNotCondition(true);
    condition.appendMetricNameClause(sb);
    Assert.assertEquals(sb.toString(), "(METRIC_NAME NOT IN (?) AND METRIC_NAME NOT LIKE ? AND METRIC_NAME NOT LIKE ?)");
    Assert.assertEquals(metricNames.get(0), "metric1");

    //Empty
    metricNames.clear();
    condition = new DefaultCondition(metricNames,null,null,null,null,null,null,null,true);
    sb = new StringBuilder();
    condition.appendMetricNameClause(sb);
    Assert.assertEquals(sb.toString(), "");

  }
}

