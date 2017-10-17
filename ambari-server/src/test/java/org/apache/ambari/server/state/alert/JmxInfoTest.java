package org.apache.ambari.server.state.alert;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Optional;

import org.apache.ambari.server.controller.jmx.JMXMetricHolder;
import org.junit.Test;

public class JmxInfoTest {
  private static final String JMX_PROP_NAME1 = "Hadoop:service=NameNode,name=FSNamesystem/CapacityUsed";
  private static final String JMX_PROP_NAME2 = "Hadoop:service=NameNode,name=FSNamesystem/CapacityRemaining";

  @Test
  public void testFindJmxMetricsAndCalculateSimpleValue() throws Exception {
    MetricSource.JmxInfo jmxInfo = jmxInfoWith("{1}");
    JMXMetricHolder metrics = metrics(12.5, 3.5);
    assertThat(jmxInfo.eval(metrics), is(Optional.of(3.5)));
  }

  @Test
  public void testFindJmxMetricsAndCalculateComplexValue() throws Exception {
    MetricSource.JmxInfo jmxInfo = jmxInfoWith("2 * ({0} + {1})");
    JMXMetricHolder metrics = metrics(12.5, 2.5);
    assertThat(jmxInfo.eval(metrics), is(Optional.of(30.0)));
  }

  @Test
  public void testReturnsEmptyWhenJmxPropertyWasNotFound() throws Exception {
    MetricSource.JmxInfo jmxInfo = new MetricSource.JmxInfo();
    jmxInfo.setPropertyList(asList("notfound/notfound"));
    JMXMetricHolder metrics = metrics(1, 2);
    assertThat(jmxInfo.eval(metrics), is(Optional.empty()));
  }

  private MetricSource.JmxInfo jmxInfoWith(String value) {
    MetricSource.JmxInfo jmxInfo = new MetricSource.JmxInfo();
    jmxInfo.setValue(value);
    jmxInfo.setPropertyList(asList(JMX_PROP_NAME1, JMX_PROP_NAME2));
    return jmxInfo;
  }

  private JMXMetricHolder metrics(final double jmxValue1, final double jmxValue2) {
    JMXMetricHolder metrics = new JMXMetricHolder();
    metrics.setBeans(asList(
      new HashMap<String,Object>() {{
        put("name", name(JMX_PROP_NAME1));
        put(key(JMX_PROP_NAME1), jmxValue1);
      }},
      new HashMap<String,Object>() {{
        put("name", name(JMX_PROP_NAME2));
        put(key(JMX_PROP_NAME2), jmxValue2);
      }}
    ));
    return metrics;
  }

  private String name(String jmxProp) {
    return jmxProp.split("/")[0];
  }

  private String key(String jmxProp) {
    return jmxProp.split("/")[1];
  }
}