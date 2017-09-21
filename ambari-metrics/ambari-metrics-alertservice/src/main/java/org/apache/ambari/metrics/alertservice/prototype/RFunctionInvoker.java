/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.metrics.alertservice.prototype;


import org.apache.ambari.metrics.alertservice.prototype.common.ResultSet;
import org.apache.ambari.metrics.alertservice.prototype.common.DataSeries;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.RVector;
import org.rosuda.JRI.Rengine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RFunctionInvoker {

  static final Log LOG = LogFactory.getLog(RFunctionInvoker.class);
  public static Rengine r = new Rengine(new String[]{"--no-save"}, false, null);
  private static String rScriptDir = "/usr/lib/ambari-metrics-collector/R-scripts";

  private static void loadDataSets(Rengine r, DataSeries trainData, DataSeries testData) {
    r.assign("train_ts", trainData.ts);
    r.assign("train_x", trainData.values);
    r.eval("train_data <- data.frame(train_ts,train_x)");
    r.eval("names(train_data) <- c(\"TS\", " + trainData.seriesName + ")");

    r.assign("test_ts", testData.ts);
    r.assign("test_x", testData.values);
    r.eval("test_data <- data.frame(test_ts,test_x)");
    r.eval("names(test_data) <- c(\"TS\", " + testData.seriesName + ")");
  }

  public static void setScriptsDir(String dir) {
    rScriptDir = dir;
  }

  public static ResultSet executeMethod(String methodType, DataSeries trainData, DataSeries testData, Map<String, String> configs) {

    ResultSet result;
    switch (methodType) {
      case "tukeys":
        result = tukeys(trainData, testData, configs);
        break;
      case "ema":
        result = ema_global(trainData, testData, configs);
        break;
      case "ks":
        result = ksTest(trainData, testData, configs);
        break;
      case "hsdev":
        result = hsdev(trainData, testData, configs);
        break;
      default:
        result = tukeys(trainData, testData, configs);
        break;
    }
    return result;
  }

  public static ResultSet tukeys(DataSeries trainData, DataSeries testData, Map<String, String> configs) {
    try {

      REXP exp1 = r.eval("source('" + rScriptDir + "/tukeys.r" + "')");

      double n = Double.parseDouble(configs.get("tukeys.n"));
      r.eval("n <- " + n);

      loadDataSets(r, trainData, testData);

      r.eval("an <- ams_tukeys(train_data, test_data, n)");
      REXP exp = r.eval("an");
      RVector cont = (RVector) exp.getContent();
      List<double[]> result = new ArrayList();
      for (int i = 0; i < cont.size(); i++) {
        result.add(cont.at(i).asDoubleArray());
      }
      return new ResultSet(result);
    } catch (Exception e) {
      LOG.error(e);
    } finally {
      r.end();
    }
    return null;
  }

  public static ResultSet ema_global(DataSeries trainData, DataSeries testData, Map<String, String> configs) {
    try {
      r.eval("source('" + rScriptDir + "/ema.r" + "')");

      int n = Integer.parseInt(configs.get("ema.n"));
      r.eval("n <- " + n);

      double w = Double.parseDouble(configs.get("ema.w"));
      r.eval("w <- " + w);

      loadDataSets(r, trainData, testData);

      r.eval("an <- ema_global(train_data, test_data, w, n)");
      REXP exp = r.eval("an");
      RVector cont = (RVector) exp.getContent();
      List<double[]> result = new ArrayList();
      for (int i = 0; i < cont.size(); i++) {
        result.add(cont.at(i).asDoubleArray());
      }
      return new ResultSet(result);

    } catch (Exception e) {
      LOG.error(e);
    } finally {
      r.end();
    }
    return null;
  }

  public static ResultSet ema_daily(DataSeries trainData, DataSeries testData, Map<String, String> configs) {
    try {
      r.eval("source('" + rScriptDir + "/ema.r" + "')");

      int n = Integer.parseInt(configs.get("ema.n"));
      r.eval("n <- " + n);

      double w = Double.parseDouble(configs.get("ema.w"));
      r.eval("w <- " + w);

      loadDataSets(r, trainData, testData);

      r.eval("an <- ema_daily(train_data, test_data, w, n)");
      REXP exp = r.eval("an");
      RVector cont = (RVector) exp.getContent();
      List<double[]> result = new ArrayList();
      for (int i = 0; i < cont.size(); i++) {
        result.add(cont.at(i).asDoubleArray());
      }
      return new ResultSet(result);

    } catch (Exception e) {
      LOG.error(e);
    } finally {
      r.end();
    }
    return null;
  }

  public static ResultSet ksTest(DataSeries trainData, DataSeries testData, Map<String, String> configs) {
    try {
      r.eval("source('" + rScriptDir + "/kstest.r" + "')");

      double p_value = Double.parseDouble(configs.get("ks.p_value"));
      r.eval("p_value <- " + p_value);

      loadDataSets(r, trainData, testData);

      r.eval("an <- ams_ks(train_data, test_data, p_value)");
      REXP exp = r.eval("an");
      RVector cont = (RVector) exp.getContent();
      List<double[]> result = new ArrayList();
      for (int i = 0; i < cont.size(); i++) {
        result.add(cont.at(i).asDoubleArray());
      }
      return new ResultSet(result);

    } catch (Exception e) {
      LOG.error(e);
    } finally {
      r.end();
    }
    return null;
  }

  public static ResultSet hsdev(DataSeries trainData, DataSeries testData, Map<String, String> configs) {
    try {
      r.eval("source('" + rScriptDir + "/hsdev.r" + "')");

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
      for (int i = 0; i < cont.size(); i++) {
        result.add(cont.at(i).asDoubleArray());
      }
      return new ResultSet(result);
    } catch (Exception e) {
      LOG.error(e);
    } finally {
      r.end();
    }
    return null;
  }
}
