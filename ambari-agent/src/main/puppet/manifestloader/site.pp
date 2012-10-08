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

configgenerator::configfile::configuration {'hdp_hadoop__mapred_queue_acls':
  filename => 'mapred-queue-acls.xml',
  module => 'hdp-hadoop',
  properties => {'mapred.queue.default.acl-submit-job' => '*',
    'mapred.queue.default.acl-administer-jobs' => '*',}
  }

configgenerator::configfile::configuration {'hdp_hadoop__hadoop_policy':
  filename => 'hadoop-policy.xml',
  module => 'hdp-hadoop',
  properties=> {'security.client.protocol.acl' => '*',
    'security.client.datanode.protocol.acl' => '*',
    'security.datanode.protocol.acl' => '*',
    'security.inter.datanode.protocol.acl' => '*',
    'security.namenode.protocol.acl' => '*',
    'security.inter.tracker.protocol.acl' => '*',
    'security.job.submission.protocol.acl' => '*',
    'security.task.umbilical.protocol.acl' => '*',
    'security.admin.operations.protocol.acl' => '',
    'security.refresh.usertogroups.mappings.protocol.acl' => '',
    'security.refresh.policy.protocol.acl' => '',}
   }

configgenerator::configfile::configuration {'hdp_hadoop__core_site':
  filename => 'core-site.xml',
  module => 'hdp-hadoop',
  properties => {'io.file.buffer.size' => '131072',
    'io.serializations' => 'org.apache.hadoop.io.serializer.WritableSerialization',
    'io.compression.codecs' => '',
    'io.compression.codec.lzo.class' => 'com.hadoop.compression.lzo.LzoCodec',
    'fs.default.name' => '',
    'fs.trash.interval' => '360',
    'fs.checkpoint.dir' => '',
    'fs.checkpoint.edits.dir' => '',
    'fs.checkpoint.period' => '21600',
    'fs.checkpoint.size' => '536870912',
    'ipc.client.idlethreshold' => '8000',
    'ipc.client.connection.maxidletime' => '30000',
    'ipc.client.connect.max.retries' => '50',
    'webinterface.private.actions' => 'false',
    'hadoop.security.authentication' => '',
    'hadoop.security.authorization' => '',
    'hadoop.security.auth_to_local' => '',}
  }

configgenerator::configfile::configuration {'hdp_hadoop__mapred_site':
  filename => 'mapred-site.xml',
  module => 'hdp-hadoop',
  properties => {'io.sort.mb' => '',
    'io.sort.record.percent' => '.2',
    'io.sort.spill.percent' => '',
    'io.sort.factor' => '100',
    'mapred.tasktracker.tasks.sleeptime-before-sigkill' => '250',
    'mapred.job.tracker.handler.count' => '50',
    'mapred.system.dir' => '',
    'mapred.job.tracker' => '',
    'mapred.job.tracker.http.address' => '',
    'mapred.local.dir' => '',
    'mapreduce.cluster.administrators' => ' hadoop',
    'mapred.reduce.parallel.copies' => '30',
    'mapred.tasktracker.map.tasks.maximum' => '',
    'mapred.tasktracker.reduce.tasks.maximum' => '',
    'tasktracker.http.threads' => '50',
    'mapred.map.tasks.speculative.execution' => 'false',
    'mapred.reduce.tasks.speculative.execution' => 'false',
    'mapred.reduce.slowstart.completed.maps' => '0.05',
    'mapred.inmem.merge.threshold' => '1000',
    'mapred.job.shuffle.merge.percent' => '0.66',
    'mapred.job.shuffle.input.buffer.percent'  => '0.7',
    'mapred.map.output.compression.codec' => '',
    'mapred.output.compression.type' => 'BLOCK',
    'mapred.jobtracker.completeuserjobs.maximum' => '0',
    'mapred.jobtracker.taskScheduler' => '',
    'mapred.jobtracker.restart.recover' => 'false',
    'mapred.job.reduce.input.buffer.percent' => '0.0',
    'mapreduce.reduce.input.limit' => '10737418240',
    'mapred.compress.map.output' => '',
    'mapred.task.timeout' => '600000',
    'jetty.connector' => 'org.mortbay.jetty.nio.SelectChannelConnector',
    'mapred.task.tracker.task-controller' => '',
    'mapred.child.root.logger' => 'INFO,TLA',
    'mapred.child.java.opts' => '',
    'mapred.cluster.map.memory.mb' => '',
    'mapred.cluster.reduce.memory.mb' => '',
    'mapred.job.map.memory.mb' => '',
    'mapred.job.reduce.memory.mb' => '',
    'mapred.cluster.max.map.memory.mb' => '',
    'mapred.cluster.max.reduce.memory.mb' => '',
    'mapred.hosts' => '',
    'mapred.hosts.exclude' => '',
    'mapred.max.tracker.blacklists' => '16',
    'mapred.healthChecker.script.path' => '',
    'mapred.healthChecker.interval' => '135000',
    'mapred.healthChecker.script.timeout' => '60000',
    'mapred.job.tracker.persist.jobstatus.active' => 'false',
    'mapred.job.tracker.persist.jobstatus.hours' => '1',
    'mapred.job.tracker.persist.jobstatus.dir' => '',
    'mapred.jobtracker.retirejob.check' => '10000',
    'mapred.jobtracker.retirejob.interval' => '0',
    'mapred.job.tracker.history.completed.location' => '/mapred/history/done',
    'mapred.task.maxvmem' => '',
    'mapred.jobtracker.maxtasks.per.job' => '',
    'mapreduce.fileoutputcommitter.marksuccessfuljobs' => 'false',
    'mapred.userlog.retain.hours' => '',
    'mapred.job.reuse.jvm.num.tasks' => '1',
    'mapreduce.jobtracker.kerberos.principal' => '',
    'mapreduce.tasktracker.kerberos.principal' => '',
    'hadoop.job.history.user.location' => 'none',
    'mapreduce.jobtracker.keytab.file' => '',
    'mapreduce.tasktracker.keytab.file' => '',
    'mapreduce.jobtracker.staging.root.dir' => '/user',
    'mapreduce.tasktracker.group' => 'hadoop',
    'mapreduce.jobtracker.split.metainfo.maxsize' => '50000000',
    'mapreduce.history.server.embedded' => 'false',
    'mapreduce.history.server.http.address' => '',
    'mapreduce.jobhistory.kerberos.principal' => '',
    'mapreduce.jobhistory.keytab.file' => '',
    'mapred.jobtracker.blacklist.fault-timeout-window' => '180',
    'mapred.jobtracker.blacklist.fault-bucket-width' => '15',
    'mapred.queue.names' => 'default',}
  }

configgenerator::configfile::configuration {'hdp_hadoop__capacity_scheduler':
  filename => 'capacity-scheduler.xml',
  module => 'hdp-hadoop',
  properties => {'mapred.capacity-scheduler.queue.default.capacity' => '100',
    'mapred.capacity-scheduler.queue.default.supports-priority' => 'false',
    'mapred.capacity-scheduler.queue.default.minimum-user-limit-percent' => '100',
    'mapred.capacity-scheduler.queue.default.maximum-initialized-jobs-per-user' => '25',}
  }

configgenerator::configfile::configuration {'hdp_hadoop__hdfs_site':
  filename => 'hdfs-site.xml',
  module => 'hdp-hadoop',
  properties => {'dfs.name.dir' => '',
    'dfs.support.append' => '',
    'dfs.webhdfs.enabled' => '',
    'dfs.datanode.failed.volumes.tolerated' => '',
    'dfs.block.local-path-access.user' => '',
    'dfs.data.dir' => '',
    'dfs.hosts.exclude' => '',
    'dfs.hosts' => '',
    'dfs.replication.max' => '50',
    'dfs.replication' => '',
    'dfs.heartbeat.interval' => '3',
    'dfs.safemode.threshold.pct' => '1.0f',
    'dfs.balance.bandwidthPerSec' => '6250000',
    'dfs.datanode.address' => '',
    'dfs.datanode.http.address' => '',
    'dfs.block.size' => '134217728',
    'dfs.http.address' => '',
    'dfs.datanode.du.reserved' => '',
    'dfs.datanode.ipc.address' => '0.0.0.0:8010',
    'dfs.blockreport.initialDelay' => '120',
    'dfs.datanode.du.pct' => '0.85f',
    'dfs.namenode.handler.count' => '40',
    'dfs.datanode.max.xcievers' => '1024',
    'dfs.umaskmode' => '077',
    'dfs.web.ugi' => 'gopher,gopher',
    'dfs.permissions' => 'true',
    'dfs.permissions.supergroup' => 'hdfs',
    'dfs.namenode.handler.count' => '100',
    'ipc.server.max.response.size' => '5242880',
    'dfs.block.access.token.enable' => 'true',
    'dfs.namenode.kerberos.principal' => '',
    'dfs.secondary.namenode.kerberos.principal' => '',
    'dfs.namenode.kerberos.https.principal' => '',
    'dfs.secondary.namenode.kerberos.https.principal' => '',
    'dfs.secondary.http.address' => '',
    'dfs.secondary.https.port' => '50490',
    'dfs.web.authentication.kerberos.principal' => '',
    'dfs.web.authentication.kerberos.keytab' => '',
    'dfs.datanode.kerberos.principal' => '',
    'dfs.namenode.keytab.file' => '',
    'dfs.secondary.namenode.keytab.file' => '',
    'dfs.datanode.keytab.file' => '',
    'dfs.https.port' => '50470',
    'dfs.https.address' => '',
    'dfs.datanode.data.dir.perm' => '',
    'dfs.access.time.precision' => '0',
    'dfs.cluster.administrators' => ' hdfs',
    'ipc.server.read.threadpool.size' => '5',
    'dfs.namenode.kerberos.internal.spnego.principal' => '',
    'dfs.secondary.namenode.kerberos.internal.spnego.principal' => '',}
  }

configgenerator::configfile::configuration {'hdp_hcat_old__hive_site':
  filename => 'hive-site.xml',
  module => 'hdp-hcat-old',
  properties => {'hive.metastore.local' => 'false',
    'javax.jdo.option.ConnectionURL' => '',
    'javax.jdo.option.ConnectionDriverName' => 'com.mysql.jdbc.Driver',
    'javax.jdo.option.ConnectionUserName' => '',
    'javax.jdo.option.ConnectionPassword' => '',
    'hive.metastore.warehouse.dir' => '/apps/hive/warehouse',
    'hive.metastore.sasl.enabled' => '',
    'hive.metastore.kerberos.keytab.file'  => '',
    'hive.metastore.kerberos.principal' => '',
    'hive.metastore.cache.pinobjtypes' => 'Table,Database,Type,FieldSchema,Order',
    'hive.metastore.uris' => '',
    'hive.semantic.analyzer.factory.impl' => 'org.apache.hcatalog.cli.HCatSemanticAnalyzerFactory',
    'hadoop.clientside.fs.operations',
    'hive.metastore.client.socket.timeout' => '60',
    'hive.metastore.execute.setugi' => 'true',}
  }

configgenerator::configfile::configuration {'hdp_hive__hive_site':
  filename => 'hive-site.xml',
  module => 'hdp-hive',
  properties => {'hive.metastore.local' => 'false',
    'javax.jdo.option.ConnectionURL' => '',
    'javax.jdo.option.ConnectionDriverName' => 'com.mysql.jdbc.Driver',
    'javax.jdo.option.ConnectionUserName' => '',
    'javax.jdo.option.ConnectionPassword' => '',
    'hive.metastore.warehouse.dir' => '/apps/hive/warehouse',
    'hive.metastore.sasl.enabled' => '',
    'hive.metastore.kerberos.keytab.file' => '',
    'hive.metastore.kerberos.principal' => '',
    'hive.metastore.cache.pinobjtypes' => 'Table,Database,Type,FieldSchema,Order',
    'hive.metastore.uris' => '',
    'hive.semantic.analyzer.factory.impl' => 'org.apache.hivealog.cli.HCatSemanticAnalyzerFactory',
    'hadoop.clientside.fs.operations' => 'true',
    'hive.metastore.client.socket.timeout' => '60',
    'hive.metastore.execute.setugi' => 'true',
    'hive.security.authorization.enabled' => 'true',
    'hive.security.authorization.manager' => 'org.apache.hcatalog.security.HdfsAuthorizationProvider',}
  }
		
configgenerator::configfile::configuration {'hdp_oozie__oozie_site':
  filename => 'oozie-site.xml',
  module => 'hdp-oozie',
  properties => {'oozie.base.url' => '',
    'oozie.system.id' => '',
    'oozie.systemmode' => 'NORMAL',
    'oozie.service.AuthorizationService.security.enabled' => 'true',
    'oozie.service.PurgeService.older.than' => '30',
    'oozie.service.PurgeService.purge.interval' => '3600',
    'oozie.service.CallableQueueService.queue.size' => '1000',
    'oozie.service.CallableQueueService.threads' => '10',
    'oozie.service.CallableQueueService.callable.concurrency' => '3',
    'oozie.service.coord.normal.default.timeout' => '120',
    'oozie.db.schema.name' => 'oozie',
    'oozie.service.StoreService.create.db.schema' => 'true',
    'oozie.service.StoreService.jdbc.driver' => 'org.apache.derby.jdbc.EmbeddedDriver',
    'oozie.service.StoreService.jdbc.url' => '',
    'oozie.service.StoreService.jdbc.username' => 'sa',
    'oozie.service.StoreService.jdbc.password' => ' ',
    'oozie.service.StoreService.pool.max.active.conn' => '10',
    'oozie.service.HadoopAccessorService.kerberos.enabled' => '',
    'local.realm' => '',
    'oozie.service.HadoopAccessorService.keytab.file' => '',
    'oozie.service.HadoopAccessorService.kerberos.principal' => '',
    'oozie.service.HadoopAccessorService.jobTracker.whitelist' => ' ',
    'oozie.authentication.type' => '',
    'oozie.authentication.kerberos.principal' => '',
    'oozie.authentication.kerberos.keytab' => '',
    'oozie.service.HadoopAccessorService.nameNode.whitelist' => ' ',
    'oozie.service.WorkflowAppService.system.libpath' => '',
    'use.system.libpath.for.mapreduce.and.pig.jobs' => 'false',
    'oozie.authentication.kerberos.name.rules' => '',}
  }

configgenerator::configfile::configuration {'hdp_templeton__templeton_site':
  filename => 'templeton-site.xml',
  module => 'hdp-templeton',
  configuration => {'templeton.port' => '50111',
    'templeton.hadoop.conf.dir' => '',
    'templeton.jar' => '',
    'templeton.libjars' => '',
    'templeton.hadoop' => '',
    'templeton.pig.archive' => '',
    'templeton.pig.path' => '',
    'templeton.hcat' => '',
    'templeton.hive.archive' => '',
    'templeton.hive.path' => '',
    'templeton.hive.properties' => '',
    'templeton.zookeeper.hosts' => '',
    'templeton.storage.class' => 'org.apache.hcatalog.templeton.tool.ZooKeeperStorage',
    'templeton.override.enabled' => 'false',
    'templeton.streaming.jar' => 'hdfs:///apps/templeton/hadoop-streaming.jar',
    'templeton.kerberos.principal' => '',
    'templeton.kerberos.keytab' => '',
    'templeton.kerberos.secret' => 'secret',}
  }
    
class manifestloader () {
    file { '/etc/puppet/agent/modules.tgz':
      ensure => present,
      source => "puppet:///modules/catalog/modules.tgz",  
      mode => '0755',
    }

    exec { 'untar_modules':
      command => "rm -rf /etc/puppet/agent/modules ; tar zxf /etc/puppet/agent/modules.tgz -C /etc/puppet/agent/ --strip-components 3",
      path    => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'
    } 

    exec { 'puppet_apply':
      command   => "sh /etc/puppet/agent/modules/puppetApply.sh",
      timeout   => 1800,
      path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
      logoutput => "true"
    }

    File['/etc/puppet/agent/modules.tgz'] -> Exec['untar_modules'] -> Exec['puppet_apply']
}

node default {
 stage{1 :}
 class {'manifestloader': stage => 1}
}

