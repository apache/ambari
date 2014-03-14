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
var validator = require('utils/validator');
var componentHelper = require('utils/component');
var batchUtils = require('utils/batch_scheduled_requests');

App.MainHostController = Em.ArrayController.extend({
  name:'mainHostController',
  content: App.Host.find(),

  clearFilters: null,

  /**
   * Components which will be shown in component filter
   * @returns {Array}
   */
  componentsForFilter:function() {
    var installedComponents = componentHelper.getInstalledComponents();
    installedComponents.setEach('checkedForHostFilter', false);
    return installedComponents;
  }.property('App.router.clusterController.isLoaded'),

  /**
   * Master components
   * @returns {Array}
   */
  masterComponents:function () {
    return this.get('componentsForFilter').filterProperty('isMaster', true);
  }.property('componentsForFilter'),

  /**
   * Slave components
   * @returns {Array}
   */
  slaveComponents:function () {
    return this.get('componentsForFilter').filterProperty('isSlave', true);
  }.property('componentsForFilter'),

  /**
   * Client components
   * @returns {Array}
   */
  clientComponents: function() {
    return this.get('componentsForFilter').filterProperty('isClient', true);
  }.property('componentsForFilter'),

  /**
   * Filter hosts by componentName of <code>component</code>
   * @param {App.HostComponent} component
   */
  filterByComponent:function (component) {
    if(!component)
      return;
    var id = component.get('componentName');
    var column = 6;
    this.get('componentsForFilter').setEach('checkedForHostFilter', false);

    var filterForComponent = {
      iColumn: column,
      value: id,
      type: 'multiple'
    };
    App.db.setFilterConditions(this.get('name'), [filterForComponent]);
  },
  /**
   * On click callback for delete button
   */
  deleteButtonPopup:function () {
    var self = this;
    App.showConfirmationPopup(function(){
      self.removeHosts();
    });
  },

  showAlertsPopup: function (event) {
    var host = event.context;
    App.router.get('mainAlertsController').loadAlerts(host.get('hostName'), "HOST");
    App.ModalPopup.show({
      header: this.t('services.alerts.headingOfList'),
      bodyClass: Ember.View.extend({
        templateName: require('templates/main/host/alerts_popup'),
        controllerBinding: 'App.router.mainAlertsController',
        alerts: function () {
          return this.get('controller.alerts');
        }.property('controller.alerts'),

        closePopup: function () {
          this.get('parentView').hide();
        }
      }),
      primary: Em.I18n.t('common.close'),
      secondary : null,
      didInsertElement: function () {
        this.$().find('.modal-footer').addClass('align-center');
        this.$().children('.modal').css({'margin-top': '-350px'});
      }
    });
    event.stopPropagation();
  },

  /**
   * remove selected hosts
   */
  removeHosts:function () {
    var hosts = this.get('content');
    var selectedHosts = hosts.filterProperty('isChecked', true);
    selectedHosts.forEach(function (_hostInfo) {
      console.log('Removing:  ' + _hostInfo.hostName);
    });
    this.get('fullContent').removeObjects(selectedHosts);
  },

  /**
   * remove hosts with id equal host_id
   * @param {String} host_id
   */
  checkRemoved:function (host_id) {
    var hosts = this.get('content');
    var selectedHosts = hosts.filterProperty('id', host_id);
    this.get('fullContent').removeObjects(selectedHosts);
  },

  /**
   * Bulk operation wrapper
   * @param {Object} operationData - data about bulk operation (action, hosts or hostComponents etc)
   * @param {Array} hosts - list of affected hosts
   */
  bulkOperation: function(operationData, hosts) {
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
      if (operationData.action === 'RESTART') {
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
  bulkOperationForHosts: function(operationData, hosts) {
    var query = [];
    hosts.forEach(function(host) {
      var subQuery = '(HostRoles/component_name.in(%@)&HostRoles/host_name=' + host.get('hostName') + ')';
      var components = [];
      host.get('hostComponents').forEach(function(hostComponent) {
        if (hostComponent.get('passiveState') == 'OFF') {
          if (hostComponent.get('isMaster') || hostComponent.get('isSlave')) {
            if (hostComponent.get('workStatus') === operationData.actionToCheck) {
              components.push(hostComponent.get('componentName'));
            }
          }
        }
      });
      if (components.length) {
        query.push(subQuery.fmt(components.join(',')));
      }
    });
    if (query.length) {
      query = query.join('|');
      App.ajax.send({
        name: 'bulk_request.hosts.all_components',
        sender: this,
        data: {
          query: query,
          state: operationData.action,
          requestInfo: operationData.message
        },
        success: 'bulkOperationForHostComponentsSuccessCallback'
      });
    }
    else {
      App.ModalPopup.show({
        header: Em.I18n.t('rolling.nothingToDo.header'),
        body: Em.I18n.t('rolling.nothingToDo.body').format(Em.I18n.t('hosts.host.maintainance.allComponents.context')),
        secondary: false
      });
    }
  },

  /**
   * Bulk restart for selected hosts
   * @param {Object} operationData - data about bulk operation (action, hostComponents etc)
   * @param {Ember.Enumerable} hosts - list of affected hosts
   */
  bulkOperationForHostsRestart: function(operationData, hosts) {
    var hostComponents = [];
    hosts.forEach(function(host) {
      hostComponents.pushObjects(host.get('hostComponents').filterProperty('passiveState','OFF').toArray());
    });
    batchUtils.restartHostComponents(hostComponents, Em.I18n.t('rollingrestart.context.allOnSelectedHosts'));
  },

  /**
   * Bulk turn on/off passive state for selected hosts
   * @param {Object} operationData - data about bulk operation (action, hostComponents etc)
   * @param {Array} hosts - list of affected hosts
   */
  bulkOperationForHostsPassiveState: function(operationData, hosts) {
    var affectedHosts = [];
    hosts.forEach(function(host) {
      if (host.get('passiveState') !== operationData.state) {
        affectedHosts.push(host);
      }
    });
    if (affectedHosts.length) {
      App.ajax.send({
        name: 'bulk_request.hosts.passive_state',
        sender: this,
        data: {
          hosts: affectedHosts,
          hostNames: affectedHosts.mapProperty('hostName').join(','),
          passive_state: operationData.state,
          requestInfo: operationData.message
        },
        success: 'updateHostPassiveState'
      });
    }
    else {
      App.ModalPopup.show({
        header: Em.I18n.t('rolling.nothingToDo.header'),
        body: Em.I18n.t('hosts.bulkOperation.passiveState.nothingToDo.body'),
        secondary: false
      });
    }
  },

  updateHostPassiveState: function(data, opt, params) {
    params.hosts.setEach('passiveState', params.passive_state);
    App.router.get('clusterController').loadUpdatedStatus(function(){
      batchUtils.infoPassiveState(params.passive_state);
    });
  },
  /**
   * Bulk operation for selected hostComponents
   * @param {Object} operationData - data about bulk operation (action, hostComponents etc)
   * @param {Array} hosts - list of affected hosts
   */
  bulkOperationForHostComponents: function(operationData, hosts) {
    var service = App.Service.find(operationData.serviceName);
    var components = service.get('hostComponents').filter(function(hc) {
      if (hc.get('componentName') != operationData.componentName || hc.get('passiveState') != 'OFF' ) {
        return false;
      }
      if(hc.get('workStatus') == operationData.action) {
        return false;
      }
      return hosts.contains(hc.get('host'));
    });

    if (components.length) {
      var hostsWithComponentInProperState = components.mapProperty('host.hostName');
      App.ajax.send({
        name: 'bulk_request.host_components',
        sender: this,
        data: {
          hostNames: hostsWithComponentInProperState.join(','),
          state: operationData.action,
          requestInfo: operationData.message + ' ' + operationData.componentNameFormatted,
          componentName: operationData.componentName
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
  },

  /**
   * Bulk decommission/recommission for selected hostComponents
   * @param {Object} operationData
   * @param {Array} hosts
   */
  bulkOperationForHostComponentsDecommission: function(operationData, hosts) {
    var service = App.Service.find(operationData.serviceName);
    var components = service.get('hostComponents').filter(function(hc) {
      if (hc.get('componentName') != operationData.realComponentName || hc.get('passiveState') != 'OFF' ) {
        return false;
      }
      return hosts.contains(hc.get('host'));
    });

    if (components.length) {
      var hostsWithComponentInProperState = components.mapProperty('host.hostName');
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
        else if (svcName === "MAPREDUCE") {
          App.router.get('mainHostDetailsController').doRecommissionAndRestart(hostNames, svcName, masterName, slaveName);
        }
      } else {
        hostsWithComponentInProperState = components.filterProperty('workStatus','STARTED').mapProperty('host.hostName');
        //For decommession
        if (svcName == "HBASE") {
          // HBASE service, decommission RegionServer in batch requests
          App.router.get('mainHostDetailsController').doDecommissionRegionServer(hostNames, svcName, masterName, slaveName);
        } else {
          var parameters = {
            "slave_type": slaveName
          };
          var contextString = turn_off? 'hosts.host.' + slaveName.toLowerCase() + '.recommission':
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
              parameters: parameters
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
   * Bulk restart for selected hostComponents
   * @param {Object} operationData
   * @param {Array} hosts
   */
  bulkOperationForHostComponentsRestart: function(operationData, hosts) {
    var service = App.Service.find(operationData.serviceName);
    var components = service.get('hostComponents').filter(function(hc) {
      if (hc.get('componentName') != operationData.componentName || hc.get('passiveState') != 'OFF') {
        return false;
      }
      return hosts.contains(hc.get('host'));
    });

    if (components.length) {
      batchUtils.showRollingRestartPopup(components.objectAt(0).get('componentName'), false, components);
    }
    else {
      App.ModalPopup.show({
        header: Em.I18n.t('rolling.nothingToDo.header'),
        body: Em.I18n.t('rolling.nothingToDo.body').format(operationData.componentNameFormatted),
        secondary: false
      });
    }
  },

  updateHostComponentsPassiveState: function(data, opt, params) {
    App.router.get('clusterController').loadUpdatedStatus(function(){
      batchUtils.infoPassiveState(params.passive_state);
    });
  },
  /**
   * Show BO popup after bulk request
   */
  bulkOperationForHostComponentsSuccessCallback: function() {
    App.router.get('applicationController').dataLoading().done(function (initValue) {
      if (initValue) {
        App.router.get('backgroundOperationsController').showPopup();
      }
    });
  }

});
