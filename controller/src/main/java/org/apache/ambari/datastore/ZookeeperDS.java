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
package org.apache.ambari.datastore;

import java.io.IOException;
import java.util.List;

import org.apache.ambari.common.rest.entities.ClusterDefinition;
import org.apache.ambari.common.rest.entities.ClusterState;
import org.apache.ambari.common.rest.entities.Stack;
import org.apache.ambari.common.util.JAXBUtil;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

/**
 * Implementation of the data store based on Zookeeper.
 */
class ZookeeperDS implements DataStore, Watcher {

  private static final String ZOOKEEPER_ROOT_PATH="/ambari";
  private static final String ZOOKEEPER_CLUSTERS_ROOT_PATH =
      ZOOKEEPER_ROOT_PATH + "/clusters";
  private static final String ZOOKEEPER_STACKS_ROOT_PATH = 
      ZOOKEEPER_ROOT_PATH + "/stacks";

  private ZooKeeper zk;
  private String credential = null;
  private boolean zkCoonected = false;

  ZookeeperDS(String authority) {
    try {
      /*
       * Connect to ZooKeeper server
       */
      zk = new ZooKeeper(authority, 600000, this);
      if(credential != null) {
        zk.addAuthInfo("digest", credential.getBytes());
      }

      while (!this.zkCoonected) {
        System.out.println("Waiting for ZK connection!");
        Thread.sleep(2000);
      }

      /*
       * Create top level directories
       */
      createDirectory (ZOOKEEPER_ROOT_PATH, new byte[0], true);
      createDirectory (ZOOKEEPER_CLUSTERS_ROOT_PATH, new byte[0], true);
      createDirectory (ZOOKEEPER_STACKS_ROOT_PATH, new byte[0], true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void close() throws IOException {
    // PASS
  }

  @Override
  public boolean clusterExists(String clusterName) throws IOException {
    try {
      if (zk.exists(ZOOKEEPER_CLUSTERS_ROOT_PATH+"/"+clusterName, false) 
            == null) {
        return false;
      }
    } catch (Exception e) {
      throw new IOException(e);
    }
    return true;
  }

  @Override
  public synchronized int storeClusterDefinition(ClusterDefinition clusterDef
      ) throws IOException {  
    /*
     * Update the cluster node
     */
    try {
      Stat stat = new Stat();
      String clusterPath = ZOOKEEPER_CLUSTERS_ROOT_PATH+"/" + 
                           clusterDef.getName();
      int newRev = 0;
      String clusterRevisionPath = clusterPath+"/"+newRev;
      String clusterLatestRevisionNumberPath = clusterPath + 
          "/latestRevisionNumber";
      if (zk.exists(clusterPath, false) == null) {
        /* 
         * create cluster path with revision 0, create cluster latest revision
         * node storing the latest revision of cluster definition.
         */
        createDirectory (clusterPath, new byte[0], false);
        createDirectory (clusterRevisionPath, 
                         JAXBUtil.write(clusterDef), false);
        createDirectory (clusterLatestRevisionNumberPath, 
                         (new Integer(newRev)).toString().getBytes(), false);
      }else {
        String latestRevision = 
            new String (zk.getData(clusterLatestRevisionNumberPath, false, 
                                   stat));
        newRev = Integer.parseInt(latestRevision) + 1;
        clusterRevisionPath = clusterPath + "/" + newRev;
        /*
         * If client passes the revision number of the checked out cluster 
         * definition following code checks if you are updating the same version
         * that you checked out.
         */
        if (clusterDef.getRevision() != null) {
          if (!latestRevision.equals(clusterDef.getRevision())) {
            throw new IOException ("Latest cluster definition does not match "+
                                   "the one client intends to modify!");
          }  
        } 
        createDirectory(clusterRevisionPath, JAXBUtil.write(clusterDef), false);
        zk.setData(clusterLatestRevisionNumberPath, 
                   (new Integer(newRev)).toString().getBytes(), -1);
      }
      return newRev;
    } catch (KeeperException e) {
      throw new IOException (e);
    } catch (InterruptedException e1) {
      throw new IOException (e1);
    }
  }

  @Override
  public synchronized void storeClusterState(String clusterName, 
                                             ClusterState clsState
                                             ) throws IOException {
    /*
     * Update the cluster state
     */
    try {
      String clusterStatePath = 
          ZOOKEEPER_CLUSTERS_ROOT_PATH+"/"+clusterName+"/state";
      if (zk.exists(clusterStatePath, false) == null) {
        // create node for the cluster state
        createDirectory (clusterStatePath, JAXBUtil.write(clsState), false);
      }else {
        zk.setData(clusterStatePath, JAXBUtil.write(clsState), -1);
      }
    } catch (KeeperException e) {
      throw new IOException (e);
    } catch (InterruptedException e1) {
      throw new IOException (e1);
    }

  }

  @Override
  public ClusterDefinition retrieveClusterDefinition(String clusterName, 
                                             int revision) throws IOException {
    try {
      Stat stat = new Stat();
      String clusterRevisionPath;
      if (revision < 0) {   
        String clusterLatestRevisionNumberPath = 
           ZOOKEEPER_CLUSTERS_ROOT_PATH+"/"+clusterName+"/latestRevisionNumber";
        String latestRevisionNumber = 
          new String (zk.getData(clusterLatestRevisionNumberPath, false, stat));
        clusterRevisionPath = 
          ZOOKEEPER_CLUSTERS_ROOT_PATH+"/"+clusterName+"/"+latestRevisionNumber;       
      } else {
        clusterRevisionPath = 
            ZOOKEEPER_CLUSTERS_ROOT_PATH+"/"+clusterName+"/"+revision;
      }
      ClusterDefinition cdef = JAXBUtil.read(zk.getData(clusterRevisionPath, 
          false, stat), ClusterDefinition.class); 
      return cdef;
    } catch (Exception e) {
      throw new IOException (e);
    }
  }

  @Override
  public ClusterState retrieveClusterState(String clusterName
                                           ) throws IOException {
    try {
      Stat stat = new Stat();
      String clusterStatePath = 
          ZOOKEEPER_CLUSTERS_ROOT_PATH+"/"+clusterName+"/state";
      ClusterState clsState = 
          JAXBUtil.read(zk.getData(clusterStatePath, false, stat), 
                        ClusterState.class); 
      return clsState;
    } catch (Exception e) {
      throw new IOException (e);
    }
  }

  @Override
  public int retrieveLatestClusterRevisionNumber(String clusterName
                                                 ) throws IOException {
    int revisionNumber;
    try {
      Stat stat = new Stat();
      String clusterLatestRevisionNumberPath = 
          ZOOKEEPER_CLUSTERS_ROOT_PATH+"/"+clusterName+"/latestRevisionNumber";
      String latestRevisionNumber = 
          new String (zk.getData(clusterLatestRevisionNumberPath, false, stat));
      revisionNumber = Integer.parseInt(latestRevisionNumber);
    } catch (Exception e) {
      throw new IOException (e);
    }
    return revisionNumber;
  }

  @Override
  public List<String> retrieveClusterList() throws IOException {
    try {
      List<String> children = zk.getChildren(ZOOKEEPER_CLUSTERS_ROOT_PATH, 
                                             false);
      return children;
    } catch (KeeperException e) {
      throw new IOException (e);
    } catch (InterruptedException e) {
      throw new IOException (e);
    }
  }

  @Override
  public void deleteCluster(String clusterName) throws IOException {
    String clusterPath = ZOOKEEPER_CLUSTERS_ROOT_PATH+"/"+clusterName;
    List<String> children;
    try {
      children = zk.getChildren(clusterPath, false);
      // Delete all the children and then the parent node
      for (String childPath : children) {
        try {
          zk.delete(childPath, -1);
        } catch (KeeperException.NoNodeException ke) {
        } catch (Exception e) { throw new IOException (e); }
      }
      zk.delete(clusterPath, -1);
    } catch (KeeperException.NoNodeException ke) {
      return;
    } catch (Exception e) {
      throw new IOException (e);
    }
  }

  @Override
  public int storeStack(String stackName, Stack stack) throws IOException {
    try {
      Stat stat = new Stat();
      String stackPath = ZOOKEEPER_STACKS_ROOT_PATH+"/"+stackName;
      int newRev = 0;
      String stackRevisionPath = stackPath+"/"+newRev;
      String stackLatestRevisionNumberPath = stackPath+"/latestRevisionNumber";
      if (zk.exists(stackPath, false) == null) {
        /* 
         * create stack path with revision 0, create stack latest revision node
         * to store the latest revision of stack definition.
         */
        createDirectory (stackPath, new byte[0], false);
        stack.setRevision(new Integer(newRev).toString());
        createDirectory (stackRevisionPath, JAXBUtil.write(stack), false);
        createDirectory (stackLatestRevisionNumberPath, 
            (new Integer(newRev)).toString().getBytes(), false);
      }else {
        String latestRevision = 
            new String (zk.getData(stackLatestRevisionNumberPath, false, stat));
        newRev = Integer.parseInt(latestRevision) + 1;
        stackRevisionPath = stackPath + "/" + newRev;
        /*
         * TODO: like cluster definition client can pass optionally the checked 
         * out version number
         * Following code checks if you are updating the same version that you 
         * checked out.
         * if (stack.getRevision() != null) {
         *   if (!latestRevision.equals(stack.getRevision())) {
         *     throw new IOException ("Latest cluster definition does not " + 
         *                           "match the one client intends to modify!");
         *   }  
         * } 
         */
        stack.setRevision(new Integer(newRev).toString());
        createDirectory (stackRevisionPath, JAXBUtil.write(stack), false);
        zk.setData(stackLatestRevisionNumberPath, 
                   (new Integer(newRev)).toString().getBytes(), -1);
      }
      return newRev;
    } catch (KeeperException e) {
      throw new IOException (e);
    } catch (InterruptedException e1) {
      throw new IOException (e1);
    }
  }

  @Override
  public Stack retrieveStack(String stackName, int revision)
      throws IOException {
    try {
      Stat stat = new Stat();
      String stackRevisionPath;
      if (revision < 0) {   
        String stackLatestRevisionNumberPath = 
            ZOOKEEPER_STACKS_ROOT_PATH+"/"+stackName+"/latestRevisionNumber";
        String latestRevisionNumber = 
            new String (zk.getData(stackLatestRevisionNumberPath, false, stat));
        stackRevisionPath = 
            ZOOKEEPER_STACKS_ROOT_PATH+"/"+stackName+"/"+latestRevisionNumber;       
      } else {
        stackRevisionPath = 
            ZOOKEEPER_STACKS_ROOT_PATH+"/"+stackName+"/"+revision;
      }
      Stack stack = JAXBUtil.read(zk.getData(stackRevisionPath, false, stat), 
          Stack.class); 
      return stack;
    } catch (Exception e) {
      throw new IOException (e);
    }
  }

  @Override
  public List<String> retrieveStackList() throws IOException {
    try {
      List<String> children = zk.getChildren(ZOOKEEPER_STACKS_ROOT_PATH, false);
      return children;
    } catch (KeeperException e) {
      throw new IOException (e);
    } catch (InterruptedException e) {
      throw new IOException (e);
    }
  }

  @Override
  public int retrieveLatestStackRevisionNumber(String stackName
                                               ) throws IOException { 
    int revisionNumber;
    try {
      Stat stat = new Stat();
      String stackLatestRevisionNumberPath = 
          ZOOKEEPER_STACKS_ROOT_PATH+"/"+stackName+"/latestRevisionNumber";
      String latestRevisionNumber = 
          new String (zk.getData(stackLatestRevisionNumberPath, false, stat));
      revisionNumber = Integer.parseInt(latestRevisionNumber);
    } catch (Exception e) {
      throw new IOException (e);
    }
    return revisionNumber;
  }

  @Override
  public void deleteStack(String stackName) throws IOException {
    String stackPath = ZOOKEEPER_STACKS_ROOT_PATH+"/"+stackName;
    List<String> children;
    try {
      children = zk.getChildren(stackPath, false);
      // Delete all the children and then the parent node
      for (String childPath : children) {
        try {
          zk.delete(childPath, -1);
        } catch (KeeperException.NoNodeException ke) {
        } catch (Exception e) { throw new IOException (e); }
      }
      zk.delete(stackPath, -1);
    } catch (KeeperException.NoNodeException ke) {
      return;
    } catch (Exception e) {
      throw new IOException (e);
    }
  }

  @Override
  public boolean stackExists(String stackName) throws IOException {
    try {
      if (zk.exists(ZOOKEEPER_STACKS_ROOT_PATH+"/"+stackName, false) == null) {
        return false;
      }
    } catch (Exception e) {
      throw new IOException(e);
    }
    return true;
  }

  @Override
  public void process(WatchedEvent event) {
    if (event.getType() == Event.EventType.None) {
      // We are are being told that the state of the
      // connection has changed
      switch (event.getState()) {
      case SyncConnected:
        // In this particular example we don't need to do anything
        // here - watches are automatically re-registered with 
        // server and any watches triggered while the client was 
        // disconnected will be delivered (in order of course)
        this.zkCoonected = true;
        break;
      case Expired:
        // It's all over
        //running = false;
        //commandHandler.stop();
        break;
      }
    }

  }

  private void createDirectory(String path, byte[] initialData, 
                               boolean ignoreIfExists
                               ) throws KeeperException, InterruptedException {
    try {
      zk.create(path, initialData, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      if(credential!=null) {
        zk.setACL(path, Ids.CREATOR_ALL_ACL, -1);
      }
      System.out.println("Created path : <" + path +">");
    } catch (KeeperException.NodeExistsException e) {
      if (!ignoreIfExists) {
        System.out.println("Path already exists <"+path+">");
        throw e;
      }
    } catch (KeeperException.AuthFailedException e) {
      System.out.println("Failed to authenticate for path <"+path+">");
      throw e;
    }
  }
}
