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
 * WITHOUT WARRANTIES   OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.uuid;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineClusterMetric;

public class HashBasedUuidGenStrategy implements MetricUuidGenStrategy {

  /**
   * Computes the UUID for a timelineClusterMetric.
   * @param timelineClusterMetric
   * @param maxLength
   * @return byte array of length 'maxlength'
   */
  @Override
  public byte[] computeUuid(TimelineClusterMetric timelineClusterMetric, int maxLength) {

    int metricNameUuidLength = 12;
    String instanceId = timelineClusterMetric.getInstanceId();

    if ((StringUtils.isEmpty(instanceId))) {
      metricNameUuidLength = 14;
    }

    String metricName = timelineClusterMetric.getMetricName();

    //Compute the individual splits.
    String[] splits = getIndidivualSplits(metricName);

    /*
    Compute the weighted ascii sum of every split in the metric name. (asciiSum += (int) splits[s].charAt(i))
    These weighted sums are 'appended' to get the unique ID for metric name.
     */
    StringBuilder splitSums = new StringBuilder();
    long totalAsciiSum = 0l;
    if (splits.length > 0) {
      for (String split : splits) {
        int asciiSum = 0;
        for (int i = 0; i < split.length(); i++) {
          asciiSum += ((i + 1) * (int) split.charAt(i)); //weighted sum for split.
        }
        splitSums.append(asciiSum); //Append the sum to the array of sums.
        totalAsciiSum += asciiSum; //Parity Sum
      }
    }

    String splitSumString = totalAsciiSum + splitSums.reverse().toString(); //Reverse and attach parity sum.
    int splitLength = splitSumString.length();

    //Compute a unique metric seed for the stemmed metric name
    String stemmedMetric = stem(metricName);
    long metricSeed = 100123456789L;
    metricSeed += computeWeightedNumericalAsciiSum(stemmedMetric);
    //Reverse the computed seed to get a metric UUID portion which is used optionally.
    byte[] metricSeedBytes = StringUtils.reverse(String.valueOf(metricSeed)).getBytes();

    int seedLength = (int)(0.25 * metricNameUuidLength);
    int sumLength = metricNameUuidLength - seedLength;
    if (splitLength < sumLength) {
      sumLength = splitLength;
      seedLength = metricNameUuidLength - sumLength;
    }

    byte[] metricUuidPortion = ArrayUtils.addAll(
      ArrayUtils.subarray(splitSumString.getBytes(), 0, sumLength)
      , ArrayUtils.subarray(metricSeedBytes, 0, seedLength));

    /*
      For appId and instanceId the logic is similar. Use a seed integer to start with and compute ascii sum.
      Based on required length, use a suffix of the computed uuid.
     */
    String appId = timelineClusterMetric.getAppId();
    int appidSeed = 11;
    for (int i = 0; i < appId.length(); i++) {
      appidSeed += appId.charAt(i);
    }
    String appIdSeedStr = String.valueOf(appidSeed);
    byte[] appUuidPortion = ArrayUtils.subarray(appIdSeedStr.getBytes(), appIdSeedStr.length() - 2, appIdSeedStr.length());

    if (StringUtils.isNotEmpty(instanceId)) {
      byte[] instanceUuidPortion = new byte[2];
      ByteBuffer buffer = ByteBuffer.allocate(4);
      int instanceIdSeed = 1489;
      for (int i = 0; i < appId.length(); i++) {
        instanceIdSeed += ((i+1) * appId.charAt(i));
      }
      buffer.putInt(instanceIdSeed);
      ArrayUtils.subarray(buffer.array(), 2, 4);
      // Concatenate all UUIDs together (metric uuid + appId uuid + instanceId uuid)
      return ArrayUtils.addAll(ArrayUtils.addAll(metricUuidPortion, appUuidPortion), instanceUuidPortion);
    }

    return ArrayUtils.addAll(metricUuidPortion, appUuidPortion);
  }

  /**
   * Splits the metric name into individual tokens.
   * For example,
   *  kafka.server.ReplicaManager.LeaderCount -> [kafka, server, ReplicaManager, LeaderCount]
   *  default.General.api_drop_table_15min_rate -> [default, General, api, drop, table, 15min, rate]
   * @param metricName
   * @return
   */
  private String[] getIndidivualSplits(String metricName) {
    List<String> tokens = new ArrayList<>();
    String[] splits = new String[0];
    if (metricName.contains(".")) {
      splits = metricName.split("\\.");
      for (String split : splits) {
        if (split.contains("_")) {
          String[] subSplits = split.split("\\_");
          tokens.addAll(Arrays.asList(subSplits));
        } else {
          tokens.add(split);
        }
      }
    }

    if (splits.length <= 1) {
      splits = metricName.split("\\_");
      return splits;
    }

    if (splits.length <= 1) {
      splits = metricName.split("\\=");
      return splits;
    }

    return tokens.toArray(new String[tokens.size()]);
  }

  /**
   * Stem the metric name. Remove a set of usual suspects characters.
   * @param metricName
   * @return
   */
  private String stem(String metricName) {
    String metric = metricName.toLowerCase();
    String regex = "[\\.\\_\\%\\-\\=\\/\\@\\(\\)\\[\\]\\:]";
    String trimmedMetric = StringUtils.removePattern(metric, regex);
    return trimmedMetric;
  }


  /**
   * Computes the UUID of a string. (hostname)
   * Uses the ascii sum of the String. Numbers in the String are treated as actual numerical values rather than ascii values.
   * @param value
   * @param maxLength
   * @return byte array of length 'maxlength'
   */
  @Override
  public byte[] computeUuid(String value, int maxLength) {

    if (StringUtils.isEmpty(value)) {
      return null;
    }

    int customAsciiSum = 1489 + (int) computeWeightedNumericalAsciiSum(value); //seed = 1489

    String customAsciiSumStr = String.valueOf(customAsciiSum);
    if (customAsciiSumStr.length() < maxLength) {
      return null;
    } else {
      return customAsciiSumStr.substring(customAsciiSumStr.length() - maxLength, customAsciiSumStr.length()).getBytes();
    }
  }

  public long computeWeightedNumericalAsciiSum(String value) {
    int len = value.length();
    long numericValue = 0;
    long sum = 0;
    int numericCtr = 0;
    for (int i = 0; i < len;) {
      int ascii = value.charAt(i);
      if (48 <= ascii && ascii <= 57 && numericCtr < 4) {
        numericValue = numericValue * 10 + (ascii - 48);
        numericCtr++;
        i++;
      } else {
        if (numericValue > 0) {
          sum += numericValue;
          numericValue = 0;
        }
        if (numericCtr < 4) {
          sum += value.charAt(i);
          i++;
        }
        numericCtr = 0;
      }
    }

    if (numericValue != 0) {
      sum +=numericValue;
    }
    return sum;
  }
}