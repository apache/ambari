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

package org.apache.ambari.view.hive.backgroundjobs;

import org.apache.ambari.view.ViewContext;

import java.util.HashMap;
import java.util.Map;

public class BackgroundJobController {
  private ViewContext context;

  protected BackgroundJobController(ViewContext context) {
    this.context = context;
  }

  private static Map<String, BackgroundJobController> viewSingletonObjects = new HashMap<String, BackgroundJobController>();
  public static BackgroundJobController getInstance(ViewContext context) {
    if (!viewSingletonObjects.containsKey(context.getInstanceName()))
      viewSingletonObjects.put(context.getInstanceName(), new BackgroundJobController(context));
    return viewSingletonObjects.get(context.getInstanceName());
  }

  private Map<String, Thread> jobs = new HashMap<String, Thread>();
  public void startJob(String key, Runnable runnable) {
    if (jobs.containsKey(key)) {
      interrupt(key);
      try {
        jobs.get(key).join();
      } catch (InterruptedException ignored) {
      }
    }
    Thread t = new Thread(runnable);
    jobs.put(key, t);
    t.start();
  }

  public Thread.State state(String key) {
    if (!jobs.containsKey(key)) {
      return Thread.State.TERMINATED;
    }

    Thread.State state = jobs.get(key).getState();

    if (state == Thread.State.TERMINATED) {
      jobs.remove(key);
    }

    return state;
  }

  public boolean interrupt(String key) {
    if (!jobs.containsKey(key)) {
      return false;
    }

    jobs.get(key).interrupt();
    return true;
  }

  public boolean isInterrupted(String key) {
    if (state(key) == Thread.State.TERMINATED) {
      return true;
    }

    return jobs.get(key).isInterrupted();
  }
}
