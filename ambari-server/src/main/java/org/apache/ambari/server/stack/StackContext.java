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

package org.apache.ambari.server.stack;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.entities.MetainfoEntity;
import org.apache.ambari.server.state.stack.LatestRepoCallable;
import org.apache.ambari.server.state.stack.OsFamily;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 * Provides external functionality to the Stack framework.
 */
public class StackContext {
  /**
   * Metainfo data access object
   */
  private MetainfoDAO metaInfoDAO;

  /**
   * Action meta data functionality
   */
  private ActionMetadata actionMetaData;

  /**
   * Operating System families
   */
  private OsFamily osFamily;

  /**
   * Executor used to get latest repo url's
   */
  private LatestRepoQueryExecutor repoUpdateExecutor = new LatestRepoQueryExecutor();

  /**
   * Repository XML base url property name
   */
  private static final String REPOSITORY_XML_PROPERTY_BASEURL = "baseurl";


  /**
   * Constructor.
   *
   * @param metaInfoDAO     metainfo data access object
   * @param actionMetaData  action meta data
   * @param osFamily        OS family information
   */
  public StackContext(MetainfoDAO metaInfoDAO, ActionMetadata actionMetaData, OsFamily osFamily) {
    this.metaInfoDAO = metaInfoDAO;
    this.actionMetaData = actionMetaData;
    this.osFamily = osFamily;
  }

  /**
   * Register a service check.
   *
   * @param serviceName  name of the service
   */
  public void registerServiceCheck(String serviceName) {
    actionMetaData.addServiceCheckAction(serviceName);
  }

  /**
   * Obtain an updated url for the repo.
   * This will check the database for a user update of the repo url.
   *
   * @param stackName     stack name
   * @param stackVersion  stack version
   * @param osType        OS type
   * @param repoId        repo id
   *
   * @return  an update url or null if the url has not been updated
   */
  public String getUpdatedRepoUrl(String stackName, String stackVersion, String osType, String repoId) {
    String key = AmbariMetaInfo.generateRepoMetaKey(stackName, stackVersion,
            osType, repoId, REPOSITORY_XML_PROPERTY_BASEURL);
    MetainfoEntity entity = metaInfoDAO.findByKey(key);
    return entity != null ? entity.getMetainfoValue() : null;
  }

  /**
   * Register a task to obtain the latest repo url from an external location.
   *
   * @param url    external repo information URL
   * @param stack  stack module
   */
  public void registerRepoUpdateTask(String url, StackModule stack) {
    repoUpdateExecutor.addTask(new LatestRepoCallable(url,
        new File(stack.getStackDirectory().getRepoDir()), stack.getModuleInfo(), osFamily));
  }

  /**
   * Execute the registered repo update tasks.
   */
  public void executeRepoTasks() {
    repoUpdateExecutor.execute();
  }

  /**
   * Determine if all registered repo update tasks have completed.
   *
   * @return true if all tasks have completed; false otherwise
   */
  public boolean haveAllRepoTasksCompleted() {
    return repoUpdateExecutor.hasCompleted();
  }


  /**
   * Executor used to execute repository update tasks.
   * Tasks will be executed in a single executor thread.
   */
  public static class LatestRepoQueryExecutor {
    /**
     * Registered tasks
     */
    private Collection<LatestRepoCallable> tasks = new ArrayList<LatestRepoCallable>();

    /**
     * Task futures
     */
    Collection<Future<Void>> futures = new ArrayList<Future<Void>>();
    /**
     * Underlying executor
     */
    private ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
      @Override
      public Thread newThread(Runnable r) {
        return new Thread(r, "Stack Version Loading Thread");
      }
    });


    /**
     * Add a task.
     *
     * @param task task to be added
     */
    public void addTask(LatestRepoCallable task) {
      tasks.add(task);
    }

    /**
     * Execute all tasks.
     */
    public void execute() {
      for (LatestRepoCallable task : tasks) {
        futures.add(executor.submit(task));
      }
      executor.shutdown();
    }

    /**
     * Determine whether all tasks have completed.
     *
     * @return true if all tasks have completed; false otherwise
     */
    public boolean hasCompleted() {
      for (Future<Void> f : futures) {
        if (! f.isDone()) {
          return false;
        }
      }
      return true;
    }
  }
}
