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

package org.apache.ambari.server.metric.system.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.server.metrics.system.MetricsSink;
import org.apache.ambari.server.metrics.system.impl.JvmMetricsSource;
import org.apache.ambari.server.metrics.system.impl.MetricsConfiguration;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Assume;
import org.junit.Test;

public class JvmMetricsSourceTest {

  @Test
  public void testJvmSourceInit_PreJVM1_8() {
    Assume.assumeThat(System.getProperty("java.version"), new LessThanVersionMatcher("1.8"));
    testJvmSourceInit(39);
  }

  @Test
  public void testJvmSourceInit_JVM1_8() {
    Assume.assumeThat(System.getProperty("java.version"), new VersionMatcher("1.8"));
    testJvmSourceInit(40);
  }

  private void testJvmSourceInit(int metricsSize) {
    JvmMetricsSource jvmMetricsSource = new JvmMetricsSource();
    MetricsConfiguration configuration = MetricsConfiguration.getMetricsConfiguration();
    MetricsSink sink = new TestAmbariMetricsSinkImpl();
    jvmMetricsSource.init(configuration, sink);
    org.junit.Assert.assertEquals(jvmMetricsSource.getMetrics().size(), metricsSize);
  }

  /* ****************************************************************
   * Matcher classes used in Assume checks
   * **************************************************************** */
  private class VersionMatcher extends BaseMatcher<String> {
    private final float version;

    VersionMatcher(String version) {
      this.version = Float.parseFloat(version);
    }

    @Override
    public boolean matches(Object o) {
      return parseVersion((String) o) == this.version;
    }

    float parseVersion(String versionString) {
      Pattern p = Pattern.compile("(\\d+(?:\\.\\d+)).*");
      Matcher matcher = p.matcher(versionString);
      if (matcher.matches()) {
        return Float.parseFloat(matcher.group(1));
      } else {
        return 0f;
      }
    }

    @Override
    public void describeTo(Description description) {

    }

    public float getVersion() {
      return version;
    }
  }

  private class LessThanVersionMatcher extends VersionMatcher {

    LessThanVersionMatcher(String version) {
      super(version);
    }

    @Override
    public boolean matches(Object o) {
      return parseVersion((String) o) < getVersion();
    }

    @Override
    public void describeTo(Description description) {

    }
  }
}