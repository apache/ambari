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
var batchUtils = require('utils/batch_scheduled_requests');
var componentsUtils = require('utils/components');
var stringUtils = require('utils/string_utils');

App.MainHostDetailsController = Em.Controller.extend({

  name: 'mainHostDetailsController',

  /**
   * Viewed host
   * @type {App.Host|null}
   */
  content: null,

  /**
   * Does user come from hosts page
   * @type {bool}
   */
  isFromHosts: false,

  /**
   * path to page visited before
   * @type {string}
   */
  referer: '',

  /**
   * Open dashboard page
   * @method routeHome
   */
  routeHome: function () {
    App.router.transitionTo('main.dashboard.index');
  },

  /**
   * Open summary page of the selected service
   * @param {object} event
   * @method routeToService
   */
  routeToService: function (event) {
    var service = event.context;
    App.router.transitionTo('main.services.service.summary', service);
  },

  /**
   * List of active (not in passive state) host components
   * @type {Ember.Enumerable}
   */
  serviceActiveComponents: function () {
    return this.get('content.hostComponents').filterProperty('service.isInPassive', false);
  }.property('content.hostComponents'),

  /**
   * List of active host components which aren't clients
   * @type {Ember.Enumerable}
   */
  serviceNonClientActiveComponents: function () {
    return this.get('serviceActiveComponents').filterProperty('isClient', false);
  }.property('serviceActiveComponents'),

  /**
   * send command to server to start selected host component
   * @param {object} event
   * @method startComponent
   */
  startComponent: function (event) {
    var self = this;
    return App.showConfirmationPopup(function () {
      var component = event.context;
      var context = Em.I18n.t('requestInfo.startHostComponent') + " " + component.get('displayName');
      self.sendComponentCommand(component, context, App.HostComponentStatus.started);
    });
  },

  /**
   * send command to server to stop selected host component
   * @param {object} event
   * @method startComponent
   */
  stopComponent: function (event) {
    var self = this;
    return App.showConfirmationPopup(function () {
      var component = event.context;
      var context = Em.I18n.t('requestInfo.stopHostComponent') + " " + component.get('displayName');
      self.sendComponentCommand(component, context, App.HostComponentStatus.stopped);
    });
  },
  /**
   * PUTs a command to server to start/stop a component. If no
   * specific component is provided, all components are started.
   * @param {object} component  When <code>null</code> all startable components are started.
   * @param {String} context  Context under which this command is beign sent.
   * @param {String} state - desired state of component can be 'STARTED' or 'STOPPED'
   * @method sendComponentCommand
   */
  sendComponentCommand: function (component, context, state) {
    var data = {
      hostName: this.get('content.hostName'),
      context: context,
      component: component,
      HostRoles: {
        state: state
      }
    };
    if (Array.isArray(component)) {
      data.query = "HostRoles/component_name.in(" + component.mapProperty('componentName').join(',') + ")";
    } else {
      data.componentName = component.get('componentName');
      data.serviceName = component.get('service.serviceName');
    }
    App.ajax.send({
      name: (Array.isArray(component)) ? 'common.host.host_components.update' : 'common.host.host_component.update',
      sender: this,
      data: data,
      success: 'sendComponentCommandSuccessCallback',
      error: 'ajaxErrorCallback'
    });
  },

  /**
   * Success callback for stop/start host component request
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method stopComponentSuccessCallback
   */
  sendComponentCommandSuccessCallback: function (data, opt, params) {
    var running = (params.HostRoles.state === App.HostComponentStatus.stopped) ? App.HostComponentStatus.stopping : App.HostComponentStatus.starting;
    console.log('Send request for ' + running + ' successfully');
    params.component.set('workStatus', running);
    if (App.get('testMode')) {
      this.mimicWorkStatusChange(params.component, running, params.HostRoles.state);
    }
    this.showBackgroundOperationsPopup();
  },

  /**
   * Default error-callback for ajax-requests in current page
   * @param {object} request
   * @param {object} ajaxOptions
   * @param {string} error
   * @param {object} opt
   * @param {object} params
   * @method ajaxErrorCallback
   */
  ajaxErrorCallback: function (request, ajaxOptions, error, opt, params) {
    return componentsUtils.ajaxErrorCallback(request, ajaxOptions, error, opt, params);
  },
  /**
   * mimic status transition in test mode
   * @param entity
   * @param transitionalState
   * @param finalState
   */
  mimicWorkStatusChange: function (entity, transitionalState, finalState) {
    if (Em.isArray(entity)) {
      entity.forEach(function (item) {
        item.set('workStatus', transitionalState);
        setTimeout(function () {
          item.set('workStatus', finalState);
        }, App.testModeDelayForActions);
      });
    } else {
      entity.set('workStatus', transitionalState);
      setTimeout(function () {
        entity.set('workStatus', finalState);
      }, App.testModeDelayForActions);
    }
  },

  /**
   * load data (if we need to show this background operations popup) from persist
   * @param callback
   */
  showBackgroundOperationsPopup: function (callback) {
    App.router.get('applicationController').dataLoading().done(function (initValue) {
      if (initValue) {
        App.router.get('backgroundOperationsController').showPopup();
      }
      if (typeof callback === 'function') {
        callback();
      }
    });
  },

  /**
   * Send command to server to delete selected host component
   * @param {object} event
   * @method deleteComponent
   */
  deleteComponent: function (event) {
    var self = this;
    var component = event.context;
    var componentName = component.get('componentName');
    var displayName = component.get('displayName');
    var isLastComponent = (this.getTotalComponent(component) === 1);
    return App.ModalPopup.show({
      header: Em.I18n.t('popup.confirmation.commonHeader'),
      primary: Em.I18n.t('hosts.host.deleteComponent.popup.confirm'),
      bodyClass: Em.View.extend({
        templateName: require('templates/main/host/details/deleteComponentPopup')
      }),
      isChecked: false,
      disablePrimary: function () {
        return !this.get('isChecked');
      }.property('isChecked'),
      lastComponent: function () {
        this.set('isChecked', !isLastComponent);
        return isLastComponent;
      }.property(),
      isZkServer: function () {
        return componentName == 'ZOOKEEPER_SERVER';
      }.property(),
      lastComponentError: Em.View.extend({
        template: Em.Handlebars.compile(Em.I18n.t('hosts.host.deleteComponent.popup.warning').format(displayName))
      }),
      restartNagiosMsg: Em.View.extend({
        template: Em.Handlebars.compile(Em.I18n.t('hosts.host.deleteComponent.popup.msg2').format(displayName))
      }),
      deleteComponentMsg: function () {
        return Em.I18n.t('hosts.host.deleteComponent.popup.msg1').format(displayName);
      }.property(),
      deleteZkServerMsg: Em.View.extend({
        template: Em.Handlebars.compile(Em.I18n.t('hosts.host.deleteComponent.popup.deleteZooKeeperServer'))
      }),
      onPrimary: function () {
        var popup = this;
        self._doDeleteHostComponent(component, function () {
          self.set('redrawComponents', true);
          popup.hide();
        });
      }
    });
  },

  /**
   * get total count of host-components
   * @method getTotalComponent
   * @param component
   * @return {Number}
   */
  getTotalComponent: function (component) {
    var count;
    if (component.get('isSlave')) {
      count = App.SlaveComponent.find(component.get('componentName')).get('totalCount');
    } else if (component.get('isClient')) {
      count = App.ClientComponent.find(component.get('componentName')).get('totalCount');
    } else {
      count = App.HostComponent.find().filterProperty('componentName', component.get('componentName')).get('length')
    }
    return count || 0;
  },

  /**
   * Trigger to reset list of master/slaves components on the view
   * @type {bool}
   */
  redrawComponents: false,

  /**
   * Deletes the given host component, or all host components.
   *
   * @param {object|null} component  When <code>null</code> all host components are deleted.
   * @return  <code>null</code> when components get deleted.
   *          <code>{xhr: XhrObj, url: "http://", method: "DELETE"}</code>
   *          when components failed to get deleted.
   * @method _doDeleteHostComponent
   */
  _doDeleteHostComponent: function (component, callback) {
    callback = callback || Em.K;
    App.ajax.send({
      name: (Em.isNone(component)) ? 'common.delete.host' : 'common.delete.host_component',
      sender: this,
      data: {
        componentName: (component) ? component.get('componentName') : '',
        hostName: this.get('content.hostName')
      },
      success: '_doDeleteHostComponentSuccessCallback',
      error: '_doDeleteHostComponentErrorCallback'
    }).then(callback, callback);
  },

  /**
   * Result of delete component(s) request
   * @type {object}
   */
  _deletedHostComponentResult: null,

  /**
   * Success callback for delete host component request
   * @method _doDeleteHostComponentSuccessCallback
   */
  _doDeleteHostComponentSuccessCallback: function (response, request, data) {
    this.set('_deletedHostComponentResult', null);
    if (data.componentName == 'ZOOKEEPER_SERVER') {
      this.set('fromDeleteZkServer', true);
      this.loadConfigs();
    }
  },

  /**
   * Error-callback for delete host component request
   * @param {object} xhr
   * @param {string} textStatus
   * @param {object} errorThrown
   * @method _doDeleteHostComponentErrorCallback
   */
  _doDeleteHostComponentErrorCallback: function (xhr, textStatus, errorThrown, data) {
    console.log('Error deleting host component');
    console.log(textStatus);
    console.log(errorThrown);
    this.set('_deletedHostComponentResult', {xhr: xhr, url: data.url, method: 'DELETE'});
  },

  /**
   * Send command to server to upgrade selected host component
   * @param {object} event
   * @method upgradeComponent
   */
  upgradeComponent: function (event) {
    var self = this;
    var component = event.context;
    return App.showConfirmationPopup(function () {
      App.ajax.send({
        name: 'host.host_component.upgrade',
        sender: self,
        data: {
          component: component,
          hostName: self.get('content.hostName'),
          componentName: component.get('componentName'),
          data: JSON.stringify({
            RequestInfo: {
              "context": Em.I18n.t('requestInfo.upgradeHostComponent') + " " + component.get('displayName')
            },
            Body: {
              HostRoles: {
                stack_id: 'HDP-1.2.2',
                state: 'INSTALLED'
              }
            }
          })
        },
        success: 'upgradeComponentSuccessCallback',
        error: 'ajaxErrorCallback'
      });
    });
  },

  /**
   * Success callback for upgrade host component request
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method upgradeComponentSuccessCallback
   */
  upgradeComponentSuccessCallback: function (data, opt, params) {
    console.log('Send request for UPGRADE successfully');

    if (App.get('testMode')) {
      this.mimicWorkStatusChange(params.component, App.HostComponentStatus.starting, App.HostComponentStatus.started);
    }
    this.showBackgroundOperationsPopup();
  },

  /**
   * Send command to server to restart selected components
   * @param {object} event
   * @method restartComponent
   */
  restartComponent: function (event) {
    var component = event.context;
    return App.showConfirmationPopup(function () {
      batchUtils.restartHostComponents([component], Em.I18n.t('rollingrestart.context.selectedComponentOnSelectedHost').format(component.get('displayName')), "HOST_COMPONENT");
    });
  },
  /**
   * get current status of security settings,
   * if true security is enabled otherwise disabled
   * @return {Boolean}
   */
  securityEnabled: function () {
    return App.router.get('mainAdminSecurityController.securityEnabled');
  }.property('App.router.mainAdminSecurityController.securityEnabled'),


  /**
   * Send command to server to install selected host component
   * @param {object} event
   * @method addComponent
   */
  addComponent: function (event) {
    var self = this;
    var component = event.context;
    var componentName = component.get('componentName');
    var missedComponents = componentsUtils.checkComponentDependencies(componentName, {
      scope: 'host',
      installedComponents: this.get('content.hostComponents').mapProperty('componentName')
    });
    if (!!missedComponents.length) {
      var popupMessage = Em.I18n.t('host.host.addComponent.popup.dependedComponents.body').format(component.get('displayName'),
        stringUtils.getFormattedStringFromArray(missedComponents.map(function(cName) {
          return App.StackServiceComponent.find(cName).get('displayName');
        })));
      return App.showAlertPopup(Em.I18n.t('host.host.addComponent.popup.dependedComponents.header'), popupMessage);
    }
    if (componentName === 'ZOOKEEPER_SERVER') {
      return App.showConfirmationPopup(function () {
        self.primary(component);
      }, Em.I18n.t('hosts.host.addComponent.addZooKeeper'));
    }
    else {
      if (this.get('securityEnabled') && componentName !== 'CLIENTS') {
        return App.showConfirmationPopup(function () {
          self.primary(component);
        }, Em.I18n.t('hosts.host.addComponent.securityNote').format(componentName, self.get('content.hostName')));
      }
      else {
        return this.addClientComponent(component);
      }
    }
  },
  /**
   * Send command to server to install client on selected host
   * @param component
   */
  addClientComponent: function (component) {
    var self = this;
    var message = this.formatClientsMessage(component);
    return App.ModalPopup.show({
      primary: Em.I18n.t('hosts.host.addComponent.popup.confirm'),
      header: Em.I18n.t('popup.confirmation.commonHeader'),

      addComponentMsg: function () {
        return Em.I18n.t('hosts.host.addComponent.msg').format(message);
      }.property(),

      bodyClass: Em.View.extend({
        templateName: require('templates/main/host/details/addComponentPopup')
      }),

      restartNagiosMsg: Em.View.extend({
        template: Em.Handlebars.compile(Em.I18n.t('hosts.host.addComponent.note').format(message))
      }),

      onPrimary: function () {
        this.hide();
        if (component.get('componentName') === 'CLIENTS') {
          // Clients component has many sub-components which
          // need to be installed.
          var scs = component.get('subComponentNames');
          scs.forEach(function (sc) {
            var c = Em.Object.create({
              displayName: App.format.role(sc),
              componentName: sc,
              serviceName: sc.replace("_CLIENT", "")
            });
            self.primary(c);
          });
        } else {
          self.primary(component);
        }
      }
    });
  },

  /**
   * format message for operation of adding clients
   * @param client
   */
  formatClientsMessage: function (client) {
    var displayName = Em.isNone(client.get('displayName')) ? '' : client.get('displayName');
    var subComponentNames = client.get('subComponentNames');
    if (subComponentNames && subComponentNames.length > 0) {
      var dns = [];
      subComponentNames.forEach(function (scn) {
        dns.push(App.format.role(scn));
      });
      displayName += " (" + dns.join(", ") + ")";
    }
    return displayName;
  },

  /**
   * Send request to add host component
   * @param {App.HostComponent} component
   * @method primary
   */
  primary: function (component) {

    var self = this;
    componentsUtils.installHostComponent(self.get('content.hostName'), component);
  },

  /**
   * Success callback for install host component request (sent in <code>addNewComponentSuccessCallback</code>)
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method installNewComponentSuccessCallback
   */
  installNewComponentSuccessCallback: function (data, opt, params) {
    if (!data.Requests || !data.Requests.id) {
      return false;
    }
    var self = this;
    console.log('Send request for INSTALLING NEW COMPONENT successfully');

    if (App.get('testMode')) {
      this.mimicWorkStatusChange(params.component, App.HostComponentStatus.installing, App.HostComponentStatus.stopped);
    }

    this.showBackgroundOperationsPopup(function () {
      if (params.componentName === 'ZOOKEEPER_SERVER') {
        self.set('zkRequestId', data.Requests.id);
        self.addObserver('App.router.backgroundOperationsController.serviceTimestamp', self, self.checkZkConfigs);
        self.checkZkConfigs();
      }
    });
    return true;
  },

  /**
   * Send command to server to resfresh configs of selected component
   * @param {object} event
   * @method refreshComponentConfigs
   */
  refreshComponentConfigs: function (event) {
    var self = this;
    return App.showConfirmationPopup(function () {
      var component = event.context;
      var context = Em.I18n.t('requestInfo.refreshComponentConfigs').format(component.get('displayName'));
      self.sendRefreshComponentConfigsCommand(component, context);
    });
  },

  /**
   * PUTs a command to server to refresh configs of host component.
   * @param {object} component
   * @param {object} context Context under which this command is beign sent.
   * @method sendRefreshComponentConfigsCommand
   */
  sendRefreshComponentConfigsCommand: function (component, context) {
    var resource_filters = [
      {
        service_name: component.get('service.serviceName'),
        component_name: component.get('componentName'),
        hosts: component.get('host.hostName')
      }
    ];
    App.ajax.send({
      name: 'host.host_component.refresh_configs',
      sender: this,
      data: {
        resource_filters: resource_filters,
        context: context
      },
      success: 'refreshComponentConfigsSuccessCallback'
    });
  },

  /**
   * Success callback for refresh host component configs request
   * @method refreshComponentConfigsSuccessCallback
   */
  refreshComponentConfigsSuccessCallback: function () {
    console.log('Send request for refresh configs successfully');
    this.showBackgroundOperationsPopup();
  },

  /**
   * Load tags
   * @method checkZkConfigs
   */
  checkZkConfigs: function () {
    var bg = App.router.get('backgroundOperationsController.services').findProperty('id', this.get('zkRequestId'));
    if (bg && !bg.get('isRunning')) {
      var self = this;
      this.removeObserver('App.router.backgroundOperationsController.serviceTimestamp', this, this.checkZkConfigs);
      setTimeout(function () {
        self.loadConfigs();
      }, App.get('componentsUpdateInterval'));
    }
  },

  /**
   * Load configs
   * @method loadConfigs
   */
  loadConfigs: function () {
    App.ajax.send({
      name: 'config.tags',
      sender: this,
      success: 'loadConfigsSuccessCallback'
    });
  },

  /**
   * Success callback for load configs request
   * @param {object} data
   * @method adConfigsSuccessCallback
   */
  loadConfigsSuccessCallback: function (data) {
    var urlParams = this.constructConfigUrlParams(data);
    if (urlParams.length > 0) {
      App.ajax.send({
        name: 'reassign.load_configs',
        sender: this,
        data: {
          urlParams: urlParams.join('|')
        },
        success: 'saveZkConfigs'
      });
      return true;
    }
    return false;
  },

  /**
   * construct URL params for query, that load configs
   * @param data {Object}
   * @return {Array}
   */
  constructConfigUrlParams: function (data) {
    var urlParams = [];
    var services = App.Service.find();
    if (App.get('isHaEnabled')) {
      urlParams.push('(type=core-site&tag=' + data.Clusters.desired_configs['core-site'].tag + ')');
    }
    if (services.someProperty('serviceName', 'HBASE')) {
      urlParams.push('(type=hbase-site&tag=' + data.Clusters.desired_configs['hbase-site'].tag + ')');
    }
    if (services.someProperty('serviceName', 'HIVE')) {
      urlParams.push('(type=webhcat-site&tag=' + data.Clusters.desired_configs['webhcat-site'].tag + ')');
      urlParams.push('(type=hive-site&tag=' + data.Clusters.desired_configs['hive-site'].tag + ')');
    }
    if (services.someProperty('serviceName', 'STORM')) {
      urlParams.push('(type=storm-site&tag=' + data.Clusters.desired_configs['storm-site'].tag + ')');
    }
    if ((services.someProperty('serviceName', 'YARN') && App.get('isHadoop22Stack')) || App.get('isRMHaEnabled')) {
      urlParams.push('(type=yarn-site&tag=' + data.Clusters.desired_configs['yarn-site'].tag + ')');
    }
    return urlParams;
  },

  /**
   * save new ZooKeeper configs to server
   * @param {object} data
   * @method saveZkConfigs
   */
  saveZkConfigs: function (data) {
    var configs = {};
    data.items.forEach(function (item) {
      configs[item.type] = item.properties;
    }, this);

    var zks = this.getZkServerHosts();
    var zksWithPort = this.concatZkNames(zks);
    this.setZKConfigs(configs, zksWithPort, zks);

    for (var site in configs) {
      if (!configs.hasOwnProperty(site)) continue;
      App.ajax.send({
        name: 'reassign.save_configs',
        sender: this,
        data: {
          siteName: site,
          properties: configs[site],
          service_config_version_note: Em.I18n.t('hosts.host.zooKeeper.configs.save.note')
        }
      });
    }
  },
  /**
   *
   * Set new values for some configs (based on available ZooKeeper Servers)
   * @param configs {object}
   * @param zksWithPort {string}
   * @param zks {array}
   * @return {Boolean}
   */
  setZKConfigs: function (configs, zksWithPort, zks) {
    if (typeof configs !== 'object' || !Array.isArray(zks)) return false;
    if (App.get('isHaEnabled')) {
      configs['core-site']['ha.zookeeper.quorum'] = zksWithPort;
    }
    if (configs['hbase-site']) {
      configs['hbase-site']['hbase.zookeeper.quorum'] = zks.join(',');
    }
    if (configs['webhcat-site']) {
      configs['webhcat-site']['templeton.zookeeper.hosts'] = zksWithPort;
    }
    if (configs['hive-site']) {
      configs['hive-site']['hive.cluster.delegation.token.store.zookeeper.connectString'] = zksWithPort;
    }
    if (configs['storm-site']) {
      configs['storm-site']['storm.zookeeper.servers'] = JSON.stringify(zks).replace(/"/g, "'");
    }
    if (App.get('isRMHaEnabled')) {
      configs['yarn-site']['yarn.resourcemanager.zk-address'] = zks.join(',');
    }
    if (App.get('isHadoop22Stack')) {
      if (configs['hive-site']) {
        configs['hive-site']['hive.zookeeper.quorum'] = zksWithPort;
      }
      if (configs['yarn-site']) {
        configs['yarn-site']['hadoop.registry.zk.quorum'] = zksWithPort;
      }
    }
    return true;
  },
  /**
   * concatenate URLs to ZOOKEEPER hosts with port "2181",
   * as value of config divided by comma
   * @param zks {array}
   */
  concatZkNames: function (zks) {
    var zks_with_port = '';
    zks.forEach(function (zk) {
      zks_with_port += zk + ':2181,';
    });
    return zks_with_port.slice(0, -1);
  },

  /**
   * Is deleteHost action id fired
   * @type {bool}
   */
  fromDeleteHost: false,

  /**
   * Is ZooKeeper Server being deleted from host
   * @type {bool}
   */
  fromDeleteZkServer: false,

  /**
   * Get list of hostnames where ZK Server is installed
   * @returns {string[]}
   * @method getZkServerHosts
   */
  getZkServerHosts: function () {
    var zks = App.HostComponent.find().filterProperty('componentName', 'ZOOKEEPER_SERVER').mapProperty('hostName');
    if (this.get('fromDeleteHost') || this.get('fromDeleteZkServer')) {
      this.set('fromDeleteHost', false);
      this.set('fromDeleteZkServer', false);
      return zks.without(this.get('content.hostName'));
    }
    return zks;
  },

  /**
   * Send command to server to install selected host component
   * @param {Object} event
   * @method installComponent
   */
  installComponent: function (event) {
    var self = this;
    var component = event.context;
    var componentName = component.get('componentName');
    var displayName = component.get('displayName');

    return App.ModalPopup.show({
      primary: Em.I18n.t('hosts.host.installComponent.popup.confirm'),
      header: Em.I18n.t('popup.confirmation.commonHeader'),
      installComponentMessage: function () {
        return Em.I18n.t('hosts.host.installComponent.msg').format(displayName);
      }.property(),
      restartNagiosMsg: Em.View.extend({
        template: Em.Handlebars.compile(Em.I18n.t('hosts.host.addComponent.note').format(displayName))
      }),
      bodyClass: Em.View.extend({
        templateName: require('templates/main/host/details/installComponentPopup')
      }),
      onPrimary: function () {
        this.hide();

        App.ajax.send({
          name: 'common.host.host_component.update',
          sender: self,
          data: {
            hostName: self.get('content.hostName'),
            serviceName: component.get('service.serviceName'),
            componentName: componentName,
            component: component,
            context: Em.I18n.t('requestInfo.installHostComponent') + " " + displayName,
            HostRoles: {
              state: 'INSTALLED'
            }
          },
          success: 'installComponentSuccessCallback',
          error: 'ajaxErrorCallback'
        });
      }
    });
  },

  /**
   * Success callback for install component request
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method installComponentSuccessCallback
   */
  installComponentSuccessCallback: function (data, opt, params) {
    console.log('Send request for REINSTALL COMPONENT successfully');
    if (App.get('testMode')) {
      this.mimicWorkStatusChange(params.component, App.HostComponentStatus.installing, App.HostComponentStatus.stopped);
    }
    this.showBackgroundOperationsPopup();
  },

  /**
   * Send command to server to run decommission on DATANODE, TASKTRACKER, NODEMANAGER, REGIONSERVER
   * @param {App.HostComponent} component
   * @method decommission
   */
  decommission: function (component) {
    var self = this;
    return App.showConfirmationPopup(function () {
      self.runDecommission.call(self, self.get('content.hostName'), component.get('service.serviceName'));
    });
  },
  /**
   * identify correct component to run decommission on them by service name,
   * in result run proper decommission method
   * @param hostName
   * @param svcName
   */
  runDecommission: function (hostName, svcName) {
    switch (svcName) {
      case 'HDFS':
        this.doDecommission(hostName, svcName, "NAMENODE", "DATANODE");
        break;
      case 'YARN':
        this.doDecommission(hostName, svcName, "RESOURCEMANAGER", "NODEMANAGER");
        break;
      case 'MAPREDUCE':
        this.doDecommission(hostName, svcName, "JOBTRACKER", "TASKTRACKER");
        break;
      case 'HBASE':
        this.warnBeforeDecommission(hostName);
    }
  },

  /**
   * Send command to server to run recommission on DATANODE, TASKTRACKER, NODEMANAGER
   * @param {App.HostComponent} component
   * @method recommission
   */
  recommission: function (component) {
    var self = this;
    return App.showConfirmationPopup(function () {
      self.runRecommission.call(self, self.get('content.hostName'), component.get('service.serviceName'));
    });
  },
  /**
   * identify correct component to run recommission on them by service name,
   * in result run proper recommission method
   * @param hostName
   * @param svcName
   */
  runRecommission: function (hostName, svcName) {
    switch (svcName) {
      case 'HDFS':
        this.doRecommissionAndStart(hostName, svcName, "NAMENODE", "DATANODE");
        break;
      case 'YARN':
        this.doRecommissionAndStart(hostName, svcName, "RESOURCEMANAGER", "NODEMANAGER");
        break;
      case 'MAPREDUCE':
        this.doRecommissionAndRestart(hostName, svcName, "JOBTRACKER", "TASKTRACKER");
        break;
      case 'HBASE':
        this.doRecommissionAndStart(hostName, svcName, "HBASE_MASTER", "HBASE_REGIONSERVER");
    }
    this.showBackgroundOperationsPopup();
  },

  /**
   * Performs Decommission (for DN, TT and NM)
   * @param {string} hostName
   * @param {string} serviceName
   * @param {string} componentName
   * @param {string} slaveType
   * @method doDecommission
   */
  doDecommission: function (hostName, serviceName, componentName, slaveType) {
    var contextNameString = 'hosts.host.' + slaveType.toLowerCase() + '.decommission';
    var context = Em.I18n.t(contextNameString);
    App.ajax.send({
      name: 'host.host_component.decommission_slave',
      sender: this,
      data: {
        context: context,
        command: 'DECOMMISSION',
        hostName: hostName,
        serviceName: serviceName,
        componentName: componentName,
        slaveType: slaveType
      },
      success: 'decommissionSuccessCallback',
      error: 'decommissionErrorCallback'
    });
  },

  /**
   * check is hbase regionserver in mm. If so - run decommission
   * otherwise shows warning
   * @method warnBeforeDecommission
   * @param {string} hostNames - list of host when run from bulk operations or current host
   */
  warnBeforeDecommission: function (hostNames) {
    if (this.get('content.hostComponents').findProperty('componentName', 'HBASE_REGIONSERVER').get('passiveState') == "OFF") {
      this.showHbaseActiveWarning();
    } else {
      this.checkRegionServerState(hostNames);
    }
  },

  /**
   *  send call to check is this regionserver last in cluster which has desired_admin_state property "INSERVICE"
   * @method checkRegionServerState
   * @param hostNames
   */
  checkRegionServerState: function (hostNames) {
    return App.ajax.send({
      name: 'host.region_servers.in_inservice',
      sender: this,
      data: {
        hostNames: hostNames
      },
      success: 'checkRegionServerStateSuccessCallback'
    });
  },

  /**
   * check is this regionserver last in cluster which has desired_admin_state property "INSERVICE"
   * @method checkRegionServerStateSuccessCallback
   * @param data
   * @param opt
   * @param params
   */
  checkRegionServerStateSuccessCallback: function (data, opt, params) {
    var hostArray = params.hostNames.split(",");
    var decommissionPossible = (data.items.mapProperty('HostRoles.host_name').filter(function (hostName) {
      return !hostArray.contains(hostName);
    }, this).length >= 1);
    if (decommissionPossible) {
      this.doDecommissionRegionServer(params.hostNames, "HBASE", "HBASE_MASTER", "HBASE_REGIONSERVER");
    } else {
      this.showRegionServerWarning();
    }
  },

  /**
   * show warning that regionserver is last in cluster which has desired_admin_state property "INSERVICE"
   * @method showRegionServerWarning
   * @param hostNames
   */
  showRegionServerWarning: function () {
    return App.ModalPopup.show({
      header: Em.I18n.t('common.warning'),
      message: Em.I18n.t('hosts.host.hbase_regionserver.decommission.warning'),
      bodyClass: Ember.View.extend({
        template: Em.Handlebars.compile('<div class="alert alert-warning">{{message}}</div>')
      }),
      secondary: false
    });
  },

  /**
   * shows warning: put hbase regionserver in passive state
   * @method showHbaseActiveWarning
   * @return {App.ModalPopup}
   */
  showHbaseActiveWarning: function () {
    return App.ModalPopup.show({
      header: Em.I18n.t('common.warning'),
      message: function () {
        return Em.I18n.t('hostPopup.reccomendation.beforeDecommission').format(App.format.components["HBASE_REGIONSERVER"]);
      }.property(),
      bodyClass: Ember.View.extend({
        template: Em.Handlebars.compile('<div class="alert alert-warning">{{message}}</div>')
      }),
      secondary: false
    });
  },

  /**
   * Performs Decommission (for RegionServer)
   * @method doDecommissionRegionServer
   * @param {string} hostNames - list of host when run from bulk operations or current host
   * @param {string} serviceName - serviceName
   * @param {string} componentName - master compoent name
   * @param {string} slaveType - slave component name
   */
  doDecommissionRegionServer: function (hostNames, serviceName, componentName, slaveType) {
    var batches = [
      {
        "order_id": 1,
        "type": "POST",
        "uri": App.get('apiPrefix') + "/clusters/" + App.get('clusterName') + "/requests",
        "RequestBodyInfo": {
          "RequestInfo": {
            "context": Em.I18n.t('hosts.host.regionserver.decommission.batch1'),
            "command": "DECOMMISSION",
            "exclusive" :"true",
            "parameters": {
              "slave_type": slaveType,
              "excluded_hosts": hostNames
            },
            'operation_level': {
              level: "HOST_COMPONENT",
              cluster_name: App.get('clusterName'),
              host_name: hostNames,
              service_name: serviceName
            }
          },
          "Requests/resource_filters": [
            {"service_name": serviceName, "component_name": componentName}
          ]
        }
      }];
    var id = 2;
    var hAray = hostNames.split(",");
    for (var i = 0; i < hAray.length; i++) {
      batches.push({
        "order_id": id,
        "type": "PUT",
        "uri": App.get('apiPrefix') + "/clusters/" + App.get('clusterName') + "/hosts/" + hAray[i] + "/host_components/" + slaveType,
        "RequestBodyInfo": {
          "RequestInfo": {
            context: Em.I18n.t('hosts.host.regionserver.decommission.batch2'),
            exclusive: true,
            operation_level: {
              level: "HOST_COMPONENT",
              cluster_name: App.get('clusterName'),
              host_name: hostNames,
              service_name: serviceName || null
            }
          },
          "Body": {
            HostRoles: {
              state: "INSTALLED"
            }
          }
        }
      });
      id++
    }
    batches.push({
        "order_id": id,
        "type": "POST",
        "uri": App.get('apiPrefix') + "/clusters/" + App.get('clusterName') + "/requests",
        "RequestBodyInfo": {
          "RequestInfo": {
            "context": Em.I18n.t('hosts.host.regionserver.decommission.batch3'),
            "command": "DECOMMISSION",
            "service_name": serviceName,
            "component_name": componentName,
            "parameters": {
              "slave_type": slaveType,
              "excluded_hosts": hostNames,
              "mark_draining_only": true
            },
            'operation_level': {
              level: "HOST_COMPONENT",
              cluster_name: App.get('clusterName'),
              host_name: hostNames,
              service_name: serviceName
            }
          },
          "Requests/resource_filters": [
            {"service_name": serviceName, "component_name": componentName}
          ]
        }
      });
    App.ajax.send({
      name: 'host.host_component.recommission_and_restart',
      sender: this,
      data: {
        intervalTimeSeconds: 1,
        tolerateSize: 0,
        batches: batches
      },
      success: 'decommissionSuccessCallback',
      error: 'decommissionErrorCallback'
    });
  },

  /**
   * Error callback for decommission requests
   * @param {object} request
   * @param {object} ajaxOptions
   * @param {string} error
   * @method decommissionErrorCallback
   */
  decommissionErrorCallback: function (request, ajaxOptions, error) {
    console.log('ERROR: ' + error);
  },

  /**
   * Success ajax response for Recommission/Decommission slaves
   * @param {object} data
   * @method decommissionSuccessCallback
   * @return {Boolean}
   */
  decommissionSuccessCallback: function (data) {
    if (data && (data.Requests || data.resources[0].RequestSchedule)) {
      this.showBackgroundOperationsPopup();
      return true;
    } else {
      console.log('cannot get request id from ', data);
      return false;
    }
  },

  /**
   * Performs Recommission and Start
   * @param {string} hostNames
   * @param {string} serviceName
   * @param {string} componentName
   * @param {string} slaveType
   * @method doRecommissionAndStart
   */
  doRecommissionAndStart: function (hostNames, serviceName, componentName, slaveType) {
    var contextNameString_1 = 'hosts.host.' + slaveType.toLowerCase() + '.recommission';
    var context_1 = Em.I18n.t(contextNameString_1);
    var contextNameString_2 = 'requestInfo.startHostComponent.' + slaveType.toLowerCase();
    var startContext = Em.I18n.t(contextNameString_2);
    var params = {
      "slave_type": slaveType,
      "included_hosts": hostNames
    };
    if (serviceName == "HBASE") {
      params.mark_draining_only = true;
    }
    var batches = [
      {
        "order_id": 1,
        "type": "POST",
        "uri": App.apiPrefix + "/clusters/" + App.get('clusterName') + "/requests",
        "RequestBodyInfo": {
          "RequestInfo": {
            "context": context_1,
            "command": "DECOMMISSION",
            "exclusive":"true",
            "parameters": params,
            'operation_level': {
              level: "HOST_COMPONENT",
              cluster_name: App.get('clusterName'),
              host_name: hostNames,
              service_name: serviceName
            }
          },
          "Requests/resource_filters": [
            {"service_name": serviceName, "component_name": componentName}
          ]
        }
      }];
    var id = 2;
    var hAray = hostNames.split(",");
    for (var i = 0; i < hAray.length; i++) {
      batches.push(    {
        "order_id": id,
        "type": "PUT",
        "uri": App.get('apiPrefix') + "/clusters/" + App.get('clusterName') + "/hosts/" + hAray[i] + "/host_components/" + slaveType,
        "RequestBodyInfo": {
          "RequestInfo": {
            context: startContext,
            operation_level: {
              level: "HOST_COMPONENT",
              cluster_name: App.get('clusterName'),
              host_name: hostNames,
              service_name: serviceName || null
            }
          },
          "Body": {
            HostRoles: {
              state: "STARTED"
            }
          }
        }
      });
      id++;
    }
    App.ajax.send({
      name: 'host.host_component.recommission_and_restart',
      sender: this,
      data: {
        intervalTimeSeconds: 1,
        tolerateSize: 1,
        batches: batches
      },
      success: 'decommissionSuccessCallback',
      error: 'decommissionErrorCallback'
    });
  },

  /**
   * Performs Recommission and Restart
   * @param {string} hostNames
   * @param {string} serviceName
   * @param {string} componentName
   * @param {string} slaveType
   * @method doRecommissionAndStart
   */
  doRecommissionAndRestart: function (hostNames, serviceName, componentName, slaveType) {
    var contextNameString_1 = 'hosts.host.' + slaveType.toLowerCase() + '.recommission';
    var context_1 = Em.I18n.t(contextNameString_1);
    var contextNameString_2 = 'hosts.host.' + slaveType.toLowerCase() + '.restart';
    var context_2 = Em.I18n.t(contextNameString_2);
    App.ajax.send({
      name: 'host.host_component.recommission_and_restart',
      sender: this,
      data: {
        intervalTimeSeconds: 1,
        tolerateSize: 1,
        batches: [
          {
            "order_id": 1,
            "type": "POST",
            "uri": App.apiPrefix + "/clusters/" + App.get('clusterName') + "/requests",
            "RequestBodyInfo": {
              "RequestInfo": {
                "context": context_1,
                "command": "DECOMMISSION",
                "exclusive":"true",
                "parameters": {
                  "slave_type": slaveType,
                  "included_hosts": hostNames
                },
                'operation_level': {
                  level: "HOST_COMPONENT",
                  cluster_name: App.get('clusterName'),
                  host_name: hostNames,
                  service_name: serviceName
                }
              },
              "Requests/resource_filters": [
                {"service_name": serviceName, "component_name": componentName}
              ]
            }
          },
          {
            "order_id": 2,
            "type": "POST",
            "uri": App.apiPrefix + "/clusters/" + App.get('clusterName') + "/requests",
            "RequestBodyInfo": {
              "RequestInfo": {
                "context": context_2,
                "command": "RESTART",
                "service_name": serviceName,
                "component_name": slaveType,
                "exclusive":"true",
                "hosts": hostNames
              }
            }
          }
        ]
      },
      success: 'decommissionSuccessCallback',
      error: 'decommissionErrorCallback'
    });
  },

  /**
   * Handler for host-menu items actions
   * @param {object} option
   * @method doAction
   */
  doAction: function (option) {
    switch (option.context.action) {
      case "deleteHost":
        this.validateAndDeleteHost();
        break;
      case "startAllComponents":
        if (!this.get('content.isNotHeartBeating')) this.doStartAllComponents();
        break;
      case "stopAllComponents":
        if (!this.get('content.isNotHeartBeating')) this.doStopAllComponents();
        break;
      case "restartAllComponents":
        if (!this.get('content.isNotHeartBeating')) this.doRestartAllComponents();
        break;
      case "onOffPassiveModeForHost":
        this.onOffPassiveModeForHost(option.context);
        break;
    }
  },

  /**
   * Turn On/Off Passive Mode for host
   * @param {object} context
   * @method onOffPassiveModeForHost
   */
  onOffPassiveModeForHost: function (context) {
    var state = context.active ? 'ON' : 'OFF';
    var self = this;
    var message = Em.I18n.t('hosts.host.details.for.postfix').format(context.label);
    return App.showConfirmationPopup(function () {
        self.hostPassiveModeRequest(state, message);
      },
      Em.I18n.t('hosts.passiveMode.popup').format(context.active ? 'On' : 'Off', this.get('content.hostName'))
    );
  },

  /**
   * Send request to get passive state for host
   * @param {string} state
   * @param {string} message
   * @method hostPassiveModeRequest
   */
  hostPassiveModeRequest: function (state, message) {
    App.ajax.send({
      name: 'bulk_request.hosts.passive_state',
      sender: this,
      data: {
        hostNames: this.get('content.hostName'),
        passive_state: state,
        requestInfo: message
      },
      success: 'updateHost'
    });
  },

  /**
   * Success callback for receiving host passive state
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method updateHost
   */
  updateHost: function (data, opt, params) {
    this.set('content.passiveState', params.passive_state);
    batchUtils.infoPassiveState(params.passive_state);
  },

  /**
   * Send request to get passive state for hostComponent
   * @param {object} component - hostComponentn object
   * @param {string} state
   * @param {string} message
   * @method hostPassiveModeRequest
   */
  updateComponentPassiveState: function (component, state, message) {
    App.ajax.send({
      name: 'common.host.host_component.passive',
      sender: this,
      data: {
        hostName: this.get('content.hostName'),
        componentName: component.get('componentName'),
        component: component,
        passive_state: state,
        context: message
      },
      success: 'updateHostComponent'
    });
  },

  /**
   * Success callback for receiving hostComponent passive state
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method updateHost
   */
  updateHostComponent: function (data, opt, params) {
    params.component.set('passiveState', params.passive_state)
    batchUtils.infoPassiveState(params.passive_state);
  },

  /**
   * Show confirmation popup for action "start all components"
   * @method doStartAllComponents
   */
  doStartAllComponents: function () {
    var self = this;
    var components = this.get('serviceNonClientActiveComponents');
    var componentsLength = Em.isNone(components) ? 0 : components.get('length');
    if (componentsLength > 0) {
      return App.showConfirmationPopup(function () {
        self.sendComponentCommand(components, Em.I18n.t('hosts.host.maintainance.startAllComponents.context'), App.HostComponentStatus.started);
      });
    }
  },

  /**
   * Show confirmation popup for action "stop all components"
   * @method doStopAllComponents
   */
  doStopAllComponents: function () {
    var self = this;
    var components = this.get('serviceNonClientActiveComponents');
    var componentsLength = Em.isNone(components) ? 0 : components.get('length');
    if (componentsLength > 0) {
      return App.showConfirmationPopup(function () {
        self.sendComponentCommand(components, Em.I18n.t('hosts.host.maintainance.stopAllComponents.context'), App.HostComponentStatus.stopped);
      });
    }
  },

  /**
   * Show confirmation popup for action "restart all components"
   * @method doRestartAllComponents
   */
  doRestartAllComponents: function () {
    var self = this;
    var components = this.get('serviceActiveComponents');
    var componentsLength = Em.isNone(components) ? 0 : components.get('length');
    if (componentsLength > 0) {
      return App.showConfirmationPopup(function () {
        batchUtils.restartHostComponents(components, Em.I18n.t('rollingrestart.context.allOnSelectedHost').format(self.get('content.hostName')), "HOST");
      });
    }
  },

  /**
   * get info about host-components, exactly:
   *  - host-components grouped by status, features
   *  - flag, that indicate whether ZooKeeper Server is installed
   * @return {Object}
   */
  getHostComponentsInfo: function () {
    var componentsOnHost = this.get('content.hostComponents');
    var allComponents = App.HostComponent.find();
    var stoppedStates = [App.HostComponentStatus.stopped,
      App.HostComponentStatus.install_failed,
      App.HostComponentStatus.upgrade_failed,
      App.HostComponentStatus.init,
      App.HostComponentStatus.unknown];
    var container = {
      zkServerInstalled: false,
      lastComponents: [],
      masterComponents: [],
      runningComponents: [],
      nonDeletableComponents: [],
      unknownComponents: []
    };

    if (componentsOnHost && componentsOnHost.get('length') > 0) {
      componentsOnHost.forEach(function (cInstance) {
        if (cInstance.get('componentName') === 'ZOOKEEPER_SERVER') {
          container.zkServerInstalled = true;
        }
        if (allComponents.filterProperty('componentName', cInstance.get('componentName')).get('length') === 1) {
          container.lastComponents.push(cInstance.get('displayName'));
        }
        var workStatus = cInstance.get('workStatus');
        if (cInstance.get('isMaster') && !cInstance.get('isDeletable')) {
          container.masterComponents.push(cInstance.get('displayName'));
        }
        if (stoppedStates.indexOf(workStatus) < 0) {
          container.runningComponents.push(cInstance.get('displayName'));
        }
        if (!cInstance.get('isDeletable')) {
          container.nonDeletableComponents.push(cInstance.get('displayName'));
        }
        if (workStatus === App.HostComponentStatus.unknown) {
          container.unknownComponents.push(cInstance.get('displayName'));
        }
      });
    }
    return container;
  },

  /**
   * Deletion of hosts not supported for this version
   * @method validateAndDeleteHost
   */
  validateAndDeleteHost: function () {
    if (!App.supports.deleteHost) {
      return false;
    }
    var container = this.getHostComponentsInfo();

    if (container.masterComponents.length > 0) {
      this.raiseDeleteComponentsError(container.masterComponents, 'masterList');
      return;
    } else if (container.nonDeletableComponents.length > 0) {
      this.raiseDeleteComponentsError(container.nonDeletableComponents, 'nonDeletableList');
      return;
    } else if (container.runningComponents.length > 0) {
      this.raiseDeleteComponentsError(container.runningComponents, 'runningList');
      return;
    }
    if (container.zkServerInstalled) {
      var self = this;
      return App.showConfirmationPopup(function () {
        self.confirmDeleteHost(container.unknownComponents, container.lastComponents);
      }, Em.I18n.t('hosts.host.addComponent.deleteHostWithZooKeeper'));
    } else {
      this.confirmDeleteHost(container.unknownComponents, container.lastComponents);
    }
  },

  /**
   * Show popup with info about reasons why host can't be deleted
   * @param {string[]} components
   * @param {string} type
   * @method raiseDeleteComponentsError
   */
  raiseDeleteComponentsError: function (components, type) {
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.cant.do.popup.title'),
      type: type,
      showBodyEnd: function () {
        return this.get('type') === 'runningList' || this.get('type') === 'masterList';
      }.property(),
      components: components,
      componentsStr: function () {
        return this.get('components').join(", ");
      }.property(),
      componentsBody: function () {
        return Em.I18n.t('hosts.cant.do.popup.' + type + '.body').format(this.get('components').length);
      }.property(),
      componentsBodyEnd: function () {
        if (this.get('showBodyEnd')) {
          return Em.I18n.t('hosts.cant.do.popup.' + type + '.body.end');
        }
        return '';
      }.property(),
      bodyClass: Em.View.extend({
        templateName: require('templates/main/host/details/raiseDeleteComponentErrorPopup')
      }),
      secondary: null
    })
  },

  /**
   * Show confirmation popup to delete host
   * @param {string[]} unknownComponents
   * @param {string[]} lastComponents
   * @method confirmDeleteHost
   */
  confirmDeleteHost: function (unknownComponents, lastComponents) {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('hosts.delete.popup.title'),
      deletePopupBody: function () {
        return Em.I18n.t('hosts.delete.popup.body').format(self.get('content.publicHostName'));
      }.property(),
      lastComponent: function () {
        if (lastComponents && lastComponents.length) {
          this.set('isChecked', false);
          return true;
        } else {
          this.set('isChecked', true);
          return false;
        }
      }.property(),
      disablePrimary: function () {
        return !this.get('isChecked');
      }.property('isChecked'),
      isChecked: false,
      lastComponentError: Em.View.extend({
        template: Em.Handlebars.compile(Em.I18n.t('hosts.delete.popup.body.msg4').format(lastComponents))
      }),
      unknownComponents: function () {
        if (unknownComponents && unknownComponents.length) {
          return unknownComponents.join(", ");
        }
        return '';
      }.property(),
      bodyClass: Em.View.extend({
        templateName: require('templates/main/host/details/doDeleteHostPopup')
      }),
      onPrimary: function () {
        var popup = this;
        var completeCallback = function () {
          popup.hide();
        };
        self.doDeleteHost(completeCallback);
      }
    })
  },

  /**
   * send DELETE calls to components of host and after delete host itself
   * @param completeCallback
   * @method doDeleteHost
   */
  doDeleteHost: function (completeCallback) {
    this.set('fromDeleteHost', true);
    var allComponents = this.get('content.hostComponents');
    var deleteError = null;
    var dfd = $.Deferred();
    var self = this;

    if (allComponents.get('length') > 0) {
      allComponents.forEach(function (component, index) {
        var length = allComponents.get('length');
        if (!deleteError) {
          this._doDeleteHostComponent(component, function () {
            deleteError = self.get('_deletedHostComponentResult');
            if (index == length - 1) {
              dfd.resolve();
            }
          });
        }
      }, this);
    } else {
      dfd.resolve();
    }
    dfd.done(function () {
      if (!deleteError) {
        App.ajax.send({
          name: 'common.delete.host',
          sender: self,
          data: {
            hostName: self.get('content.hostName')
          },
          callback: completeCallback,
          success: 'deleteHostSuccessCallback',
          error: 'deleteHostErrorCallback'
        })
      }
      else {
        completeCallback();
        deleteError.xhr.responseText = "{\"message\": \"" + deleteError.xhr.statusText + "\"}";
        App.ajax.defaultErrorHandler(deleteError.xhr, deleteError.url, deleteError.method, deleteError.xhr.status);
      }
    });
  },
  deleteHostSuccessCallback: function (data) {
    var self = this
    App.router.get('updateController').updateHost(function () {
      self.loadConfigs();
      App.router.transitionTo('hosts.index');
    });
    App.router.get('clusterController').getAllHostNames();
  },
  deleteHostErrorCallback: function (xhr, textStatus, errorThrown, opt) {
    console.log('Error deleting host.');
    console.log(textStatus);
    console.log(errorThrown);
    xhr.responseText = "{\"message\": \"" + xhr.statusText + "\"}";
    this.loadConfigs();
    App.ajax.defaultErrorHandler(xhr, opt.url, 'DELETE', xhr.status);
  },

  /**
   * Send command to server to restart all host components with stale configs
   * @method restartAllStaleConfigComponents
   */
  restartAllStaleConfigComponents: function () {
    var self = this;
    return App.showConfirmationPopup(function () {
      var staleComponents = self.get('content.componentsWithStaleConfigs');
      batchUtils.restartHostComponents(staleComponents, Em.I18n.t('rollingrestart.context.allWithStaleConfigsOnSelectedHost').format(self.get('content.hostName')), "HOST");
    });
  },

  /**
   * open Reassign Master Wizard with selected component
   * @param {object} event
   * @method moveComponent
   */
  moveComponent: function (event) {
    return App.showConfirmationPopup(function () {
      var component = event.context;
      var reassignMasterController = App.router.get('reassignMasterController');
      reassignMasterController.saveComponentToReassign(component);
      reassignMasterController.getSecurityStatus();
      reassignMasterController.setCurrentStep('1');
      App.router.transitionTo('reassign');
    });
  },

  /**
   * Restart clients host components to apply config changes
   * @param {object} event
   * @method refreshConfigs
   */
  refreshConfigs: function (event) {
    var self = this;
    var components = event.context.filter(function (component) {
      return component.get('staleConfigs');
    });
    if (components.get('length') > 0) {
      return App.showConfirmationPopup(function () {
        batchUtils.restartHostComponents(components, Em.I18n.t('rollingrestart.context.allClientsOnSelectedHost').format(self.get('content.hostName')), "HOST");
      });
    }
  },

  toggleMaintenanceMode: function (event) {
    var self = this;
    var state = event.context.get('passiveState') === "ON" ? "OFF" : "ON";
    var message = Em.I18n.t('passiveState.turn' + state.toCapital() + 'For').format(event.context.get('displayName'));
    return App.showConfirmationPopup(function () {
      self.updateComponentPassiveState(event.context, state, message);
    });
  },

  downloadClientConfigs: function (event) {
    componentsUtils.downloadClientConfigs.call(this, {
      hostName: event.context.get('hostName'),
      componentName: event.context.get('componentName'),
      displayName: event.context.get('displayName')
    });
  },

  reinstallClients: function(event) {
    var clientsToInstall = event.context.filter(function(component) {
      return ['INIT', 'INSTALL_FAILED'].contains(component.get('workStatus'));
    });
    if (!clientsToInstall.length) return;
    this.sendComponentCommand(clientsToInstall, Em.I18n.t('host.host.details.installClients'), 'INSTALLED');
  },

  /**
   * On click handler for custom command from items menu
   * @param context
   */
  executeCustomCommand: function(event) {
    var controller = this;
    var context = event.context;
    return App.showConfirmationPopup(function() {
      App.ajax.send({
        name : 'service.item.executeCustomCommand',
        sender: controller,
        data : {
          command : context.command,
          context : Em.I18n.t('services.service.actions.run.executeCustomCommand.context').format(context.command),
          hosts : context.hosts,
          serviceName : context.service,
          componentName : context.component
        },
        success : 'executeCustomCommandSuccessCallback',
        error : 'executeCustomCommandErrorCallback'
      });
    });
  },

  executeCustomCommandSuccessCallback  : function(data, ajaxOptions, params) {
    if (data.Requests.id) {
      App.router.get('backgroundOperationsController').showPopup();
    } else {
      console.warn('Error during execution of ' + params.command + ' custom command on' + params.componentName);
    }
  },
  executeCustomCommandErrorCallback : function(data) {
    var error = Em.I18n.t('services.service.actions.run.executeCustomCommand.error');
    if(data && data.responseText){
      try {
        var json = $.parseJSON(data.responseText);
        error += json.message;
      } catch (err) {}
    }
    App.showAlertPopup(Em.I18n.t('services.service.actions.run.executeCustomCommand.error'), error);
    console.warn('Error during executing custom command');
  }
});
