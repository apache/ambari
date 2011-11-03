package org.apache.ambari.datastore.impl;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ambari.common.rest.entities.ClusterDefinition;
import org.apache.ambari.common.rest.entities.ClusterState;
import org.apache.ambari.common.rest.entities.Stack;
import org.apache.ambari.common.util.JAXBUtil;
import org.apache.ambari.controller.Stacks;
import org.apache.ambari.datastore.PersistentDataStore;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class ZookeeperDS implements PersistentDataStore, Watcher {

    private static final String DEFAULT_ZOOKEEPER_ADDRESS="localhost:2181";
    private static final String ZOOKEEPER_ROOT_PATH="/ambari";
    private static final String ZOOKEEPER_CLUSTERS_ROOT_PATH=ZOOKEEPER_ROOT_PATH+"/clusters";
    private static final String ZOOKEEPER_STACKS_ROOT_PATH=ZOOKEEPER_ROOT_PATH+"/stacks";
    
    private ZooKeeper zk;
    private String credential = null;
    private boolean zkCoonected = false;
    
    private static ZookeeperDS ZookeeperDSRef=null;
    private ZookeeperDS() {
        /*
         * TODO: Read ZooKeeper address and credential from config file
         */
        String zookeeperAddress = DEFAULT_ZOOKEEPER_ADDRESS;
        try {
            /*
             * Connect to ZooKeeper server
             */
            zk = new ZooKeeper(zookeeperAddress, 600000, this);
            if(credential != null) {
              zk.addAuthInfo("digest", credential.getBytes());
            }
            
            while (!this.zkCoonected) {
                Thread.sleep(5000);
                System.out.println("Waiting for ZK connection");
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
    
    public static synchronized ZookeeperDS getInstance() {
        if(ZookeeperDSRef == null) {
            ZookeeperDSRef = new ZookeeperDS();
        }
        return ZookeeperDSRef;
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
    
    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean clusterExists(String clusterName) throws IOException {
        try {
            if (zk.exists(ZOOKEEPER_CLUSTERS_ROOT_PATH+"/"+clusterName, false) == null) {
                return false;
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
        return true;
    }
    
    @Override
    public synchronized int storeClusterDefinition(ClusterDefinition clusterDef) throws IOException {  
        /*
         * Update the cluster node
         */
        try {
            Stat stat = new Stat();
            String clusterPath = ZOOKEEPER_CLUSTERS_ROOT_PATH+"/"+clusterDef.getName();
            int newRev = 0;
            String clusterRevisionPath = clusterPath+"/"+newRev;
            String clusterLatestRevisionNumberPath = clusterPath+"/latestRevisionNumber";
            if (zk.exists(clusterPath, false) == null) {
                /* 
                 * create cluster path with revision 0, create cluster latest revision node 
                 * storing the latest revision of cluster definition.
                 */
                createDirectory (clusterPath, new byte[0], false);
                createDirectory (clusterRevisionPath, JAXBUtil.write(clusterDef), false);
                createDirectory (clusterLatestRevisionNumberPath, (new Integer(newRev)).toString().getBytes(), false);
            }else {
                String latestRevision = new String (zk.getData(clusterLatestRevisionNumberPath, false, stat));
                newRev = Integer.parseInt(latestRevision) + 1;
                clusterRevisionPath = clusterPath + "/" + newRev;
                if (clusterDef.getRevision() != null) {
                    if (!latestRevision.equals(clusterDef.getRevision())) {
                        throw new IOException ("Latest cluster definition does not match the one client intends to modify!");
                    }  
                } 
                createDirectory (clusterRevisionPath, JAXBUtil.write(clusterDef), false);
                zk.setData(clusterLatestRevisionNumberPath, (new Integer(newRev)).toString().getBytes(), -1);
            }
            return newRev;
        } catch (KeeperException e) {
            throw new IOException (e);
        } catch (InterruptedException e1) {
            throw new IOException (e1);
        }
    }

    @Override
    public synchronized void storeClusterState(String clusterName, ClusterState clsState)
            throws IOException {
        /*
         * Update the cluster state
         */
        try {
            String clusterStatePath = ZOOKEEPER_CLUSTERS_ROOT_PATH+"/"+clusterName+"/state";
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
    public ClusterDefinition retrieveClusterDefinition(String clusterName, int revision) throws IOException {
        try {
            Stat stat = new Stat();
            String clusterRevisionPath;
            if (revision < 0) {   
                String clusterLatestRevisionNumberPath = ZOOKEEPER_CLUSTERS_ROOT_PATH+"/"+clusterName+"/latestRevisionNumber";
                String latestRevisionNumber = new String (zk.getData(clusterLatestRevisionNumberPath, false, stat));
                clusterRevisionPath = ZOOKEEPER_CLUSTERS_ROOT_PATH+"/"+clusterName+"/"+latestRevisionNumber;       
            } else {
                clusterRevisionPath = ZOOKEEPER_CLUSTERS_ROOT_PATH+"/"+clusterName+"/"+revision;
            }
            ClusterDefinition cdef = JAXBUtil.read(zk.getData(clusterRevisionPath, false, stat), ClusterDefinition.class); 
            return cdef;
        } catch (Exception e) {
            throw new IOException (e);
        }
    }

    @Override
    public ClusterState retrieveClusterState(String clusterName) throws IOException {
        try {
            Stat stat = new Stat();
            String clusterStatePath = ZOOKEEPER_CLUSTERS_ROOT_PATH+"/"+clusterName+"/state";
            ClusterState clsState = JAXBUtil.read(zk.getData(clusterStatePath, false, stat), ClusterState.class); 
            return clsState;
        } catch (Exception e) {
            throw new IOException (e);
        }
    }
    
    @Override
    public int retrieveLatestClusterRevisionNumber(String clusterName) throws IOException {
        int revisionNumber;
        try {
            Stat stat = new Stat();
            String clusterLatestRevisionNumberPath = ZOOKEEPER_CLUSTERS_ROOT_PATH+"/"+clusterName+"/latestRevisionNumber";
            String latestRevisionNumber = new String (zk.getData(clusterLatestRevisionNumberPath, false, stat));
            revisionNumber = Integer.parseInt(latestRevisionNumber);
        } catch (Exception e) {
            throw new IOException (e);
        }
        return revisionNumber;
    }
    
    @Override
    public List<String> retrieveClusterList() throws IOException {
        try {
            List<String> children = zk.getChildren(ZOOKEEPER_CLUSTERS_ROOT_PATH, false);
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
    public void purgeClusterDefinitionRevisions(String clusterName,
            int lessThanRevision) throws IOException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void updateClusterState(String clusterName, ClusterState newstate)
            throws IOException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int storeStack(String stackName, Stack stack) throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Stack retrieveStack(String stackName, int revision)
            throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NameRevisionPair> retrieveStackList() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int deleteStack(String stackName) throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void deleteStackRevisions(String stackName, int lessThanRevision)
            throws IOException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void updateComponentState(String clusterName, String componentName,
            String state) throws IOException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getComponentState(String clusterName, String componentName)
            throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteComponentState(String clusterName, String componentName)
            throws IOException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void updateRoleState(String clusterName, String componentName,
            String roleName, String state) throws IOException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getRoleState(String clusterName, String componentName,
            String RoleName) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteRoleState(String clusterName, String componentName,
            String roleName) throws IOException {
        // TODO Auto-generated method stub
        
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
    
    private void createDirectory(String path, byte[] initialData, boolean ignoreIfExists) throws KeeperException, InterruptedException {
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
