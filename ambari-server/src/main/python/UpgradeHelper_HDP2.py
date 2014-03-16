#!/usr/bin/env python

'''
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

import optparse
from pprint import pprint
import sys
import datetime
import os.path
import logging
import shutil
import json
import subprocess
import time


# action commands
GET_MR_MAPPING_ACTION = "save-mr-mapping"
DELETE_MR_ACTION = "delete-mr"
ADD_YARN_MR2_ACTION = "add-yarn-mr2"
MODIFY_CONFIG_ACTION = "update-configs"
BACKUP_CONFIG_ACTION = "backup-configs"
INSTALL_YARN_MR2_ACTION = "install-yarn-mr2"
VALID_ACTIONS = ', '.join([GET_MR_MAPPING_ACTION, DELETE_MR_ACTION, ADD_YARN_MR2_ACTION, MODIFY_CONFIG_ACTION,
                           INSTALL_YARN_MR2_ACTION, BACKUP_CONFIG_ACTION])

MR_MAPPING_FILE = "mr_mapping"
UPGRADE_LOG_FILE = "upgrade_log"
CAPACITY_SCHEDULER_TAG = "capacity-scheduler"
MAPRED_SITE_TAG = "mapred-site"
GLOBAL_TAG = "global"
HDFS_SITE_TAG = "hdfs-site"
CORE_SITE_TAG = "core-site"
YARN_SITE_TAG = "yarn-site"
HBASE_SITE_TAG = "hbase-site"
HIVE_SITE_TAG = "hive-site"
REPLACE_JH_HOST_NAME_TAG = "REPLACE_JH_HOST"
REPLACE_RM_HOST_NAME_TAG = "REPLACE_RM_HOST"
REPLACE_WITH_TAG = "REPLACE_WITH_"
DELETE_OLD_TAG = "DELETE_OLD"

AUTH_FORMAT = '{0}:{1}'
URL_FORMAT = 'http://{0}:8080/api/v1/clusters/{1}'

logger = logging.getLogger()

# old : new
PROPERTY_MAPPING = {
  "create.empty.dir.if.nonexist": "mapreduce.jobcontrol.createdir.ifnotexist",
  "dfs.access.time.precision": "dfs.namenode.accesstime.precision",
  "dfs.backup.address": "dfs.namenode.backup.address",
  "dfs.backup.http.address": "dfs.namenode.backup.http-address",
  "dfs.balance.bandwidthPerSec": "dfs.datanode.balance.bandwidthPerSec",
  "dfs.block.size": "dfs.blocksize",
  "dfs.data.dir": "dfs.datanode.data.dir",
  "dfs.datanode.max.xcievers": "dfs.datanode.max.transfer.threads",
  "dfs.df.interval": "fs.df.interval",
  "dfs.federation.nameservice.id": "dfs.nameservice.id",
  "dfs.federation.nameservices": "dfs.nameservices",
  "dfs.http.address": "dfs.namenode.http-address",
  "dfs.https.address": "dfs.namenode.https-address",
  "dfs.https.client.keystore.resource": "dfs.client.https.keystore.resource",
  "dfs.https.need.client.auth": "dfs.client.https.need-auth",
  "dfs.max.objects": "dfs.namenode.max.objects",
  "dfs.max-repl-streams": "dfs.namenode.replication.max-streams",
  "dfs.name.dir": "dfs.namenode.name.dir",
  "dfs.name.dir.restore": "dfs.namenode.name.dir.restore",
  "dfs.name.edits.dir": "dfs.namenode.edits.dir",
  "dfs.permissions": "dfs.permissions.enabled",
  "dfs.permissions.supergroup": "dfs.permissions.superusergroup",
  "dfs.read.prefetch.size": "dfs.client.read.prefetch.size",
  "dfs.replication.considerLoad": "dfs.namenode.replication.considerLoad",
  "dfs.replication.interval": "dfs.namenode.replication.interval",
  "dfs.replication.min": "dfs.namenode.replication.min",
  "dfs.replication.pending.timeout.sec": "dfs.namenode.replication.pending.timeout-sec",
  "dfs.safemode.extension": "dfs.namenode.safemode.extension",
  "dfs.safemode.threshold.pct": "dfs.namenode.safemode.threshold-pct",
  "dfs.secondary.http.address": "dfs.namenode.secondary.http-address",
  "dfs.socket.timeout": "dfs.client.socket-timeout",
  "dfs.umaskmode": "fs.permissions.umask-mode",
  "dfs.write.packet.size": "dfs.client-write-packet-size",
  "fs.checkpoint.dir": "dfs.namenode.checkpoint.dir",
  "fs.checkpoint.edits.dir": "dfs.namenode.checkpoint.edits.dir",
  "fs.checkpoint.period": "dfs.namenode.checkpoint.period",
  "fs.default.name": "fs.defaultFS",
  "hadoop.configured.node.mapping": "net.topology.configured.node.mapping",
  "hadoop.job.history.location": "mapreduce.jobtracker.jobhistory.location",
  "hadoop.native.lib": "io.native.lib.available",
  "hadoop.net.static.resolutions": "mapreduce.tasktracker.net.static.resolutions",
  "hadoop.pipes.command-file.keep": "mapreduce.pipes.commandfile.preserve",
  "hadoop.pipes.executable.interpretor": "mapreduce.pipes.executable.interpretor",
  "hadoop.pipes.executable": "mapreduce.pipes.executable",
  "hadoop.pipes.java.mapper": "mapreduce.pipes.isjavamapper",
  "hadoop.pipes.java.recordreader": "mapreduce.pipes.isjavarecordreader",
  "hadoop.pipes.java.recordwriter": "mapreduce.pipes.isjavarecordwriter",
  "hadoop.pipes.java.reducer": "mapreduce.pipes.isjavareducer",
  "hadoop.pipes.partitioner": "mapreduce.pipes.partitioner",
  "heartbeat.recheck.interval": "dfs.namenode.heartbeat.recheck-interval",
  "io.bytes.per.checksum": "dfs.bytes-per-checksum",
  "io.sort.factor": "mapreduce.task.io.sort.factor",
  "io.sort.mb": "mapreduce.task.io.sort.mb",
  "io.sort.spill.percent": "mapreduce.map.sort.spill.percent",
  "jobclient.completion.poll.interval": "mapreduce.client.completion.pollinterval",
  "jobclient.output.filter": "mapreduce.client.output.filter",
  "jobclient.progress.monitor.poll.interval": "mapreduce.client.progressmonitor.pollinterval",
  "job.end.notification.url": "mapreduce.job.end-notification.url",
  "job.end.retry.attempts": "mapreduce.job.end-notification.retry.attempts",
  "job.end.retry.interval": "mapreduce.job.end-notification.retry.interval",
  "job.local.dir": "mapreduce.job.local.dir",
  "keep.failed.task.files": "mapreduce.task.files.preserve.failedtasks",
  "keep.task.files.pattern": "mapreduce.task.files.preserve.filepattern",
  "key.value.separator.in.input.line": "mapreduce.input.keyvaluelinerecordreader.key.value.separator",
  "local.cache.size": "mapreduce.tasktracker.cache.local.size",
  "map.input.file": "mapreduce.map.input.file",
  "map.input.length": "mapreduce.map.input.length",
  "map.input.start": "mapreduce.map.input.start",
  "map.output.key.field.separator": "mapreduce.map.output.key.field.separator",
  "map.output.key.value.fields.spec": "mapreduce.fieldsel.map.output.key.value.fields.spec",
  "mapred.acls.enabled": "mapreduce.cluster.acls.enabled",
  "mapred.binary.partitioner.left.offset": "mapreduce.partition.binarypartitioner.left.offset",
  "mapred.binary.partitioner.right.offset": "mapreduce.partition.binarypartitioner.right.offset",
  "mapred.cache.archives": "mapreduce.job.cache.archives",
  "mapred.cache.archives.timestamps": "mapreduce.job.cache.archives.timestamps",
  "mapred.cache.files": "mapreduce.job.cache.files",
  "mapred.cache.files.timestamps": "mapreduce.job.cache.files.timestamps",
  "mapred.cache.localArchives": "mapreduce.job.cache.local.archives",
  "mapred.cache.localFiles": "mapreduce.job.cache.local.files",
  "mapred.child.tmp": "mapreduce.task.tmp.dir",
  "mapred.cluster.average.blacklist.threshold": "mapreduce.jobtracker.blacklist.average.threshold",
  "mapred.cluster.map.memory.mb": "mapreduce.cluster.mapmemory.mb",
  "mapred.cluster.max.map.memory.mb": "mapreduce.jobtracker.maxmapmemory.mb",
  "mapred.cluster.max.reduce.memory.mb": "mapreduce.jobtracker.maxreducememory.mb",
  "mapred.cluster.reduce.memory.mb": "mapreduce.cluster.reducememory.mb",
  "mapred.committer.job.setup.cleanup.needed": "mapreduce.job.committer.setup.cleanup.needed",
  "mapred.compress.map.output": "mapreduce.map.output.compress",
  "mapred.data.field.separator": "mapreduce.fieldsel.data.field.separator",
  "mapred.debug.out.lines": "mapreduce.task.debugout.lines",
  "mapred.healthChecker.interval": "mapreduce.tasktracker.healthchecker.interval",
  "mapred.healthChecker.script.args": "mapreduce.tasktracker.healthchecker.script.args",
  "mapred.healthChecker.script.path": "mapreduce.tasktracker.healthchecker.script.path",
  "mapred.healthChecker.script.timeout": "mapreduce.tasktracker.healthchecker.script.timeout",
  "mapred.heartbeats.in.second": "mapreduce.jobtracker.heartbeats.in.second",
  "mapred.hosts.exclude": "mapreduce.jobtracker.hosts.exclude.filename",
  "mapred.hosts": "mapreduce.jobtracker.hosts.filename",
  "mapred.inmem.merge.threshold": "mapreduce.reduce.merge.inmem.threshold",
  "mapred.input.dir.formats": "mapreduce.input.multipleinputs.dir.formats",
  "mapred.input.dir.mappers": "mapreduce.input.multipleinputs.dir.mappers",
  "mapred.input.dir": "mapreduce.input.fileinputformat.inputdir",
  "mapred.input.pathFilter.class": "mapreduce.input.pathFilter.class",
  "mapred.jar": "mapreduce.job.jar",
  "mapred.job.classpath.archives": "mapreduce.job.classpath.archives",
  "mapred.job.classpath.files": "mapreduce.job.classpath.files",
  "mapred.job.id": "mapreduce.job.id",
  "mapred.jobinit.threads": "mapreduce.jobtracker.jobinit.threads",
  "mapred.job.map.memory.mb": "mapreduce.map.memory.mb",
  "mapred.job.name": "mapreduce.job.name",
  "mapred.job.priority": "mapreduce.job.priority",
  "mapred.job.queue.name": "mapreduce.job.queuename",
  "mapred.job.reduce.input.buffer.percent": "mapreduce.reduce.input.buffer.percent",
  "mapred.job.reduce.markreset.buffer.percent": "mapreduce.reduce.markreset.buffer.percent",
  "mapred.job.reduce.memory.mb": "mapreduce.reduce.memory.mb",
  "mapred.job.reduce.total.mem.bytes": "mapreduce.reduce.memory.totalbytes",
  "mapred.job.reuse.jvm.num.tasks": "mapreduce.job.jvm.numtasks",
  "mapred.job.shuffle.input.buffer.percent": "mapreduce.reduce.shuffle.input.buffer.percent",
  "mapred.job.shuffle.merge.percent": "mapreduce.reduce.shuffle.merge.percent",
  "mapred.job.tracker.handler.count": "mapreduce.jobtracker.handler.count",
  "mapred.job.tracker.history.completed.location": "mapreduce.jobtracker.jobhistory.completed.location",
  "mapred.job.tracker.http.address": "mapreduce.jobtracker.http.address",
  "mapred.jobtracker.instrumentation": "mapreduce.jobtracker.instrumentation",
  "mapred.jobtracker.job.history.block.size": "mapreduce.jobtracker.jobhistory.block.size",
  "mapred.job.tracker.jobhistory.lru.cache.size": "mapreduce.jobtracker.jobhistory.lru.cache.size",
  "mapred.job.tracker": "mapreduce.jobtracker.address",
  "mapred.jobtracker.maxtasks.per.job": "mapreduce.jobtracker.maxtasks.perjob",
  "mapred.job.tracker.persist.jobstatus.active": "mapreduce.jobtracker.persist.jobstatus.active",
  "mapred.job.tracker.persist.jobstatus.dir": "mapreduce.jobtracker.persist.jobstatus.dir",
  "mapred.job.tracker.persist.jobstatus.hours": "mapreduce.jobtracker.persist.jobstatus.hours",
  "mapred.jobtracker.restart.recover": "mapreduce.jobtracker.restart.recover",
  "mapred.job.tracker.retiredjobs.cache.size": "mapreduce.jobtracker.retiredjobs.cache.size",
  "mapred.job.tracker.retire.jobs": "mapreduce.jobtracker.retirejobs",
  "mapred.jobtracker.taskalloc.capacitypad": "mapreduce.jobtracker.taskscheduler.taskalloc.capacitypad",
  "mapred.jobtracker.taskScheduler": "mapreduce.jobtracker.taskscheduler",
  "mapred.jobtracker.taskScheduler.maxRunningTasksPerJob": "mapreduce.jobtracker.taskscheduler.maxrunningtasks.perjob",
  "mapred.join.expr": "mapreduce.join.expr",
  "mapred.join.keycomparator": "mapreduce.join.keycomparator",
  "mapred.lazy.output.format": "mapreduce.output.lazyoutputformat.outputformat",
  "mapred.line.input.format.linespermap": "mapreduce.input.lineinputformat.linespermap",
  "mapred.linerecordreader.maxlength": "mapreduce.input.linerecordreader.line.maxlength",
  "mapred.local.dir": "mapreduce.cluster.local.dir",
  "mapred.local.dir.minspacekill": "mapreduce.tasktracker.local.dir.minspacekill",
  "mapred.local.dir.minspacestart": "mapreduce.tasktracker.local.dir.minspacestart",
  "mapred.map.child.env": "mapreduce.map.env",
  "mapred.map.child.java.opts": "mapreduce.map.java.opts",
  "mapred.map.child.log.level": "mapreduce.map.log.level",
  "mapred.map.max.attempts": "mapreduce.map.maxattempts",
  "mapred.map.output.compression.codec": "mapreduce.map.output.compress.codec",
  "mapred.mapoutput.key.class": "mapreduce.map.output.key.class",
  "mapred.mapoutput.value.class": "mapreduce.map.output.value.class",
  "mapred.mapper.regex.group": "mapreduce.mapper.regexmapper..group",
  "mapred.mapper.regex": "mapreduce.mapper.regex",
  "mapred.map.task.debug.script": "mapreduce.map.debug.script",
  "mapred.map.tasks": "mapreduce.job.maps",
  "mapred.map.tasks.speculative.execution": "mapreduce.map.speculative",
  "mapred.max.map.failures.percent": "mapreduce.map.failures.maxpercent",
  "mapred.max.reduce.failures.percent": "mapreduce.reduce.failures.maxpercent",
  "mapred.max.split.size": "mapreduce.input.fileinputformat.split.maxsize",
  "mapred.max.tracker.blacklists": "mapreduce.jobtracker.tasktracker.maxblacklists",
  "mapred.max.tracker.failures": "mapreduce.job.maxtaskfailures.per.tracker",
  "mapred.merge.recordsBeforeProgress": "mapreduce.task.merge.progress.records",
  "mapred.min.split.size": "mapreduce.input.fileinputformat.split.minsize",
  "mapred.min.split.size.per.node": "mapreduce.input.fileinputformat.split.minsize.per.node",
  "mapred.min.split.size.per.rack": "mapreduce.input.fileinputformat.split.minsize.per.rack",
  "mapred.output.compression.codec": "mapreduce.output.fileoutputformat.compress.codec",
  "mapred.output.compression.type": "mapreduce.output.fileoutputformat.compress.type",
  "mapred.output.compress": "mapreduce.output.fileoutputformat.compress",
  "mapred.output.dir": "mapreduce.output.fileoutputformat.outputdir",
  "mapred.output.key.class": "mapreduce.job.output.key.class",
  "mapred.output.key.comparator.class": "mapreduce.job.output.key.comparator.class",
  "mapred.output.value.class": "mapreduce.job.output.value.class",
  "mapred.output.value.groupfn.class": "mapreduce.job.output.group.comparator.class",
  "mapred.permissions.supergroup": "mapreduce.cluster.permissions.supergroup",
  "mapred.pipes.user.inputformat": "mapreduce.pipes.inputformat",
  "mapred.reduce.child.env": "mapreduce.reduce.env",
  "mapred.reduce.child.java.opts": "mapreduce.reduce.java.opts",
  "mapred.reduce.child.log.level": "mapreduce.reduce.log.level",
  "mapred.reduce.max.attempts": "mapreduce.reduce.maxattempts",
  "mapred.reduce.parallel.copies": "mapreduce.reduce.shuffle.parallelcopies",
  "mapred.reduce.slowstart.completed.maps": "mapreduce.job.reduce.slowstart.completedmaps",
  "mapred.reduce.task.debug.script": "mapreduce.reduce.debug.script",
  "mapred.reduce.tasks": "mapreduce.job.reduces",
  "mapred.reduce.tasks.speculative.execution": "mapreduce.reduce.speculative",
  "mapred.seqbinary.output.key.class": "mapreduce.output.seqbinaryoutputformat.key.class",
  "mapred.seqbinary.output.value.class": "mapreduce.output.seqbinaryoutputformat.value.class",
  "mapred.shuffle.connect.timeout": "mapreduce.reduce.shuffle.connect.timeout",
  "mapred.shuffle.read.timeout": "mapreduce.reduce.shuffle.read.timeout",
  "mapred.skip.attempts.to.start.skipping": "mapreduce.task.skip.start.attempts",
  "mapred.skip.map.auto.incr.proc.count": "mapreduce.map.skip.proc-count.auto-incr",
  "mapred.skip.map.max.skip.records": "mapreduce.map.skip.maxrecords",
  "mapred.skip.on": "mapreduce.job.skiprecords",
  "mapred.skip.out.dir": "mapreduce.job.skip.outdir",
  "mapred.skip.reduce.auto.incr.proc.count": "mapreduce.reduce.skip.proc-count.auto-incr",
  "mapred.skip.reduce.max.skip.groups": "mapreduce.reduce.skip.maxgroups",
  "mapred.speculative.execution.slowNodeThreshold": "mapreduce.job.speculative.slownodethreshold",
  "mapred.speculative.execution.slowTaskThreshold": "mapreduce.job.speculative.slowtaskthreshold",
  "mapred.speculative.execution.speculativeCap": "mapreduce.job.speculative.speculativecap",
  "mapred.submit.replication": "mapreduce.client.submit.file.replication",
  "mapred.system.dir": "mapreduce.jobtracker.system.dir",
  "mapred.task.cache.levels": "mapreduce.jobtracker.taskcache.levels",
  "mapred.task.id": "mapreduce.task.attempt.id",
  "mapred.task.is.map": "mapreduce.task.ismap",
  "mapred.task.partition": "mapreduce.task.partition",
  "mapred.task.profile": "mapreduce.task.profile",
  "mapred.task.profile.maps": "mapreduce.task.profile.maps",
  "mapred.task.profile.params": "mapreduce.task.profile.params",
  "mapred.task.profile.reduces": "mapreduce.task.profile.reduces",
  "mapred.task.timeout": "mapreduce.task.timeout",
  "mapred.tasktracker.dns.interface": "mapreduce.tasktracker.dns.interface",
  "mapred.tasktracker.dns.nameserver": "mapreduce.tasktracker.dns.nameserver",
  "mapred.tasktracker.events.batchsize": "mapreduce.tasktracker.events.batchsize",
  "mapred.tasktracker.expiry.interval": "mapreduce.jobtracker.expire.trackers.interval",
  "mapred.task.tracker.http.address": "mapreduce.tasktracker.http.address",
  "mapred.tasktracker.indexcache.mb": "mapreduce.tasktracker.indexcache.mb",
  "mapred.tasktracker.instrumentation": "mapreduce.tasktracker.instrumentation",
  "mapred.tasktracker.map.tasks.maximum": "mapreduce.tasktracker.map.tasks.maximum",
  "mapred.tasktracker.memory_calculator_plugin": "mapreduce.tasktracker.resourcecalculatorplugin",
  "mapred.tasktracker.memorycalculatorplugin": "mapreduce.tasktracker.resourcecalculatorplugin",
  "mapred.tasktracker.reduce.tasks.maximum": "mapreduce.tasktracker.reduce.tasks.maximum",
  "mapred.task.tracker.report.address": "mapreduce.tasktracker.report.address",
  "mapred.task.tracker.task-controller": "mapreduce.tasktracker.taskcontroller",
  "mapred.tasktracker.taskmemorymanager.monitoring-interval": "mapreduce.tasktracker.taskmemorymanager.monitoringinterval",
  "mapred.tasktracker.tasks.sleeptime-before-sigkill": "mapreduce.tasktracker.tasks.sleeptimebeforesigkill",
  "mapred.temp.dir": "mapreduce.cluster.temp.dir",
  "mapred.text.key.comparator.options": "mapreduce.partition.keycomparator.options",
  "mapred.text.key.partitioner.options": "mapreduce.partition.keypartitioner.options",
  "mapred.textoutputformat.separator": "mapreduce.output.textoutputformat.separator",
  "mapred.tip.id": "mapreduce.task.id",
  "mapreduce.combine.class": "mapreduce.job.combine.class",
  "mapreduce.inputformat.class": "mapreduce.job.inputformat.class",
  "mapreduce.job.counters.limit": "mapreduce.job.counters.max",
  "mapreduce.jobtracker.permissions.supergroup": "mapreduce.cluster.permissions.supergroup",
  "mapreduce.map.class": "mapreduce.job.map.class",
  "mapreduce.outputformat.class": "mapreduce.job.outputformat.class",
  "mapreduce.partitioner.class": "mapreduce.job.partitioner.class",
  "mapreduce.reduce.class": "mapreduce.job.reduce.class",
  "mapred.used.genericoptionsparser": "mapreduce.client.genericoptionsparser.used",
  "mapred.userlog.limit.kb": "mapreduce.task.userlog.limit.kb",
  "mapred.userlog.retain.hours": "mapreduce.job.userlog.retain.hours",
  "mapred.working.dir": "mapreduce.job.working.dir",
  "mapred.work.output.dir": "mapreduce.task.output.dir",
  "min.num.spills.for.combine": "mapreduce.map.combine.minspills",
  "reduce.output.key.value.fields.spec": "mapreduce.fieldsel.reduce.output.key.value.fields.spec",
  "security.job.submission.protocol.acl": "security.job.client.protocol.acl",
  "security.task.umbilical.protocol.acl": "security.job.task.protocol.acl",
  "sequencefile.filter.class": "mapreduce.input.sequencefileinputfilter.class",
  "sequencefile.filter.frequency": "mapreduce.input.sequencefileinputfilter.frequency",
  "sequencefile.filter.regex": "mapreduce.input.sequencefileinputfilter.regex",
  "session.id": "dfs.metrics.session-id",
  "slave.host.name": "dfs.datanode.hostname",
  "slave.host.name": "mapreduce.tasktracker.host.name",
  "tasktracker.contention.tracking": "mapreduce.tasktracker.contention.tracking",
  "tasktracker.http.threads": "mapreduce.tasktracker.http.threads",
  "topology.node.switch.mapping.impl": "net.topology.node.switch.mapping.impl",
  "topology.script.file.name": "net.topology.script.file.name",
  "topology.script.number.args": "net.topology.script.number.args",
  "user.name": "mapreduce.job.user.name",
  "webinterface.private.actions": "mapreduce.jobtracker.webinterface.trusted"
}

CAPACITY_SCHEDULER = {
  "yarn.scheduler.capacity.maximum-am-resource-percent": "0.2",
  "yarn.scheduler.capacity.maximum-applications": "10000",
  "yarn.scheduler.capacity.root.acl_administer_queues": "*",
  "yarn.scheduler.capacity.root.capacity": "100",
  "yarn.scheduler.capacity.root.default.acl_administer_jobs": "*",
  "yarn.scheduler.capacity.root.default.acl_submit_jobs": "*",
  "yarn.scheduler.capacity.root.default.capacity": "100",
  "yarn.scheduler.capacity.root.default.maximum-capacity": "100",
  "yarn.scheduler.capacity.root.default.state": "RUNNING",
  "yarn.scheduler.capacity.root.default.user-limit-factor": "1",
  "yarn.scheduler.capacity.root.queues": "default",
  "yarn.scheduler.capacity.root.unfunded.capacity": "50"
}

MAPRED_SITE = {
  "hadoop.job.history.location": "DELETE_OLD",
  "hadoop.job.history.user.location": "DELETE_OLD",
  "io.sort.record.percent": "DELETE_OLD",
  "jetty.connector": "DELETE_OLD",
  "mapred.child.java.opts": "DELETE_OLD",
  "mapred.child.root.logger": "DELETE_OLD",
  "mapred.create.symlink": "DELETE_OLD",
  "mapred.fairscheduler.allocation.file": "DELETE_OLD",
  "mapred.fairscheduler.assignmultiple": "DELETE_OLD",
  "mapreduce.job.priority": "DELETE_OLD",
  "mapred.jobtracker.blacklist.fault-bucket-width": "DELETE_OLD",
  "mapred.jobtracker.blacklist.fault-timeout-window": "DELETE_OLD",
  "mapred.jobtracker.completeuserjobs.maximum": "DELETE_OLD",
  "mapred.jobtracker.job.history.block.size": "DELETE_OLD",
  "mapred.jobtracker.retirejob.check": "DELETE_OLD",
  "mapred.jobtracker.retirejob.interval": "DELETE_OLD",
  "mapred.jobtracker.taskScheduler": "DELETE_OLD",
  "mapred.permissions.supergroup": "DELETE_OLD",
  "mapred.queue.names": "DELETE_OLD",
  "mapreduce.cluster.acls.enabled": "DELETE_OLD",
  "mapreduce.cluster.local.dir": "DELETE_OLD",
  "mapreduce.cluster.mapmemory.mb": "DELETE_OLD",
  "mapreduce.cluster.permissions.supergroup": "DELETE_OLD",
  "mapreduce.cluster.reducememory.mb": "DELETE_OLD",
  "mapreduce.cluster.temp.dir": "DELETE_OLD",
  "mapreduce.jobtracker.jobinit.threads": "DELETE_OLD",
  "mapreduce.jobtracker.permissions.supergroup": "DELETE_OLD",
  "mapreduce.job.cache.symlink.create": "DELETE_OLD",
  "mapreduce.job.speculative.slownodethreshold": "DELETE_OLD",
  "mapreduce.job.userlog.retain.hours": "DELETE_OLD",
  "mapreduce.admin.map.child.java.opts": "-Djava.net.preferIPv4Stack=true -Dhadoop.metrics.log.level=WARN",
  "mapreduce.admin.reduce.child.java.opts": "-Djava.net.preferIPv4Stack=true -Dhadoop.metrics.log.level=WARN",
  "mapreduce.admin.user.env": "LD_LIBRARY_PATH=/usr/lib/hadoop/lib/native:/usr/lib/hadoop/lib/native/`$JAVA_HOME/bin/java -d32 -version &amp;&gt; /dev/null;if [ $? -eq 0 ]; then echo Linux-i386-32; else echo Linux-amd64-64;fi`",
  "mapreduce.am.max-attempts": "2",
  "mapreduce.application.classpath": "$HADOOP_MAPRED_HOME/share/hadoop/mapreduce/*,$HADOOP_MAPRED_HOME/share/hadoop/mapreduce/lib/*",
  "mapreduce.fileoutputcommitter.marksuccessfuljobs": "DELETE_OLD",
  "mapreduce.framework.name": "yarn",
  "mapreduce.history.server.embedded": "DELETE_OLD",
  "mapreduce.history.server.http.address": "DELETE_OLD",
  "mapreduce.job.committer.setup.cleanup.needed": "DELETE_OLD",
  "mapreduce.job.jvm.numtasks": "DELETE_OLD",
  "mapreduce.jobhistory.address": "REPLACE_JH_HOST:10020",
  "mapreduce.jobhistory.done-dir": "/mr-history/done",
  "mapreduce.jobhistory.intermediate-done-dir": "/mr-history/tmp",
  "mapreduce.jobhistory.webapp.address": "REPLACE_JH_HOST:19888",
  "mapreduce.jobtracker.address": "DELETE_OLD",
  "mapreduce.jobtracker.blacklist.average.threshold": "DELETE_OLD",
  "mapreduce.jobtracker.expire.trackers.interval": "DELETE_OLD",
  "mapreduce.jobtracker.handler.count": "DELETE_OLD",
  "mapreduce.jobtracker.heartbeats.in.second": "DELETE_OLD",
  "mapreduce.jobtracker.hosts.exclude.filename": "DELETE_OLD",
  "mapreduce.jobtracker.hosts.filename": "DELETE_OLD",
  "mapreduce.jobtracker.http.address": "DELETE_OLD",
  "mapreduce.jobtracker.instrumentation": "DELETE_OLD",
  "mapreduce.jobtracker.jobhistory.block.size": "DELETE_OLD",
  "mapreduce.jobtracker.jobhistory.completed.location": "DELETE_OLD",
  "mapreduce.jobtracker.jobhistory.location": "DELETE_OLD",
  "mapreduce.jobtracker.jobhistory.lru.cache.size": "DELETE_OLD",
  "mapreduce.jobtracker.maxmapmemory.mb": "DELETE_OLD",
  "mapreduce.jobtracker.maxreducememory.mb": "DELETE_OLD",
  "mapreduce.jobtracker.maxtasks.perjob": "DELETE_OLD",
  "mapreduce.jobtracker.persist.jobstatus.active": "DELETE_OLD",
  "mapreduce.jobtracker.persist.jobstatus.dir": "DELETE_OLD",
  "mapreduce.jobtracker.persist.jobstatus.hours": "DELETE_OLD",
  "mapreduce.jobtracker.restart.recover": "DELETE_OLD",
  "mapreduce.jobtracker.retiredjobs.cache.size": "DELETE_OLD",
  "mapreduce.jobtracker.retirejobs": "DELETE_OLD",
  "mapreduce.jobtracker.split.metainfo.maxsize": "DELETE_OLD",
  "mapreduce.jobtracker.staging.root.dir": "DELETE_OLD",
  "mapreduce.jobtracker.system.dir": "DELETE_OLD",
  "mapreduce.jobtracker.taskcache.levels": "DELETE_OLD",
  "mapreduce.jobtracker.taskscheduler": "DELETE_OLD",
  "mapreduce.jobtracker.taskscheduler.maxrunningtasks.perjob": "DELETE_OLD",
  "mapreduce.jobtracker.taskscheduler.taskalloc.capacitypad": "DELETE_OLD",
  "mapreduce.jobtracker.tasktracker.maxblacklists": "DELETE_OLD",
  "mapreduce.jobtracker.webinterface.trusted": "DELETE_OLD",
  "mapreduce.map.java.opts": "-Xmx756m",
  "mapreduce.map.log.level": "INFO",
  "mapreduce.map.memory.mb": "1024",
  "mapreduce.map.output.compress": "false",
  "mapreduce.map.output.compress.codec": "DELETE_OLD",
  "mapreduce.map.sort.spill.percent": "0.7",
  "mapreduce.output.fileoutputformat.compress": "false",
  "mapreduce.reduce.input.limit": "DELETE_OLD",
  "mapreduce.reduce.java.opts": "-Xmx756m",
  "mapreduce.reduce.log.level": "INFO",
  "mapreduce.reduce.memory.mb": "1024",
  "mapreduce.reduce.merge.inmem.threshold": "DELETE_OLD",
  "mapreduce.shuffle.port": "13562",
  "mapreduce.task.timeout": "300000",
  "mapreduce.task.userlog.limit.kb": "DELETE_OLD",
  "mapreduce.tasktracker.cache.local.size": "DELETE_OLD",
  "mapreduce.tasktracker.contention.tracking": "DELETE_OLD",
  "mapreduce.tasktracker.dns.interface": "DELETE_OLD",
  "mapreduce.tasktracker.dns.nameserver": "DELETE_OLD",
  "mapreduce.tasktracker.events.batchsize": "DELETE_OLD",
  "mapreduce.tasktracker.group": "DELETE_OLD",
  "mapreduce.tasktracker.healthchecker.interval": "DELETE_OLD",
  "mapreduce.tasktracker.healthchecker.script.args": "DELETE_OLD",
  "mapreduce.tasktracker.healthchecker.script.path": "DELETE_OLD",
  "mapreduce.tasktracker.healthchecker.script.timeout": "DELETE_OLD",
  "mapreduce.tasktracker.host.name": "DELETE_OLD",
  "mapreduce.tasktracker.http.address": "DELETE_OLD",
  "mapreduce.tasktracker.http.threads": "DELETE_OLD",
  "mapreduce.tasktracker.indexcache.mb": "DELETE_OLD",
  "mapreduce.tasktracker.instrumentation": "DELETE_OLD",
  "mapreduce.tasktracker.local.dir.minspacekill": "DELETE_OLD",
  "mapreduce.tasktracker.local.dir.minspacestart": "DELETE_OLD",
  "mapreduce.tasktracker.map.tasks.maximum": "DELETE_OLD",
  "mapreduce.tasktracker.net.static.resolutions": "DELETE_OLD",
  "mapreduce.tasktracker.reduce.tasks.maximum": "DELETE_OLD",
  "mapreduce.tasktracker.report.address": "DELETE_OLD",
  "mapreduce.tasktracker.resourcecalculatorplugin": "DELETE_OLD",
  "mapreduce.tasktracker.taskcontroller": "DELETE_OLD",
  "mapreduce.tasktracker.taskmemorymanager.monitoringinterval": "DELETE_OLD",
  "mapreduce.tasktracker.tasks.sleeptimebeforesigkill": "DELETE_OLD",
  "yarn.app.mapreduce.am.admin-command-opts": "-Djava.net.preferIPv4Stack=true -Dhadoop.metrics.log.level=WARN",
  "yarn.app.mapreduce.am.command-opts": "-Xmx312m",
  "yarn.app.mapreduce.am.log.level": "INFO",
  "yarn.app.mapreduce.am.resource.mb": "512",
  "yarn.app.mapreduce.am.staging-dir": "/user"
}

GLOBAL = {
  "datanode_du_reserved": "1073741824",
  "dfs_block_local_path_access_user": "DELETE_OLD",
  "dfs_datanode_data_dir": "REPLACE_WITH_dfs_data_dir",
  "dfs_exclude": "dfs.exclude",
  "dfs_include": "DELETE_OLD",
  "dfs_namenode_checkpoint_dir": "REPLACE_WITH_fs_checkpoint_dir",
  "dfs_namenode_checkpoint_period": "REPLACE_WITH_fs_checkpoint_period",
  "dfs_namenode_name_dir": "REPLACE_WITH_dfs_name_dir",
  "fs_checkpoint_size": "DELETE_OLD",
  "io_sort_spill_percent": "DELETE_OLD",
  "hadoop_conf_dir": "/etc/hadoop/conf",
  "hdfs_support_append": "DELETE_OLD",
  "hfile_blockcache_size": "0.25",
  "hregion_majorcompaction": "604800000",
  "hstore_blockingstorefiles": "10",
  "jtnode_heapsize": "DELETE_OLD",
  "jtnode_opt_maxnewsize": "DELETE_OLD",
  "jtnode_opt_newsize": "DELETE_OLD",
  "mapred_child_java_opts_sz": "DELETE_OLD",
  "mapred_cluster_map_mem_mb": "DELETE_OLD",
  "mapred_cluster_max_map_mem_mb": "DELETE_OLD",
  "mapred_cluster_max_red_mem_mb": "DELETE_OLD",
  "mapred_cluster_red_mem_mb": "DELETE_OLD",
  "mapred_hosts_exclude": "mapred.exclude",
  "mapred_hosts_include": "mapred.include",
  "mapred_job_map_mem_mb": "DELETE_OLD",
  "mapred_job_red_mem_mb": "DELETE_OLD",
  "mapred_local_dir": "DELETE_OLD",
  "mapred_log_dir_prefix": "/var/log/hadoop-mapreduce",
  "mapred_map_tasks_max": "DELETE_OLD",
  "mapred_pid_dir_prefix": "/var/run/hadoop-mapreduce",
  "mapred_red_tasks_max": "DELETE_OLD",
  "mapreduce_jobtracker_system_dir": "REPLACE_WITH_mapred_system_dir",
  "mapreduce_map_memory_mb": "DELETE_OLD",
  "mapreduce_reduce_memory_mb": "DELETE_OLD",
  "mapreduce_task_io_sort_mb": "REPLACE_WITH_io_sort_mb",
  "maxtasks_per_job": "DELETE_OLD",
  "mapreduce_userlog_retainhours": "DELETE_OLD",
  "namenode_opt_maxnewsize": "640m",
  "nodemanager_heapsize": "1024",
  "rca_enabled": "DELETE_OLD",
  "resourcemanager_heapsize": "1024",
  "scheduler_name": "DELETE_OLD",
  "snappy_enabled": "DELETE_OLD",
  "task_controller": "org.apache.hadoop.mapred.DefaultTaskController",
  "yarn_heapsize": "1024",
  "yarn_log_dir_prefix": "/var/log/hadoop-yarn",
  "yarn_pid_dir_prefix": "/var/run/hadoop-yarn",
  "yarn_user": "yarn",
  "zookeeper_sessiontimeout": "30000"
}

HDFS_SITE = {
  "dfs.block.local-path-access.user": "DELETE_OLD",
  "dfs.client.read.shortcircuit": "true",
  "dfs.client.read.shortcircuit.streams.cache.size": "4096",
  "dfs.datanode.du.pct": "DELETE_OLD",
  "dfs.datanode.du.reserved": "1073741824",
  "dfs.datanode.socket.write.timeout": "DELETE_OLD",
  "dfs.domain.socket.path": "/var/lib/hadoop-hdfs/dn_socket",
  "dfs.hosts": "DELETE_OLD",
  "dfs.journalnode.http-address": "0.0.0.0:8480",
  "dfs.secondary.https.port": "DELETE_OLD",
  "dfs.web.ugi": "DELETE_OLD",
  "fs.permissions.umask-mode": "022",
  "ipc.server.max.response.size": "DELETE_OLD",
  "ipc.server.read.threadpool.size": "DELETE_OLD",
  "dfs.support.append": "true"
}

CORE_SITE = {
  "fs.checkpoint.size": "DELETE_OLD",
  "hadoop.security.auth_to_local": "\n        RULE:[2:$1@$0]([rn]m@.*)s/.*/yarn/\n        RULE:[2:$1@$0](jhs@.*)s/.*/mapred/\n        RULE:[2:$1@$0]([nd]n@.*)s/.*/hdfs/\n        RULE:[2:$1@$0](hm@.*)s/.*/hbase/\n        RULE:[2:$1@$0](rs@.*)s/.*/hbase/\n        DEFAULT\n    ",
  "hadoop.security.authentication": "simple",
  "hadoop.security.authorization": "false",
  "io.compression.codec.lzo.class": "DELETE_OLD",
  "io.compression.codecs": "org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec",
}

YARN_SITE = {
  "yarn.acl.enable": "true",
  "yarn.admin.acl": "*",
  "yarn.application.classpath": "/etc/hadoop/conf,/usr/lib/hadoop/*,/usr/lib/hadoop/lib/*,/usr/lib/hadoop-hdfs/*,/usr/lib/hadoop-hdfs/lib/*,/usr/lib/hadoop-yarn/*,/usr/lib/hadoop-yarn/lib/*,/usr/lib/hadoop-mapreduce/*,/usr/lib/hadoop-mapreduce/lib/*",
  "yarn.log-aggregation-enable": "true",
  "yarn.log-aggregation.retain-seconds": "2592000",
  "yarn.log.server.url": "http://REPLACE_JH_HOST:19888/jobhistory/logs",
  "yarn.nodemanager.address": "0.0.0.0:45454",
  "yarn.nodemanager.admin-env": "MALLOC_ARENA_MAX=$MALLOC_ARENA_MAX",
  "yarn.nodemanager.aux-services": "mapreduce_shuffle",
  "yarn.nodemanager.aux-services.mapreduce_shuffle.class": "org.apache.hadoop.mapred.ShuffleHandler",
  "yarn.nodemanager.container-executor.class": "org.apache.hadoop.yarn.server.nodemanager.DefaultContainerExecutor",
  "yarn.nodemanager.container-monitor.interval-ms": "3000",
  "yarn.nodemanager.delete.debug-delay-sec": "0",
  "yarn.nodemanager.disk-health-checker.min-healthy-disks": "0.25",
  "yarn.nodemanager.health-checker.interval-ms": "135000",
  "yarn.nodemanager.health-checker.script.timeout-ms": "60000",
  "yarn.nodemanager.linux-container-executor.group": "hadoop",
  "yarn.nodemanager.local-dirs": "/var/log/hadoop/yarn/local",
  "yarn.nodemanager.log-aggregation.compression-type": "gz",
  "yarn.nodemanager.log-dirs": "/var/log/hadoop/yarn/log",
  "yarn.nodemanager.log.retain-second": "604800",
  "yarn.nodemanager.remote-app-log-dir": "/app-logs",
  "yarn.nodemanager.remote-app-log-dir-suffix": "logs",
  "yarn.nodemanager.resource.memory-mb": "5120",
  "yarn.nodemanager.vmem-check-enabled": "false",
  "yarn.nodemanager.vmem-pmem-ratio": "2.1",
  "yarn.resourcemanager.address": "REPLACE_RM_HOST:8050",
  "yarn.resourcemanager.admin.address": "REPLACE_RM_HOST:8141",
  "yarn.resourcemanager.am.max-attempts": "2",
  "yarn.resourcemanager.hostname": "REPLACE_RM_HOST",
  "yarn.resourcemanager.resource-tracker.address": "REPLACE_RM_HOST:8025",
  "yarn.resourcemanager.scheduler.address": "REPLACE_RM_HOST:8030",
  "yarn.resourcemanager.scheduler.class": "org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.CapacityScheduler",
  "yarn.resourcemanager.webapp.address": "REPLACE_RM_HOST:8088",
  "yarn.scheduler.maximum-allocation-mb": "2048",
  "yarn.scheduler.minimum-allocation-mb": "512"
}

HBASE_SITE = {
  "dfs.client.read.shortcircuit": "DELETE_OLD",
  "dfs.support.append": "DELETE_OLD",
  "hbase.defaults.for.version.skip": "true",
  "hbase.hregion.majorcompaction": "604800000",
  "hbase.hregion.max.filesize": "10737418240",
  "hbase.hstore.blockingStoreFiles": "10",
  "hbase.hstore.flush.retries.number": "120",
  "hbase.regionserver.global.memstore.lowerLimit": "0.38",
  "hbase.regionserver.handler.count": "60",
  "hbase.rpc.engine": "DELETE_OLD",
  "hfile.block.cache.size": "0.40",
  "zookeeper.session.timeout": "30000"
}

HIVE_SITE = {
  "hive.security.authorization.manager": "org.apache.hadoop.hive.ql.security.authorization.StorageBasedAuthorizationProvider",
  "hive.security.metastore.authorization.manager": "org.apache.hadoop.hive.ql.security.authorization.StorageBasedAuthorizationProvider",
  "hive.security.authenticator.manager": "org.apache.hadoop.hive.ql.security.ProxyUserAuthenticator"
}


class FatalException(Exception):
  def __init__(self, code, reason):
    self.code = code
    self.reason = reason

  def __str__(self):
    return repr("Fatal exception: %s, exit code %s" % (self.reason, self.code))

  def _get_message(self):
    return str(self)

# Copy file and save with file.# (timestamp)
def backup_file(filePath):
  if filePath is not None and os.path.exists(filePath):
    timestamp = datetime.datetime.now()
    format = '%Y%m%d%H%M%S'
    try:
      shutil.copyfile(filePath, filePath + "." + timestamp.strftime(format))
      os.remove(filePath)
    except (Exception), e:
      logger.warn('Could not backup file "%s": %s' % (filePath, str(e)))
  return 0


def write_mapping(hostmapping):
  if os.path.isfile(MR_MAPPING_FILE):
    os.remove(MR_MAPPING_FILE)
  json.dump(hostmapping, open(MR_MAPPING_FILE, 'w'))
  pass


def write_config(config, type, tag):
  file_name = type + "_" + tag
  if os.path.isfile(file_name):
    os.remove(file_name)
  json.dump(config, open(file_name, 'w'))
  pass


def read_mapping():
  if os.path.isfile(MR_MAPPING_FILE):
    return json.load(open(MR_MAPPING_FILE))
  else:
    raise FatalException(-1, "MAPREDUCE host mapping file, mr_mapping, is not available or badly formatted. Execute "
                             "action save-mr-mapping. Ensure the file is present in the directory where you are "
                             "executing this command.")
  pass


def get_mr1_mapping(options):
  components = ["MAPREDUCE_CLIENT", "JOBTRACKER", "TASKTRACKER", "HISTORYSERVER"]
  GET_URL_FORMAT = URL_FORMAT + '/services/MAPREDUCE/components/{2}'
  hostmapping = {}
  for component in components:
    hostlist = []
    response = curl(False, '-u',
                    AUTH_FORMAT.format(options.user, options.password),
                    GET_URL_FORMAT.format(options.hostname, options.clustername, component))
    retcode, errdata = validate_response(response, True)
    if not retcode == 0:
      raise FatalException(retcode, errdata)

    structured_resp = json.loads(response)
    if 'host_components' in structured_resp:
      for hostcomponent in structured_resp['host_components']:
        if 'HostRoles' in hostcomponent:
          if 'host_name' in hostcomponent['HostRoles']:
            hostlist.append(hostcomponent['HostRoles']['host_name'])
            pass
          pass
        pass
      pass
    pass
    hostmapping[component] = hostlist
  write_mapping(hostmapping)


def get_YN_input(prompt, default):
  yes = set(['yes', 'ye', 'y'])
  no = set(['no', 'n'])
  return get_choice_string_input(prompt, default, yes, no)


def get_choice_string_input(prompt, default, firstChoice, secondChoice):
  choice = raw_input(prompt).lower()
  if choice in firstChoice:
    return True
  elif choice in secondChoice:
    return False
  elif choice is "": # Just enter pressed
    return default
  else:
    print "input not recognized, please try again: "
    return get_choice_string_input(prompt, default, firstChoice, secondChoice)


def delete_mr(options):
  saved_mr_mapping = get_YN_input("Have you saved MR host mapping using action save-mr-mapping [y/n] (n)? ", False)
  if not saved_mr_mapping:
    raise FatalException(1, "Ensure MAPREDUCE host component mapping is saved before deleting it. Use action "
                            "save-mr-mapping.")

  SERVICE_URL_FORMAT = URL_FORMAT + '/services/MAPREDUCE'
  COMPONENT_URL_FORMAT = URL_FORMAT + '/hosts/{2}/host_components/{3}'
  NON_CLIENTS = ["JOBTRACKER", "TASKTRACKER", "HISTORYSERVER"]
  PUT_IN_DISABLED = """{"HostRoles": {"state": "DISABLED"}}"""
  hostmapping = read_mapping()

  for key, value in hostmapping.items():
    if (key in NON_CLIENTS) and (len(value) > 0):
      for host in value:
        response = curl(options.printonly, '-u',
                        AUTH_FORMAT.format(options.user, options.password),
                        '-H', 'X-Requested-By: ambari',
                        '-X', 'PUT', '-d',
                        PUT_IN_DISABLED,
                        COMPONENT_URL_FORMAT.format(options.hostname, options.clustername, host, key))
        retcode, errdata = validate_response(response, False)
        if not retcode == 0:
          raise FatalException(retcode, errdata)
        pass
      pass
    pass
  pass

  response = curl(options.printonly, '-u',
                  AUTH_FORMAT.format(options.user, options.password),
                  '-H', 'X-Requested-By: ambari',
                  '-X', 'DELETE',
                  SERVICE_URL_FORMAT.format(options.hostname, options.clustername))
  retcode, errdata = validate_response(response, False)
  if not retcode == 0:
    raise FatalException(retcode, errdata)
  pass


def add_services(options):
  SERVICE_URL_FORMAT = URL_FORMAT + '/services/{2}'
  COMPONENT_URL_FORMAT = SERVICE_URL_FORMAT + '/components/{3}'
  HOST_COMPONENT_URL_FORMAT = URL_FORMAT + '/hosts/{2}/host_components/{3}'
  service_comp = {
    "YARN": ["NODEMANAGER", "RESOURCEMANAGER", "YARN_CLIENT"],
    "MAPREDUCE2": ["HISTORYSERVER", "MAPREDUCE2_CLIENT"]}
  new_old_host_map = {
    "NODEMANAGER": "TASKTRACKER",
    "HISTORYSERVER": "HISTORYSERVER",
    "RESOURCEMANAGER": "JOBTRACKER",
    "YARN_CLIENT": "MAPREDUCE_CLIENT",
    "MAPREDUCE2_CLIENT": "MAPREDUCE_CLIENT"}
  hostmapping = read_mapping()

  for service in service_comp.keys():
    response = curl(options.printonly, '-u',
                    AUTH_FORMAT.format(options.user, options.password),
                    '-H', 'X-Requested-By: ambari',
                    '-X', 'POST',
                    SERVICE_URL_FORMAT.format(options.hostname, options.clustername, service))
    retcode, errdata = validate_response(response, False)
    if not retcode == 0:
      raise FatalException(retcode, errdata)
    for component in service_comp[service]:
      response = curl(options.printonly, '-u',
                      AUTH_FORMAT.format(options.user, options.password),
                      '-H', 'X-Requested-By: ambari',
                      '-X', 'POST',
                      COMPONENT_URL_FORMAT.format(options.hostname, options.clustername, service, component))
      retcode, errdata = validate_response(response, False)
      if not retcode == 0:
        raise FatalException(retcode, errdata)
      for host in hostmapping[new_old_host_map[component]]:
        response = curl(options.printonly, '-u',
                        AUTH_FORMAT.format(options.user, options.password),
                        '-H', 'X-Requested-By: ambari',
                        '-X', 'POST',
                        HOST_COMPONENT_URL_FORMAT.format(options.hostname, options.clustername, host, component))
        retcode, errdata = validate_response(response, False)
        if not retcode == 0:
          raise FatalException(retcode, errdata)
        pass
      pass
    pass
  pass


def update_config(options, properties, type):
  tag = "version" + str(int(time.time() * 1000))
  properties_payload = {"Clusters": {"desired_config": {"type": type, "tag": tag, "properties": properties}}}
  response = curl(options.printonly, '-u',
                  AUTH_FORMAT.format(options.user, options.password),
                  '-H', 'X-Requested-By: ambari',
                  '-X', 'PUT', '-d',
                  json.dumps(properties_payload),
                  URL_FORMAT.format(options.hostname, options.clustername))
  retcode, errdata = validate_response(response, False)
  if not retcode == 0:
    raise FatalException(retcode, errdata)
  pass


def get_config(options, type):
  tag, structured_resp = get_config_resp(options, type)
  properties = None
  if 'items' in structured_resp:
    for item in structured_resp['items']:
      if (tag == item['tag']) or (type == item['type']):
        properties = item['properties']
  if (properties is None):
    raise FatalException(-1, "Unable to read configuration for type " + type + " and tag " + tag)
  else:
    logger.info("Read configuration for type " + type + " and tag " + tag)
  return properties


def get_config_resp(options, type, error_if_na=True):
  CONFIG_URL_FORMAT = URL_FORMAT + '/configurations?type={2}&tag={3}'
  response = curl(False, '-u',
                  AUTH_FORMAT.format(options.user, options.password),
                  URL_FORMAT.format(options.hostname, options.clustername))
  retcode, errdata = validate_response(response, True)
  if not retcode == 0:
    raise FatalException(retcode, errdata)
    # Read the config version
  tag = None
  structured_resp = json.loads(response)
  if 'Clusters' in structured_resp:
    if 'desired_configs' in structured_resp['Clusters']:
      if type in structured_resp['Clusters']['desired_configs']:
        tag = structured_resp['Clusters']['desired_configs'][type]['tag']

  if tag != None:
    # Get the config with the tag and return properties
    response = curl(False, '-u',
                    AUTH_FORMAT.format(options.user, options.password),
                    CONFIG_URL_FORMAT.format(options.hostname, options.clustername, type, tag))
    retcode, errdata = validate_response(response, True)
    if not retcode == 0:
      raise FatalException(retcode, errdata)
    structured_resp = json.loads(response)
    return (tag, structured_resp)
  else:
    if error_if_na:
      raise FatalException(-1, "Unable to get the current version for config type " + type)
    else:
      return (tag, None)
  pass


def modify_configs(options, config_type):
  properties_to_move = [
    "dfs.namenode.checkpoint.edits.dir",
    "dfs.namenode.checkpoint.dir",
    "dfs.namenode.checkpoint.period"]
  hostmapping = read_mapping()

  # Add capacity-scheduler, yarn-site  (added with default values)
  if (config_type is None) or (config_type == CAPACITY_SCHEDULER_TAG):
    update_config(options, CAPACITY_SCHEDULER, CAPACITY_SCHEDULER_TAG)
    pass

  jt_host = hostmapping["JOBTRACKER"][0]

  if (config_type is None) or (config_type == YARN_SITE_TAG):
    for key in YARN_SITE.keys():
      if REPLACE_JH_HOST_NAME_TAG in YARN_SITE[key]:
        YARN_SITE[key] = YARN_SITE[key].replace(REPLACE_JH_HOST_NAME_TAG, jt_host, 1)
      if REPLACE_RM_HOST_NAME_TAG in YARN_SITE[key]:
        YARN_SITE[key] = YARN_SITE[key].replace(REPLACE_RM_HOST_NAME_TAG, jt_host, 1)
        pass
      pass
    pass
    update_config(options, YARN_SITE, YARN_SITE_TAG)
    pass

  # Update global config
  if (config_type is None) or (config_type == GLOBAL_TAG):
    update_config_using_existing(options, GLOBAL_TAG, GLOBAL.copy())
    pass

  core_site_latest = rename_all_properties(get_config(options, CORE_SITE_TAG), PROPERTY_MAPPING)
  hdfs_site_latest = rename_all_properties(get_config(options, HDFS_SITE_TAG), PROPERTY_MAPPING)
  mapred_site_latest = rename_all_properties(get_config(options, MAPRED_SITE_TAG), PROPERTY_MAPPING)

  for property in properties_to_move:
    if property in core_site_latest.keys():
      hdfs_site_latest[property] = core_site_latest[property]
      del core_site_latest[property]
    pass

  # Update mapred-site config
  mapred_updated = MAPRED_SITE.copy()
  if (config_type is None) or (config_type == MAPRED_SITE_TAG):
    for key in mapred_updated.keys():
      if REPLACE_JH_HOST_NAME_TAG in mapred_updated[key]:
        mapred_updated[key] = mapred_updated[key].replace(REPLACE_JH_HOST_NAME_TAG, jt_host, 1)
        pass
      pass
    pass
    update_config_using_existing_properties(options, MAPRED_SITE_TAG, mapred_updated, mapred_site_latest)
    pass

  # Update hdfs-site, core-site
  if (config_type is None) or (config_type == HDFS_SITE_TAG):
    update_config_using_existing_properties(options, HDFS_SITE_TAG, HDFS_SITE.copy(), hdfs_site_latest)
    pass
  if (config_type is None) or (config_type == CORE_SITE_TAG):
    update_config_using_existing_properties(options, CORE_SITE_TAG, CORE_SITE.copy(), core_site_latest)
    pass

  # Update hbase-site if exists
  if (config_type is None) or (config_type == HBASE_SITE_TAG):
    tag, structured_resp = get_config_resp(options, HBASE_SITE_TAG, False)
    if structured_resp is not None:
      update_config_using_existing(options, HBASE_SITE_TAG, HBASE_SITE.copy())
      pass
    pass

  # Update hive-site if exists
  if (config_type is None) or (config_type == HIVE_SITE_TAG):
    tag, structured_resp = get_config_resp(options, HIVE_SITE_TAG, False)
    if structured_resp is not None:
      update_config_using_existing(options, HIVE_SITE_TAG, HIVE_SITE.copy())
      pass
  pass


def rename_all_properties(properties, name_mapping):
  for key, val in name_mapping.items():
    if (key in properties.keys()) and (val not in properties.keys()):
      properties[val] = properties[key]
      del properties[key]
    pass
  return properties


def update_config_using_existing(options, type, properties_template):
  site_properties = get_config(options, type)
  update_config_using_existing_properties(options, type, properties_template, site_properties)
  pass


def update_config_using_existing_properties(options, type, properties_template,
                                            site_properties):
  keys_processed = []
  keys_to_delete = []
  for key in properties_template.keys():
    keys_processed.append(key)
    if properties_template[key] == DELETE_OLD_TAG:
      keys_to_delete.append(key)
      pass
    if properties_template[key].find(REPLACE_WITH_TAG) == 0:
      name_to_lookup = key
      if len(properties_template[key]) > len(REPLACE_WITH_TAG):
        name_to_lookup = properties_template[key][len(REPLACE_WITH_TAG):]
        keys_processed.append(name_to_lookup)
      value = ""
      if name_to_lookup in site_properties.keys():
        value = site_properties[name_to_lookup]
        pass
      else:
        logger.warn("Unable to find the equivalent for " + key + ". Looked for " + name_to_lookup)
      properties_template[key] = value
      pass
    pass
  pass

  for key in site_properties.keys():
    if key not in keys_processed:
      properties_template[key] = site_properties[key]
      pass
    pass
  pass

  for key in keys_to_delete:
    del properties_template[key]
  pass
  update_config(options, properties_template, type)


def backup_configs(options, type=None):
  types_to_save = {"global": True, "mapred-site": True, "hdfs-site": True, "core-site": True,
                   "webhcat-site": False, "hive-site": False, "hbase-site": False, "oozie-site": False}
  for type in types_to_save.keys():
    backup_single_config_type(options, type, types_to_save[type])
    pass
  pass


def backup_single_config_type(options, type, error_if_na=True):
  tag, response = get_config_resp(options, type, error_if_na)
  if response is not None:
    logger.info("Saving config for type: " + type + " and tag: " + tag)
    write_config(response, type, tag)
  else:
    logger.info("Unable to obtain config for type: " + type)
    pass
  pass


def install_services(options):
  SERVICE_URL_FORMAT = URL_FORMAT + '/services/{2}'
  SERVICES = ["MAPREDUCE2", "YARN"]
  PUT_IN_INSTALLED = ["""{"RequestInfo":{"context":"Install MapReduce2"}, "Body":{"ServiceInfo": {"state":"INSTALLED"}}}""",
                      """{"RequestInfo":{"context":"Install YARN"}, "Body":{"ServiceInfo": {"state":"INSTALLED"}}}"""]
  err_retcode = 0
  err_message = ""
  for index in [0, 1]:
    response = curl(options.printonly, '-u',
                    AUTH_FORMAT.format(options.user, options.password),
                    '-H', 'X-Requested-By: ambari',
                    '-X', 'PUT', '-d',
                    PUT_IN_INSTALLED[index],
                    SERVICE_URL_FORMAT.format(options.hostname, options.clustername, SERVICES[index]))
    retcode, errdata = validate_response(response, not options.printonly)
    if not retcode == 0:
      err_retcode = retcode
      error_msg = err_message + " Error while installing " + SERVICES[index] + ". Details: " + errdata + "."
  pass

  if err_retcode != 0:
    raise FatalException(err_retcode, error_msg + "(Services may already be installed or agents are not yet started.)")

  options.exit_message = "Requests has been submitted to install YARN and MAPREDUCE2. Use Ambari Web to monitor " \
                         "the status of the install requests."
  pass


def validate_response(response, expect_body):
  if expect_body:
    if "\"href\" : \"" not in response:
      return (1, response)
    else:
      return (0, "")
  elif len(response) > 0:
    return (1, response)
  else:
    return (0, "")
  pass


def curl(print_only, *args):
  curl_path = '/usr/bin/curl'
  curl_list = [curl_path]
  for arg in args:
    curl_list.append(arg)
  if print_only:
    logger.info("Command to be executed: " + ' '.join(curl_list))
    return ""
  pass
  logger.info(' '.join(curl_list))
  osStat = subprocess.Popen(
    curl_list,
    stderr=subprocess.PIPE,
    stdout=subprocess.PIPE)
  out, err = osStat.communicate()
  if 0 != osStat.returncode:
    error = "curl call failed. out: " + out + " err: " + err
    logger.error(error)
    raise FatalException(osStat.returncode, error)
  return out

#
# Main.
#
def main():
  parser = optparse.OptionParser(usage="usage: %prog [options] action\n  Valid actions: " + VALID_ACTIONS
                                       + "\n  update-configs accepts type, e.g. hdfs-site to update specific configs", )

  parser.add_option("-n", "--printonly",
                    action="store_true", dest="printonly", default=False,
                    help="Prints all the curl commands to be executed (only for write/update actions)")
  parser.add_option("-o", "--log", dest="logfile", default=UPGRADE_LOG_FILE,
                    help="Log file")

  parser.add_option('--hostname', default=None, help="Hostname for Ambari server", dest="hostname")
  parser.add_option('--user', default=None, help="Ambari admin user", dest="user")
  parser.add_option('--password', default=None, help="Ambari admin password", dest="password")
  parser.add_option('--clustername', default=None, help="Cluster name", dest="clustername")

  (options, args) = parser.parse_args()

  options.warnings = []
  if options.user is None:
    options.warnings.append("User name must be provided (e.g. admin)")
  if options.hostname is None:
    options.warnings.append("Ambari server host name must be provided")
  if options.clustername is None:
    options.warnings.append("Cluster name must be provided")
  if options.password is None:
    options.warnings.append("Ambari admin user's password name must be provided (e.g. admin)")

  if len(options.warnings) != 0:
    print parser.print_help()
    for warning in options.warnings:
      print "  " + warning
    parser.error("Invalid or missing options")

  if len(args) == 0:
    print parser.print_help()
    parser.error("No action entered")

  action = args[0]

  options.exit_message = "Upgrade action '%s' completed successfully." % action
  if options.printonly:
    options.exit_message = "Simulated execution of action '%s'. Verify the list edit calls." % action

  backup_file(options.logfile)
  global logger
  logger = logging.getLogger('UpgradeHelper')
  handler = logging.FileHandler(options.logfile)
  formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
  handler.setFormatter(formatter)
  logger.addHandler(handler)
  logging.basicConfig(level=logging.DEBUG)

  try:
    if action == GET_MR_MAPPING_ACTION:
      get_mr1_mapping(options)
      pprint("File mr_mapping contains the host mapping for mapreduce components. This file is critical for later "
             "steps.")
    elif action == DELETE_MR_ACTION:
      delete_mr(options)
    elif action == ADD_YARN_MR2_ACTION:
      add_services(options)
    elif action == MODIFY_CONFIG_ACTION:
      config_type = None
      if len(args) > 1:
        config_type = args[1]
      modify_configs(options, config_type)
    elif action == INSTALL_YARN_MR2_ACTION:
      install_services(options)
    elif action == BACKUP_CONFIG_ACTION:
      backup_configs(options)
    else:
      parser.error("Invalid action")

  except FatalException as e:
    if e.reason is not None:
      error = "ERROR: Exiting with exit code {0}. Reason: {1}".format(e.code, e.reason)
      pprint(error)
      logger.error(error)
    sys.exit(e.code)

  if options.exit_message is not None:
    print options.exit_message


if __name__ == "__main__":
  try:
    main()
  except (KeyboardInterrupt, EOFError):
    print("\nAborting ... Keyboard Interrupt.")
    sys.exit(1)
