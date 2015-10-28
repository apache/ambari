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
var batchUtils = require('utils/batch_scheduled_requests');
var hostsManagement = require('utils/hosts');
var O = Em.Object;
/**
 * @class BulkOperationsController
 */
App.BulkOperationsController = Em.Controller.extend({

  name: 'bulkOperationsController',

  /**
   * Bulk operation wrapper
   * @param {Object} operationData - data about bulk operation (action, hosts or hostComponents etc)
   * @param {Array} hosts - list of affected hosts
   */
  bulkOperation: function (operationData, hosts) {
    if (operationData.componentNameFormatted) {
      if (operationData.action === 'RESTART') {
        this.bulkOperationForHostComponentsRestart(operationData, hosts);
      }
      else {
        if (operationData.action.indexOf('DECOMMISSION') != -1) {
          this.bulkOperationForHostComponentsDecommission(operationData, hosts);
        }
        else {
          this.bulkOperationForHostComponents(operationData, hosts);
        }
      }
    }
    else {
      if (operationData.action === 'SET_RACK_INFO') {
        this.bulkOperationForHostsSetRackInfo(operationData, hosts);
      } else if (operationData.action === 'RESTART') {
        this.bulkOperationForHostsRestart(operationData, hosts);
      }
      else {
        if (operationData.action === 'PASSIVE_STATE') {
          this.bulkOperationForHostsPassiveState(operationData, hosts);
        }
        else {
          this.bulkOperationForHosts(operationData, hosts);
        }
      }
    }
  },

  /**
   * Bulk operation (start/stop all) for selected hosts
   * @param {Object} operationData - data about bulk operation (action, hostComponents etc)
   * @param {Array} hosts - list of affected hosts
   */
  bulkOperationForHosts: function (operationData, hosts) {
    var self = this;

    batchUtils.getComponentsFromServer({
      hosts: hosts.mapProperty('hostName'),
      passiveState: 'OFF',
      displayParams: ['host_components/HostRoles/component_name']
    }, function (data) {
      self.bulkOperationForHostsCallback(operationData, data);
    });
  },
  /**
   * run Bulk operation (start/stop all) for selected hosts
   * after host and components are loaded
   * @param operationData
   * @param data
   */
  bulkOperationForHostsCallback: function (operationData, data) {
    var query = [];
    var hostNames = [];
    var hostsMap = {};

    data.items.forEach(function (host) {
      host.host_components.forEach(function (hostComponent) {
        if (!App.components.get('clients').contains((hostComponent.HostRoles.component_name))) {
          if (hostsMap[host.Hosts.host_name]) {
            hostsMap[host.Hosts.host_name].push(hostComponent.HostRoles.component_name);
          } else {
            hostsMap[host.Hosts.host_name] = [hostComponent.HostRoles.component_name];
          }
        }
      });
    });

    var nn_hosts = [];
    for (var hostName in hostsMap) {
      var subQuery = '(HostRoles/component_name.in(%@)&HostRoles/host_name=' + hostName + ')';
      var components = hostsMap[hostName];

      if (components.length) {
        if (components.indexOf('NAMENODE') >= 0) {
          nn_hosts.push(hostName);
        }
        query.push(subQuery.fmt(components.join(',')));
      }
      hostNames.push(hostName);
    }
    hostNames = hostNames.join(",");
    if (query.length) {
      query = query.join('|');
      var self = this;
      // if NameNode included, check HDFS NameNode checkpoint before stop NN
      if (nn_hosts.length == 1 && operationData.action === 'INSTALLED' && App.Service.find().filterProperty('serviceName', 'HDFS').someProperty('workStatus', App.HostComponentStatus.started)) {
        var hostName = nn_hosts[0];
        App.router.get('mainHostDetailsController').checkNnLastCheckpointTime(function () {
          App.ajax.send({
            name: 'common.host_components.update',
            sender: self,
            data: {
              query: query,
              HostRoles: {
                state: operationData.action
              },
              context: operationData.message,
              hostName: hostNames,
              noOpsMessage: Em.I18n.t('hosts.host.maintainance.allComponents.context')
            },
            success: 'bulkOperationForHostComponentsSuccessCallback'
          });
        }, hostName);
      } else if (nn_hosts.length == 2 && operationData.action === 'INSTALLED' && App.Service.find().filterProperty('serviceName', 'HDFS').someProperty('workStatus', App.HostComponentStatus.started)) {
        // HA enabled
        App.router.get('mainServiceItemController').checkNnLastCheckpointTime(function () {
          App.ajax.send({
            name: 'common.host_components.update',
            sender: self,
            data: {
              query: query,
              HostRoles: {
                state: operationData.action
              },
              context: operationData.message,
              hostName: hostNames,
              noOpsMessage: Em.I18n.t('hosts.host.maintainance.allComponents.context')
            },
            success: 'bulkOperationForHostComponentsSuccessCallback'
          });
        });
      } else {
        App.ajax.send({
          name: 'common.host_components.update',
          sender: self,
          data: {
            query: query,
            HostRoles: {
              state: operationData.action
            },
            context: operationData.message,
            hostName: hostNames,
            noOpsMessage: Em.I18n.t('hosts.host.maintainance.allComponents.context')
          },
          success: 'bulkOperationForHostComponentsSuccessCallback'
        });
      }
    }
    else {
      App.ModalPopup.show({
        header: Em.I18n.t('rolling.nothingToDo.header'),
        body: Em.I18n.t('rolling.nothingToDo.body').format(Em.I18n.t('hosts.host.maintainance.allComponents.context')),
        secondary: false
      });
    }
  },

  bulkOperationForHostsSetRackInfo: function (operationData, hosts) {
    hostsManagement.setRackInfo(operationData, hosts);
  },

  /**
   * Bulk restart for selected hosts
   * @param {Object} operationData - data about bulk operation (action, hostComponents etc)
   * @param {Ember.Enumerable} hosts - list of affected hosts
   */
  bulkOperationForHostsRestart: function (operationData, hosts) {
    batchUtils.getComponentsFromServer({
      passiveState: 'OFF',
      hosts: hosts.mapProperty('hostName'),
      displayParams: ['host_components/HostRoles/component_name']
    }, function (data) {
      var hostComponents = [];
      data.items.forEach(function (host) {
        host.host_components.forEach(function (hostComponent) {
          hostComponents.push(O.create({
            componentName: hostComponent.HostRoles.component_name,
            hostName: host.Hosts.host_name
          }));
        })
      });
      // if NameNode included, check HDFS NameNode checkpoint before restart NN
      var nn_count = hostComponents.filterProperty('componentName', 'NAMENODE').get('length');
      if (nn_count == 1 && App.Service.find().filterProperty('serviceName', 'HDFS').someProperty('workStatus', App.HostComponentStatus.started)) {
        var hostName = hostComponents.findProperty('componentName', 'NAMENODE').get('hostName');
        App.router.get('mainHostDetailsController').checkNnLastCheckpointTime(function () {
          batchUtils.restartHostComponents(hostComponents, Em.I18n.t('rollingrestart.context.allOnSelectedHosts'), "HOST");
        }, hostName);
      } else if (nn_count == 2 && App.Service.find().filterProperty('serviceName', 'HDFS').someProperty('workStatus', App.HostComponentStatus.started)) {
        // HA enabled
        App.router.get('mainServiceItemController').checkNnLastCheckpointTime(function () {
          batchUtils.restartHostComponents(hostComponents, Em.I18n.t('rollingrestart.context.allOnSelectedHosts'), "HOST");
        });
      } else {
        batchUtils.restartHostComponents(hostComponents, Em.I18n.t('rollingrestart.context.allOnSelectedHosts'), "HOST");
      }
    });
  },

  /**
   * Bulk turn on/off passive state for selected hosts
   * @param {Object} operationData - data about bulk operation (action, hostComponents etc)
   * @param {Array} hosts - list of affected hosts
   */
  bulkOperationForHostsPassiveState: function (operationData, hosts) {
    var self = this;

    batchUtils.getComponentsFromServer({
      hosts: hosts.mapProperty('hostName'),
      displayParams: ['Hosts/maintenance_state']
    }, function (data) {
      var hostNames = [];

      data.items.forEach(function (host) {
        if (host.Hosts.maintenance_state !== operationData.state) {
          hostNames.push(host.Hosts.host_name);
        }
      });
      if (hostNames.length) {
        App.ajax.send({
          name: 'bulk_request.hosts.passive_state',
          sender: self,
          data: {
            hostNames: hostNames.join(','),
            passive_state: operationData.state,
            requestInfo: operationData.message
          },
          success: 'updateHostPassiveState'
        });
      } else {
        App.ModalPopup.show({
          header: Em.I18n.t('rolling.nothingToDo.header'),
          body: Em.I18n.t('hosts.bulkOperation.passiveState.nothingToDo.body'),
          secondary: false
        });
      }
    });
  },

  updateHostPassiveState: function (data, opt, params) {
    batchUtils.infoPassiveState(params.passive_state);
  },
  /**
   * Bulk operation for selected hostComponents
   * @param {Object} operationData - data about bulk operation (action, hostComponents etc)
   * @param {Array} hosts - list of affected hosts
   */
  bulkOperationForHostComponents: function (operationData, hosts) {
    var self = this;

    batchUtils.getComponentsFromServer({
      components: [operationData.componentName],
      hosts: hosts.mapProperty('hostName'),
      passiveState: 'OFF'
    }, function (data) {
      if (data.items.length) {
        var hostsWithComponentInProperState = data.items.mapProperty('Hosts.host_name');
        App.ajax.send({
          name: 'common.host_components.update',
          sender: self,
          data: {
            HostRoles: {
              state: operationData.action
            },
            query: 'HostRoles/component_name=' + operationData.componentName + '&HostRoles/host_name.in(' + hostsWithComponentInProperState.join(',') + ')&HostRoles/maintenance_state=OFF',
            context: operationData.message + ' ' + operationData.componentNameFormatted,
            level: 'SERVICE',
            noOpsMessage: operationData.componentNameFormatted
          },
          success: 'bulkOperationForHostComponentsSuccessCallback'
        });
      }
      else {
        App.ModalPopup.show({
          header: Em.I18n.t('rolling.nothingToDo.header'),
          body: Em.I18n.t('rolling.nothingToDo.body').format(operationData.componentNameFormatted),
          secondary: false
        });
      }
    });
  },

  /**
   * Bulk decommission/recommission for selected hostComponents
   * @param {Object} operationData
   * @param {Array} hosts
   */
  bulkOperationForHostComponentsDecommission: function (operationData, hosts) {
    var self = this;

    batchUtils.getComponentsFromServer({
      components: [operationData.realComponentName],
      hosts: hosts.mapProperty('hostName'),
      passiveState: 'OFF',
      displayParams: ['host_components/HostRoles/state']
    }, function (data) {
      self.bulkOperationForHostComponentsDecommissionCallBack(operationData, data)
    });
  },

  /**
   * run Bulk decommission/recommission for selected hostComponents
   * after host and components are loaded
   * @param operationData
   * @param data
   */
  bulkOperationForHostComponentsDecommissionCallBack: function (operationData, data) {
    var service = App.Service.find(operationData.serviceName);
    var components = [];

    data.items.forEach(function (host) {
      host.host_components.forEach(function (hostComponent) {
        components.push(O.create({
          componentName: hostComponent.HostRoles.component_name,
          hostName: host.Hosts.host_name,
          workStatus: hostComponent.HostRoles.state
        }))
      });
    });

    if (components.length) {
      var hostsWithComponentInProperState = components.mapProperty('hostName');
      var turn_off = operationData.action.indexOf('OFF') !== -1;
      var svcName = operationData.serviceName;
      var masterName = operationData.componentName;
      var slaveName = operationData.realComponentName;
      var hostNames = hostsWithComponentInProperState.join(',');
      if (turn_off) {
        // For recommession
        if (svcName === "YARN" || svcName === "HBASE" || svcName === "HDFS") {
          App.router.get('mainHostDetailsController').doRecommissionAndStart(hostNames, svcName, masterName, slaveName);
        }
      } else {
        hostsWithComponentInProperState = components.filterProperty('workStatus', 'STARTED').mapProperty('hostName');
        //For decommession
        if (svcName == "HBASE") {
          // HBASE service, decommission RegionServer in batch requests
          this.warnBeforeDecommission(hostNames);
        } else {
          var parameters = {
            "slave_type": slaveName
          };
          var contextString = turn_off ? 'hosts.host.' + slaveName.toLowerCase() + '.recommission' :
          'hosts.host.' + slaveName.toLowerCase() + '.decommission';
          if (turn_off) {
            parameters['included_hosts'] = hostsWithComponentInProperState.join(',')
          }
          else {
            parameters['excluded_hosts'] = hostsWithComponentInProperState.join(',');
          }
          App.ajax.send({
            name: 'bulk_request.decommission',
            sender: this,
            data: {
              context: Em.I18n.t(contextString),
              serviceName: service.get('serviceName'),
              componentName: operationData.componentName,
              parameters: parameters,
              noOpsMessage: operationData.componentNameFormatted
            },
            success: 'bulkOperationForHostComponentsSuccessCallback'
          });
        }
      }
    }
    else {
      App.ModalPopup.show({
        header: Em.I18n.t('rolling.nothingToDo.header'),
        body: Em.I18n.t('rolling.nothingToDo.body').format(operationData.componentNameFormatted),
        secondary: false
      });
    }
  },


  /**
   * get info about regionserver passive_state
   * @method warnBeforeDecommission
   * @param {String} hostNames
   * @return {$.ajax}
   */
  warnBeforeDecommission: function (hostNames) {
    return App.ajax.send({
      'name': 'host_components.hbase_regionserver.active',
      'sender': this,
      'data': {
        hostNames: hostNames
      },
      success: 'warnBeforeDecommissionSuccess'
    });
  },

  /**
   * check is hbase regionserver in mm. If so - run decommission
   * otherwise shows warning
   * @method warnBeforeDecommission
   * @param {Object} data
   * @param {Object} opt
   * @param {Object} params
   */
  warnBeforeDecommissionSuccess: function(data, opt, params) {
    if (Em.get(data, 'items.length')) {
      App.router.get('mainHostDetailsController').showHbaseActiveWarning();
    } else {
      App.router.get('mainHostDetailsController').checkRegionServerState(params.hostNames);
    }
  },
  /**
   * Bulk restart for selected hostComponents
   * @param {Object} operationData
   * @param {Array} hosts
   */
  bulkOperationForHostComponentsRestart: function (operationData, hosts) {
    var service = App.Service.find(operationData.serviceName);

    batchUtils.getComponentsFromServer({
      components: [operationData.componentName],
      hosts: hosts.mapProperty('hostName'),
      passiveState: 'OFF',
      displayParams: ['Hosts/maintenance_state', 'host_components/HostRoles/stale_configs', 'host_components/HostRoles/maintenance_state']
    }, function (data) {
      var wrappedHostComponents = [];

      data.items.forEach(function (host) {
        host.host_components.forEach(function (hostComponent) {
          wrappedHostComponents.push(O.create({
            componentName: hostComponent.HostRoles.component_name,
            serviceName: operationData.serviceName,
            hostName: host.Hosts.host_name,
            hostPassiveState: host.Hosts.maintenance_state,
            staleConfigs: hostComponent.HostRoles.stale_configs,
            passiveState: hostComponent.HostRoles.maintenance_state
          }))
        });
      });

      if (wrappedHostComponents.length) {
        batchUtils.showRollingRestartPopup(wrappedHostComponents.objectAt(0).get('componentName'), service.get('displayName'), service.get('passiveState') === "ON", false, wrappedHostComponents);
      } else {
        App.ModalPopup.show({
          header: Em.I18n.t('rolling.nothingToDo.header'),
          body: Em.I18n.t('rolling.nothingToDo.body').format(operationData.componentNameFormatted),
          secondary: false
        });
      }
    });
  },

  updateHostComponentsPassiveState: function (data, opt, params) {
    batchUtils.infoPassiveState(params.passive_state);
  },
  /**
   * Show BO popup after bulk request
   */
  bulkOperationForHostComponentsSuccessCallback: function (data, opt, params, req) {
    if (!data && req.status == 200) {
      App.ModalPopup.show({
        header: Em.I18n.t('rolling.nothingToDo.header'),
        body: Em.I18n.t('rolling.nothingToDo.body').format(params.noOpsMessage || Em.I18n.t('hosts.host.maintainance.allComponents.context')),
        secondary: false
      });
    } else {
      App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
        if (initValue) {
          App.router.get('backgroundOperationsController').showPopup();
        }
      });
    }
  },

  /**
   * Returns all hostNames if amount is less than {minShown} or
   * first elements of array (number of elements - {minShown}) converted to string
   * @param {Array} hostNames - array of all listed hostNames
   * @param {String} divider - string to separate hostNames
   * @param {Number} minShown - min amount of hostName to be shown
   * @returns {String} hostNames
   * @method showHostNames
   */
  showHostNames: function(hostNames, divider, minShown) {
    if (hostNames.length > minShown) {
      return hostNames.slice(0, minShown).join(divider) + divider + Em.I18n.t("installer.step8.other").format(hostNames.length - minShown);
    } else {
      return hostNames.join(divider);
    }
  },

  /**
   * Confirmation Popup for bulk Operations
   */
  bulkOperationConfirm: function(operationData, selection) {
    var hostsNames = [],
      queryParams = [];
    // @todo remove using external controller
    switch(selection) {
      case 's':
        hostsNames = App.router.get('mainHostController.content').filterProperty('selected').mapProperty('hostName');
        if(hostsNames.length > 0){
          queryParams.push({
            key: 'Hosts/host_name',
            value: hostsNames,
            type: 'MULTIPLE'
          });
        }
        break;
      case 'f':
        queryParams = App.router.get('mainHostController').getQueryParameters(true).filter(function (obj) {
          return !(obj.key == 'page_size' || obj.key == 'from');
        });
        break;
    }

    if (operationData.action === 'SET_RACK_INFO') {
      this.getHostsForBulkOperations(queryParams, operationData, null);
      return;
    }

    var loadingPopup = App.ModalPopup.show({
      header: Em.I18n.t('jobs.loadingTasks'),
      primary: false,
      secondary: false,
      bodyClass: Em.View.extend({
        template: Em.Handlebars.compile('<div class="spinner"></div>')
      })
    });

    this.getHostsForBulkOperations(queryParams, operationData, loadingPopup);
  },

  getHostsForBulkOperations: function (queryParams, operationData, loadingPopup) {
    var params = App.router.get('updateController').computeParameters(queryParams);

    App.ajax.send({
      name: 'hosts.bulk.operations',
      sender: this,
      data: {
        parameters: params,
        operationData: operationData,
        loadingPopup: loadingPopup
      },
      success: 'getHostsForBulkOperationSuccessCallback'
    });
  },

  convertHostsObjects: function (hosts) {
    return hosts.map(function (host) {
      return {
        index: host.index,
        id: host.id,
        clusterId: host.cluster_id,
        passiveState: host.passive_state,
        hostName: host.host_name,
        hostComponents: host.host_components
      }
    });
  },

  getHostsForBulkOperationSuccessCallback: function(json, opt, param) {
    var self = this,
      operationData = param.operationData,
      hosts = this.convertHostsObjects(App.hostsMapper.map(json, true));
    // no hosts - no actions
    if (!hosts.length) {
      console.log('No bulk operation if no hosts selected.');
      return;
    }
    var hostNames = hosts.mapProperty('hostName');
    var hostsToSkip = [];
    if (operationData.action == "DECOMMISSION") {
      var hostComponentStatusMap = {}; // "DATANODE_c6401.ambari.apache.org" => "STARTED"
      var hostComponentIdMap = {}; // "DATANODE_c6401.ambari.apache.org" => "DATANODE"
      if (json.items) {
        json.items.forEach(function(host) {
          if (host.host_components) {
            host.host_components.forEach(function(component) {
              hostComponentStatusMap[component.id] = component.HostRoles.state;
              hostComponentIdMap[component.id] = component.HostRoles.component_name;
            });
          }
        });
      }
      hostsToSkip = hosts.filter(function(host) {
        var invalidStateComponents = host.hostComponents.filter(function(component) {
          return hostComponentIdMap[component] == operationData.realComponentName && hostComponentStatusMap[component] == 'INSTALLED';
        });
        return invalidStateComponents.length > 0;
      });
    }
    var hostNamesSkipped = hostsToSkip.mapProperty('hostName');
    if (operationData.action === 'PASSIVE_STATE') {
      hostNamesSkipped = [];
      var outOfSyncHosts = App.StackVersion.find().findProperty('isCurrent').get('outOfSyncHosts');
      for (var i = 0; i < outOfSyncHosts.length; i++) {
        if (hostNames.contains(outOfSyncHosts[i])) {
          hostNamesSkipped.push(outOfSyncHosts[i]);
        }
      }
    }
    var message;
    if (operationData.componentNameFormatted) {
      message = Em.I18n.t('hosts.bulkOperation.confirmation.hostComponents').format(operationData.message, operationData.componentNameFormatted, hostNames.length);
    }
    else {
      message = Em.I18n.t('hosts.bulkOperation.confirmation.hosts').format(operationData.message, hostNames.length);
    }

    if (param.loadingPopup) {
      param.loadingPopup.hide();
    }

    if (operationData.action === 'SET_RACK_INFO') {
      self.bulkOperation(operationData, hosts);
      return;
    }

    App.ModalPopup.show({
      header: Em.I18n.t('hosts.bulkOperation.confirmation.header'),
      hostNames: hostNames.join("\n"),
      visibleHosts: self.showHostNames(hostNames, "\n", 3),
      hostNamesSkippedVisible: self.showHostNames(hostNamesSkipped, "\n", 3),
      hostNamesSkipped: function() {
        return hostNamesSkipped.length ? hostNamesSkipped.join("\n") : false;
      }.property(),
      expanded: false,
      didInsertElement: function() {
        this.set('expanded', hostNames.length <= 3);
      },
      onPrimary: function() {
        self.bulkOperation(operationData, hosts);
        this._super();
      },
      bodyClass: Em.View.extend({
        templateName: require('templates/main/host/bulk_operation_confirm_popup'),
        message: message,
        warningInfo: function() {
          switch (operationData.action) {
            case "DECOMMISSION":
              return Em.I18n.t('hosts.bulkOperation.warningInfo.body');
            case "PASSIVE_STATE":
              return operationData.state === 'OFF' ? Em.I18n.t('hosts.passiveMode.popup.version.mismatch.multiple')
                .format(App.StackVersion.find().findProperty('isCurrent').get('repositoryVersion.repositoryVersion')) : "";
            default:
              return ""
          }
        }.property(),
        textareaVisible: false,
        textTrigger: function() {
          this.set('textareaVisible', !this.get('textareaVisible'));
        },
        showAll: function() {
          this.set('parentView.visibleHosts', this.get('parentView.hostNames'));
          this.set('parentView.hostNamesSkippedVisible', this.get('parentView.hostNamesSkipped'));
          this.set('parentView.expanded', true);
        },
        putHostNamesToTextarea: function() {
          var hostNames = this.get('parentView.hostNames');
          if (this.get('textareaVisible')) {
            var wrapper = $(".task-detail-log-maintext");
            $('.task-detail-log-clipboard').html(hostNames).width(wrapper.width()).height(250);
            Em.run.next(function() {
              $('.task-detail-log-clipboard').select();
            });
          }
        }.observes('textareaVisible')
      })
    });
  }

});