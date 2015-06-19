/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
* http://www.apache.org/licenses/LICENSE-2.0
 * 
* Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.ambari.server.testing;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 
 * Monitoring of deadlocks thread
 * Please note. This class can not be used outside of tests
 */
public class DeadlockWarningThread extends Thread {

  private Thread parentThread;
  private final List<String> errorMessages;
  private static final int MAX_STACK_DEPTH = 30;
  private Collection<Thread> monitoredThreads = null;
  private boolean deadlocked = false;
  private static final ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
  private String stacktrace = "";

  public List<String> getErrorMessages() {
    return errorMessages;
  }

  public boolean isDeadlocked() {
    return deadlocked;
  }

  public DeadlockWarningThread(Collection<Thread> monitoredThreads) {
    this.errorMessages = new ArrayList<String>();
    this.monitoredThreads = monitoredThreads;
    parentThread = Thread.currentThread();
    start();
  }

  public String getThreadsStacktraces(long[] ids) {
    StringBuilder errBuilder = new StringBuilder();
      for (long id : ids) {
        ThreadInfo ti = mbean.getThreadInfo(id, MAX_STACK_DEPTH);
        errBuilder.append("Deadlocked Thread:\n").
                append("------------------\n").
                append(ti).append('\n');
        for (StackTraceElement ste : ti.getStackTrace()) {
          errBuilder.append('\t').append(ste);
        }
        errBuilder.append('\n');
      }
    return errBuilder.toString();
  }
 
  
  @Override
  public void run() {
    while (true) {
      try {
        Thread.sleep(3000);
      } catch (InterruptedException ex) {
      }
      long[] ids = mbean.findMonitorDeadlockedThreads();
      StringBuilder errBuilder = new StringBuilder();
      if (ids != null && ids.length > 0) {
          errBuilder.append(getThreadsStacktraces(ids));
          errorMessages.add(errBuilder.toString());
          System.out.append(errBuilder.toString());
         //Exit if deadlocks have been found         
          deadlocked = true;
          break;
      } else {
        //Exit if all monitored threads were finished
        boolean hasLive = false;
        Set<Thread> activeThreads = new HashSet<Thread>();
        for (Thread monTh : monitoredThreads) {
          ThreadGroup group = monTh.getThreadGroup();
          if (group == null) {
            //expected if thread died, ignore it
            continue;
          }
          int activeCount = group.activeCount();
          Thread[] groupThreads = new Thread[activeCount];
          group.enumerate(groupThreads, true);
          activeThreads.addAll(Arrays.asList(groupThreads));
        }
        activeThreads.remove(Thread.currentThread());
        activeThreads.remove(parentThread);
        Set<Long> idSet = new TreeSet<Long>();
        for (Thread activeThread : activeThreads) {
          if (activeThread.isAlive()) {
            hasLive = true;
            idSet.add(activeThread.getId());
          }     
        }
        long[] tid = new long[idSet.size()];
        if (!hasLive) {
          deadlocked = false;
          break;
        } else {
          int cnt = 0;
          for (Long id : idSet) {
            tid[cnt] = id;
            cnt++;
          }
          String currentStackTrace = getThreadsStacktraces(tid);
          if (stacktrace.equals(currentStackTrace)) {
            errBuilder.append(currentStackTrace);
            errorMessages.add(currentStackTrace);
            System.out.append(currentStackTrace);
            deadlocked = true;
            break;            
          } else {
            stacktrace = currentStackTrace;
          }
        }
      }
    }
  }  
}
