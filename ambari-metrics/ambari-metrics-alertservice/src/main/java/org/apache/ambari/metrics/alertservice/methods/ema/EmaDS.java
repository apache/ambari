package org.apache.ambari.metrics.alertservice.methods.ema;

import com.sun.org.apache.commons.logging.Log;
import com.sun.org.apache.commons.logging.LogFactory;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
public class EmaDS implements Serializable {

    String metricName;
    String appId;
    String hostname;
    double ema;
    double ems;
    double weight;
    int timessdev;
    private static final Log LOG = LogFactory.getLog(EmaDS.class);

    public EmaDS(String metricName, String appId, String hostname, double weight, int timessdev) {
        this.metricName = metricName;
        this.appId = appId;
        this.hostname = hostname;
        this.weight = weight;
        this.timessdev = timessdev;
        this.ema = 0.0;
        this.ems = 0.0;
    }


    public EmaResult testAndUpdate(double metricValue) {

        double diff  = Math.abs(ema - metricValue) - (timessdev * ems);

        ema = weight * ema + (1 - weight) * metricValue;
        ems = Math.sqrt(weight * Math.pow(ems, 2.0) + (1 - weight) * Math.pow(metricValue - ema, 2.0));

        System.out.println(ema + ", " + ems);
        LOG.info(ema + ", " + ems);
        return diff > 0 ? new EmaResult(diff) : null;
    }

    public void update(double metricValue) {
        ema = weight * ema + (1 - weight) * metricValue;
        ems = Math.sqrt(weight * Math.pow(ems, 2.0) + (1 - weight) * Math.pow(metricValue - ema, 2.0));
        System.out.println(ema + ", " + ems);
        LOG.info(ema + ", " + ems);
    }

    public EmaResult test(double metricValue) {
        double diff  = Math.abs(ema - metricValue) - (timessdev * ems);
        return diff > 0 ? new EmaResult(diff) : null;
    }

}
