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

package org.apache.ambari.view.hive2.backgroundjobs;

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

  private Map<String, BackgroundJob> jobs = new HashMap<String, BackgroundJob>();
  public void startJob(final String key, Runnable runnable)  {
    if (jobs.containsKey(key)) {
      try {
        interrupt(key);
        jobs.get(key).getJobThread().join();
      } catch (InterruptedException ignored) {
      } catch (BackgroundJobException e) {
      }
    }
    Thread t = new Thread(runnable);
    t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(Thread t, Throwable e) {
        if (e instanceof BackgroundJobException) {
          jobs.get(key).setJobException((BackgroundJobException) e);
        }
      }
    });
    jobs.put(key, new BackgroundJob(t));
    t.start();
  }

  public Thread.State state(String key) {
    if (!jobs.containsKey(key)) {
      return Thread.State.TERMINATED;
    }

    Thread.State state = jobs.get(key).getJobThread().getState();

    if (state == Thread.State.TERMINATED) {
      jobs.remove(key);
    }

    return state;
  }

  public boolean interrupt(String key) {
    if (!jobs.containsKey(key)) {
      return false;
    }

    jobs.get(key).getJobThread().interrupt();
    return true;
  }

  public boolean isInterrupted(String key) {
    if (state(key) == Thread.State.TERMINATED) {
      return true;
    }

    return jobs.get(key).getJobThread().isInterrupted();
  }

  class BackgroundJob {

    private Thread jobThread;
    private BackgroundJobException jobException;

    public BackgroundJob(Thread jobThread) {
      this.jobThread = jobThread;
    }

    public Thread getJobThread() {
      if(jobException != null) throw jobException;
      return jobThread;
    }

    public void setJobException(BackgroundJobException exception) {
      this.jobException = exception;
    }
  }

}
