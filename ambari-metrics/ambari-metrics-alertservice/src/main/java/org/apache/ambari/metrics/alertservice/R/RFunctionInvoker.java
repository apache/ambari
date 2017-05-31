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
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.RVector;
import org.rosuda.JRI.Rengine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RFunctionInvoker {

    public static Rengine r = new Rengine(new String[]{"--no-save"}, false, null);


    private static void loadDataSets(Rengine r, DataSet trainData, DataSet testData) {
        r.assign("train_ts", trainData.ts);
        r.assign("train_x", trainData.values);
        r.eval("train_data <- data.frame(train_ts,train_x)");
        r.eval("names(train_data) <- c(\"TS\", " + trainData.metricName + ")");

        r.assign("test_ts", testData.ts);
        r.assign("test_x", testData.values);
        r.eval("test_data <- data.frame(test_ts,test_x)");
        r.eval("names(test_data) <- c(\"TS\", " + testData.metricName + ")");
    }


    public static ResultSet tukeys(DataSet trainData, DataSet testData, Map<String, String> configs) {
        try {
            r.eval("source('tukeys.r', echo=TRUE)");

            int n = Integer.parseInt(configs.get("tukeys.n"));
            r.eval("n <- " + n);

            loadDataSets(r, trainData, testData);

            r.eval("an <- ams_tukeys(train_data, test_data, n)");
            REXP exp = r.eval("an");
            RVector cont = (RVector) exp.getContent();
            List<double[]> result = new ArrayList();
            for (int i = 0; i< cont.size(); i++) {
                result.add(cont.at(i).asDoubleArray());
            }
            return new ResultSet(result);
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            r.end();
        }
        return null;
    }

    public static ResultSet ema_global(DataSet trainData, DataSet testData, Map<String, String> configs) {
        try {
            r.eval("source('ema.R', echo=TRUE)");

            int n = Integer.parseInt(configs.get("ema.n"));
            r.eval("n <- " + n);

            double w = Double.parseDouble(configs.get("ema.w"));
            r.eval("w <- " + w);

            loadDataSets(r, trainData, testData);

            r.eval("an <- ema_global(train_data, test_data, w, n)");
            REXP exp = r.eval("an");
            RVector cont = (RVector) exp.getContent();
            List<double[]> result = new ArrayList();
            for (int i = 0; i< cont.size(); i++) {
                result.add(cont.at(i).asDoubleArray());
            }
            return new ResultSet(result);

        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            r.end();
        }
        return null;
    }

    public static ResultSet ema_daily(DataSet trainData, DataSet testData, Map<String, String> configs) {
        try {
            r.eval("source('ema.R', echo=TRUE)");

            int n = Integer.parseInt(configs.get("ema.n"));
            r.eval("n <- " + n);

            double w = Double.parseDouble(configs.get("ema.w"));
            r.eval("w <- " + w);

            loadDataSets(r, trainData, testData);

            r.eval("an <- ema_daily(train_data, test_data, w, n)");
            REXP exp = r.eval("an");
            RVector cont = (RVector) exp.getContent();
            List<double[]> result = new ArrayList();
            for (int i = 0; i< cont.size(); i++) {
                result.add(cont.at(i).asDoubleArray());
            }
            return new ResultSet(result);

        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            r.end();
        }
        return null;
    }

    public static ResultSet ksTest(DataSet trainData, DataSet testData, Map<String, String> configs) {
        try {
            r.eval("source('kstest.r', echo=TRUE)");

            double p_value = Double.parseDouble(configs.get("ks.p_value"));
            r.eval("p_value <- " + p_value);

            loadDataSets(r, trainData, testData);

            r.eval("an <- ams_ks(train_data, test_data, p_value)");
            REXP exp = r.eval("an");
            RVector cont = (RVector) exp.getContent();
            List<double[]> result = new ArrayList();
            for (int i = 0; i< cont.size(); i++) {
                result.add(cont.at(i).asDoubleArray());
            }
            return new ResultSet(result);

        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            r.end();
        }
        return null;
    }

    public static ResultSet hsdev(DataSet trainData, DataSet testData, Map<String, String> configs) {
        try {
            r.eval("source('hsdev.r', echo=TRUE)");

            int n = Integer.parseInt(configs.get("hsdev.n"));
            r.eval("n <- " + n);

            int nhp = Integer.parseInt(configs.get("hsdev.nhp"));
            r.eval("nhp <- " + nhp);

            long interval = Long.parseLong(configs.get("hsdev.interval"));
            r.eval("interval <- " + interval);

            long period = Long.parseLong(configs.get("hsdev.period"));
            r.eval("period <- " + period);

            loadDataSets(r, trainData, testData);

            r.eval("an2 <- hsdev_daily(train_data, test_data, n, nhp, interval, period)");
            REXP exp = r.eval("an2");
            RVector cont = (RVector) exp.getContent();

            List<double[]> result = new ArrayList();
            for (int i = 0; i< cont.size(); i++) {
                result.add(cont.at(i).asDoubleArray());
            }
            return new ResultSet(result);
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            r.end();
        }
        return null;
    }
}
