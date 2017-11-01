package org.apache.ambari.metrics.adservice.subsystem.trend

import org.apache.ambari.metrics.adservice.common.{Season, TimeRange}
import org.apache.ambari.metrics.adservice.metadata.MetricKey
import org.apache.ambari.metrics.adservice.model.AnomalyDetectionMethod.AnomalyDetectionMethod
import org.apache.ambari.metrics.adservice.model.AnomalyType.AnomalyType
import org.apache.ambari.metrics.adservice.model.{AnomalyType, SingleMetricAnomalyInstance}

case class TrendAnomalyInstance (metricKey: MetricKey,
                                 anomalousPeriod: TimeRange,
                                 referencePeriod: TimeRange,
                                 methodType: AnomalyDetectionMethod,
                                 anomalyScore: Double,
                                 seasonInfo: Season,
                                 modelParameters: String) extends SingleMetricAnomalyInstance {

  override val anomalyType: AnomalyType = AnomalyType.POINT_IN_TIME

  private def anomalyToString : String = {
    "Method=" + methodType + ", AnomalyScore=" + anomalyScore + ", Season=" + anomalousPeriod.toString +
      ", Model Parameters=" + modelParameters
  }

  @Override
  override def toString: String = {
    "Metric : [" + metricKey.toString + ", AnomalousPeriod=" + anomalousPeriod + ", ReferencePeriod=" + referencePeriod +
      "], Anomaly : [" + anomalyToString + "]"
  }
}
