/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');
var date = require('utils/date');
var numberUtils = require('utils/number_utils');

App.MainHiveJobDetailsView = Em.View.extend({
  templateName : require('templates/main/jobs/hive_job_details'),

  selectedVertex : null,
  content : null,
  summaryMetricType: 'input',
  summaryMetricTypesDisplay : [ Em.I18n.t('jobs.hive.tez.metric.input'), Em.I18n.t('jobs.hive.tez.metric.output'), Em.I18n.t('jobs.hive.tez.metric.recordsRead'),
                                Em.I18n.t('jobs.hive.tez.metric.recordsWrite'), Em.I18n.t('jobs.hive.tez.metric.tezTasks') ],
  summaryMetricTypeDisplay: function(){
    return Em.I18n.t('jobs.hive.tez.metric.'+this.get('summaryMetricType'));
  }.property('summaryMetricType'),

  initialDataLoaded : function() {
    var loaded = this.get('controller.loaded');
    if (loaded) {
      this.set('content', this.get('controller.content'));
    }
  }.observes('controller.loaded'),

  jobObserver : function() {
    var content = this.get('content');
    var selectedVertex = this.get('selectedVertex');
    if (selectedVertex == null && content != null) {
      var vertices = content.get('tezDag.vertices');
      if (vertices) {
        vertices.setEach('isSelected', false);
        this.doSelectVertex({
          context : vertices.objectAt(0)
        });
      }
    }
  }.observes('selectedVertex', 'content.tezDag.vertices.@each.id'),

  doSelectVertex : function(event) {
    var newVertex = event.context;
    var currentVertex = this.get('selectedVertex');
    if (currentVertex != null) {
      currentVertex.set('isSelected', false);
    }
    newVertex.set('isSelected', true);
    this.set('selectedVertex', newVertex);
  },

  doSelectSummaryMetricType: function(event) {
    var summaryType = event.context;
    switch (summaryType) {
    case Em.I18n.t('jobs.hive.tez.metric.input'):
      summaryType = 'input';
      break;
    case Em.I18n.t('jobs.hive.tez.metric.output'):
      summaryType = 'output';
      break;
    case Em.I18n.t('jobs.hive.tez.metric.recordsRead'):
      summaryType = 'recordsRead';
      break;
    case Em.I18n.t('jobs.hive.tez.metric.recordsWrite'):
      summaryType = 'recordsWrite';
      break;
    case Em.I18n.t('jobs.hive.tez.metric.tezTasks'):
      summaryType = 'tezTasks';
      break;
    default:
      break;
    }
    this.set('summaryMetricType', summaryType);
  },

  /**
   * Provides display information for vertex I/O.
   * 
   * {
   *  'file': {
   *    'read': {
   *      'ops': '100 reads',
   *      'bytes': '10 MB'
   *    }
   *    'write: {
   *      'ops': '200 writes',
   *      'bytes': '20 MB'
   *    }
   *  },
   *  'hdfs': {
   *    'read': {
   *      'ops': '100 reads',
   *      'bytes': '10 MB'
   *    }
   *    'write: {
   *      'ops': '200 writes',
   *      'bytes': '20 MB'
   *    }
   *  },
   *  'records': {
   *    'read': '100 records',
   *    'write': '123 records'
   *  }
   * }
   */
  selectedVertexIODisplay : function() {
    var v = this.get('selectedVertex');
    return {
      file : {
        read : {
          ops : Em.I18n.t('jobs.hive.tez.reads').format(v.get('fileReadOps')),
          bytes : numberUtils.bytesToSize(v.get('fileReadBytes'))
        },
        write : {
          ops : Em.I18n.t('jobs.hive.tez.writes').format(v.get('fileWriteOps')),
          bytes : numberUtils.bytesToSize(v.get('fileWriteBytes'))
        }
      },
      hdfs : {
        read : {
          ops : Em.I18n.t('jobs.hive.tez.reads').format(v.get('hdfsReadOps')),
          bytes : numberUtils.bytesToSize(v.get('hdfsReadBytes'))
        },
        write : {
          ops : Em.I18n.t('jobs.hive.tez.writes').format(v.get('hdfsWriteOps')),
          bytes : numberUtils.bytesToSize(v.get('hdfsWriteBytes'))
        }
      },
      records : {
        read : Em.I18n.t('jobs.hive.tez.records.count').format(v.get('recordReadCount')),
        write : Em.I18n.t('jobs.hive.tez.records.count').format(v.get('recordWriteCount')),
      }
    };
  }.property('selectedVertex.fileReadOps', 'selectedVertex.fileWriteOps', 'selectedVertex.hdfsReadOps', 'selectedVertex.hdfdWriteOps',
      'selectedVertex.fileReadBytes', 'selectedVertex.fileWriteBytes', 'selectedVertex.hdfsReadBytes', 'selectedVertex.hdfdWriteBytes',
      'selectedVertex.recordReadCount', 'selectedVertes.recordWriteCount')
});
