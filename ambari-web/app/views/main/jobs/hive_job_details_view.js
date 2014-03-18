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
var dateUtils = require('utils/date');
var stringUtils = require('utils/string_utils');
var sort = require('views/common/sort_view');

App.MainHiveJobDetailsView = Em.View.extend({
  templateName : require('templates/main/jobs/hive_job_details'),

  selectedVertex : null,
  content : null,
  zoomScaleFrom : 1,
  zoomScaleTo: 2,
  zoomScale : 1,
  zoomStep : function() {
    var zoomStep = 0.01;
    var zoomFrom = this.get('zoomScaleFrom');
    var zoomTo = this.get('zoomScaleTo');
    if (zoomFrom < zoomTo) {
      zoomStep = (zoomTo - zoomFrom) / 5;
    }
    return zoomStep;
  }.property('zoomScaleFrom', 'zoomScaleTo'),
  isGraphMaximized: false,

  showQuery : false,
  toggleShowQuery : function () {
    this.toggleProperty('showQuery');
  },
  toggleShowQueryText : function () {
    return this.get('showQuery') ? Em.I18n.t('jobs.hive.less') : Em.I18n.t('jobs.hive.more');
  }.property('showQuery'),

  summaryMetricType: 'input',
  summaryMetricTypesDisplay : [
    Em.I18n.t('jobs.hive.tez.metric.input'),
    Em.I18n.t('jobs.hive.tez.metric.output'),
   /* Em.I18n.t('jobs.hive.tez.metric.recordsRead'),
    Em.I18n.t('jobs.hive.tez.metric.recordsWrite'), */
    Em.I18n.t('jobs.hive.tez.metric.tezTasks'),
    Em.I18n.t('jobs.hive.tez.metric.spilledRecords')
  ],
  summaryMetricTypeDisplay: function(){
    return Em.I18n.t('jobs.hive.tez.metric.'+this.get('summaryMetricType'));
  }.property('summaryMetricType'),

  sortedVertices : function() {
    var sortColumn = this.get('controller.sortingColumn');
    if(sortColumn && sortColumn.get('status')){
      var sortColumnStatus = sortColumn.get('status');
      var sorted = sortColumn.get('parentView').sort(sortColumn, sortColumnStatus === "sorting_desc", true);
      sortColumn.set('status', sortColumnStatus);
      return sorted;
    }
    var vertices = this.get('controller.content.tezDag.vertices');
    if (vertices != null) {
      vertices = vertices.toArray();
      return vertices;
    }
    return vertices;
  }.property('content.tezDag.vertices','controller.sortingColumn'),

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
    case Em.I18n.t('jobs.hive.tez.metric.spilledRecords'):
      summaryType = 'spilledRecords';
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
   *  },
   *  'started': 'Feb 12, 2014 10:30am',
   *  'ended': 'Feb 12, 2014 10:35am',
   *  'status': 'Running'
   * }
   */
  selectedVertexIODisplay : function() {
    var v = this.get('selectedVertex');
    var status = v.get('state');
    if (status) {
      status = stringUtils.getCamelCase(status);
    }
    var fileReadOps = v.get('fileReadOps');
    var fileWriteOps = v.get('fileWriteOps');
    var hdfsReadOps = v.get('hdfsReadOps');
    var hdfsWriteOps = v.get('hdfsWriteOps');
    var naString = Em.I18n.t('common.na');
    if (fileReadOps === null) {
      fileReadOps = naString;
    }
    if (fileWriteOps === null) {
      fileWriteOps = naString;
    }
    if (hdfsReadOps === null) {
      hdfsReadOps = naString;
    }
    if (hdfsWriteOps === null) {
      hdfsWriteOps = naString;
    }
    return {
      file : {
        read : {
          ops : Em.I18n.t('jobs.hive.tez.reads').format(fileReadOps),
          bytes : numberUtils.bytesToSize(v.get('fileReadBytes'))
        },
        write : {
          ops : Em.I18n.t('jobs.hive.tez.writes').format(fileWriteOps),
          bytes : numberUtils.bytesToSize(v.get('fileWriteBytes'))
        }
      },
      hdfs : {
        read : {
          ops : Em.I18n.t('jobs.hive.tez.reads').format(hdfsReadOps),
          bytes : numberUtils.bytesToSize(v.get('hdfsReadBytes'))
        },
        write : {
          ops : Em.I18n.t('jobs.hive.tez.writes').format(hdfsWriteOps),
          bytes : numberUtils.bytesToSize(v.get('hdfsWriteBytes'))
        }
      },
      records : {
        read : v.get('recordReadCount') == null ? null : Em.I18n.t('jobs.hive.tez.records.count').format(v.get('recordReadCount')),
        write : v.get('recordWriteCount') == null ? null : Em.I18n.t('jobs.hive.tez.records.count').format(v.get('recordWriteCount'))
      },
      started: v.get('startTime') ? dateUtils.dateFormat(v.get('startTime'), true) : '',
      ended: v.get('endTime') ? dateUtils.dateFormat(v.get('endTime'), true) : '',
      status: status
    };
  }.property('selectedVertex.fileReadOps', 'selectedVertex.fileWriteOps', 'selectedVertex.hdfsReadOps', 'selectedVertex.hdfdWriteOps',
      'selectedVertex.fileReadBytes', 'selectedVertex.fileWriteBytes', 'selectedVertex.hdfsReadBytes', 'selectedVertex.hdfdWriteBytes',
      'selectedVertex.recordReadCount', 'selectedVertex.recordWriteCount', 'selectedVertex.status'),

  canGraphZoomIn : function() {
    var zoomTo = this.get('zoomScaleTo');
    var zoomScale = this.get('zoomScale');
    console.debug("canGraphZoomIn? : ", (zoomScale < zoomTo), " (scaleTo=", zoomTo,", scale=",zoomScale,")");
    return zoomScale < zoomTo;
  }.property('zoomScale', 'zoomScaleTo'),

  canGraphZoomOut : function() {
    var zoomFrom = this.get('zoomScaleFrom');
    var zoomScale = this.get('zoomScale');
    console.debug("canGraphZoomOut? : ", (zoomScale > zoomFrom), " (scaleFrom=", zoomFrom,", scale=",zoomScale,")");
    return zoomScale > zoomFrom;
  }.property('zoomScale', 'zoomScaleFrom'),

  doGraphZoomIn: function() {
    var zoomTo = this.get('zoomScaleTo');
    var zoomScale = this.get('zoomScale');
    var zoomStep = this.get('zoomStep');
    if (zoomScale < zoomTo) {
      var step = Math.min(zoomStep, (zoomTo - zoomScale));
      zoomScale += step;
      console.debug("doGraphZoomIn(): New scale = ", zoomScale);
      this.set('zoomScale', zoomScale);
    }
  },

  doGraphZoomOut: function() {
    var zoomFrom = this.get('zoomScaleFrom');
    var zoomScale = this.get('zoomScale');
    var zoomStep = this.get('zoomStep');
    if (zoomScale > zoomFrom) {
      var step = Math.min(zoomStep, (zoomScale - zoomFrom));
      zoomScale -= step;
      console.debug("doGraphZoomOut(): New scale = ", zoomScale);
      this.set('zoomScale', zoomScale);
    }
   },

   doGraphMaximize: function() {
     this.set('isGraphMaximized', true);
   },

   doGraphMinimize: function() {
     this.set('isGraphMaximized', false);
   }
});

App.MainHiveJobDetailsVerticesTableView = App.TableView.extend({
  sortView: sort.wrapperView,

  didInsertElement: function () {
    if(!this.get('controller.sortingColumn')){
      var columns = this.get('childViews')[0].get('childViews')
      if(columns && columns.findProperty('name', 'name')){
        columns.findProperty('name','name').set('status', 'sorting_asc');
        this.get('controller').set('sortingColumn', columns.findProperty('name','name'))
      }
    }
  },

  nameSort: sort.fieldView.extend({
    column: 0,
    name: 'name',
    displayName: Em.I18n.t('common.name'),
    type: 'string'
  }),
  tasksSort: sort.fieldView.extend({
    column: 1,
    name: 'tasksNumber',
    displayName: Em.I18n.t('common.tasks'),
    type: 'number'
  }),
  inputSort: sort.fieldView.extend({
    column: 2,
    name: 'totalReadBytesDisplay',
    displayName: Em.I18n.t('apps.item.dag.input'),
    type: 'number'
  }),
  outputSort: sort.fieldView.extend({
    column: 3,
    name: 'totalWriteBytesDisplay',
    displayName: Em.I18n.t('apps.item.dag.output'),
    type: 'number'
  }),
  durationSort: sort.fieldView.extend({
    column: 4,
    name: 'durationDisplay',
    displayName: Em.I18n.t('apps.item.dag.duration'),
    type: 'number'
  })
});