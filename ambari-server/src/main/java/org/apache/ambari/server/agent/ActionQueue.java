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
package org.apache.ambari.server.agent;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ActionQueue {

  private static Log LOG = LogFactory.getLog(ActionQueue.class);

  Map<String, Queue<AgentCommand>> hostQueues;

  public ActionQueue() {
    hostQueues = new TreeMap<String, Queue<AgentCommand>>();
  }

  private synchronized Queue<AgentCommand> getQueue(String hostname) {
    return hostQueues.get(hostname);
  }

  private synchronized void addQueue(String hostname, Queue<AgentCommand> q) {
    hostQueues.put(hostname, q);
  }

  public void enqueue(String hostname, AgentCommand cmd) {

    Queue<AgentCommand> q = getQueue(hostname);
    if (q == null) {
      q = new LinkedList<AgentCommand>();
      addQueue(hostname, q);
    }
    if (q.contains(cmd)) {
      LOG.warn("cmd already exists in the queue, not adding again");
      return;
    }
    synchronized (q) {
      q.add(cmd);
    }
  }

  public AgentCommand dequeue(String hostname) {
    Queue<AgentCommand> q = getQueue(hostname);
    if (q == null) {
      return null;
    }
    synchronized (q) {
      return q.remove();
    }
  }

  public List<AgentCommand> dequeueAll(String hostname) {
    Queue<AgentCommand> q = getQueue(hostname);
    List<AgentCommand> l = new ArrayList<AgentCommand>();
    synchronized (q) {
      while (true) {
        try {
          l.add(q.remove());
        } catch (NoSuchElementException ex) {
          return l;
        }
      }
    }
  }
}
