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
package org.apache.ambari.metrics.alertservice.R;

import org.apache.ambari.metrics.alertservice.common.ResultSet;
import org.apache.ambari.metrics.alertservice.common.DataSet;
import org.apache.commons.lang.ArrayUtils;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.RVector;
import org.rosuda.JRI.Rengine;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AmsRTest {

    public static void main(String[] args) {

        String metricName = "TestMetric";
        double[] ts = getTS(1000);

        double[] train_ts = ArrayUtils.subarray(ts, 0,750);
        double[] train_x = getData(750);
        DataSet trainData = new DataSet(metricName, train_ts, train_x);

        double[] test_ts = ArrayUtils.subarray(ts, 750,1000);
        double[] test_x = getData(250);
        test_x[50] = 5.5; //Anomaly
        DataSet testData = new DataSet(metricName, test_ts, test_x);
        ResultSet rs;

        Map<String, String> configs = new HashMap();

        System.out.println("TUKEYS");
        configs.put("tukeys.n", "3");
        rs = RFunctionInvoker.tukeys(trainData, testData, configs);
        rs.print();
        System.out.println("--------------");

        System.out.println("EMA Global");
        configs.put("ema.n", "3");
        configs.put("ema.w", "0.8");
        rs = RFunctionInvoker.ema_global(trainData, testData, configs);
        rs.print();
        System.out.println("--------------");

        System.out.println("EMA Daily");
        rs = RFunctionInvoker.ema_daily(trainData, testData, configs);
        rs.print();
        System.out.println("--------------");

        configs.put("ks.p_value", "0.05");
        System.out.println("KS Test");
        rs = RFunctionInvoker.ksTest(trainData, testData, configs);
        rs.print();
        System.out.println("--------------");

        ts = getTS(5000);
        train_ts = ArrayUtils.subarray(ts, 30,4800);
        train_x = getData(4800);
        trainData = new DataSet(metricName, train_ts, train_x);
        test_ts = ArrayUtils.subarray(ts, 4800,5000);
        test_x = getData(200);
        for (int i =0; i<200;i++) {
            test_x[i] = test_x[i]*5;
        }
        testData = new DataSet(metricName, test_ts, test_x);
        configs.put("hsdev.n", "3");
        configs.put("hsdev.nhp", "3");
        configs.put("hsdev.interval", "86400000");
        configs.put("hsdev.period", "604800000");
        System.out.println("HSdev");
        rs = RFunctionInvoker.hsdev(trainData, testData, configs);
        rs.print();
        System.out.println("--------------");

    }

    static double[] getTS(int n) {
        long currentTime = System.currentTimeMillis();
        double[] ts = new double[n];
        currentTime = currentTime - (currentTime % (5*60*1000));

        for (int i = 0,j=n-1; i<n; i++,j--) {
            ts[j] = currentTime;
            currentTime = currentTime - (5*60*1000);
        }
        return ts;
    }

    static void testBasic() {
        Rengine r = new Rengine(new String[]{"--no-save"}, false, null);
        try {
            r.eval("library(ambarimetricsAD)");
            r.eval("source('~/dev/AMS/AD/ambarimetricsAD/org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.alerting.R/test.org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.alerting.R', echo=TRUE)");
            r.eval("source('~/dev/AMS/AD/ambarimetricsAD/org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.alerting.R/util.org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.alerting.R', echo=TRUE)");
            r.eval("source('~/dev/AMS/AD/ambarimetricsAD/org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.alerting.R/tukeys.org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.alerting.R', echo=TRUE)");
            double[] ts = getTS(5000);
            double[] x = getData(5000);
            r.assign("ts", ts);
            r.assign("x", x);
            r.eval("x[1000] <- 4.5");
            r.eval("x[2000] <- 4.75");
            r.eval("x[3000] <- 3.5");
            r.eval("x[4000] <- 5.5");
            r.eval("x[5000] <- 5.0");
            r.eval("data <- data.frame(ts,x)");
            r.eval("names(data) <- c(\"TS\", \"Metric\")");
            System.out.println(r.eval("data"));
            REXP exp = r.eval("t_an <- test_methods(data)");
            exp = r.eval("t_an");
            String strExp = exp.asString();
            System.out.println("result:" + exp);
            RVector cont = (RVector) exp.getContent();
            double[] an_ts = cont.at(0).asDoubleArray();
            double[] an_x = cont.at(1).asDoubleArray();
            System.out.println("result:" + strExp);
        }
        finally {
            r.end();
        }
    }
    static double[] getData(int n) {
        double[] metrics = new double[n];
        Random random = new Random();
        for (int i = 0; i<n; i++) {
            metrics[i] = random.nextDouble();
        }
        return metrics;
    }
}
