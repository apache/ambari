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

var App = require('app');

App.NameNodeFederationWizardStep4Controller = App.HighAvailabilityProgressPageController.extend(App.WizardEnableDone, {

  name: "nameNodeFederationWizardStep4Controller",

  commands: ['stopRequiredServices', 'reconfigureServices', 'installNameNode', 'installZKFC', 'startJournalNodes', 'formatNameNode', 'formatZKFC', 'startZKFC', 'startNameNode', 'bootstrapNameNode', 'createWidgets', 'startZKFC2', 'startNameNode2', 'restartAllServices'],

  tasksMessagesPrefix: 'admin.nameNodeFederation.wizard.step',

  newNameNodeHosts: function () {
    return this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').filterProperty('isInstalled', false).mapProperty('hostName');
  }.property('content.masterComponentHosts.@each.hostName'),

  stopRequiredServices: function () {
    this.stopServices(["ZOOKEEPER"]);
  },

  reconfigureServices: function () {
    var configs = [];
    var data = this.get('content.serviceConfigProperties');
    var note = Em.I18n.t('admin.nameNodeFederation.wizard,step4.save.configuration.note');
    configs.push({
      Clusters: {
        desired_config: this.reconfigureSites(['hdfs-site'], data, note)
      }
    });
    if (App.Service.find().someProperty('serviceName', 'RANGER')) {
      configs.push({
        Clusters: {
          desired_config: this.reconfigureSites(['ranger-tagsync-site'], data, note)
        }
      });
    }
    return App.ajax.send({
      name: 'common.service.multiConfigurations',
      sender: this,
      data: {
        configs: configs
      },
      error: 'onTaskError',
      success: 'installHDFSClients'
    });
  },

  installHDFSClients: function () {
    var nnHostNames = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').mapProperty('hostName');
    var jnHostNames = App.HostComponent.find().filterProperty('componentName', 'JOURNALNODE').mapProperty('hostName');
    var hostNames = nnHostNames.concat(jnHostNames).uniq();
    this.createInstallComponentTask('HDFS_CLIENT', hostNames, 'HDFS');
  },

  installNameNode: function () {
    this.createInstallComponentTask('NAMENODE', this.get('newNameNodeHosts'), "HDFS");
  },

  installZKFC: function () {
    this.createInstallComponentTask('ZKFC', this.get('newNameNodeHosts'), "HDFS");
  },

  startJournalNodes: function () {
    var hostNames = App.HostComponent.find().filterProperty('componentName', 'JOURNALNODE').mapProperty('hostName');
    this.updateComponent('JOURNALNODE', hostNames, "HDFS", "Start");
  },

  formatNameNode: function () {
    App.ajax.send({
      name: 'nameNode.federation.formatNameNode',
      sender: this,
      data: {
        host: this.get('newNameNodeHosts')[0]
      },
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  formatZKFC: function () {
    App.ajax.send({
      name: 'nameNode.federation.formatZKFC',
      sender: this,
      data: {
        host: this.get('newNameNodeHosts')[0]
      },
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  startZKFC: function () {
    this.updateComponent('ZKFC', this.get('newNameNodeHosts')[0], "HDFS", "Start");
  },

  startNameNode: function () {
    this.updateComponent('NAMENODE', this.get('newNameNodeHosts')[0], "HDFS", "Start");
  },

  bootstrapNameNode: function () {
    App.ajax.send({
      name: 'nameNode.federation.bootstrapNameNode',
      sender: this,
      data: {
        host: this.get('newNameNodeHosts')[1]
      },
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  createWidgets: function () {
    var self = this;
    this.getNameNodeWidgets().done(function (data) {
      var newWidgetsIds = [];
      var oldWidgetIds = [];
      var nameservice1 = App.HDFSService.find().objectAt(0).get('masterComponentGroups')[0].name;
      var nameservice2 = self.get('content.nameServiceId');
      var widgetsCount = data.items.length;
      data.items.forEach(function (widget) {
        if (!widget.WidgetInfo.tag) {
          var oldId = widget.WidgetInfo.id;
          oldWidgetIds.push(oldId);
          delete widget.href;
          delete widget.WidgetInfo.id;
          delete widget.WidgetInfo.cluster_name;
          delete widget.WidgetInfo.author;
          widget.WidgetInfo.tag = nameservice1;
          widget.WidgetInfo.metrics = JSON.parse(widget.WidgetInfo.metrics);
          widget.WidgetInfo.values = JSON.parse(widget.WidgetInfo.values);
          self.createWidget(widget).done(function (w) {
            newWidgetsIds.push(w.resources[0].WidgetInfo.id);
            widget.WidgetInfo.tag = nameservice2;
            self.createWidget(widget).done(function (w) {
              newWidgetsIds.push(w.resources[0].WidgetInfo.id);
              self.deleteWidget(oldId).done(function () {
                if (!--widgetsCount) {
                  self.getDefaultHDFStWidgetLayout().done(function (layout) {
                    layout = layout.items[0].WidgetLayoutInfo;
                    layout.widgets = layout.widgets.filter(function (w) {
                      return !oldWidgetIds.contains(w.WidgetInfo.id);
                    }).map(function (w) {
                      return w.WidgetInfo.id;
                    }).concat(newWidgetsIds);
                    self.updateDefaultHDFStWidgetLayout(layout).done(function () {
                      self.onTaskCompleted();
                    });
                  });
                }
              });
            });
          });
        } else {
          widgetsCount--;
        }
      });
    });
  },

  createWidget: function (data) {
    return App.ajax.send({
      name: 'widgets.wizard.add',
      sender: this,
      data: {
        data: data
      }
    });
  },

  deleteWidget: function (id) {
    return App.ajax.send({
      name: 'widget.action.delete',
      sender: self,
      data: {
        id: id
      }
    });
  },

  startZKFC2: function () {
    this.updateComponent('ZKFC', this.get('newNameNodeHosts')[1], "HDFS", "Start");
  },

  startNameNode2: function () {
    this.updateComponent('NAMENODE', this.get('newNameNodeHosts')[1], "HDFS", "Start");
  },

  restartAllServices: function () {
    App.ajax.send({
      name: 'restart.allServices',
      sender: this,
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  getNameNodeWidgets: function () {
    return App.ajax.send({
      name: 'widgets.get',
      sender: this,
      data: {
        urlParams: 'WidgetInfo/widget_type.in(GRAPH,NUMBER,GAUGE)&WidgetInfo/scope=CLUSTER&WidgetInfo/metrics.matches(.*\"component_name\":\"NAMENODE\".*)&fields=*'
      }
    });
  },

  getDefaultHDFStWidgetLayout: function () {
    return App.ajax.send({
      name: 'widget.layout.get',
      sender: this,
      data: {
        urlParams: 'WidgetLayoutInfo/layout_name=default_hdfs_dashboard'
      }
    });
  },

  updateDefaultHDFStWidgetLayout: function (widgetLayoutData) {
    var layout = widgetLayoutData;
    var data = {
      "WidgetLayoutInfo": {
        "display_name": layout.display_name,
        "layout_name": layout.layout_name,
        "id": layout.id,
        "scope": "USER",
        "section_name": layout.section_name,
        "widgets": layout.widgets.map(function (id) {
          return {
            "id":id
          }
        })
      }
    };
    return App.ajax.send({
      name: 'widget.layout.edit',
      sender: this,
      data: {
        layoutId: layout.id,
        data: data
      },
    });
  }
});
