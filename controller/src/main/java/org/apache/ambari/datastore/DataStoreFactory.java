package org.apache.ambari.datastore;

import java.io.IOException;

import org.apache.ambari.datastore.impl.ZookeeperDS;

public class DataStoreFactory {
    
    public static String  ZOOKEEPER_TYPE = "zookeeper";
    
    public static PersistentDataStore getDataStore(String storeType) {
        if (storeType.equalsIgnoreCase(ZOOKEEPER_TYPE)) {
            return ZookeeperDS.getInstance();
        }
        return null;
    }
    
    public static void main (String args) {
        try {
            PersistentDataStore ds = DataStoreFactory.getDataStore(ZOOKEEPER_TYPE);
        } catch (Exception e) {
        }
    }
}
