module.exports =
{
  "haConfig": {
    serviceName: 'MISC',
    displayName: 'MISC',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'HDFS', displayName: 'HDFS'}),
      App.ServiceConfigCategory.create({ name: 'HBASE', displayName: 'HBase'})
    ],
    sites: ['global', 'core-site', 'hdfs-site','hbase-site'],
    configs: [
    /**********************************************HDFS***************************************/
      {
        "id": "site property",
        "name": "dfs.journalnode.edits.dir",
        "displayName": "dfs.journalnode.edits.dir",
        "description": "The Directory where the JournalNode will store its local state.",
        "isReconfigurable": true,
        "defaultValue": "/hadoop/hdfs/journal",
        "value": "/hadoop/hdfs/journal",
        "defaultDirectory": "/hadoop/hdfs/journal",
        "displayType": "directory",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "id": "site property",
        "name": "fs.defaultFS",
        "displayName": "fs.defaultFS",
        "description": "The default path prefix used by the Hadoop FS client when none is given.",
        "defaultValue": "hdfs://haCluster",
        "isReconfigurable": false,
        "value": "hdfs://haCluster",
        "category": "HDFS",
        "filename": "core-site",
        serviceName: 'MISC'
      },
      {
        "id": "site property",
        "name": "ha.zookeeper.quorum",
        "displayName": "ha.zookeeper.quorum",
        "isReconfigurable": false,
        "description": "This lists the host-port pairs running the ZooKeeper service.",
        "defaultValue": "zk1.example.com:2181,zk2.example.com:2181,zk3.example.com:2181",
        "value": "zk1.example.com:2181,zk2.example.com:2181,zk3.example.com:2181",
        "category": "HDFS",
        "filename": "core-site",
        "serviceName": 'MISC'
      },
      {
        "id": "site property",
        "name": "dfs.nameservices",
        "displayName": "dfs.nameservices",
        "description": "Comma-separated list of nameservices.",
        "isReconfigurable": false,
        "defaultValue": "haCluster",
        "value": "haCluster",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "id": "site property",
        "name": "dfs.ha.namenodes.${dfs.nameservices}",
        "displayName": "dfs.ha.namenodes.${dfs.nameservices}",
        "description": "The prefix for a given nameservice, contains a comma-separated list of namenodes for a given nameservice.",
        "isReconfigurable": false,
        "defaultValue": "nn1,nn2",
        "value": "nn1,nn2",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "id": "site property",
        "name": "dfs.namenode.rpc-address.${dfs.nameservices}.nn1",
        "displayName": "dfs.namenode.rpc-address.${dfs.nameservices}.nn1",
        "description": "RPC address that handles all clients requests for nn1.",
        "isReconfigurable": false,
        "defaultValue": "0.0.0.0:8020",
        "value": "0.0.0.0:8020",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "id": "site property",
        "name": "dfs.namenode.rpc-address.${dfs.nameservices}.nn2",
        "displayName": "dfs.namenode.rpc-address.${dfs.nameservices}.nn2",
        "description": "RPC address that handles all clients requests for nn2.",
        "isReconfigurable": false,
        "defaultValue": "0.0.0.0:8020",
        "value": "0.0.0.0:8020",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "id": "site property",
        "name": "dfs.namenode.http-address.${dfs.nameservices}.nn1",
        "displayName": "dfs.namenode.http-address.${dfs.nameservices}.nn1",
        "description": "The fully-qualified HTTP address for nn1 NameNode.",
        "isReconfigurable": false,
        "defaultValue": "0.0.0.0:50070",
        "value": "0.0.0.0:50070",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "id": "site property",
        "name": "dfs.namenode.http-address.${dfs.nameservices}.nn2",
        "displayName": "dfs.namenode.http-address.${dfs.nameservices}.nn2",
        "description": "The fully-qualified HTTP address for nn2 NameNode.",
        "isReconfigurable": false,
        "defaultValue": "0.0.0.0:50070",
        "value": "0.0.0.0:50070",
        "category": "HDFS",
        "filename": "hdfs-site",
        serviceName: 'MISC'
      },
      {
        "id": "site property",
        "name": "dfs.namenode.https-address.${dfs.nameservices}.nn1",
        "displayName": "dfs.namenode.https-address.${dfs.nameservices}.nn1",
        "description": "The fully-qualified HTTP address for nn1 NameNode.",
        "isReconfigurable": false,
        "defaultValue": "0.0.0.0:50470",
        "value": "0.0.0.0:50470",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "id": "site property",
        "name": "dfs.namenode.https-address.${dfs.nameservices}.nn2",
        "displayName": "dfs.namenode.https-address.${dfs.nameservices}.nn2",
        "description": "The fully-qualified HTTP address for nn2 NameNode.",
        "isReconfigurable": false,
        "defaultValue": "0.0.0.0:50470",
        "value": "0.0.0.0:50470",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "id": "site property",
        "name": "dfs.client.failover.proxy.provider.${dfs.nameservices}",
        "displayName": "dfs.client.failover.proxy.provider.${dfs.nameservices}",
        "description": "The Java class that HDFS clients use to contact the Active NameNode.",
        "defaultValue": "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider",
        "isReconfigurable": false,
        "value": "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "id": "site property",
        "name": "dfs.namenode.shared.edits.dir",
        "displayName": "dfs.namenode.shared.edits.dir",
        "description": " The URI which identifies the group of JNs where the NameNodes will write/read edits.",
        "isReconfigurable": false,
        "defaultValue": "qjournal://node1.example.com:8485;node2.example.com:8485;node3.example.com:8485/mycluster",
        "value": "qjournal://node1.example.com:8485;node2.example.com:8485;node3.example.com:8485/mycluster",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "id": "site property",
        "name": "dfs.ha.fencing.methods",
        "displayName": "dfs.ha.fencing.methods",
        "description": "A list of scripts or Java classes which will be used to fence the Active NameNode during a failover.",
        "isReconfigurable": false,
        "defaultValue": "shell(/bin/true)",
        "value": "shell(/bin/true)",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "id": "site property",
        "name": "dfs.ha.automatic-failover.enabled",
        "displayName": "dfs.ha.automatic-failover.enabled",
        "description": "Enable Automatic failover.",
        "isReconfigurable": false,
        "defaultValue": true,
        "value": true,
        "displayType": "checkbox",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "id": "site property",
        "name": "hbase.rootdir",
        "displayName": "hbase.rootdir",
        "description": "The directory shared by region servers and into which HBase persists.",
        "isReconfigurable": false,
        "defaultValue": "/hadoop/hdfs/journal",
        "value": "/hadoop/hdfs/journal",
        "category": "HBASE",
        "filename": "hbase-site",
        "serviceName": 'MISC'
      }
    ]
  }
};