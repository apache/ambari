package org.apache.ambari.metrics.alertservice.methods;

import org.apache.ambari.metrics.alertservice.common.MetricAnomaly;
import org.apache.ambari.metrics.alertservice.common.TimelineMetric;

import java.util.List;

public interface MetricAnomalyModel {

    public List<MetricAnomaly> onNewMetric(TimelineMetric metric);
    public List<MetricAnomaly> test(TimelineMetric metric);
}
