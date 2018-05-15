/*
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

package org.apache.ambari.view.hive2.actor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import com.google.common.base.Joiner;
import org.apache.ambari.view.hive2.actor.message.GetMoreLogs;
import org.apache.ambari.view.hive2.actor.message.HiveMessage;
import org.apache.ambari.view.hive2.actor.message.LogAggregationFinished;
import org.apache.ambari.view.hive2.actor.message.StartLogAggregation;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.ambari.view.utils.hdfs.HdfsApiException;
import org.apache.ambari.view.utils.hdfs.HdfsUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.hive.jdbc.HiveStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Reads the logs for a ExecuteJob from the Statement and writes them into hdfs.
 */
public class LogAggregator extends HiveActor {

  private final Logger LOG = LoggerFactory.getLogger(getClass());

  public static final int AGGREGATION_INTERVAL = 5 * 1000;
  private final HdfsApi hdfsApi;
  private HiveStatement statement;
  private final String logFile;

  private Cancellable moreLogsScheduler;
  private ActorRef parent;
  private boolean hasStartedFetching = false;
  private boolean shouldFetchMore = true;
  private String allLogs = "";

  public LogAggregator(HdfsApi hdfsApi, String logFile) {
    this.hdfsApi = hdfsApi;
    this.logFile = logFile;
  }

  @Override
  public void handleMessage(HiveMessage hiveMessage) {
    Object message = hiveMessage.getMessage();
    if (message instanceof StartLogAggregation) {
      start((StartLogAggregation) message);
    }

    if (message instanceof GetMoreLogs) {
      try {
        getMoreLogs();
      } catch (SQLException e) {
        LOG.error("SQL Error while getting logs. Tried writing to: {}. Exception: {}", logFile, e);
      } catch (HdfsApiException e) {
        LOG.warn("HDFS Error while getting writing logs to {}. Exception: {}", logFile, e);
      }
    }
  }

  private void start(StartLogAggregation message) {
    this.statement = message.getHiveStatement();
    parent = this.getSender();
    hasStartedFetching = false;
    shouldFetchMore = true;
    String logTitle = "Logs for Query '" + message.getStatement() + "'";
    String repeatSeperator = StringUtils.repeat("=", logTitle.length());
    allLogs += String.format("\n\n%s\n%s\n%s\n", repeatSeperator, logTitle, repeatSeperator);

    if (!(moreLogsScheduler == null || moreLogsScheduler.isCancelled())) {
      moreLogsScheduler.cancel();
    }
    this.moreLogsScheduler = getContext().system().scheduler().schedule(
      Duration.Zero(), Duration.create(AGGREGATION_INTERVAL, TimeUnit.MILLISECONDS),
      this.getSelf(), new GetMoreLogs(), getContext().dispatcher(), null);
  }

  private void getMoreLogs() throws SQLException, HdfsApiException {
    List<String> logs = statement.getQueryLog();
    if (logs.size() > 0 && shouldFetchMore) {
      allLogs = allLogs + "\n" + Joiner.on("\n").skipNulls().join(logs);
      HdfsUtil.putStringToFile(hdfsApi, logFile, allLogs);
      if(!statement.hasMoreLogs()) {
        shouldFetchMore = false;
      }
    } else {
      // Cancel the timer only when log fetching has been started
      if(!shouldFetchMore) {
        moreLogsScheduler.cancel();
        parent.tell(new LogAggregationFinished(), ActorRef.noSender());
      }
    }
  }

  @Override
  public void postStop() throws Exception {
    if (moreLogsScheduler != null && !moreLogsScheduler.isCancelled()) {
      moreLogsScheduler.cancel();
    }

  }

}
