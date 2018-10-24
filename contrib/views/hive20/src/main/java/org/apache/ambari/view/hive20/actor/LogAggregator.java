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

package org.apache.ambari.view.hive20.actor;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import org.apache.ambari.view.hive20.actor.message.GetMoreLogs;
import org.apache.ambari.view.hive20.actor.message.HiveMessage;
import org.apache.ambari.view.hive20.actor.message.StartLogAggregation;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.ambari.view.utils.hdfs.HdfsUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.hive.jdbc.HiveStatement;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Reads the logs for a ExecuteJob from the Statement and writes them into hdfs.
 */
public class LogAggregator extends HiveActor {

  private final LoggingAdapter LOG = Logging.getLogger(getContext().system(), this);

  public static final int AGGREGATION_INTERVAL = 5 * 1000;
  private final HdfsApi hdfsApi;
  private HiveStatement statement;
  private final String logFile;

  private Cancellable moreLogsScheduler;
  private ActorRef parent;
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
      getMoreLogs();
    }
  }

  private void start(StartLogAggregation message) {

    if (null != this.statement) {
      LOG.debug("fetching logs for previous statement before switching to the new one. for {}", getSelf());
      getMoreLogs();
    }
    this.statement = message.getHiveStatement();
    parent = this.getSender();
    String logTitle = "Logs for Query '" + message.getStatement() + "'";
    String repeatSeperator = StringUtils.repeat("=", logTitle.length());
    allLogs += String.format("\n\n%s\n%s\n%s\n", repeatSeperator, logTitle, repeatSeperator);

    if( null == moreLogsScheduler) {
      setupScheduler();
    }
  }

  @VisibleForTesting
  protected void setupScheduler() {
    this.moreLogsScheduler = getContext().system().scheduler().schedule(
    Duration.Zero(), Duration.create(AGGREGATION_INTERVAL, TimeUnit.MILLISECONDS),
    getSelf(), new GetMoreLogs(), getContext().dispatcher(), null);
  }


  private void getMoreLogs() {
    LOG.debug("fetching more logs for : {}", getSelf());
    if ((null != this.statement)){
      List<String> logs;
      try{
        logs = this.statement.getQueryLog();
        LOG.debug("got more logs : {} for : {}", logs, getSelf());
        if (logs.size() > 0){
          this.allLogs = (this.allLogs + "\n" + Joiner.on("\n").skipNulls().join(logs));
          HdfsUtil.putStringToFile(this.hdfsApi, this.logFile, this.allLogs);
        }
      }
      catch (Exception e){
        LOG.error("Error occurred while fetching logs  for : {}", getSelf(), e);
      }
    }
  }

  @Override
  public void postStop() throws Exception {
    LOG.debug("Stopping logaggregator after fetching the logs one last time : {}", getSelf());

    getMoreLogs();

    if (moreLogsScheduler != null && !moreLogsScheduler.isCancelled()) {
      moreLogsScheduler.cancel();
    }
  }

}
