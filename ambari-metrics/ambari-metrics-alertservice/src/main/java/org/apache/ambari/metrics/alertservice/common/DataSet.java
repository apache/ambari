package org.apache.ambari.metrics.alertservice.common;

import java.util.Arrays;

public class DataSet {

    public String metricName;
    public double[] ts;
    public double[] values;

    public DataSet(String metricName, double[] ts, double[] values) {
        this.metricName = metricName;
        this.ts = ts;
        this.values = values;
    }

    @Override
    public String toString() {
        return metricName + Arrays.toString(ts) + Arrays.toString(values);
    }
}
