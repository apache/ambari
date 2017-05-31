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
        LOG.info(ema + ", " + ems);
        return diff > 0 ? new EmaResult(diff) : null;
    }

    public void update(double metricValue) {
        ema = weight * ema + (1 - weight) * metricValue;
        ems = Math.sqrt(weight * Math.pow(ems, 2.0) + (1 - weight) * Math.pow(metricValue - ema, 2.0));
        LOG.info(ema + ", " + ems);
    }

    public EmaResult test(double metricValue) {
        double diff  = Math.abs(ema - metricValue) - (timessdev * ems);
        return diff > 0 ? new EmaResult(diff) : null;
    }

}
