import "/Users/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/python/puppet/modules/hdp/manifests/*.pp"
import "/Users/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/python/puppet/modules/hdp-hadoop/manifests/*.pp"
import "/Users/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/python/puppet/modules/hdp-hbase/manifests/*.pp"
import "/Users/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/python/puppet/modules/hdp-zookeeper/manifests/*.pp"
import "/Users/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/python/puppet/modules/hdp-oozie/manifests/*.pp""
import "/Users/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/python/puppet/modules/hdp-pig/manifests/*.pp"
import "/Users/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/python/puppet/modules/hdp-sqoop/manifests/*.pp"
import "/Users/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/python/puppet/modules/hdp-templeton/manifests/*.pp"
import "/Users/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/python/puppet/modules/hdp-hive/manifests/*.pp"
import "/Users/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/python/puppet/modules/hdp-hcat/manifests/*.pp"
import "/Users/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/python/puppet/modules/hdp-mysql/manifests/*.pp"
import "/Users/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/python/puppet/modules/hdp-monitor-webserver/manifests/*.pp"
$NAMENODE= ['h2.hortonworks.com']
$DATANODE= ['h1.hortonworks.com', 'h2.hortonworks.com']
$hdfs_user="hdfs"
$jdk_location="lah/blah"
$configuration =  {
$hdfs_site=> {
"dfs.block.size" => "256000000",
"dfs.replication" => "1"
}
$core_site=> {
"fs.default.name" => "hrt8n36.cc1.ygridcore.net"
}

}
node /default/ {
 stage{0 :} -> stage{1 :}
class {'hdp-hadoop::datanode': stage => 1, service_state => running}
class {'hdp-hadoop::namenode': stage => 2, service_state => installed_and_configured}
}
