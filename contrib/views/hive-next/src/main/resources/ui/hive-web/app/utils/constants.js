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

import Ember from 'ember';
import helpers from 'hive/utils/functions';

export default Ember.Object.create({
  appTitle: 'Hive',

  /**
   * This should reflect the naming conventions accross the application.
   * Changing one value also means changing the filenames for the chain of files
   * represented by that value (routes, controllers, models etc).
   * This dependency goes both ways.
  */
  namingConventions: {
    routes: {
      index: 'index',
      savedQuery: 'savedQuery',
      historyQuery: 'historyQuery',
      queries: 'queries',
      history: 'history',
      udfs: 'udfs',
      logs: 'logs',
      results: 'results',
      explain: 'explain',
      uploadTable :'upload-table',
      visualization: 'visualization'
    },

    subroutes: {
      savedQuery: 'index.savedQuery',
      historyQuery: 'index.historyQuery',
      jobLogs: 'index.historyQuery.logs',
      jobResults: 'index.historyQuery.results',
      jobExplain: 'index.historyQuery.explain'
    },

    index: 'index',
    udf: 'udf',
    udfs: 'udfs',
    udfInsertPrefix: 'create temporary function ',
    fileInsertPrefix: 'add jar ',
    explainPrefix: 'EXPLAIN ',
    explainFormattedPrefix: 'EXPLAIN FORMATTED ',
    insertUdfs: 'insert-udfs',
    job: 'job',
    jobs: 'jobs',
    savedQuery: 'saved-query',
    database: 'database',
    ldap:'ldap',
    databases: 'databases',
    openQueries: 'open-queries',
    visualExplain: 'visual-explain',
    notify: 'notify',
    history: 'history',
    tezUI: 'tez-ui',
    file: 'file',
    fileResource: 'file-resource',
    alerts: 'alerts',
    logs: 'logs',
    results: 'results',
    jobResults: 'index/history-query/results',
    jobLogs: 'index/history-query/logs',
    jobExplain: 'index/history-query/explain',
    databaseTree: 'databases-tree',
    databaseSearch: 'databases-search-results',
    settings: 'settings',
    jobProgress: 'job-progress',
    queryTabs: 'query-tabs',
    session: 'session'
  },

  hiveParameters: [
    {
      name: 'hive.tez.container.size',
      validate: helpers.regexes.digits
    },

    {
      name: 'hive.prewarm.enabled',
      values: helpers.validationValues.bool
    },
    {
      name: 'hive.prewarm.numcontainers',
      validate: helpers.regexes.digits
    },
    {
      name: 'hive.tez.auto.reducer.parallelism',
      values: helpers.validationValues.bool
    },
    {
      name: 'hive.execution.engine',
      values: helpers.validationValues.execEngine
    },
    {
      name: 'hive.vectorized.execution.enabled',
      values: helpers.validationValues.bool
    },
    {
      name: 'hive.auto.convert.join',
      values: helpers.validationValues.bool
    },
    {
      name: 'tez.am.resource.memory.mb',
      validate: helpers.regexes.digits
    },
    {
      name: 'tez.am.container.idle.release-timeout-min.millis',
      validate: helpers.regexes.digits
    },
    {
      name: 'tez.am.container.idle.release-timeout-max.millis',
      validate: helpers.regexes.digits
    },
    {
      name: 'tez.queue.name',
      validate: helpers.regexes.name
    },
    {
      name: 'tez.runtime.io.sort.mb',
      validate: helpers.regexes.digits
    },
    {
      name: 'tez.runtime.sort.threads',
      validate: helpers.regexes.digits
    },
    {
      name: 'tez.runtime.compress.codec',
      validate: helpers.regexes.dotPath
    },
    {
      name: 'tez.grouping.min-size',
      validate: helpers.regexes.digits
    },
    {
      name: 'tez.grouping.max-size',
      validate: helpers.regexes.digits
    },
    {
      name: 'tez.generate.debug.artifacts',
      values: helpers.validationValues.bool
    }
  ],

  jobReferrer: {
    sample: 'sample',
    explain: 'explain',
    visualExplain: 'visualExplain',
    job: 'job'
  },

  statuses: {
    unknown: "UNKNOWN",
    initialized: "INITIALIZED",
    running: "RUNNING",
    succeeded: "SUCCEEDED",
    canceled: "CANCELED",
    closed: "CLOSED",
    error: "ERROR",
    failed: 'FAILED',
    killed: 'KILLED',
    pending: "PENDING"
  },

  alerts: {
    warning: 'warning',
    error: 'danger',
    success: 'success'
  },

  results: {
    save: {
      csv: 'Save as csv',
      hdfs: 'Save to HDFS'
    },
    statuses: {
      terminated: "TERMINATED",
      runnable: "RUNNABLE"
    }
  },

  //this can be replaced by a string.format implementation
  adapter: {
    version: '2.0.0',
    instance: 'Hive',
    apiPrefix: '/api/v1/views/HIVE/versions/',
    assetPrefix: '/views/HIVE/',
    instancePrefix: '/instances/',
    resourcePrefix: 'resources/'
  },

  sampleDataQuery: 'SELECT * FROM %@ LIMIT 100;',

  notify: {
    ERROR:  {
      typeClass : 'alert-danger',
      typeIcon  : 'fa-exclamation-triangle'
    },
    WARN: {
      typeClass : 'alert-warning',
      typeIcon  : 'fa-times-circle'
    },
    SUCCESS: {
      typeClass : 'alert-success',
      typeIcon  : 'fa-check'
    },
    INFO: {
      typeClass : 'alert-info',
      typeIcon  : 'fa-info'
    }
  },

  defaultVisualizationRowCount: 10000

});
