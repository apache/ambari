#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Add or remove replication peers

require 'optparse'
include Java

java_import org.apache.hadoop.hbase.HBaseConfiguration
java_import org.apache.hadoop.hbase.HConstants
java_import org.apache.hadoop.hbase.client.replication.ReplicationAdmin
java_import org.apache.hadoop.hbase.replication.ReplicationPeerConfig
java_import org.apache.commons.logging.Log
java_import org.apache.commons.logging.LogFactory
java_import org.apache.hadoop.hbase.util.VersionInfo

# Name of this script
NAME = "hbase_replication"

# Do command-line parsing
options = {}
optparse = OptionParser.new do |opts|
  opts.banner = "Usage: ./hbase org.jruby.Main #{NAME}.rb [options] add|remove|update|list <peerId|delimited_peerIds> <cluster_key|delimited_cluster_keys>"
  opts.separator 'Add remote a single Slave cluster for replication.  List Slave Clusters.  Or Update Slave clusters to given hash delimited new slave clusters.'
  opts.on('-h', '--help', 'Display usage information') do
    puts opts
    exit
  end
  options[:debug] = false
  opts.on('-d', '--debug', 'Display extra debug logging') do
    options[:debug] = true
  end
end
optparse.parse!

def getConfiguration()
  hbase_twenty = VersionInfo.getVersion().match('0\.20\..*')
  # Get configuration to use.
  if hbase_twenty
    c = HBaseConfiguration.new()
  else
    c = HBaseConfiguration.create()
    end
    # Set hadoop filesystem configuration using the hbase.rootdir.
    # Otherwise, we'll always use localhost though the hbase.rootdir
    # might be pointing at hdfs location. Do old and new key for fs.
    c.set("fs.default.name", c.get(HConstants::HBASE_DIR))
    c.set("fs.defaultFS", c.get(HConstants::HBASE_DIR))
    return c
end

def removePeer(options, peerId)
    unless peerId !~ /\D/
      raise 'peerId should be Integer ID for peer cluster'
    end
    replAdm = ReplicationAdmin.new(getConfiguration())
    replAdm.removePeer(peerId)
end

#wrapper over addPeer method of ReplicationAdmin
#args - tableCfs the table and column-family list which will be replicated for this peer.
#A map from tableName to column family names. An empty collection can be passed
#to indicate replicating all column families. Pass null(nil in ruby) for replicating all table and column
#families
# clusterKey:  zkquorum:port:parentzkey
# c7007.ambari.apache.org,c7008.ambari.apache.org,c7009.ambari.apache.org:2181:/hbase
def addPeer(options, peerId, clusterKey, tableCfs=nil, endpointClass=nil, isTemporary="false")
    unless peerId !~ /\D/
      raise 'peerId should be Integer ID for peer cluster'
    end
    replAdm = ReplicationAdmin.new(getConfiguration())
    replPeerConfig = ReplicationPeerConfig.new()
    replPeerConfig.setClusterKey(clusterKey)
    printf "Ading Peer Id %s with ClusterKey %s\n", peerId, clusterKey
    #replPeerConfig.getConfiguration().put("IS_TEMPORARY", cluster.isTemporary)
    if endpointClass
        replPeerConfig.setReplicationEndpointImpl(endpointClass)
        peerId.gsub!("-", "*") # odr TenantReplicationEndpoint expects hyphens to be escaped to astericks
    end
    replPeerConfig.getConfiguration().put("IS_TEMPORARY", isTemporary)

    replAdm.addPeer(peerId, replPeerConfig, tableCfs)
end

# Gets the list of configured replication peers for a cluster.
#
#This method will return list of peers using ReplicationAdmin interface
#  The resulting array will be the rows of existing peers.
#   [["id1","hosts:port","state"],
#    ["id2","hosts:port","state"]]
#
def getReplicationPeers()
  replAdm = ReplicationAdmin.new(getConfiguration())

  repPeers = Array.new()
  repPeers = replAdm.listPeerConfigs
  existingPeerClusters = Array.new
  repPeers.entrySet().each do |e|
    state = replAdm.getPeerState(e.key)
    existingPeerClusters.push([ e.key, e.value.getClusterKey, state ])
  end
  
  replAdm.close()

  return existingPeerClusters
end
  

# list peers
def listPeers(options)
  '''
  peersList = getReplicationPeers
  puts "\n\nOutput\n PEER_ID  CLUSTER_KEY  STATE :\n"
  peersList.each {|peer| puts "#{peer[0]}  #{peer[1]}  #{peer[2]}"
  '''
  servers = getReplicationPeers
  puts "Replication Peers are: " + servers.size().to_s  
  servers.each {|server| puts server}
end

arguments = ARGV[1..ARGV.size()]

# Create a logger and disable the DEBUG-level annoying client logging
def configureLogging(options)
  apacheLogger = LogFactory.getLog(NAME)
  # Configure log4j to not spew so much
  unless (options[:debug]) 
    logger = org.apache.log4j.Logger.getLogger("org.apache.hadoop.hbase")
    logger.setLevel(org.apache.log4j.Level::WARN)
  end
  return apacheLogger
end


def updateReplicationPeers(options, delimitedPeerIds, delimitedClusterKeys, delimiterChar='#')
    # Using delimited Given PeerIds and ClusterKeys, get All Slave Clusters Desired and fill in inputSlaveClusters
    peerIds = delimitedPeerIds.split(delimiterChar)
    clusterKeys = delimitedClusterKeys.split(delimiterChar)
    peer_ids_count = peerIds.size()

    if peer_ids_count != clusterKeys.size()
      raise "PeerIds " + peer_ids_count.to_s + " and ClusterKeys " + clusterKeys.size().to_s + " must be equal in number" 
    end

    inputSlaveClusters = Array.new
    i = 0

    while i < peer_ids_count  do
      inputSlaveClusters.push([ peerIds[i], clusterKeys[i] ])
      i +=1
    end

    toBeAddedClusters = inputSlaveClusters.dup
    existingPeerClusters = getReplicationPeers
    toBeRemovedClusters = existingPeerClusters.dup

    #Validate existing instance file and figure out new clusters to be added
    inputSlaveClusters.each do |inputSlaveCluster|
        # Handle case for empty slave list
        printf "Required Peer Id %s with ClusterKey %s\n ", inputSlaveCluster[0], inputSlaveCluster[1]
        existingPeerClusters.each do |existingPeerCluster|
            # Handle case for empty slave list
            printf "Comparing with existing replication peer %s and Cluster Key %s\n", existingPeerCluster[0], existingPeerCluster[1]
            if existingPeerCluster[0].eql? inputSlaveCluster[0]  and !existingPeerCluster[1].eql? inputSlaveCluster[1]
                  raise "Conflict in instance file with existing peers, same peer id already exists for different cluster " + existingPeerCluster[1]
            elsif !existingPeerCluster[0].eql? inputSlaveCluster[0]  and existingPeerCluster[1].eql? inputSlaveCluster[1]
                  raise "Conflict in instance file with existing peers, same cluster already exists with different peer id " + existingPeerCluster[0]
            end
            if existingPeerCluster[0].eql? inputSlaveCluster[0]
                  puts "Cluster already exists in peers list. So ignoring..."
                  toBeAddedClusters.delete(inputSlaveCluster)
                  break
            end
          end
    end

    #Compare existing peers with new peers from instance file and fetch peers to be removed
    existingPeerClusters.each do |toBeRemovedCluster|
        printf "Existing replication peer %s and Cluster Key %s\n", toBeRemovedCluster[0], toBeRemovedCluster[1]
        inputSlaveClusters.each do |inputSlaveCluster|
            # Handle case for empty slave list
            printf "Comparing with configured replication peer %s and ClusterKey %s\n", inputSlaveCluster[0], inputSlaveCluster[1]
            if toBeRemovedCluster[0].eql? inputSlaveCluster[0]
                  puts "Same also exists in ml configued so deleting from remove list"
                  toBeRemovedClusters.delete(toBeRemovedCluster)
                  break
            end
        end
      end

    if toBeAddedClusters.size == 0
        puts "Nothing need to be added..."
    else
        toBeAddedClusters.each do |toBeAddedCluster|
            printf "To Be Added Cluster peerId: %s, clusterKey: %s\n", toBeAddedCluster[0], toBeAddedCluster[1]
            addPeer(options, toBeAddedCluster[0], toBeAddedCluster[1])
        end
    end

    if toBeRemovedClusters.size == 0
        puts "Nothing need to be removed..."
    else
        toBeRemovedClusters.each do |toBeRemovedCluster|
            printf "To Be Removed Cluster peerId: %s, clusterKey: %s\n", toBeRemovedCluster[0], toBeRemovedCluster[1]
            removePeer(options, toBeRemovedCluster[0])
        end
    end
end


# Create a logger and save it to ruby global
$LOG = configureLogging(options)
case ARGV[0]
  when 'add'
    if ARGV.length < 3
      puts optparse
      exit 1
    end
    peerId = ARGV[1]
    clusterKey = ARGV[2]
    addPeer(options, peerId, clusterKey)
  when 'remove'
    if ARGV.length < 2
      puts optparse
      exit 1
    end
    peerId = ARGV[1]
    removePeer(options, peerId)
  when 'update'
    if ARGV.length < 2
      puts optparse
      exit 1
    end
    delimitedPeerIds = ARGV[1]
    delimitedClusterKeys = ARGV[2]
    updateReplicationPeers(options, delimitedPeerIds, delimitedClusterKeys)    
  when 'list'
    listPeers(options)
  else
    puts optparse
    exit 3
end
