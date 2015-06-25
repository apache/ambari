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
  'utils/Globals',
  'modules/Vent',
  'utils/LangSupport',
  'models/VTopology',
  'models/VSpout',
  'models/VBolt',
  'models/VTopologyConfig',
  'views/Spout/SpoutCollectionView',
  'views/Topology/TopologyGraphView',
  'utils/TableLayout',
  'utils/Utils',
  'bootbox',
  'hbs!tmpl/topology/topologyDetail'
], function(require, Globals, vent, localization, vTopology, vSpout, vBolt, vTopologyConfig, vSpoutCollectionView, vTopologyGraphView, TableLayout, Utils, bootbox, tmpl) {

  'use strict';

  var masterView = Marionette.LayoutView.extend({

    template: tmpl,

    regions: {
      'rTopologyDetailsTbl': '#topologyDetail',
      'rTopologyGraph': '#graph',
      'rTopologySummary': '#topo-summary',
      'rSpoutsTable': '#SpoutsTable',
      'rBoltsSummaryTable': '#BoltsSummaryTable',
      'rTopologyConfigTable': '#TopologyConfigTable'
    },

    ui: {
      topologySummary: '#topo-summary',
      summaryLoader: '#summaryLoader',
      BoltsSummaryDetails: '[data-id="BoltsSummary"]'
    },

    events: {
      'change #tFrame': 'evChangeTimeFrame',
      'click #btnActivate': 'evActivateTopology',
      'click #btnDeactivate': 'evDeactivateTopology',
      'click #btnRebalance': 'evRebalanceTopology',
      'click #btnKill': 'evKillTopology',
      'click #openAll': 'openAllTables',
      'change #sysBolt': 'evSysBoltToggle'
    },

    initialize: function(options) {
      this.model = new vTopology();
      this.systemBoltFlag = false;
      this.topologyDetailsColl = new Backbone.Collection();
      this.summaryArr = [];
      this.windowTimeFrame = ":all-time";
      this.fetchData(options.id, this.systemBoltFlag, this.windowTimeFrame);
      this.generateTemplate();

      this.spoutsCollection = new Backbone.Collection();
      this.boltsCollection = new Backbone.Collection();
      this.topoConfigCollection = new Backbone.Collection();
    },

    fetchData: function(id, flag, timeFrame){
      var that = this;
      $('.loading').show();
      this.model.getDetails({
        id: id,
        sysBoltFlag: flag,
        windowTimeFrame: timeFrame,
        success: function(model, response, options) {
          vent.trigger('LastUpdateRefresh', true);
          that.model = new vTopology(model);
          that.getDetails(that.model);
          that.render();
          that.disableBtnAction(model.status);
          that.$('#sysBolt').prop("checked",that.systemBoltFlag)
          $('.loading').hide();
        },
        error: function(model, response, options) {
          vent.trigger('LastUpdateRefresh', true);
          $('.loading').hide();
          Utils.notifyError(model.statusText);
        }
      });
    },

    generateTemplate: function() {
      this.summaryTemplate = _.template('<table class="table table-borderless"><tbody>' +
        '<tr>' +
        '<th>'+localization.tt('lbl.emitted')+'</th>' +
        '<td><%if (emitted) { %> <%=emitted%> <%} else { %> 0 <% } %></td>' +
        '</tr>' +
        '<tr>' +
        '<th>'+localization.tt('lbl.transferred')+'</th>' +
        '<td><%if (transferred) { %> <%=transferred%> <%} else { %> 0 <% } %></td>' +
        '</tr>' +
        '<tr>' +
        '<th>'+localization.tt('lbl.completeLatency')+'</th>' +
        '<td><%if (completeLatency) { %> <%=completeLatency%> <%} else { %> 0 <% } %></td>' +
        '</tr>' +
        '<tr>' +
        '<th>'+localization.tt('lbl.acked')+'</th>' +
        '<td><%if (acked) { %> <%=acked%> <%} else { %> 0 <% } %></td>' +
        '</tr>' +
        '<tr>' +
        '<th>'+localization.tt('lbl.failed')+'</th>' +
        '<td><%if (failed) { %> <%=failed%> <%} else { %> 0 <% } %></td>' +
        '</tr>' +
        '</tbody></table>');

    },

    onRender: function() {
      if(! this.$el.hasClass('topologyDetailView')){
        this.$el.addClass('topologyDetailView');
      }
      this.$('.topology-title').html(this.model.has('name') ? this.model.get('name') : '');
      this.showDetailsTable(this.topologyDetailsColl);
      if(!_.isUndefined(this.summaryArr) && this.summaryArr.length){
        this.setTimeFrameOptions();
      } else {
        $('.loading').hide();
      }

      this.windowTimeFrame = this.model.get('window');
      this.$('#tFrame').val(this.windowTimeFrame);
      this.showSummaryTable(this.windowTimeFrame);

      this.showBoltsSummaryTable();
      this.showTopologyConfigTable();

      this.showSpoutsSummaryTable();

      if(this.model.has('id')){
        this.rTopologyGraph.show(new vTopologyGraphView({
          id: this.model.get('id')
        }));
      }

      this.$('.collapse').on('shown.bs.collapse', function() {
        $(this).parent().find(".fa-plus-square").removeClass("fa-plus-square").addClass("fa-minus-square");
      }).on('hidden.bs.collapse', function() {
        $(this).parent().find(".fa-minus-square").removeClass("fa-minus-square").addClass("fa-plus-square");
      });

      this.$('[data-id="r_tableList"]').parent().removeClass('col-md-12');
      if(this.$el.parent().hasClass('active')){
        this.showBreadCrumbs();
      }
    },

    disableBtnAction: function(status){
      _.each(this.$el.find('.btn.btn-success.btn-sm'),function(elem){
        $(elem).removeAttr('disabled');
      });
      switch(status){
        case "ACTIVE":
          this.$el.find('#btnActivate').attr('disabled','disabled');
          break;
        case "INACTIVE":
          this.$el.find('#btnDeactivate').attr('disabled','disabled');
          break;
        case "REBALANCING":
          this.$el.find('#btnRebalance').attr('disabled','disabled');
          break;
        case "KILLED":
          this.$el.find('#btnKill').attr('disabled','disabled');
          break;
      }
    },

    setTimeFrameOptions: function() {
        var html = '';
      _.each(this.summaryArr, function(obj){
        switch(obj.window){
          case "600":
            obj.windowPretty = localization.tt('lbl.last10Min');
            break;
          case "10800":
            obj.windowPretty = localization.tt('lbl.last3Hr');
            break;
          case "86400":
            obj.windowPretty = localization.tt('lbl.last1Day');
            break;
        }
        html += "<option value="+obj.window+">"+obj.windowPretty+"</option>";
      });
      this.$('#tFrame').append(html);
    },

    evChangeTimeFrame: function(e) {
      this.windowTimeFrame = $(e.currentTarget).val();
      this.fetchData(this.model.get('id'), this.systemBoltFlag, this.windowTimeFrame);
    },

    getDetails: function(model) {
      var detModel = new Backbone.Model(_.pick(model.attributes, 'name', 'id', 'owner', 'status', 'uptime', 'workersTotal', 'executorsTotal', 'tasksTotal', 'schedulerInfo'));

      this.topologyDetailsColl.reset(detModel);

      this.summaryArr = model.get('topologyStats');

      var that = this;

      var spoutsModel = new vSpout();

      var s_arr = [];
      _.each(model.get('spouts'), function(spout){
        var spoutsModel = new vSpout(spout);
        s_arr.push(spoutsModel)
      });
      that.spoutsCollection.reset(s_arr);

      var b_arr =[];
      _.each(this.model.get("bolts"), function(object){
        b_arr.push(new vBolt(object));
      });
      that.boltsCollection.reset(b_arr);

      var topoConfigModel = this.model.get("configuration");
      if (model) {
        var arr = [];
        for(var key in topoConfigModel){
          var obj = {};
          obj.key = key;
          obj.value = topoConfigModel[key];
          arr.push(new vTopologyConfig(obj));
        }
        that.topoConfigCollection.reset(arr);
      }
    },

    showSpoutsSummaryTable: function() {
      this.rSpoutsTable.show(new vSpoutCollectionView({
        collection: this.spoutsCollection,
        systemBoltFlag: this.systemBoltFlag,
        topologyId: this.model.get('id'),
        windowTimeFrame: this.windowTimeFrame
      }));
    },

    showBoltsSummaryTable: function() {

      this.rBoltsSummaryTable.show(new TableLayout({
        columns: this.getBoltColumns(),
        collection: this.boltsCollection,
        includeFilter: false,
        includePagination: false,
        includeFooterRecords: false,
        gridOpts: {
          emptyText: localization.tt('msg.noBoltFound'),
          className: 'table table-borderless table-striped cluster-table'
        }
      }));
    },

    showTopologyConfigTable: function() {
      this.rTopologyConfigTable.show(new TableLayout({
        columns: this.getTopoConfigColumns(),
        collection: this.topoConfigCollection,
        includeFilter: false,
        includePagination: false,
        includeFooterRecords: false,
        gridOpts: {
          emptyText: localization.tt('msg.noTopologyConfigFound'),
          className: 'table table-borderless table-striped cluster-table'
        }
      }));
    },


    showDetailsTable: function(collection) {
      this.rTopologyDetailsTbl.show(new TableLayout({
        columns: this.getColumns(),
        collection: collection,
        gridOpts: {
          className: 'table table-bordered table-striped backgrid'
        }
      }));
    },

    getColumns: function() {
      this.countActive = 0;
      var cols = [{
        name: "name",
        cell: "string",
        label: localization.tt("lbl.name"),
        hasTooltip: true,
        tooltipText: localization.tt('msg.topologySummaryName')
      }, {
        name: "id",
        cell: "string",
        label: localization.tt("lbl.id"),
        hasTooltip: true,
        tooltipText: localization.tt('msg.topologySummaryId')
      }, {
        name: "owner",
        cell: "string",
        label: localization.tt("lbl.owner"),
        hasTooltip: true,
        tooltipText: localization.tt('msg.topologySummaryOwner')
      }, {
        name: "status",
        cell: "string",
        label: localization.tt("lbl.status"),
        hasTooltip: true,
        tooltipText: localization.tt('msg.topologySummaryStatus')
      }, {
        name: "uptime",
        cell: "string",
        label: localization.tt("lbl.uptime"),
        hasTooltip: true,
        tooltipText: localization.tt('msg.topologySummaryUptime')
      }, {
        name: "workersTotal",
        cell: "string",
        label: "# "+localization.tt("lbl.workers"),
        hasTooltip: true,
        tooltipText: localization.tt('msg.topologySummaryWorkers')
      }, {
        name: "executorsTotal",
        cell: "string",
        label: "# "+localization.tt("lbl.executors"),
        hasTooltip: true,
        tooltipText: localization.tt('msg.topologySummaryExecutors')
      }, {
        name: "tasksTotal",
        cell: "string",
        label: "# "+localization.tt("lbl.tasks"),
        hasTooltip: true,
        tooltipText: localization.tt('msg.topologySummaryTasks')
      }, {
        name: "schedulerInfo",
        cell: "string",
        label: localization.tt("lbl.schedulerInfo"),
        hasTooltip: true,
        tooltipText: localization.tt('msg.topologySummaryScheduler')
      }];
      return cols;
    },

    showSummaryTable: function(id) {
      var object = _.findWhere(this.summaryArr, {
        window: id
      });
      if(_.isNull(object) || _.isUndefined(object)){
        object = {};
        object.emitted = 0;
        object.transferred = 0;
        object.completeLatency = 0;
        object.acked = 0;
        object.failed = 0;
        object.window = id;
      }
      this.ui.topologySummary.html(this.summaryTemplate(object));
    },

    evActivateTopology: function() {
      var that = this;
      bootbox.confirm({
        message: localization.tt('dialogMsg.activateTopologyMsg'),
        buttons: {
          confirm: {
            label: localization.tt('btn.yes'),
            className: "btn-success",
          },
          cancel: {
            label: localization.tt('btn.no'),
            className: "btn-default",
          }
        },
        callback: function(result){
          if(result){
            that.model.activateTopology({
              id: that.model.get('id'),
              success: function(model, response, options){
                Utils.notifySuccess(localization.tt('dialogMsg.topologyActivateSuccessfully'));
                that.fetchData(that.model.get('id'), that.systemBoltFlag, that.windowTimeFrame);
              },
              error: function(model, response, options){
                Utils.notifyError(model.statusText);
              }
            });
          }
        }
      });

    },
    evDeactivateTopology: function() {
      var that = this;
      bootbox.confirm({
        message: localization.tt('dialogMsg.deactivateTopologyMsg'),
        buttons: {
          confirm: {
            label: localization.tt('btn.yes'),
            className: "btn-success",
          },
          cancel: {
            label: localization.tt('btn.no'),
            className: "btn-default",
          }
        },
        callback: function(result){
          if(result){
            that.model.deactivateTopology({
              id: that.model.get('id'),
              success: function(model, response, options){
                Utils.notifySuccess(localization.tt('dialogMsg.topologyDeactivateSuccessfully'));
                that.fetchData(that.model.get('id'), that.systemBoltFlag, that.windowTimeFrame);
              },
              error: function(model, response, options){
                Utils.notifyError(model.statusText);
              }
            });
          }
        }
      });
    },
    evRebalanceTopology: function() {
      var that = this;
      if(this.view){
        this.onDialogClosed();
      }
      require(['views/Topology/RebalanceForm'], function(rebalanceForm){
        var reBalanceModel = new Backbone.Model();
        reBalanceModel.set('workers',that.topologyDetailsColl.models[0].get('workersTotal'));
        reBalanceModel.set('waitTime',30);
        that.view = new rebalanceForm({
          spoutCollection : that.spoutsCollection,
          boltCollection: that.boltsCollection,
          model: reBalanceModel
        });
        that.view.render();

        bootbox.dialog({
          message: that.view.$el,
          title: localization.tt('lbl.rebalanceTopology'),
          className: "rebalance-modal",
          buttons: {
            cancel: {
              label: localization.tt('btn.cancel'),
              className: "btn-default",
              callback: function(){
                that.onDialogClosed();
              }
            },
            success: {
              label: localization.tt('btn.apply'),
              className: "btn-success",
              callback: function(){
                var err = that.view.validate();
                if(_.isEmpty(err)){
                  that.rebalanceTopology();
                } else return false;
              }
            }
          }
        });
      });
    },
    rebalanceTopology: function(){
      var that = this,
          attr = this.view.getValue(),
          obj = {"rebalanceOptions":{}};

      if(!_.isUndefined(attr.workers.min) && !_.isNull(attr.workers.min)){
        obj.rebalanceOptions.numWorkers = attr.workers.min;
      }

      var spoutBoltObj = {};
      for(var key in attr){
        if(!_.isEqual(key,'workers') && !_.isEqual(key,'waitTime')){
          if(!_.isNull(attr[key])){
            spoutBoltObj[key] = attr[key];
          }
        }
      }

      if(_.keys(spoutBoltObj).length){
        obj.rebalanceOptions.executors = spoutBoltObj;
      }

      $.ajax({
        url: Globals.baseURL + '/api/v1/topology/' + that.model.get('id') + '/rebalance/' + attr.waitTime,
        data: (_.keys(obj.rebalanceOptions).length) ? JSON.stringify(obj) : null,
        cache: false,
        contentType: 'application/json',
        type: 'POST',
        success: function(model, response, options){
          if(!_.isUndefined(model.error)){
            if(model.errorMessage.search("msg:") != -1){
              var startIndex = model.errorMessage.search("msg:") + 4;
              var endIndex = model.errorMessage.split("\n")[0].search("\\)");
              Utils.notifyError(model.error+":<br/>"+model.errorMessage.substring(startIndex, endIndex));
            } else {
              Utils.notifyError(model.error);
            }
          } else {
            Utils.notifySuccess(localization.tt('dialogMsg.topologyRebalanceSuccessfully'));
            that.fetchData(that.model.get('id'), that.systemBoltFlag, that.windowTimeFrame);
          }
        },
        error: function(model, response, options){
          Utils.notifyError(model.statusText);
        }
      });
    },
    evKillTopology: function() {
      var that = this;
      bootbox.prompt({
        title: localization.tt('dialogMsg.killTopologyMsg'),
        value: "30",
        buttons: {
          confirm: {
            label: localization.tt('btn.yes'),
            className: "btn-success",
          },
          cancel: {
            label: localization.tt('btn.no'),
            className: "btn-default",
          }
        },
        callback: function(result) {
          if(result != null){
            that.model.killTopology({
              id: that.model.get('id'),
              waitTime: result,
              success: function(model, response, options){
                Utils.notifySuccess(localization.tt('dialogMsg.topologyKilledSuccessfully'));
                that.fetchData(that.model.get('id'), that.systemBoltFlag, that.windowTimeFrame);
              },
              error: function(model, response, options){
                Utils.notifyError(model.statusText);
              }
            });
          }
        }
      });
    },
    onDialogClosed: function() {
      if (this.view) {
        this.view.close();
        this.view.remove();
        this.view = null;
      }
    },
    openAllTables: function(){
      console.log("Open All !!");
    },
    evSysBoltToggle: function(e){
      this.systemBoltFlag = $(e.currentTarget).is(':checked');
      this.fetchData(this.model.get('id'), this.systemBoltFlag, this.windowTimeFrame);
    },

    getSpoutColumns: function() {
      var cols = [{
        name: "spoutId",
        cell: "string",
        label: localization.tt("lbl.id"),
        sortable: true,
        hasTooltip: true,
        tooltipText: localization.tt('msg.spoutId')
      },
      {
        name: "executors",
        cell: "string",
        label: localization.tt("lbl.executors"),
        hasTooltip: true,
        tooltipText: localization.tt('msg.spoutExecutors'),
        sortable: true
      },
      {
        name: "tasks",
        cell: "string",
        label: localization.tt("lbl.tasks"),
        hasTooltip: true,
        tooltipText: localization.tt('msg.spoutTask'),
        sortable: true,
      },
      {
        name: "emitted",
        cell: "string",
        label: localization.tt("lbl.emitted"),
        hasTooltip: true,
        tooltipText: localization.tt('msg.emitted'),
        sortable: true
      },
      {
        name: "transferred",
        cell: "string",
        label: localization.tt("lbl.transferred"),
        hasTooltip: true,
        tooltipText: localization.tt('msg.transferred'),
        sortable: true
      },
      {
        name: "completeLatency",
        cell: "string",
        label: localization.tt("lbl.completeLatency"),
        hasTooltip: true,
        tooltipText: localization.tt('msg.completeLatency'),
        sortable: true
      },
      {
        name: "acked",
        cell: "string",
        label: localization.tt("lbl.acked"),
        hasTooltip: true,
        tooltipText: localization.tt('msg.acked'),
        sortable: true
      },
      {
        name: "failed",
        cell: "string",
        label: localization.tt("lbl.failed"),
        hasTooltip: true,
        tooltipText: localization.tt('msg.failed'),
        sortable: true
      }
      ];
      return cols;
    },

    getBoltColumns: function() {
      var cols = [{
        name: "boltId",
        cell: "string",
        label: localization.tt('lbl.id'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.spoutId'),
        sortable: true
      }, {
        name: "executors",
        cell: "string",
        label: localization.tt('lbl.executors'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.spoutExecutors'),
      }, {
        name: "tasks",
        cell: "string",
        label: localization.tt('lbl.tasks'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.spoutTasks'),
      }, {
        name: "emitted",
        cell: "string",
        label: localization.tt('lbl.emitted'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.emitted'),
      }, {
        name: "transferred",
        cell: "string",
        label: localization.tt('lbl.transferred'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.transferred'),
      }, {
        name: "capacity",
        cell: "string",
        label: localization.tt('lbl.capacity'),
        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
          fromRaw: function(rawValue, model) {
            if (model) {
              return (parseFloat(model.attributes.capacity) < 1 ? " < 1%" : " "+model.get('capacity')+" %");
            }
          }
        }),
        hasTooltip: true,
        tooltipText: localization.tt('msg.boltCapacity'),
      }, {
        name: "executeLatency",
        cell: "string",
        label: localization.tt('lbl.executeLatency'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.boltExecuteLatency'),
      }, {
        name: "executed",
        cell: "string",
        label: localization.tt('lbl.executed'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.boltExected'),
      }, {
        name: "processLatency",
        cell: "string",
        label: localization.tt('lbl.processLatency'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.boltProcessLatency'),
      }, {
        name: "acked",
        cell: "string",
        label: localization.tt('lbl.acked'),
        hasTooltip: true,
        tooltipText: localization.tt('msg.boltAcked'),
      }];
      return cols;
    },

    showBreadCrumbs: function(){
      vent.trigger('Breadcrumb:Show', (this.model.has('name')) ? this.model.get('name') : 'No-Name');
    },

    getTopoConfigColumns: function() {
      var cols = [
        {
          name: "key",
          cell: "string",
          label: localization.tt('lbl.key'),
          sortable: true
        }, {
          name: "value",
          cell: "string",
          label: localization.tt('lbl.value'),
        }
      ];
      return cols;
    }

  });
  return masterView;
});
