/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.metrics.adservice.db

object PhoenixQueryConstants {

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  /* Table Name constants */
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  val METRIC_PROFILE_TABLE_NAME = "METRIC_PROFILE"
  val METHOD_PARAMETERS_TABLE_NAME = "METHOD_PARAMETERS"
  val PIT_ANOMALY_METRICS_TABLE_NAME = "PIT_METRIC_ANOMALIES"
  val TREND_ANOMALY_METRICS_TABLE_NAME = "TREND_METRIC_ANOMALIES"
  val MODEL_SNAPSHOT = "MODEL_SNAPSHOT"

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  /* CREATE statement constants */
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  val CREATE_METHOD_PARAMETERS_TABLE: String = "CREATE TABLE IF NOT EXISTS %s (" +
    "METHOD_NAME VARCHAR, " +
    "METHOD_TYPE VARCHAR, " +
    "PARAMETERS VARCHAR " +
    "CONSTRAINT pk PRIMARY KEY (METHOD_NAME)) " +
    "DATA_BLOCK_ENCODING='FAST_DIFF', IMMUTABLE_ROWS=true, COMPRESSION='SNAPPY'"

  val CREATE_PIT_ANOMALY_METRICS_TABLE_SQL: String = "CREATE TABLE IF NOT EXISTS %s (" +
    "METRIC_UUID BINARY(20) NOT NULL, " +
    "METHOD_NAME VARCHAR, " +
    "ANOMALY_TIMESTAMP UNSIGNED_LONG NOT NULL, " +
    "METRIC_VALUE DOUBLE, " +
    "SEASONAL_INFO VARCHAR, " +
    "ANOMALY_SCORE DOUBLE, " +
    "MODEL_PARAMETERS VARCHAR, " +
    "DETECTION_TIME UNSIGNED_LONG " +
    "CONSTRAINT pk PRIMARY KEY (METRIC_UUID, METHOD_NAME, ANOMALY_TIMESTAMP)) " +
    "DATA_BLOCK_ENCODING='FAST_DIFF', IMMUTABLE_ROWS=true, TTL=%s, COMPRESSION='SNAPPY'"

  val CREATE_TREND_ANOMALY_METRICS_TABLE_SQL: String = "CREATE TABLE IF NOT EXISTS %s (" +
    "METRIC_UUID BINARY(20) NOT NULL, " +
    "METHOD_NAME VARCHAR, " +
    "ANOMALY_PERIOD_START UNSIGNED_LONG NOT NULL, " +
    "ANOMALY_PERIOD_END UNSIGNED_LONG NOT NULL, " +
    "TEST_PERIOD_START UNSIGNED_LONG NOT NULL, " +
    "TEST_PERIOD_END UNSIGNED_LONG NOT NULL, " +
    "SEASONAL_INFO VARCHAR, " +
    "ANOMALY_SCORE DOUBLE, " +
    "MODEL_PARAMETERS VARCHAR, " +
    "DETECTION_TIME UNSIGNED_LONG " +
    "CONSTRAINT pk PRIMARY KEY (METRIC_UUID, METHOD_NAME, ANOMALY_PERIOD_START, ANOMALY_PERIOD_END, TEST_PERIOD_START, TEST_PERIOD_END)) " +
    "DATA_BLOCK_ENCODING='FAST_DIFF', IMMUTABLE_ROWS=true, TTL=%s, COMPRESSION='SNAPPY'"

  val CREATE_MODEL_SNAPSHOT_TABLE: String = "CREATE TABLE IF NOT EXISTS %s (" +
    "METRIC_UUID BINARY(20) NOT NULL, " +
    "METHOD_NAME VARCHAR, " +
    "METHOD_TYPE VARCHAR, " +
    "PARAMETERS VARCHAR, " +
    "SNAPSHOT_TIME UNSIGNED_LONG NOT NULL " +
    "CONSTRAINT pk PRIMARY KEY (METRIC_UUID, METHOD_NAME, SNAPSHOT_TIME)) " +
    "DATA_BLOCK_ENCODING='FAST_DIFF', IMMUTABLE_ROWS=true, COMPRESSION='SNAPPY'"

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  /* UPSERT statement constants */
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  val UPSERT_METHOD_PARAMETERS_SQL: String = "UPSERT INTO %s (METHOD_NAME, METHOD_TYPE, PARAMETERS) VALUES (?,?,?)"

  val UPSERT_PIT_ANOMALY_METRICS_SQL: String = "UPSERT INTO %s (METRIC_UUID, ANOMALY_TIMESTAMP, METRIC_VALUE, METHOD_NAME, " +
    "SEASONAL_INFO, ANOMALY_SCORE, MODEL_PARAMETERS, DETECTION_TIME) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"

  val UPSERT_TREND_ANOMALY_METRICS_SQL: String = "UPSERT INTO %s (METRIC_UUID, ANOMALY_PERIOD_START, ANOMALY_PERIOD_END, " +
    "TEST_PERIOD_START, TEST_PERIOD_END, METHOD_NAME, ANOMALY_SCORE, MODEL_PARAMETERS, DETECTION_TIME) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"

  val UPSERT_MODEL_SNAPSHOT_SQL: String = "UPSERT INTO %s (METRIC_UUID, METHOD_NAME, METHOD_TYPE, PARAMETERS) VALUES (?, ?, ?, ?)"

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  /* GET statement constants */
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  val GET_METHOD_PARAMETERS_SQL: String = "SELECT METHOD_NAME, METHOD_TYPE, PARAMETERS FROM %s WHERE METHOD_NAME = %s"

  val GET_PIT_ANOMALY_METRIC_SQL: String = "SELECT METRIC_UUID, ANOMALY_TIMESTAMP, METRIC_VALUE, METHOD_NAME, SEASONAL_INFO, " +
    "ANOMALY_SCORE, MODEL_PARAMETERS, DETECTION_TIME FROM %s WHERE ANOMALY_TIMESTAMP > ? AND ANOMALY_TIMESTAMP <= ? " +
    "ORDER BY ANOMALY_SCORE DESC"

  val GET_TREND_ANOMALY_METRIC_SQL: String = "SELECT METRIC_UUID, ANOMALY_PERIOD_START, ANOMALY_PERIOD_END, TEST_PERIOD_START, " +
    "TEST_PERIOD_END, METHOD_NAME, SEASONAL_INFO, ANOMALY_SCORE, MODEL_PARAMETERS, DETECTION_TIME FROM %s WHERE ANOMALY_PERIOD_END > ? " +
    "AND ANOMALY_PERIOD_END <= ? ORDER BY ANOMALY_SCORE DESC"

  val GET_MODEL_SNAPSHOT_SQL: String = "SELECT METRIC_UUID, METHOD_NAME, METHOD_TYPE, PARAMETERS FROM %s WHERE UUID = %s AND METHOD_NAME = %s"

}
