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

define([
  'require',
  'utils/LangSupport',
  'models/VOutputStat',
  'models/VExecutor',
  'models/VError',
  'utils/TableLayout',
  'hbs!tmpl/spout/spoutItemView'
], function(require, localization, vOutputStat, vExecutors, vError, TableLayout, tmpl) {
  'use strict';

  var spoutItemView = Marionette.ItemView.extend({
    template: tmpl,
    tagName: 'div',
    templateHelpers: function() {
      return {
        id: this.model.id,
        name: this.model.get('name')
      };
    },
    initialize: function(options) {
      this.spoutsCollection = new Backbone.Collection();
      options.model.opstCollection = new Backbone.Collection();
      options.model.extrCollection = new Backbone.Collection();
      options.model.errorCollection = new Backbone.Collection();
      if(!_.isUndefined(options.topologyId) && options.model.has('errorWorkerLogLink')){
        this.getDetails(options.model, options.topologyId, options.systemBoltFlag, options.windowTimeFrame);
      }
    },

    events: {},

    onRender: function() {
      this.showSpoutsSummaryTable();
      this.showOpstSummaryTable();
    },

    getDetails: function(model, topologyId, systemBoltFlag, windowTimeFrame) {
      var that = this;
      this.spoutsCollection.trigger('request', this.spoutsCollection);
      model.getDetails({
        topologyId: topologyId,
        spoutId: model.get('spoutId'),
        systemBoltFlag: systemBoltFlag,
        windowTimeFrame: windowTimeFrame,
        success: function(spoutsModel, response, options) {
          that.spoutsCollection.trigger('sync', that.spoutsCollection);
          if (spoutsModel) {
            spoutsModel = new Backbone.Model(spoutsModel);
            if (spoutsModel.has('outputStats') && spoutsModel.get('outputStats').length) {

              var arr = [];
              _.each(spoutsModel.get('outputStats'), function(object) {
                arr.push(new vOutputStat(object))
              });
              model.opstCollection.reset(arr);
            }

            if (spoutsModel.has('executorStats') && spoutsModel.get('executorStats').length) {
              var arr = [];
              _.each(spoutsModel.get('executorStats'), function(object) {
                arr.push(new vExecutors(object))
              });
              model.extrCollection.reset(arr);
            }

            if (spoutsModel.has('componentErrors') && spoutsModel.get('componentErrors').length) {

              var arr = [];
              _.each(spoutsModel.get('componentErrors'), function(object) {
                arr.push(new vError(object))
              });
              model.errorCollection.reset(arr);
            }

            that.spoutsCollection.reset(model);
          }
        },
        error: function() {
          that.spoutsCollection.trigger('error', that.spoutsCollection);
          return null;
        }
      });
    },

    showSpoutsSummaryTable: function() {

      this.$('[data-id="SpoutsSummaryTable"]').html(new TableLayout({
        columns: this.getSpoutColumns(),
        collection: this.spoutsCollection,
        includeFilter: false,
        includePagination: false,
        includeFooterRecords: false,
        gridOpts: {
          emptyText: localization.tt('msg.noSpoutFound'),
          className: 'table table-borderless table-striped table-header'
        }
      }).render().$el);
    },

    showOpstSummaryTable: function() {
      var that = this;

      that.$('[data-id="OpstSummaryTable"]').html(new TableLayout({
        columns: that.getOpstColumns(),
        collection: that.model.opstCollection,
        includeFilter: false,
        includePagination: false,
        includeFooterRecords: false,
        gridOpts: {
          emptyText: localization.tt('msg.noOutputStatsFound'),
          className: 'table table-borderless table-striped'
        }
      }).render().$el);

      that.$('[data-id="ExtrSummaryTable"]').html(new TableLayout({
        columns: that.getExtrColumns(),
        collection: that.model.extrCollection,
        includeFilter: false,
        includePagination: false,
        includeFooterRecords: false,
        gridOpts: {
          emptyText: localization.tt('msg.noExecutorsFound'),
          className: 'table table-borderless table-striped'
        }
      }).render().$el);

      that.$('[data-id="ErrorSummaryTable"]').html(new TableLayout({
        columns: that.getErrorColumns(),
        collection: that.model.errorCollection,
        includeFilter: false,
        includePagination: false,
        includeFooterRecords: false,
        gridOpts: {
          emptyText: localization.tt('msg.noErrorFound'),
          className: 'table table-borderless table-striped'
        }
      }).render().$el);
    },


    getSpoutColumns: function() {
      var cols = [{
        name: "spoutId",
        cell: "string",
        label: localization.tt('lbl.id'),
        sortable: true,
        hasTooltip: true,
        tooltipText: localization.tt('msg.spoutId')
      }, {
        name: "executors",
        cell: "string",
        label: localization.tt('lbl.executors'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.spoutExecutors')
      }, {
        name: "tasks",
        cell: "string",
        label: localization.tt('lbl.tasks'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.spoutTasks')
      }, {
        name: "emitted",
        cell: "string",
        label: localization.tt('lbl.emitted'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.emitted')
      }, {
        name: "transferred",
        cell: "string",
        label: localization.tt('lbl.transferred'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.transferred')
      }, {
        name: "completeLatency",
        cell: "string",
        label: localization.tt('lbl.completeLatency'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.completeLatency')
      }, {
        name: "acked",
        cell: "string",
        label: localization.tt('lbl.acked'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.acked')
      }, {
        name: "failed",
        cell: "string",
        label: localization.tt('lbl.failed'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.failed')
      }];
      return cols;
    },

    getOpstColumns: function() {
      var cols = [{
        name: "stream",
        cell: "string",
        label: localization.tt('lbl.stream'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.stream'),
        sortable: true
      }, {
        name: "emitted",
        cell: "string",
        label: localization.tt('lbl.emitted'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.emitted')
      }, {
        name: "transferred",
        cell: "string",
        label: localization.tt('lbl.transferred'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.transferred')
      }, {
        name: "completeLatency",
        cell: "string",
        label: localization.tt('lbl.completeLatencyMS'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.completeLatency')
      }, {
        name: "acked",
        cell: "string",
        label: localization.tt('lbl.acked'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.acked')
      }, {
        name: "failed",
        cell: "string",
        label: localization.tt('lbl.failed'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.failed')
      }];
      return cols;
    },

    getExtrColumns: function() {
      var cols = [{
        name: "id",
        cell: "string",
        label: localization.tt('lbl.id'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.uniqueExecutorId'),
        sortable: true
      }, {
        name: "uptime",
        cell: "string",
        label: localization.tt('lbl.uptime'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.extensionUptime')
      }, {
        name: "host",
        cell: "string",
        label: localization.tt('lbl.host'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.extensionHost')
      }, {
        name: "port",
        cell: "string",
        label: localization.tt('lbl.port'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.extensionPort')
      }, {
        name: "emitted",
        cell: "string",
        label: localization.tt('lbl.emitted'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.emitted')
      }, {
        name: "transferred",
        cell: "string",
        label: localization.tt('lbl.transferred'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.transferred')
      }, {
        name: "completeLatency",
        cell: "string",
        label: localization.tt('lbl.completeLatencyMS'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.completeLatency')
      }, {
        name: "acked",
        cell: "string",
        label: localization.tt('lbl.acked'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.acked')
      }, {
        name: "failed",
        cell: "string",
        label: localization.tt('lbl.failed'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.failed')
      }, {
        name: "logs",
        cell: "Html",
        label: '',
        sortable: false,
        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
          fromRaw: function(rawValue, model) {
            if (model) {
              return "<a href="+model.get('workerLogLink')+" target='_blank' class='btn btn-success btn-xs center-block'>"+localization.tt('lbl.viewLogs')+"</a>";
            }
          }
        })
      }];
      return cols;
    },

    getErrorColumns: function() {
      var cols = [
        {
          name: "time",
          cell: "string",
          label: localization.tt('lbl.time'),
          sortable: true
        }, {
          name: "errorHost",
          cell: "string",
          label: localization.tt('lbl.errorHost'),
        }, {
          name: "errorPort",
          cell: "string",
          label: localization.tt('lbl.errorPort'),
        }, {
          name: "error",
          cell: "string",
          label: localization.tt('lbl.error'),
        }
      ];
      return cols;
    },

    onClose: function() {}
  });
  return spoutItemView;
});