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
package org.apache.hadoop.metrics2.sink.timeline.cache;

import junit.framework.Assert;
import org.apache.hadoop.metrics2.sink.timeline.PostProcessingUtil;
import org.apache.hadoop.metrics2.sink.timeline.SingleValuedTimelineMetric;
import org.junit.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class PostProcessingUtilTest {

  @Test
  public void testInterpolateMissinData() throws Exception {

    Map<Long, Double> metricValues = new TreeMap<Long, Double>();
    long interval = 60*1000;

    long currentTime = System.currentTimeMillis();

    for(int i = 10; i>=1;i--) {
      if (i%4 != 0 && i != 5) { //Skip time points 4,5,8
        metricValues.put(currentTime - i*interval, (double)i);
      }
    }
    metricValues = PostProcessingUtil.interpolateMissingData(metricValues, interval);
    Assert.assertTrue(metricValues.size() == 10);

    Iterator it = metricValues.entrySet().iterator();
    double sum = 0;
    while (it.hasNext()) {
      Map.Entry entry = (Map.Entry)it.next();
      sum+= (double)entry.getValue();
    }
    Assert.assertEquals(sum, 55.0);
  }

  @Test
  public void testInterpolate() throws Exception {

    long t2 = System.currentTimeMillis();
    long t1 = t2 - 60000;
    double interpolatedValue;

    //Test Equal Values
    interpolatedValue = PostProcessingUtil.interpolate((t1 + 30000), t1, 10.0, t2, 10.0);
    Assert.assertEquals(interpolatedValue, 10.0);

    //Test Linear increase Values
    interpolatedValue = PostProcessingUtil.interpolate((t1 + 30000), t1, 10.0, t2, 20.0);
    Assert.assertEquals(interpolatedValue, 15.0);

    //Test Linear decrease Values
    interpolatedValue = PostProcessingUtil.interpolate((t1 + 30000), t1, 20.0, t2, 10.0);
    Assert.assertEquals(interpolatedValue, 15.0);

    //Test interpolation with non mid point time
    interpolatedValue = PostProcessingUtil.interpolate((t1 + 20000), t1, 15.0, t2, 30.0); // 1:2 ratio
    Assert.assertEquals(interpolatedValue, 20.0);

    //Test interpolation with past time
    interpolatedValue = PostProcessingUtil.interpolate((t1 - 60000), t1, 20.0, t2, 30.0);
    Assert.assertEquals(interpolatedValue, 10.0);

  }

  }
