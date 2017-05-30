package org.apache.ambari.metrics.alertservice.common;

public class MetricAnomaly {

    private String metricKey;
    private long timestamp;
    private double metricValue;
    private MethodResult methodResult;

    public MetricAnomaly(String metricKey, long timestamp, double metricValue, MethodResult methodResult) {
        this.metricKey = metricKey;
        this.timestamp = timestamp;
        this.metricValue = metricValue;
        this.methodResult = methodResult;
    }

    public String getMetricKey() {
        return metricKey;
    }

    public void setMetricName(String metricName) {
        this.metricKey = metricName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(double metricValue) {
        this.metricValue = metricValue;
    }

    public MethodResult getMethodResult() {
        return methodResult;
    }

    public void setMethodResult(MethodResult methodResult) {
        this.methodResult = methodResult;
    }

    public String getAnomalyAsString() {
        return metricKey + ":" + timestamp + ":" + metricValue + ":" + methodResult.prettyPrint();
    }
}
