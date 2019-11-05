package org.apache.hadoop.metrics2.sink.kafka;

import java.util.Map;
import org.junit.Test;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.MetricName;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;

public class JvmMetricSetTest {

  @Test
  public void testGetJvmMetrics() {

    Map<MetricName, Gauge<?>> result = JvmMetricSet.getInstance().getJvmMetrics();

    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertThat(
      result.keySet()
        .stream()
        .map(MetricName::getName)
        .collect(toList()),
      hasItems("heap_usage", "thread-states.blocked", "thread-states.timed_waiting", "uptime"));
  }

}