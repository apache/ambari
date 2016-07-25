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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.agent.AgentCommand.AgentCommandType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

@Singleton
public class ActionQueue {

  private static Logger LOG = LoggerFactory.getLogger(ActionQueue.class);

  private static HashSet<String> EMPTY_HOST_LIST = new HashSet<String>();

  final ConcurrentMap<String, Queue<AgentCommand>> hostQueues;

  HashSet<String> hostsWithPendingTask = new HashSet<String>();

  public ActionQueue() {
    hostQueues = new ConcurrentHashMap<String, Queue<AgentCommand>>();
  }

  private Queue<AgentCommand> getQueue(String hostname) {
    return hostQueues.get(hostname);
  }

  /**
   * Adds command to queue for given hostname
   * @param hostname - hostname of node
   * @param cmd - command to add to queue
   * @throws NullPointerException - if hostname is {@code}null{@code}
   */
  public void enqueue(String hostname, AgentCommand cmd) {
    Queue<AgentCommand> q = getQueue(hostname);

    if (q == null) {
      //try to add new queue to map if not found
      q = hostQueues.putIfAbsent(hostname, new ConcurrentLinkedQueue<AgentCommand>());
      if (q == null) {
        //null means that new queue was added to map, get it
        q = getQueue(hostname);
      }
      //otherwise we got existing queue (and put nothing!)
    }

    q.add(cmd);
  }

  /**
   * Get command from queue for given hostname
   * @param hostname
   * @return
   */
  public AgentCommand dequeue(String hostname) {
    Queue<AgentCommand> q = getQueue(hostname);
    if (q == null) {
      return null;
    }

    return q.poll();
  }

  /**
   * Dequeue's all commands of a specified type for the given host.
   *
   * @param hostname
   *          the host to remove commands for (not {@code null}).
   * @param commandType
   *          the type of command to remove (not {@code null}).
   * @return the commands removed, or an empty list if none (never {@code null}
   *         ).
   */
  public List<AgentCommand> dequeue(String hostname,
      AgentCommandType commandType) {
    if (null == hostname || null == commandType) {
      return Collections.emptyList();
    }

    Queue<AgentCommand> queue = getQueue(hostname);
    if (null == queue) {
      return null;
    }

    List<AgentCommand> removedCommands = new ArrayList<AgentCommand>(
        queue.size());

    Iterator<AgentCommand> iterator = queue.iterator();
    while (iterator.hasNext()) {
      AgentCommand command = iterator.next();
      if (command.getCommandType() == commandType) {
        removedCommands.add(command);
        iterator.remove();
      }
    }

    return removedCommands;
  }

  /**
   * Try to dequeue command with provided id.
   *
   * @param hostname
   * @param commandId
   * @return
   */
  public AgentCommand dequeue(String hostname, String commandId) {
    Queue<AgentCommand> q = getQueue(hostname);
    if (q == null) {
      return null;
    }
    if (q.isEmpty()) {
      return null;
    } else {
      AgentCommand c = null;
      for (Iterator<AgentCommand> it = q.iterator(); it.hasNext();) {
        AgentCommand ac = it.next();
        if (ac instanceof ExecutionCommand && ((ExecutionCommand) ac)
          .getCommandId().equals(commandId)) {
          c = ac;
          it.remove();
          break;
        }
      }
      return c;
    }
  }

  public int size(String hostname) {
    Queue<AgentCommand> q = getQueue(hostname);
    if (q == null) {
      return 0;
    }
      return q.size();
  }

  public List<AgentCommand> dequeueAll(String hostname) {
    Queue<AgentCommand> q = getQueue(hostname);
    if (q == null) {
      return null;
    }

    List<AgentCommand> l = new ArrayList<AgentCommand>();

    AgentCommand command;
    do {
      //get commands from queue until empty
      command = q.poll();
      if (command != null) {
        l.add(command);
      }
    } while (command != null);

    return l;
  }

  /**
   * Update the cache of hosts that have pending tasks
   *
   * @param hosts
   */
  public void updateListOfHostsWithPendingTask(HashSet<String> hosts) {
    if (hosts != null) {
      hostsWithPendingTask = hosts;
    } else if (hostsWithPendingTask.size() > 0) {
      hostsWithPendingTask = EMPTY_HOST_LIST;
    }
  }

  /**
   * Checks whether host has pending tasks
   * @param hostName
   * @return
   */
  public boolean hasPendingTask(String hostName) {
    HashSet<String> copyHostsWithTaskPending = hostsWithPendingTask;
    if (copyHostsWithTaskPending != null) {
      return copyHostsWithTaskPending.contains(hostName);
    }

    return false;
  }
}
