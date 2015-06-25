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
  'models/VTopology',
  'collection/VTopologyList',
  'utils/TableLayout',
  'utils/LangSupport',
  'bootbox',
  'utils/Utils',
  'utils/Globals',
  'hbs!tmpl/topology/topologySummary',
  'bootstrap'
], function(require, vent, mTopology, cTopologyList, TableLayout, localization, bootbox, Utils, Globals, tmpl) {
  'use strict';

  var TopologySummaryTableLayout = Marionette.LayoutView.extend({

    template: tmpl,

    templateHelpers: function() {},

    events: {
      // 'click [data-id="deployBtn"]': 'evDeployTopology'
    },

    ui: {
      summaryDetails: '[data-id="summary"]'
    },

    regions: {
      'rTableList': '#summaryTable',
    },

    initialize: function() {
      this.collection = new cTopologyList();
      vent.trigger('Breadcrumb:Hide');
    },

    fetchSummary: function(topologyName) {
      var that = this;
      this.collection.fetch({
        success: function(collection, response, options) {
          vent.trigger('LastUpdateRefresh');
          if (collection && collection.length) {
            var arr = [];
            _.each(collection.models[0].get('topologies'), function(object){
              if(!_.isUndefined(topologyName) && object.name === topologyName){
                Backbone.history.navigate('!/topology/'+object.id, {trigger:true});
              } else {
                arr.push(new mTopology(object));
              }
            });
            if(_.isUndefined(topologyName)){
              that.countActive = 0;
              that.collection.reset(arr);
              that.showSummaryDetail();
            } else {
              $('.loading').hide();
            }
          }
        },
        error: function(collection, response, options){
          vent.trigger('LastUpdateRefresh');
          Utils.notifyError(response.statusText);
        }
      })
    },

    onRender: function() {
      this.showSummaryTable(this.collection);
      $('.loading').hide();
      this.fetchSummary();
      this.showSummaryDetail();
    },

    showSummaryDetail: function() {
      var totalTopologies = 0,
        activeTopologies = 0,
        inactiveTopologies = 0;
      if (this.collection && this.collection.length) {
        totalTopologies = this.collection.length;
        activeTopologies = this.countActive;
        inactiveTopologies = this.collection.length - this.countActive;
      }
      var template = _.template('<label style="margin-right:10px">' + localization.tt('lbl.topologySummary') + ' </label>' +
        '<span class="topology-summary-stats"><%- total%> ' + localization.tt('lbl.total') + '</span> | ' +
        '<span class="topology-summary-stats"><%- active%> ' + localization.tt('lbl.active') + '</span> | ' +
        '<span class="topology-summary-stats"><%- inactive%> ' + localization.tt('lbl.inactive') + '</span>');
      this.ui.summaryDetails.html(template({
        total: totalTopologies,
        active: activeTopologies,
        inactive: inactiveTopologies
      }));
    },

    showSummaryTable: function(collection) {
      this.rTableList.show(new TableLayout({
        columns: this.getColumns(),
        collection: this.collection,
        gridOpts: {
          emptyText: localization.tt('msg.noTopologyFound')
        }
      }));
      this.rTableList.$el.find('[data-id="r_tableList"]').attr("style","height:655px");
    },

    getColumns: function() {
      var that = this;
      var cols = [{
        name: "name",
        cell: "uri",
        href: function(model) {
          if(_.isEqual(model.get('status'),'ACTIVE')){
            that.countActive++;
          }
          return '#!/topology/' + model.get('id');
        },
        label: localization.tt("lbl.name"),
        sortable: true,
        editable: false,
        hasTooltip: true,
        tooltipText: localization.tt("msg.topologySummaryName")
      }, {
        name: "id",
        cell: "string",
        label: localization.tt("lbl.id"),
        sortable: true,
        editable: false,
        hasTooltip: true,
        tooltipText: localization.tt("msg.topologySummaryId")
      }, {
        name: "owner",
        cell: "string",
        label: localization.tt("lbl.owner"),
        sortable: true,
        editable: false,
        hasTooltip: true,
        tooltipText: localization.tt("msg.topologySummaryOwner")
      }, {
        name: "status",
        cell: "string",
        label: localization.tt("lbl.status"),
        sortable: true,
        editable: false,
        hasTooltip: true,
        tooltipText: localization.tt("msg.topologySummaryStatus")
      }, {
        name: "uptime",
        cell: "string",
        label: localization.tt("lbl.uptime"),
        sortable: true,
        editable: false,
        hasTooltip: true,
        tooltipText: localization.tt("msg.topologySummaryUptime")
      }, {
        name: "workersTotal",
        cell: "string",
        label: localization.tt("lbl.workers"),
        sortable: true,
        editable: false,
        hasTooltip: true,
        tooltipText: localization.tt("msg.topologySummaryWorkers")
      }, {
        name: "executorsTotal",
        cell: "string",
        label: localization.tt("lbl.executors"),
        sortable: true,
        editable: false,
        hasTooltip: true,
        tooltipText: localization.tt("msg.topologySummaryExecutors")
      }, {
        name: "tasksTotal",
        cell: "string",
        label: localization.tt("lbl.tasks"),
        sortable: true,
        editable: false,
        hasTooltip: true,
        tooltipText: localization.tt("msg.topologySummaryTasks")
      }, {
        name: "schedulerInfo",
        cell: "string",
        label: localization.tt("lbl.schedulerInfo"),
        sortable: true,
        editable: false,
        hasTooltip: true,
        tooltipText: localization.tt("msg.topologySummaryScheduler")
      }];
      return cols;
    },

    evDeployTopology: function(e) {
      var that = this;

      if (that.view) {
        that.onDialogClosed();
      }

      require(['views/Topology/TopologyForm'], function(TopologyFormView) {
        that.view = new TopologyFormView();
        that.view.render();

        bootbox.dialog({
          message: that.view.el,
          title: localization.tt('btn.deployNewTopology'),
          className: "topology-modal",
          buttons: {
            cancel: {
              label: localization.tt('btn.cancel'),
              className: "btn-default",
              callback: function() {
                that.onDialogClosed();
              }
            },
            success: {
              label: localization.tt('btn.save'),
              className: "btn-success",
              callback: function() {
                var errs = that.view.validate();
                 if(_.isEmpty(errs)){
                  that.submitTopology();
                } else return false;
              }
            }
          }
        });
      });
    },

    submitTopology: function() {
      Utils.notifyInfo(localization.tt("dialogMsg.topologyBeingDeployed"));
      var attrs = this.view.getData(),
          formData = new FormData(),
          obj = {},
          url = Globals.baseURL + '/api/v1/uploadTopology',
          that = this;

      if(!_.isEqual(attrs.jar.name.split('.').pop().toLowerCase(),'jar')){
        Utils.notifyError(localization.tt("dialogMsg.invalidFile"));
        return false;
      }
      formData.append('topologyJar', attrs.jar);
      obj.topologyMainClass = attrs.topologyClass;
      obj.topologyMainClassArgs = [];
      obj.topologyMainClassArgs.push(attrs.name);

      if(!_.isEmpty(attrs.arguments)){
        Array.prototype.push.apply(obj.topologyMainClassArgs,attrs.arguments.split(' '));
      }

      formData.append("topologyConfig", JSON.stringify(obj));

      var successCallback = function(response){
        if(_.isString(response)){
          response = JSON.parse(response);
        }
        if(_.isEqual(response.status, 'failed')){
          $('.loading').hide();
          Utils.notifyError(response.error);
        } else {
          Utils.notifySuccess(localization.tt("dialogMsg.topologyDeployedSuccessfully"));
          that.fetchSummary(attrs.name);
        }
      };

      var errorCallback = function(){
        $('.loading').hide();
        Utils.notifyError(localization.tt("dialogMsg.topologyDeployFailed"));
      };

      Utils.uploadFile(url,formData,successCallback, errorCallback);
      $('.loading').show();
    },

    onDialogClosed: function() {
      if (this.view) {
        this.view.close();
        this.view.remove();
        this.view = null;
      }
    }

  });
  return TopologySummaryTableLayout;
});