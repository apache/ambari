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

package org.apache.ambari.view.hive20.client;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import com.google.common.collect.Lists;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive20.actor.message.lifecycle.KeepAlive;
import org.apache.ambari.view.hive20.utils.HiveActorConfiguration;
import org.apache.ambari.view.hive20.utils.ServiceFormattedException;
import org.apache.ambari.view.hive20.actor.message.job.FetchFailed;
import org.apache.ambari.view.hive20.actor.message.job.Next;
import org.apache.ambari.view.hive20.actor.message.job.NoMoreItems;
import org.apache.ambari.view.hive20.actor.message.job.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper over iterator actor and blocks to fetch Rows and ColumnDescription whenever there is no more Rows to be
 * returned.
 */
public class NonPersistentCursor implements Cursor<Row, ColumnDescription> {
  private final Logger LOG = LoggerFactory.getLogger(getClass());
  private static long DEFAULT_WAIT_TIMEOUT = 60 * 1000L;

  private final ActorSystem system;
  private final ActorRef actorRef;
  private final ViewContext context;
  private final HiveActorConfiguration actorConfiguration;
  private final Queue<Row> rows = Lists.newLinkedList();
  private final List<ColumnDescription> descriptions = Lists.newLinkedList();
  private int offSet = 0;
  private boolean endReached = false;
  private Inbox inbox;

  public NonPersistentCursor(ViewContext context, ActorSystem system, ActorRef actorRef) {
    this.context = context;
    this.system = system;
    this.actorRef = actorRef;
    actorConfiguration = new HiveActorConfiguration(context);
    inbox = Inbox.create(system);
  }

  @Override
  public boolean isResettable() {
    return false;
  }

  @Override
  public void reset() {
    // Do nothing
  }

  @Override
  public int getOffset() {
    return offSet;
  }

  @Override
  public List<ColumnDescription> getDescriptions() {
    fetchIfNeeded();
    return descriptions;
  }

  @Override
  public void keepAlive() {
    Inbox inbox = Inbox.create(system);
    inbox.send(actorRef, new KeepAlive());
  }

  @Override
  public Iterator<Row> iterator() {
    return this;
  }

  @Override
  public boolean hasNext() {
    fetchIfNeeded();
    return !endReached;
  }

  @Override
  public Row next() {
    fetchIfNeeded();
    offSet++;
    return rows.poll();
  }

  @Override
  public void remove() {
    throw new RuntimeException("Read only cursor. Method not supported");
  }

  private void fetchIfNeeded() {
    if (endReached || rows.size() > 0) return;
    getNextRows();
  }

  private void getNextRows() {
    inbox.send(actorRef, new Next());
    Object receive;
    try {
      receive = inbox.receive(Duration.create(actorConfiguration.getResultFetchTimeout(DEFAULT_WAIT_TIMEOUT),
        TimeUnit.MILLISECONDS));
    } catch (Throwable ex) {
      String errorMessage = "Result fetch timed out";
      LOG.error(errorMessage, ex);
      throw new ServiceFormattedException(errorMessage, ex);
    }

    if (receive instanceof Result) {
      Result result = (Result) receive;
      if (descriptions.isEmpty()) {
        descriptions.addAll(result.getColumns());
      }
      rows.addAll(result.getRows());
    }

    if (receive instanceof NoMoreItems) {
      if(descriptions.isEmpty()) {
        descriptions.addAll(((NoMoreItems)receive).getColumns());
      }
      endReached = true;
    }

    if (receive instanceof FetchFailed) {
      FetchFailed error = (FetchFailed) receive;
      LOG.error("Failed to fetch results ");
      throw new ServiceFormattedException(error.getMessage(), error.getError());
    }
  }
}
