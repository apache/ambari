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

define(['require',
  'modules/Vent',
  'models/Cluster',
  'models/VNimbus',
  'models/VSupervisor',
  'models/VNimbusConfig',
  'utils/TableLayout',
  'utils/LangSupport',
  'utils/Globals',
  'utils/Utils',
  'hbs!tmpl/cluster/clusterSummary',
  'backgrid'
], function(require, vent, vCluster, vNimbus, vSupervisor, vNimbusConfig, TableLayout, localization, Globals, Utils, tmpl) {
  'use strict';

  var ClusterSummaryTableLayout = Marionette.LayoutView.extend({

    template: tmpl,

    templateHelpers: function() {},

    ui: {

      clusterSummaryDetails: '[data-id="clusterSummary"]',
      nbsSummaryDetails: '[data-id="nbsSummary"]',
      sprsSummaryDetails: '[data-id="sprSummary"]',
      nbsConfigDetails: '[data-id="nbsConfig"]'
    },

    regions: {
      'rCluster': '#clusterSummaryTable',
      'rNbsList': '#nbsSummaryTable',
      'rSprList': '#sprSummaryTable',
      'rnbsConfigList': '#nbsConfigTable'
    },

    initialize: function() {
      this.clusterModel = new vCluster();
      this.supervisorModel = new vSupervisor();
      this.nimbusSummaryModel = new vNimbus();
      this.nimbusConfigModel = new vNimbusConfig();

      this.clusterCollection = new Backbone.Collection();
      this.sprCollection = new Backbone.Collection();
      this.nimbusConfigCollection = new Backbone.Collection();
      this.nbsCollection = new Backbone.Collection();

    },

    onRender: function() {
      this.showCtrSummary(this.clusterCollection);
      this.showNbsSummary(this.nbsCollection);
      this.showSprSummary(this.sprCollection);
      this.showNbsConSummary(this.nimbusConfigCollection);
      this.fetchData();

      this.$('.collapse').on('shown.bs.collapse', function() {
        $(this).parent().find(".fa-plus-square").removeClass("fa-plus-square").addClass("fa-minus-square");
      }).on('hidden.bs.collapse', function() {
        $(this).parent().find(".fa-minus-square").removeClass("fa-minus-square").addClass("fa-plus-square");
      });

    },
    fetchData: function() {
      this.getClusterSummary(this.clusterModel);
      this.getSupervisorSummary(this.supervisorModel);
      this.getNimbusConfig(this.nimbusConfigModel);
      this.getNimbusSummary(this.nimbusSummaryModel);
    },

    getClusterSummary: function(model) {
      var that = this;
      this.clusterCollection.trigger('request', this.clusterCollection);
      model.fetch({
        success: function(model, response, options) {
          vent.trigger('LastUpdateRefresh');
          that.clusterCollection.trigger('sync', that.clusterCollection);
          if (model) {
            that.clusterCollection.reset(model);
          }
        },
        error: function(model, response, options) {
          vent.trigger('LastUpdateRefresh');
          that.clusterCollection.trigger('error', that.clusterCollection);
          Utils.notifyError(response.statusText);
          return null;
        }
      });
    },

    getSupervisorSummary: function(model) {
      var that = this;
      this.sprCollection.trigger('request', this.sprCollection);
      model.fetch({
        success: function(model, response, options) {
          vent.trigger('LastUpdateRefresh');
          that.sprCollection.trigger('sync', that.sprCollection);
          if (model.has('supervisors') && model.get('supervisors').length) {
            var arr = [];
            _.each(model.get('supervisors'), function(object) {
              arr.push(new vSupervisor(object))
            });
            that.sprCollection.reset(arr);
          }
        },
        error: function(model, response, options) {
          vent.trigger('LastUpdateRefresh');
          that.sprCollection.trigger('error', that.sprCollection);
          Utils.notifyError(response.statusText);
        }
      });
    },

    getNimbusConfig: function(model) {
      var that = this;
      this.nimbusConfigCollection.trigger('request', this.nimbusConfigCollection);
      model.fetch({
        success: function(model, response, options) {
          vent.trigger('LastUpdateRefresh');
          that.nimbusConfigCollection.trigger('sync', that.nimbusConfigCollection);
          if (model) {
            var arr = [];
            for(var key in model.attributes){
              var obj = {};
              obj.key = key;
              obj.value = model.get(key);
              arr.push(new vNimbusConfig(obj));
            }
            that.nimbusConfigCollection.reset(arr);
          }
        },
        error: function(model, response, options) {
          vent.trigger('LastUpdateRefresh');
          that.nimbusConfigCollection.trigger('error', that.nimbusConfigCollection);
          Utils.notifyError(response.statusText);
        }
      });
    },

    getNimbusSummary: function(model){
      var that = this;
      this.nbsCollection.trigger('request', this.nbsCollection);
      model.fetch({
        success: function(model, response, options) {
          vent.trigger('LastUpdateRefresh');
          that.nbsCollection.trigger('sync', that.nbsCollection);
          if (model.has('nimbuses') && model.get('nimbuses').length) {
            var arr = [];
            _.each(model.get('nimbuses'), function(object) {
              arr.push(new vNimbus(object))
            });
            that.nbsCollection.reset(arr);
          }
        },
        error: function(model, response, options) {
          vent.trigger('LastUpdateRefresh');
          that.nbsCollection.trigger('error', that.nbsCollection);
          Utils.notifyError(response.statusText);
        }
      });
    },

    showCtrSummary: function(collection) {
      this.rCluster.show(new TableLayout({
        columns: this.getCtrColumns(),
        collection: collection,
        gridOpts: {
          emptyText: localization.tt('msg.noClusterFound'),
          className: 'table table-borderless table-striped cluster-table'
        }
      }));
    },

    showNbsSummary: function(collection) {
      this.rNbsList.show(new TableLayout({
        columns: this.getNbsColumns(),
        collection: this.nbsCollection,
        gridOpts: {
          emptyText: localization.tt('msg.noNimbusFound'),
          className: 'table table-borderless table-striped cluster-table'
        }
      }));
    },

    showSprSummary: function(collection) {
      this.rSprList.show(new TableLayout({
        columns: this.getSprColumns(),
        collection: collection,
        gridOpts: {
          emptyText: localization.tt('msg.noSupervisorFound'),
          className: 'table table-borderless table-striped cluster-table'
        }
      }));
    },

    showNbsConSummary: function(collection) {
      this.rnbsConfigList.show(new TableLayout({
        columns: this.getNbsConColumns(),
        collection: collection,
        gridOpts: {
          emptyText: localization.tt('msg.noNimbusConfigFound'),
          className: 'table table-borderless table-striped cluster-table'
        }
      }));
    },

    getCtrColumns: function() {
      return [{
        name: "supervisors",
        cell: "string",
        label: localization.tt("lbl.supervisors"),
        hasTooltip: true,
        tooltipText: localization.tt('msg.clusterSummarySupervisors')

      }, {
        name: "slotsUsed",
        cell: "string",
        label: localization.tt("lbl.usedSlots"),
        hasTooltip: true,
        tooltipText: localization.tt('msg.clusterSummarySlots')
      }, {
        name: "slotsFree",
        cell: "string",
        label: localization.tt("lbl.freeSlots"),
        hasTooltip: true,
        tooltipText: localization.tt('msg.clusterSummarySlots')

      }, {
        name: "slotsTotal",
        cell: "string",
        label: localization.tt("lbl.totalSlots"),
        hasTooltip: true,
        tooltipText: localization.tt('msg.clusterSummarySlots')
      }, {
        name: "executorsTotal",
        cell: "string",
        label: localization.tt("lbl.executors"),
        hasTooltip: true,
        tooltipText: localization.tt('msg.clusterSummaryExecutors')
      }, {
        name: "tasksTotal",
        cell: "string",
        label: localization.tt("lbl.tasks"),
        hasTooltip: true,
        tooltipText: localization.tt('msg.clusterSummaryTasks')
      }];
    },

    getNbsColumns: function() {
      return [{
          name: "host",
          cell: "string",
          label: localization.tt("lbl.host")
        }, {
          name: "port",
          cell: "string",
          label: localization.tt("lbl.port")
        }, {
          name: "status",
          cell: "string",
          label: localization.tt("lbl.status")
        }, {
          name: "version",
          cell: "string",
          label: localization.tt("lbl.version")
        }, {
          name: "nimbusUpTime",
          cell: "string",
          label: localization.tt("lbl.uptimeSeconds")
        }, {
          name: "logs",
          cell: "Html",
          label: '',
          formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
            fromRaw: function(rawValue, model) {
              if (model) {
                return "<a href="+model.get('nimbusLogLink')+" target='_blank' class='btn btn-success btn-xs center-block'>"+localization.tt('lbl.viewLogs')+"</a>";
              }
            }
          })
        }
      ];
    },

    getSprColumns: function() {
      return [{
          name: "id",
          cell: "string",
          label: localization.tt("lbl.id"),
          hasTooltip: true,
          tooltipText: localization.tt('msg.supervisorId')
        }, {
          name: "host",
          cell: "string",
          label: localization.tt("lbl.host"),
          hasTooltip: true,
          tooltipText: localization.tt('msg.supervisorHost')
        }, {
          name: "uptime",
          cell: "string",
          label: localization.tt("lbl.uptime"),
          hasTooltip: true,
          tooltipText: localization.tt('msg.supervisorUptime')
        }, {
          name: "slotsTotal",
          cell: "string",
          label: localization.tt("lbl.slots"),
          hasTooltip: true,
          tooltipText: localization.tt('msg.clusterSummarySlots')
        }, {
          name: "slotsUsed",
          cell: "string",
          label: localization.tt("lbl.usedSlots"),
          hasTooltip: true,
          tooltipText: localization.tt('msg.clusterSummarySlots')
        }
      ];
    },

    getNbsConColumns: function() {
      var cols = [{
        name: "key",
        cell: "string",
        label: localization.tt("lbl.key"),
      }, {
        name: "value",
        cell: "string",
        label: localization.tt("lbl.value")
      }];
      return cols;
    }

  });
  return ClusterSummaryTableLayout;
});