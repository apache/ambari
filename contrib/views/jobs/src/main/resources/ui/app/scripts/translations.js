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

Ember.I18n.translations = {

  'any': 'Any',
  'apply': 'Apply',
  'ok': 'Ok',
  'cancel': 'Cancel',

  'jobs.type':'Jobs Type',
  'jobs.type.hive':'Hive',
  'jobs.show.up.to':'Show up to',
  'jobs.filtered.jobs':'%@ jobs showing',
  'jobs.filtered.clear':'clear filters',
  'jobs.column.id':'Id',
  'jobs.column.user':'User',
  'jobs.column.start.time':'Start Time',
  'jobs.column.end.time':'End Time',
  'jobs.column.duration':'Duration',
  'jobs.new_jobs.info':'New jobs available on server.',
  'jobs.loadingTasks': 'Loading...',

  'jobs.nothingToShow': 'No jobs to display',
  'jobs.error.ats.down': 'Jobs data cannot be shown since YARN App Timeline Server is not running.',
  'jobs.error.400': 'Unable to load data.',
  'jobs.table.custom.date.am':'AM',
  'jobs.table.custom.date.pm':'PM',
  'jobs.table.custom.date.header':'Select Custom Dates',
  'jobs.table.job.fail':'Job failed to run',
  'jobs.customDateFilter.error.required':'This field is required',
  'jobs.customDateFilter.error.date.order':'End Date must be after Start Date',
  'jobs.customDateFilter.startTime':'Start Time',
  'jobs.customDateFilter.endTime':'End Time',
  'jobs.hive.failed':'JOB FAILED',
  'jobs.hive.more':'show more',
  'jobs.hive.less':'show less',
  'jobs.hive.query':'Hive Query',
  'jobs.hive.stages':'Stages',
  'jobs.hive.yarnApplication':'YARN&nbsp;Application',
  'jobs.hive.tez.tasks':'Tez Tasks',
  'jobs.hive.tez.hdfs':'HDFS',
  'jobs.hive.tez.localFiles':'Local Files',
  'jobs.hive.tez.spilledRecords':'Spilled Records',
  'jobs.hive.tez.records':'Records',
  'jobs.hive.tez.reads':'{0} reads',
  'jobs.hive.tez.writes':'{0} writes',
  'jobs.hive.tez.records.count':'{0} Records',
  'jobs.hive.tez.operatorPlan':'Operator Plan',
  'jobs.hive.tez.dag.summary.metric':'Summary Metric',
  'jobs.hive.tez.dag.error.noDag.title':'No Tez Information',
  'jobs.hive.tez.dag.error.noDag.message':'This job does not identify any Tez information.',
  'jobs.hive.tez.dag.error.noDagId.title':'No Tez Information',
  'jobs.hive.tez.dag.error.noDagId.message':'No Tez information was found for this job. Either it is waiting to be run, or has exited unexpectedly.',
  'jobs.hive.tez.dag.error.noDagForId.title':'No Tez Information',
  'jobs.hive.tez.dag.error.noDagForId.message':'No details were found for the Tez ID given to this job.',
  'jobs.hive.tez.metric.input':'Input',
  'jobs.hive.tez.metric.output':'Output',
  'jobs.hive.tez.metric.recordsRead':'Records Read',
  'jobs.hive.tez.metric.recordsWrite':'Records Written',
  'jobs.hive.tez.metric.tezTasks':'Tez Tasks',
  'jobs.hive.tez.metric.spilledRecords':'Spilled Records',
  'jobs.hive.tez.edge.':'Unknown',
  'jobs.hive.tez.edge.contains':'Contains',
  'jobs.hive.tez.edge.broadcast':'Broadcast',
  'jobs.hive.tez.edge.scatter_gather':'Shuffle'

};
