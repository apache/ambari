#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#
class hdp-hbase(
  $type,
  $service_state) 
{
  include hdp-hbase::params
 
  $hbase_user = $hdp-hbase::params::hbase_user
  $config_dir = $hdp-hbase::params::conf_dir
  
  $hdp::params::component_exists['hdp-hbase'] = true

  #Configs generation  
  configgenerator::configfile{'hbase-site.xml.erb': 
    module => 'hdp-hbase',
    properties => {'hbase.rootdir' => 'hdfs://<%=scope.function_hdp_host("namenode_host")%>:8020<%=scope.function_hdp_template_var("hbase_hdfs_root_dir")%>',
      'hbase.cluster.distributed' => 'true',
      'hbase.tmp.dir' => '<%=scope.function_hdp_template_var("hbase_tmp_dir")%>',
      'hbase.master.info.bindAddress' => '<%=scope.function_hdp_host("hbase_master_host")%>',
      'hbase.regionserver.global.memstore.upperLimit' => '<%=scope.function_hdp_template_var("regionserver_memstore_upperlimit")%>',
      'hbase.regionserver.handler.count' => '<%=scope.function_hdp_template_var("regionserver_handlers")%>',
      'hbase.hregion.majorcompaction' => '<%=scope.function_hdp_template_var("hregion_majorcompaction")%>',
      'hbase.regionserver.global.memstore.lowerLimit' => '<%=scope.function_hdp_template_var("regionserver_memstore_lowerlimit")%>',
      'hbase.hregion.memstore.block.multiplier' => '<%=scope.function_hdp_template_var("hregion_blockmultiplier")%>',
      'hbase.hregion.memstore.flush.size' => '<%=scope.function_hdp_template_var("hregion_memstoreflushsize")%>',
      'hbase.hregion.memstore.mslab.enabled' => '<%=scope.function_hdp_template_var("regionserver_memstore_lab")%>',
      'hbase.hregion.max.filesize' => '<%=scope.function_hdp_template_var("hstorefile_maxsize")%>',
      'hbase.client.scanner.caching' => '<%=scope.function_hdp_template_var("client_scannercaching")%>',
      'zookeeper.session.timeout' => '<%=scope.function_hdp_template_var("zookeeper_sessiontimeout")%>',
      'hbase.client.keyvalue.maxsize' => '<%=scope.function_hdp_template_var("hfile_max_keyvalue_size")%>',
      'hbase.hstore.compactionThreshold' => '<%=scope.function_hdp_template_var("hstore_compactionthreshold")%>',
      'hbase.hstore.blockingStoreFiles' => '<%=scope.function_hdp_template_var("hstore_blockingstorefiles")%>',
      'hfile.block.cache.size' => '<%=scope.function_hdp_template_var("hfile_blockcache_size")%>',
      'hbase.master.keytab.file' => '<%=scope.function_hdp_template_var("keytab_path")%>/hm.service.keytab',
      'hbase.master.kerberos.principal' => 'hm/_HOST@<%=scope.function_hdp_template_var("kerberos_domain")%>',
      'hbase.regionserver.keytab.file' => '<%=scope.function_hdp_template_var("keytab_path")%>/rs.service.keytab',
      'hbase.regionserver.kerberos.principal' => 'rs/_HOST@<%=scope.function_hdp_template_var("kerberos_domain")%>',
      'hbase.superuser' => 'hbase',
      'hbase.coprocessor.region.classes' => '<%=scope.function_hdp_template_var("preloaded_regioncoprocessor_classes")%>',
      'hbase.coprocessor.master.classes' => '<%=scope.function_hdp_template_var("preloaded_mastercoprocessor_classes")%>',
      'hbase.zookeeper.quorum' => '<%=zkh=scope.function_hdp_host("zookeeper_hosts");scope.function_hdp_is_empty(zkh) ? "" : [zkh].flatten.join(",")%>',
      'dfs.support.append' => '<%=scope.function_hdp_template_var("hdfs_support_append")%>',
      'dfs.client.read.shortcircuit' => '<%=scope.function_hdp_template_var("hdfs_enable_shortcircuit_read")%>',
      'dfs.client.read.shortcircuit.skip.checksum' => '<%=scope.function_hdp_template_var("hdfs_enable_shortcircuit_skipchecksum")%>',}
      }

  configgenerator::configfile{'hbase-policy.xml.erb': 
    module => 'hdp-hbase',
    properties => {'security.client.protocol.acl' => '*',
      'security.admin.protocol.acl' => '*',
      'security.masterregion.protocol.acl' => '*',}
      }

  anchor{'hdp-hbase::begin':}
  anchor{'hdp-hbase::end':}

  if ($service_state == 'uninstalled') {
    hdp::package { 'hbase':
      ensure => 'uninstalled'
    }
    hdp::directory { $config_dir:
      service_state => $service_state,
      force => true
    }

    Anchor['hdp-hbase::begin'] -> Hdp::Package['hbase'] -> Hdp::Directory[$config_dir] -> Anchor['hdp-hbase::end']

  } else {  
    hdp::package { 'hbase': }
  
    hdp::user{ $hbase_user:}
 
    hdp::directory { $config_dir: 
      service_state => $service_state,
      force => true
    }

   hdp-hbase::configfile { ['hbase-env.sh','hbase-site.xml','hbase-policy.xml','log4j.properties','hadoop-metrics.properties']: 
      type => $type
    }
    hdp-hbase::configfile { 'regionservers':}
    Anchor['hdp-hbase::begin'] -> Hdp::Package['hbase'] -> Hdp::User[$hbase_user] -> Hdp::Directory[$config_dir] -> 
    Hdp-hbase::Configfile<||> ->  Anchor['hdp-hbase::end']
  }
}

### config files
define hdp-hbase::configfile(
  $mode = undef,
  $hbase_master_host = undef,
  $template_tag = undef,
  $type = undef,
) 
{
  if ($name == 'hadoop-metrics.properties') {
    if ($type == 'master') {
    $tag = GANGLIA-MASTER
  } else {
     $tag = GANGLIA-RS
  }
   } else {
    $tag = $template_tag
}
  hdp::configfile { "${hdp-hbase::params::conf_dir}/${name}":
    component         => 'hbase',
    owner             => $hdp-hbase::params::hbase_user,
    mode              => $mode,
    hbase_master_host => $hbase_master_host,
    template_tag      => $tag
  }
}
