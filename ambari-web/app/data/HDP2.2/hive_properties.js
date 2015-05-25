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

var App = require('app');

var hivePropsByCategory = {

  'Performance': [
    'hive.cbo.enable',
    'hive.optimize.reducededuplication.min.reducer',
    'hive.optimize.reducededuplication',
    'hive.orc.splits.include.file.footer',
    'hive.merge.mapfiles',
    'hive.merge.mapredfiles',
    'hive.merge.tezfiles',
    'hive.merge.smallfiles.avgsize',
    'hive.merge.size.per.task',
    'hive.merge.orcfile.stripe.level',
    'hive.auto.convert.join',
    'hive.auto.convert.join.noconditionaltask',
    'hive.auto.convert.join.noconditionaltask.size',
    'hive.optimize.bucketmapjoin.sortedmerge',
    'hive.tez.smb.number.waves',
    'hive.map.aggr.hash.percentmemory',
    'hive.map.aggr',
    'hive.optimize.sort.dynamic.partition',
    'hive.stats.autogather',
    'hive.stats.fetch.column.stats',
    'hive.vectorized.execution.enabled',
    'hive.vectorized.execution.reduce.enabled',
    'hive.vectorized.groupby.checkinterval',
    'hive.vectorized.groupby.flush.percent',
    'hive.limit.pushdown.memory.usage',
    'hive.optimize.index.filter',
    'hive.exec.reducers.bytes.per.reducer',
    'hive.smbjoin.cache.rows',
    'hive.exec.orc.default.stripe.size',
    'hive.fetch.task.conversion',
    'hive.fetch.task.conversion.threshold',
    'hive.fetch.task.aggr',
    'hive.compute.query.using.stats',
    'hive.tez.auto.reducer.parallelism',
    'hive.tez.max.partition.factor',
    'hive.tez.min.partition.factor',
    'hive.tez.dynamic.partition.pruning',
    'hive.tez.dynamic.partition.pruning.max.event.size',
    'hive.tez.dynamic.partition.pruning.max.data.size'
  ],

  'General': [
    'hive.exec.pre.hooks',
    'hive.exec.post.hooks',
    'hive.exec.failure.hooks',
    'hive.execution.engine',
    'hive.exec.dynamic.partition',
    'hive.exec.dynamic.partition.mode',
    'hive.exec.max.dynamic.partitions',
    'hive.exec.max.dynamic.partitions.pernode',
    'hive.exec.max.created.files',
    'hive.enforce.bucketing',
    'datanucleus.cache.level2.type',
    'hive.metastore.uris',
    'hive.metastore.warehouse.dir',
    'hive.exec.parallel.thread.number',
    'hive.security.authorization.enabled',
    'hive.security.authorization.manager',
    'hive.security.metastore.authenticator.manager',
    'hive.security.metastore.authorization.manager',
    'hive.server2.authentication',
    'hive.server2.enable.doAs',
    'hive.server2.tez.default.queues',
    'hive.server2.tez.initialize.default.sessions',
    'hive.server2.tez.sessions.per.default.queue',
    'hive.server2.thrift.http.path',
    'hive.server2.thrift.http.port',
    'hive.server2.thrift.max.worker.threads',
    'hive.server2.thrift.port',
    'hive.server2.thrift.sasl.qop',
    'hive.server2.transport.mode',
    'hive.server2.use.SSL',
    'hive.tez.container.size',
    'hive.tez.java.opts',
    'hive.tez.log.level',
    'hive.txn.manager',
    'hive.txn.timeout',
    'hive.txn.max.open.batch',
    'hive.compactor.initiator.on',
    'hive.compactor.worker.threads',
    'hive.compactor.worker.timeout',
    'hive.compactor.check.interval',
    'hive.compactor.delta.num.threshold',
    'hive.compactor.delta.pct.threshold'
  ]
};

var hiveProps = [];

for (var category in hivePropsByCategory) {
  hiveProps = hiveProps.concat(App.config.generateConfigPropertiesByName(hivePropsByCategory[category], 
    { category: category, serviceName: 'HIVE', filename: 'hive-site.xml'}));
}

module.exports = hiveProps;

