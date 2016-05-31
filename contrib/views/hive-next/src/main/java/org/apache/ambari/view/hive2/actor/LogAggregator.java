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
  private final HiveStatement statement;
  private final String logFile;
  private final ActorSystem system;

  private Cancellable moreLogsScheduler;
  private ActorRef parent;

  public LogAggregator(ActorSystem system, HdfsApi hdfsApi, HiveStatement statement, String logFile) {
    this.system = system;
    this.hdfsApi = hdfsApi;
    this.statement = statement;
    this.logFile = logFile;
  }

  @Override
  public void handleMessage(HiveMessage hiveMessage) {
    Object message = hiveMessage.getMessage();
    if (message instanceof StartLogAggregation) {
      start();
    }

    if (message instanceof GetMoreLogs) {
      try {
        getMoreLogs();
      } catch (SQLException e) {
        LOG.error("SQL Error while getting logs. Tried writing to: {}", logFile);
      } catch (HdfsApiException e) {
        LOG.warn("HDFS Error while getting writing logs to {}", logFile);

      }
    }
  }

  private void start() {
    parent = this.getSender();
    this.moreLogsScheduler = system.scheduler().schedule(
      Duration.Zero(), Duration.create(AGGREGATION_INTERVAL, TimeUnit.MILLISECONDS),
      this.getSelf(), new GetMoreLogs(), system.dispatcher(), null);
  }

  private void getMoreLogs() throws SQLException, HdfsApiException {
    if (statement.hasMoreLogs()) {
      List<String> logs = statement.getQueryLog();
      String allLogs = Joiner.on("\n").skipNulls().join(logs);
      HdfsUtil.putStringToFile(hdfsApi, logFile, allLogs);
    } else {
      moreLogsScheduler.cancel();
      parent.tell(new LogAggregationFinished(), ActorRef.noSender());
    }
  }

  @Override
  public void postStop() throws Exception {
    if(moreLogsScheduler != null && !moreLogsScheduler.isCancelled()){
      moreLogsScheduler.cancel();
    }

  }

}
