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

App.JobView = Em.View.extend({

  templateName: 'job/job',

  selectedVertex: null,

  content: null,

  zoomScaleFrom: 1,

  zoomScaleTo: 2,

  zoomScale: 1,

  /**
   * Is query visible
   * @type {bool}
   */
  showQuery: false,

  /**
   * Current graph zoom
   * @type {number}
   */
  zoomStep: function () {
    var zoomStep = 0.01;
    var zoomFrom = this.get('zoomScaleFrom');
    var zoomTo = this.get('zoomScaleTo');
    if (zoomFrom < zoomTo) {
      zoomStep = (zoomTo - zoomFrom) / 5;
    }
    return zoomStep;
  }.property('zoomScaleFrom', 'zoomScaleTo'),

  /**
   * Is graph in maximum zoom
   * @type {bool}
   */
  isGraphMaximized: false,

  actions: {

    /**
     * Summary metric change handler
     * @param {string} summaryType
     * @method doSelectSummaryMetricType
     */
    doSelectSummaryMetricType: function (summaryType) {
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
     * "Show more/less" click handler
     * @method toggleShowQuery
     */
    toggleShowQuery: function () {
      this.toggleProperty('showQuery');
      var queryBlock = $('.query-info');
      if (this.get('showQuery')) {
        queryBlock.slideDown();
      }
      else {
        queryBlock.slideUp();
      }
    },

    /**
     * Click handler for vertex-name in the table of vertexes
     * @param {App.TezDagVertex} event
     * @param {bool} notTableClick
     * @method actionDoSelectVertex
     */
    actionDoSelectVertex: function (event, notTableClick) {
      this.doSelectVertex(event, notTableClick);
    },

    /**
     * Zoom-In click-handler
     * @method doGraphZoomIn
     */
    doGraphZoomIn: function () {
      var zoomTo = this.get('zoomScaleTo'),
        zoomScale = this.get('zoomScale'),
        zoomStep = this.get('zoomStep');
      if (zoomScale < zoomTo) {
        zoomScale += Math.min(zoomStep, (zoomTo - zoomScale));
        this.set('zoomScale', zoomScale);
      }
    },

    /**
     * Zoom-out click-handler
     * @method doGraphZoomOut
     */
    doGraphZoomOut: function () {
      var zoomFrom = this.get('zoomScaleFrom'),
        zoomScale = this.get('zoomScale'),
        zoomStep = this.get('zoomStep');
      if (zoomScale > zoomFrom) {
        zoomScale -= Math.min(zoomStep, (zoomScale - zoomFrom));
        this.set('zoomScale', zoomScale);
      }
    },

    /**
     * Maximize graph
     * @method doGraphMaximize
     */
    doGraphMaximize: function () {
      this.set('isGraphMaximized', true);
    },

    /**
     * Minimize graph
     * @method doGraphMinimize
     */
    doGraphMinimize: function () {
      this.set('isGraphMaximized', false);
    }

  },

  /**
   * "Show more/less"-message
   * @type {string}
   */
  toggleShowQueryText: function () {
    return this.get('showQuery') ? Em.I18n.t('jobs.hive.less') : Em.I18n.t('jobs.hive.more');
  }.property('showQuery'),

  /**
   * Current metric type in the metrics-type listbox
   * @type {string}
   */
  summaryMetricType: 'input',

  /**
   * List of available values for <code>summaryMetricType</code>
   * @type {string[]}
   */
  summaryMetricTypesDisplay: [
    Em.I18n.t('jobs.hive.tez.metric.input'),
    Em.I18n.t('jobs.hive.tez.metric.output'),
    Em.I18n.t('jobs.hive.tez.metric.recordsRead'),
    Em.I18n.t('jobs.hive.tez.metric.recordsWrite'),
    Em.I18n.t('jobs.hive.tez.metric.tezTasks'),
    Em.I18n.t('jobs.hive.tez.metric.spilledRecords')
  ],

  /**
   * Display-value for <code>summaryMetricType</code>
   * @type {string}
   */
  summaryMetricTypeDisplay: function () {
    return Em.I18n.t('jobs.hive.tez.metric.' + this.get('summaryMetricType'));
  }.property('summaryMetricType'),

  /**
   * List of sorted vertexes for current job
   * @type {App.TezDagVertex[]}
   */
  sortedVertices: function () {
    var sortColumn = this.get('controller.sortingColumn');
    if (sortColumn && sortColumn.get('status')) {
      var sortColumnStatus = sortColumn.get('status');
      var sorted = sortColumn.get('parentView').sort(sortColumn, sortColumnStatus === "sorting_desc", true);
      sortColumn.set('status', sortColumnStatus);
      return sorted;
    }
    var vertices = this.get('content.tezDag.vertices');
    if (vertices != null) {
      vertices = vertices.toArray();
    }
    return vertices;
  }.property('content.tezDag.vertices', 'controller.sortingColumn'),

  /**
   * When all data loaded in the controller, set <code>content</code>-value
   * @method initialDataLoaded
   */
  initialDataLoaded: function () {
    if (this.get('controller.loaded')) {
      this.set('content', this.get('controller.content'));
    }
  }.observes('controller.loaded'),

  /**
   * Set proper value to <code>isSelected</code> for each vertex
   * @method jobObserver
   */
  jobObserver: function () {
    var content = this.get('content'),
      selectedVertex = this.get('selectedVertex');
    if (selectedVertex == null && content != null) {
      var vertices = content.get('tezDag.vertices');
      if (vertices) {
        vertices.setEach('isSelected', false);
        this.doSelectVertex(vertices.objectAt(0), false);
      }
    }
  }.observes('selectedVertex', 'content.tezDag.vertices.@each.id'),

  /**
   * Set <code>selectedVertex</code>
   * @param {App.TezDagVertex} newVertex
   * @param {bool} notTableClick
   * @method doSelectVertex
   */
  doSelectVertex: function (newVertex, notTableClick) {
    var currentVertex = this.get('selectedVertex');
    if (currentVertex != null) {
      currentVertex.set('isSelected', false);
    }
    newVertex.set('notTableClick', !!notTableClick);
    newVertex.set('isSelected', true);
    this.set('selectedVertex', newVertex);
  },

  /**
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
  selectedVertexIODisplay: {},

  /**
   * Handler to call <code>selectedVertexIODisplayObs</code> once
   * @method selectedVertexIODisplayObsOnce
   */
  selectedVertexIODisplayObsOnce: function() {
    Em.run.once(this, 'selectedVertexIODisplayObs');
  }.observes(
      'selectedVertex.fileReadOps',
      'selectedVertex.fileWriteOps',
      'selectedVertex.hdfsReadOps',
      'selectedVertex.hdfdWriteOps',
      'selectedVertex.fileReadBytes',
      'selectedVertex.fileWriteBytes',
      'selectedVertex.hdfsReadBytes',
      'selectedVertex.hdfdWriteBytes',
      'selectedVertex.recordReadCount',
      'selectedVertex.recordWriteCount',
      'selectedVertex.status'
    ),

  /**
   * Provides display information for vertex I/O.
   * @method selectedVertexIODisplayObs
   */
  selectedVertexIODisplayObs: function () {
    var v = this.get('selectedVertex'),
      naString = Em.I18n.t('common.na'),
      status = App.Helpers.string.getCamelCase(v.getWithDefault('state', '')),
      fileReadOps = v.getWithDefault('fileReadOps', naString),
      fileWriteOps = v.getWithDefault('fileWriteOps', naString),
      hdfsReadOps = v.getWithDefault('hdfsReadOps', naString),
      hdfsWriteOps = v.getWithDefault('hdfsWriteOps', naString),
      r = {
      file: {
        read: {
          ops: Em.I18n.t('jobs.hive.tez.reads').fmt(fileReadOps),
          bytes: App.Helpers.number.bytesToSize(v.get('fileReadBytes'))
        },
        write: {
          ops: Em.I18n.t('jobs.hive.tez.writes').fmt(fileWriteOps),
          bytes: App.Helpers.number.bytesToSize(v.get('fileWriteBytes'))
        }
      },
      hdfs: {
        read: {
          ops: Em.I18n.t('jobs.hive.tez.reads').fmt(hdfsReadOps),
          bytes: App.Helpers.number.bytesToSize(v.get('hdfsReadBytes'))
        },
        write: {
          ops: Em.I18n.t('jobs.hive.tez.writes').fmt(hdfsWriteOps),
          bytes: App.Helpers.number.bytesToSize(v.get('hdfsWriteBytes'))
        }
      },
      records: {
        read: v.get('recordReadCount') == null ? null : Em.I18n.t('jobs.hive.tez.records.count').fmt(v.get('recordReadCount')),
        write: v.get('recordWriteCount') == null ? null : Em.I18n.t('jobs.hive.tez.records.count').fmt(v.get('recordWriteCount'))
      },
      started: v.get('startTime') ? App.Helpers.date.dateFormat(v.get('startTime'), true, true) : '',
      ended: v.get('endTime') ? App.Helpers.date.dateFormat(v.get('endTime'), true, true) : '',
      status: status
    };
    this.set('selectedVertexIODisplay', r);
  },

  /**
   * Stop updating job info when user navigate away from job's page
   * @method willDestroyElement
   */
  willDestroyElement: function() {
    this.get('controller').stop();
  },

  /**
   * Can graph be zoomed-in
   * @type {bool}
   */
  canGraphZoomIn: function () {
    return this.get('zoomScale') < this.get('zoomScaleTo');
  }.property('zoomScale', 'zoomScaleTo'),

  /**
   * Can graph be zoomed-out
   * @type {bool}
   */
  canGraphZoomOut: function () {
    return this.get('zoomScale') > this.get('zoomScaleFrom');
  }.property('zoomScale', 'zoomScaleFrom')

});

App.MainHiveJobDetailsVerticesTableView = App.TableView.extend({

  sortView: App.Sorts.wrapperView,

  didInsertElement: function () {
    if (!this.get('controller.sortingColumn')) {
      var columns = this.get('childViews')[0].get('childViews');
      if (columns && columns.findProperty('name', 'name')) {
        columns.findProperty('name', 'name').set('status', 'sorting_asc');
        this.get('controller').set('sortingColumn', columns.findProperty('name', 'name'))
      }
    }
  },

  nameSort: App.Sorts.fieldView.extend({
    column: 0,
    name: 'name',
    displayName: Em.I18n.t('common.name'),
    type: 'string'
  }),

  tasksSort: App.Sorts.fieldView.extend({
    column: 1,
    name: 'tasksNumber',
    displayName: Em.I18n.t('common.tasks'),
    type: 'number'
  }),

  inputSort: App.Sorts.fieldView.extend({
    column: 2,
    name: 'totalReadBytes',
    displayName: Em.I18n.t('apps.item.dag.input'),
    type: 'number'
  }),

  outputSort: App.Sorts.fieldView.extend({
    column: 3,
    name: 'totalWriteBytes',
    displayName: Em.I18n.t('apps.item.dag.output'),
    type: 'number'
  }),

  durationSort: App.Sorts.fieldView.extend({
    column: 4,
    name: 'duration',
    displayName: Em.I18n.t('apps.item.dag.duration'),
    type: 'number'
  })

});
