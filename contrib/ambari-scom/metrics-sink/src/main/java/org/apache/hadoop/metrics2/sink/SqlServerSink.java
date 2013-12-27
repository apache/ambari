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

package org.apache.hadoop.metrics2.sink;

import java.lang.Exception;
import java.net.InetAddress;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.SubsetConfiguration;
import org.apache.hadoop.metrics2.Metric;
import org.apache.hadoop.metrics2.MetricsException;
import org.apache.hadoop.metrics2.MetricsRecord;
import org.apache.hadoop.metrics2.MetricsSink;
import org.apache.hadoop.metrics2.MetricsTag;
import org.apache.log4j.Logger;

/**
 * This class stores published metrics to the SQL Server database.
 */
public class SqlServerSink implements MetricsSink {
  private static final String DATABASE_URL_KEY = "databaseUrl";
  private static final boolean DEBUG = true;
  private static final String NAME_URL_KEY = "fs.default.name";
  private static final Pattern NAME_URL_REGEX = Pattern.compile(
      "hdfs://([^ :/]*)", Pattern.CASE_INSENSITIVE);
  private static final String DFS_BLOCK_SIZE_KEY = "dfs.block.size";
  private int blockSize = -1;
  private String currentServiceName = "";
  private String databaseUrl;
  private Connection conn = null;
  StringBuilder tagsListBuffer = new StringBuilder();
  String nodeName = null;
  String nodeIPAddress = null;
  org.apache.hadoop.conf.Configuration hadoopConfig = null;
  String clusterName = "localhost";

  static Logger logger = Logger.getLogger(SqlServerSink.class);

  @Override
  public void init(SubsetConfiguration conf) {
    String nameNodeUrl;
    String blockSizeString;

    logger.info("Entering init");

    currentServiceName = getFirstConfigPrefix(conf);

    databaseUrl = conf.getString(DATABASE_URL_KEY);

    if (databaseUrl == null)
      throw new MetricsException(
          "databaseUrl required in the metrics2 configuration for SqlServerSink.");

    try {
      Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
    } catch (ClassNotFoundException cnfe) {
      throw new MetricsException(
          "SqlServerSink requires the Microsoft JDBC driver for SQL Server.");
    }

    hadoopConfig = new org.apache.hadoop.conf.Configuration();
    if (hadoopConfig != null) {
      nameNodeUrl = hadoopConfig.get(NAME_URL_KEY);
      if (nameNodeUrl != null) {
        Matcher matcher = NAME_URL_REGEX.matcher(nameNodeUrl);
        if (matcher.find()) {
          clusterName = matcher.group(1);
        }
      }
      blockSizeString = hadoopConfig.get(DFS_BLOCK_SIZE_KEY);
      if (blockSizeString != null) {
        try {
          blockSize = Integer.parseInt(blockSizeString);
        } catch (NumberFormatException nfe) {
          // do nothing
        }
      }

    }
    logger.info("Exit init, cluster name = " + clusterName);
  }

  private String getFirstConfigPrefix(SubsetConfiguration conf) {
    while (conf.getParent() instanceof SubsetConfiguration) {
      conf = (SubsetConfiguration) conf.getParent();
    }
    return conf.getPrefix();
  }

  @Override
  public void putMetrics(MetricsRecord record) {
    long metricRecordID = getMetricRecordID(record.context(), record.name(),
        getLocalNodeName(), getLocalNodeIPAddress(), getClusterNodeName(), currentServiceName,
        getTagString(record.tags()), record.timestamp());
    if (metricRecordID < 0)
      return;

    for (Metric metric : record.metrics()) {
      insertMetricValue(metricRecordID, metric.name(), String.valueOf(metric
          .value()));
      if (metric.name().equals("BlockCapacity")) {
        insertMetricValue(metricRecordID, "BlockSize", Integer
            .toString(blockSize));
      }
    }
  }

  @Override
  public void flush() {
    try {
      if (conn != null)
        conn.close();
    } catch (Exception e) {
      // do nothing
    }
    conn = null;
  }

  public String getLocalNodeName() {
    if (nodeName == null) {
      try {
        nodeName = InetAddress.getLocalHost().getCanonicalHostName();
      } catch (Exception e) {
        if (DEBUG)
          logger.info("Error during getLocalHostName: " + e.toString());
      }
      if (nodeName == null)
        nodeName = "Unknown";
    }
    return nodeName;
  }

  public String getClusterNodeName() {
    if (clusterName.equalsIgnoreCase("localhost"))
      return getLocalNodeName();
    try {
      return InetAddress.getByName(clusterName).getCanonicalHostName();
    } catch (Exception e) {
      if (DEBUG)
        logger.info("Error during getClusterNodeName: " + e.toString());
    }

    return clusterName;
  }

  public String getLocalNodeIPAddress() {
    if (nodeIPAddress == null) {
      try {
        nodeIPAddress = InetAddress.getLocalHost().getHostAddress();
      } catch (Exception e) {
        if (DEBUG)
          logger.info("Error during getLocalNodeIPAddress: " + e.toString());
      }
    }
    if (nodeIPAddress == null)
      nodeIPAddress = "127.0.0.1";
    return nodeIPAddress;
  }

  /*
   *  TODO: Keep a cache of all tag strings, potentially caching the TagSetID.
   *  Caching the TagSetID will require some new stored procedures and new DAL
   *  methods.
   */
  public String getTagString(Iterable<MetricsTag> desiredTags) {
    /*
     * We don't return tags (even sourceName) if no tags available. Most likely,
     * when tags are empty - we don't need to distinguish services
     */
    if (desiredTags == null)
      return null;

    tagsListBuffer.setLength(0);
    tagsListBuffer.append("sourceName:").append(currentServiceName);
    String separator = ",";
    for (MetricsTag tag : desiredTags) {
      tagsListBuffer.append(separator);
      tagsListBuffer.append(tag.name());
      tagsListBuffer.append(":");
      tagsListBuffer.append(String.valueOf(tag.value()));
    }

    return tagsListBuffer.toString();
  }

  public boolean ensureConnection() {
    if (conn == null) {
      try {
        if (databaseUrl != null) {
          conn = DriverManager.getConnection(databaseUrl);
        }
      } catch (Exception e) {
        if (DEBUG)
          logger.info("Error during getConnection: " + e.toString());
      }
    }
    return conn != null;
  }

  public long getMetricRecordID(String recordTypeContext,
                                String recordTypeName, String nodeName, String sourceIP,
                                String clusterName, String serviceName, String tagPairs, long recordTimestamp) {
    CallableStatement cstmt = null;
    long result;
    if (recordTypeContext == null || recordTypeName == null || nodeName == null
        || sourceIP == null || tagPairs == null)
      return -1;

    int colid = 1;
    try {
      if (ensureConnection()) {
        cstmt = conn
            .prepareCall("{call dbo.uspGetMetricRecord(?, ?, ?, ?, ?, ?, ?, ?, ?)}");
        cstmt.setNString(colid++, recordTypeContext);
        cstmt.setNString(colid++, recordTypeName);
        cstmt.setNString(colid++, nodeName);
        cstmt.setNString(colid++, sourceIP);
        cstmt.setNString(colid++, clusterName);
        cstmt.setNString(colid++, serviceName);
        cstmt.setNString(colid++, tagPairs);
        cstmt.setLong(colid++, recordTimestamp);
        cstmt.registerOutParameter(colid, java.sql.Types.BIGINT);
        cstmt.execute();
        result = cstmt.getLong(colid);
        if (cstmt.wasNull())
          return -1;
        return result;
      }
    } catch (Exception e) {
      if (DEBUG)
        logger.info("Error during getMetricRecordID call sproc: "
            + e.toString());
      flush();
    } finally {
      if (cstmt != null) {
        try {
          cstmt.close();
        } catch (SQLException se) {
          if (DEBUG)
            logger.info("Error during getMetricRecordID close cstmt: "
                + se.toString());
        }
        /*
         * We don't close the connection here because we are likely to be
         * writing
         * metric values next and it is more efficient to share the connection.
         */
      }
    }
    return -1;
  }

  /* 
   * TODO: Think about sending all of this in one SP call if JDBC supports table
   * valued parameters.
   */
  public void insertMetricValue(long metricRecordID, String metricName,
                                String metricValue) {
    CallableStatement cstmt = null;
    if (metricRecordID < 0 || metricName == null || metricValue == null)
      return;
    try {
      if (ensureConnection()) {
        cstmt = conn.prepareCall("{call dbo.uspInsertMetricValue(?, ?, ?)}");
        cstmt.setLong(1, metricRecordID);
        cstmt.setNString(2, metricName);
        cstmt.setNString(3, metricValue);
        cstmt.execute();
      }
    } catch (Exception e) {
      if (DEBUG)
        logger.info("Error during insertMetricValue call sproc: "
            + e.toString());
      flush();
    } finally {
      if (cstmt != null) {
        try {
          cstmt.close();
        } catch (SQLException se) {
          if (DEBUG)
            logger.info("Error during insertMetricValue close cstmt: "
                + se.toString());
        }
        /*
         * We don't close the connection here because we are likely to be
         * writing
         * more metric values next and it is more efficient to share the
         * connection.
         */
      }
    }
  }
}