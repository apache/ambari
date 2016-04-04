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
var hostsManagement = require('utils/hosts');
var stringUtils = require('utils/string_utils');

App.MainHostDetailsController = Em.Controller.extend(App.SupportClientConfigsDownload, App.InstallComponent, App.InstallNewVersion, {

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
   * Are we adding hive server2 component
   * @type {bool}
   */
  addHiveServer: false,

  /**
   * path to page visited before
   * @type {string}
   */
  referer: '',

  /**
   *  Host on which Hive Metastore will be added
   * @type {string}
   */
  hiveMetastoreHost: '',

  /**
   * Deferred object will be resolved when Oozie configs are downloaded
   * @type {object}
   */
  isOozieConfigLoaded: $.Deferred(),

  /**
   * @type {bool}
   */
  isOozieServerAddable: true,

  /**
   * Open dashboard page
   * @method routeHome
   */
  routeHome: function () {
    App.router.transitionTo('main.dashboard.index');
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
    if (event.context.get('componentName') == 'NAMENODE' ) {
      this.checkNnLastCheckpointTime(function () {
        return App.showConfirmationPopup(function () {
          var component = event.context;
          var context = Em.I18n.t('requestInfo.stopHostComponent') + " " + component.get('displayName');
          self.sendComponentCommand(component, context, App.HostComponentStatus.stopped);
        });
      });
    } else {
      return App.showConfirmationPopup(function () {
        var component = event.context;
        var context = Em.I18n.t('requestInfo.stopHostComponent') + " " + component.get('displayName');
        self.sendComponentCommand(component, context, App.HostComponentStatus.stopped);
      });
    }
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
   * Return true if hdfs user data is loaded via App.MainServiceInfoConfigsController
   */
  getHdfsUser: function () {
    var self = this;
    var dfd = $.Deferred();
    var miscController = App.MainAdminServiceAccountsController.create();
    miscController.loadUsers();
    var interval = setInterval(function () {
      if (miscController.get('dataIsLoaded') && miscController.get('users')) {
        self.set('hdfsUser', miscController.get('users').findProperty('name', 'hdfs_user').get('value'));
        dfd.resolve();
        clearInterval(interval);
      }
    }, 10);
    return dfd.promise();
  },

  /**
   * this function will be called from :1) stop NN 2) restart NN 3) stop all components
   * @param callback - callback function to continue next operation
   * @param hostname - namenode host (by default is current host)
   */
  checkNnLastCheckpointTime: function(callback, hostName) {
    var self = this;
    this.pullNnCheckPointTime(hostName).complete(function () {
      var isNNCheckpointTooOld = self.get('isNNCheckpointTooOld');
      self.set('isNNCheckpointTooOld', null);
      if (isNNCheckpointTooOld) {
        // too old
        self.getHdfsUser().done(function() {
          var msg = Em.Object.create({
            confirmMsg: Em.I18n.t('services.service.stop.HDFS.warningMsg.checkPointTooOld').format(App.nnCheckpointAgeAlertThreshold) +
              Em.I18n.t('services.service.stop.HDFS.warningMsg.checkPointTooOld.instructions').format(isNNCheckpointTooOld, self.get('hdfsUser')),
            confirmButton: Em.I18n.t('common.next')
          });
          return App.showConfirmationFeedBackPopup(callback, msg);
        });
      } else if (isNNCheckpointTooOld == null) {
        // not available
        return App.showConfirmationPopup(
          callback, Em.I18n.t('services.service.stop.HDFS.warningMsg.checkPointNA'), null,
          Em.I18n.t('common.warning'), Em.I18n.t('common.proceedAnyway'), true
        );
      } else {
        // still young
        callback();
      }
    });
  },

  pullNnCheckPointTime: function (hostName) {
    return App.ajax.send({
      name: 'common.host_component.getNnCheckPointTime',
      sender: this,
      data: {
        host: hostName || this.get('content.hostName')
      },
      success: 'parseNnCheckPointTime'
    });
  },

  parseNnCheckPointTime: function (data) {
    var lastCheckpointTime = Em.get(data, 'metrics.dfs.FSNamesystem.LastCheckpointTime');
    var hostName = Em.get(data, 'HostRoles.host_name');

    if (Em.get(data, 'metrics.dfs.FSNamesystem.HAState') == 'active') {
      if (!lastCheckpointTime) {
        this.set("isNNCheckpointTooOld", null);
      } else {
        var time_criteria = App.nnCheckpointAgeAlertThreshold; // time in hours to define how many hours ago is too old
        var time_ago = (Math.round(App.dateTime() / 1000) - (time_criteria * 3600)) *1000;
        if (lastCheckpointTime <= time_ago) {
          // too old, set the effected hostName
          this.set("isNNCheckpointTooOld", hostName);
        } else {
          // still young
          this.set("isNNCheckpointTooOld", false);
        }
      }
    } else if (Em.get(data, 'metrics.dfs.FSNamesystem.HAState') == 'standby') {
      this.set("isNNCheckpointTooOld", false);
    }
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
    App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
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
    if ($(event.target).closest('li').hasClass('disabled')) {
      return;
    }
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
      isHiveMetastore: componentName == 'HIVE_METASTORE',
      isWebHCatServer: componentName == 'WEBHCAT_SERVER',
      isNimbus: componentName == 'NIMBUS',
      isRangerKMSServer: componentName == 'RANGER_KMS_SERVER',
      isZkServer: componentName == 'ZOOKEEPER_SERVER',

      deleteHiveMetastoreMsg: Em.I18n.t('hosts.host.deleteComponent.popup.deleteHiveMetastore'),
      deleteWebHCatServerMsg: Em.I18n.t('hosts.host.deleteComponent.popup.deleteWebHCatServer'),
      deleteNimbusMsg: Em.I18n.t('hosts.host.deleteComponent.popup.deleteNimbus'),
      deleteRangerKMSServereMsg: Em.I18n.t('hosts.host.deleteComponent.popup.deleteRangerKMSServer'),
      lastComponentError: Em.I18n.t('hosts.host.deleteComponent.popup.warning').format(displayName),
      deleteComponentMsg: Em.I18n.t('hosts.host.deleteComponent.popup.msg1').format(displayName),
      deleteZkServerMsg: Em.I18n.t('hosts.host.deleteComponent.popup.deleteZooKeeperServer'),

      isChecked: false,

      disablePrimary: Em.computed.not('isChecked'),

      lastComponent: function () {
        this.set('isChecked', !isLastComponent);
        return isLastComponent;
      }.property(),

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
      count = App.HostComponent.find().filterProperty('componentName', component.get('componentName')).get('length');
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
    this.removeHostComponentModel(data.componentName, data.hostName);
    if (data.componentName == 'ZOOKEEPER_SERVER') {
      this.set('fromDeleteZkServer', true);
      this.updateStormConfigs();
      var self = this;
      var callback =   function () {
        self.loadConfigs();
      };
      self.isServiceMetricsLoaded(callback);
    } else if (data.componentName == 'HIVE_METASTORE') {
      this.set('deleteHiveMetaStore', true);
      this.loadConfigs('loadHiveConfigs');
    } else if (data.componentName == 'WEBHCAT_SERVER') {
      this.set('deleteWebHCatServer', true);
      this.loadConfigs('loadWebHCatConfigs');
    } else if (data.componentName == 'HIVE_SERVER') {
      this.set('deleteHiveServer', true);
      this.loadConfigs('loadHiveConfigs');
    } else if (data.componentName == 'NIMBUS') {
      this.set('deleteNimbusHost', true);
      this.loadConfigs('loadStormConfigs');
    } else if (data.componentName == 'RANGER_KMS_SERVER') {
      this.set('deleteRangerKMSServer', true);
      this.loadConfigs('loadRangerConfigs');
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
   * Remove host component data from App.HostComponent model.
   *
   * @param {String} componentName
   * @param {String} hostName
   */
  removeHostComponentModel: function (componentName, hostName) {
    var component = App.HostComponent.find().filterProperty('componentName', componentName).findProperty('hostName', hostName);
    var serviceInCache = App.cache['services'].findProperty('ServiceInfo.service_name', component.get('service.serviceName'));
    serviceInCache.host_components = serviceInCache.host_components.without(component.get('id'));
    App.serviceMapper.deleteRecord(component);
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
    if (event.context.get('componentName') == 'NAMENODE') {
      this.checkNnLastCheckpointTime(function () {
        return App.showConfirmationPopup(function () {
          batchUtils.restartHostComponents([component], Em.I18n.t('rollingrestart.context.selectedComponentOnSelectedHost').format(component.get('displayName')), "HOST_COMPONENT");
        });
      });
    } else {
      return App.showConfirmationPopup(function () {
        batchUtils.restartHostComponents([component], Em.I18n.t('rollingrestart.context.selectedComponentOnSelectedHost').format(component.get('displayName')), "HOST_COMPONENT");
      });
    }
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
   * add component as <code>addComponent<code> method but perform
   * kdc sessionstate if cluster is secure;
   * @param event
   */
  addComponentWithCheck: function (event) {
    var componentName = event.context ? event.context.get('componentName') : "";
    event.hiveMetastoreHost = (componentName == "HIVE_METASTORE" && !!this.get('content.hostName')) ? this.get('content.hostName') : null;
    App.get('router.mainAdminKerberosController').getSecurityType(function (event) {
      App.get('router.mainAdminKerberosController').getKDCSessionState(this.addComponent.bind(this, event));
    }.bind(this, event));
  },
  /**
   * Send command to server to install selected host component
   * @param {object} event
   * @method addComponent
   */
  addComponent: function (event) {
    var
      returnFunc,
      self = this,
      component = event.context,
      hostName = event.selectedHost || this.get('content.hostName'),
      componentName = component.get('componentName'),
      missedComponents = event.selectedHost ? [] : this.checkComponentDependencies(componentName, {
        scope: 'host',
        installedComponents: this.get('content.hostComponents').mapProperty('componentName')
      }),
      isManualKerberos = App.get('router.mainAdminKerberosController.isManualKerberos'),
      manualKerberosWarning = isManualKerberos ? Em.I18n.t('hosts.host.manualKerberosWarning') : '';

    if (!!missedComponents.length) {
      var popupMessage = Em.I18n.t('host.host.addComponent.popup.dependedComponents.body').format(component.get('displayName'),
        stringUtils.getFormattedStringFromArray(missedComponents.map(function (cName) {
          return App.StackServiceComponent.find(cName).get('displayName');
        })));
      return App.showAlertPopup(Em.I18n.t('host.host.addComponent.popup.dependedComponents.header'), popupMessage);
    }

    switch (componentName) {
      case 'ZOOKEEPER_SERVER':
        returnFunc = App.showConfirmationPopup(function () {
          self.installHostComponentCall(self.get('content.hostName'), component)
        }, Em.I18n.t('hosts.host.addComponent.' + componentName) + manualKerberosWarning);
        break;
      case 'HIVE_METASTORE':
        returnFunc = App.showConfirmationPopup(function () {
          self.set('hiveMetastoreHost', hostName);
          self.loadConfigs("loadHiveConfigs");
        }, Em.I18n.t('hosts.host.addComponent.' + componentName) + manualKerberosWarning);
        break;
      case 'WEBHCAT_SERVER':
        returnFunc = App.showConfirmationPopup(function () {
          self.set('webhcatServerHost', hostName);
          self.loadConfigs("loadWebHCatConfigs");
        }, Em.I18n.t('hosts.host.addComponent.' + componentName) + manualKerberosWarning);
        break;
      case 'NIMBUS':
        returnFunc = App.showConfirmationPopup(function () {
          self.set('nimbusHost', hostName);
          self.loadConfigs("loadStormConfigs");
        }, Em.I18n.t('hosts.host.addComponent.' + componentName) + manualKerberosWarning);
        break;
      case 'RANGER_KMS_SERVER':
        returnFunc = App.showConfirmationPopup(function () {
          self.set('rangerKMSServerHost', hostName);
          self.loadConfigs("loadRangerConfigs");
        }, Em.I18n.t('hosts.host.addComponent.' + componentName) + manualKerberosWarning);
        break;
      default:
        returnFunc = this.addClientComponent(component, isManualKerberos);
    }
    return returnFunc;
  },
  /**
   * Send command to server to install client on selected host
   * @param component
   */
  addClientComponent: function (component, isManualKerberos) {
    var self = this;
    var message = this.formatClientsMessage(component);

    return this.showAddComponentPopup(message, isManualKerberos, function () {
      self.installHostComponentCall(self.get('content.hostName'), component);
    });
  },

  showAddComponentPopup: function (message, isManualKerberos, primary) {
    isManualKerberos = isManualKerberos || false;

    return App.ModalPopup.show({
      primary: Em.I18n.t('hosts.host.addComponent.popup.confirm'),
      header: Em.I18n.t('popup.confirmation.commonHeader'),

      addComponentMsg: function () {
        return Em.I18n.t('hosts.host.addComponent.msg').format(message);
      }.property(),

      manualKerberosWarning: function () {
        return isManualKerberos ? Em.I18n.t('hosts.host.manualKerberosWarning') : '';
      }.property(),

      bodyClass: Em.View.extend({
        templateName: require('templates/main/host/details/addComponentPopup')
      }),

      onPrimary: function () {
        this.hide();
        primary();
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
        dns.push(App.format.role(scn, false));
      });
      displayName += " (" + dns.join(", ") + ")";
    }
    return displayName;
  },

  /**
   * Success callback for install host component request (sent in <code>addNewComponentSuccessCallback</code>)
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method installNewComponentSuccessCallback
   */
  installNewComponentSuccessCallback: function (data, opt, params) {
    if (!data || !data.Requests || !data.Requests.id) {
      return false;
    }
    var self = this;
    console.log('Send request for INSTALLING NEW COMPONENT successfully');

    if (App.get('testMode')) {
      this.mimicWorkStatusChange(params.component, App.HostComponentStatus.installing, App.HostComponentStatus.stopped);
    }

    this.showBackgroundOperationsPopup(function () {
      if (params.componentName === 'ZOOKEEPER_SERVER' || params.componentName === 'HIVE_SERVER') {
        self.set(params.componentName === 'ZOOKEEPER_SERVER' ? 'zkRequestId' : 'hiveRequestId', data.Requests.id);
        self.addObserver(
          'App.router.backgroundOperationsController.serviceTimestamp',
          self,
          (params.componentName === 'ZOOKEEPER_SERVER' ? self.checkZkConfigs : self.checkHiveDone)
        );
        params.componentName === 'ZOOKEEPER_SERVER' ? self.checkZkConfigs() : self.checkHiveDone();
      }
    });
    return true;
  },

  /**
   * Call <code>setRackInfo</code> function to show Set Rack Id popup
   * @param data
   */
  setRackId: function (data) {
    var rack = data.context.get('rack');
    var hosts = [data.context];
    var operationData = {message: Em.I18n.t('hosts.host.details.setRackId')};
    hostsManagement.setRackInfo(operationData, hosts, rack);
  },

  /**
   * Call load tags
   * @method checkHiveDone
   */
  checkHiveDone: function () {
    var bg = App.router.get('backgroundOperationsController.services').findProperty('id', this.get('hiveRequestId'));
    if (bg && !bg.get('isRunning')) {
      var self = this;
      this.removeObserver('App.router.backgroundOperationsController.serviceTimestamp', this, this.checkHiveDone);
      setTimeout(function () {
        self.set('addHiveServer', true);
        self.loadConfigs("loadHiveConfigs");
      }, App.get('componentsUpdateInterval'));
    }
  },

  /**
   * Success callback for load configs request
   * @param {object} data
   * @method loadOozieConfigs
   */
  loadOozieConfigs: function (data) {
    return App.ajax.send({
      name: 'admin.get.all_configurations',
      sender: this,
      data: {
        urlParams: '(type=oozie-env&tag=' + data.Clusters.desired_configs['oozie-env'].tag + ')'
      },
      success: 'onLoadOozieConfigs',
      error: 'onLoadConfigsErrorCallback'
    });
  },

  /**
   * get Oozie database config and set databaseType
   * @param {object} data
   * @method onLoadHiveConfigs
   */
  onLoadOozieConfigs: function (data) {
    var configs = {};
    data.items.forEach(function (item) {
      $.extend(configs, item.properties);
    });
    this.set('isOozieServerAddable', !(Em.isEmpty(configs["oozie_database"]) || configs["oozie_database"] === 'New Derby Database'));
    this.get('isOozieConfigLoaded').resolve();
  },


  /**
   * Success callback for Storm load configs request
   * @param {object} data
   * @method loadStormConfigs
   */
  loadStormConfigs: function (data) {
    App.ajax.send({
      name: 'admin.get.all_configurations',
      sender: this,
      data: {
        urlParams: '(type=storm-site&tag=' + data.Clusters.desired_configs['storm-site'].tag + ')'
      },
      success: 'onLoadStormConfigs'
    });
  },

  /**
   * Update zk configs
   * @param {object} configs
   * @method updateZkConfigs
   */
  updateZkConfigs: function (configs) {
    var zks = this.getZkServerHosts();
    var portValue = configs['zoo.cfg'] && Em.get(configs['zoo.cfg'], 'clientPort');
    var zkPort = typeof portValue === 'udefined' ? '2181' : portValue;
    var zksWithPort = this.concatZkNames(zks, zkPort);
    this.setZKConfigs(configs, zksWithPort, zks);
  },

  /**
   * update and save Storm related configs to server
   * @param {object} data
   * @method onLoadStormConfigs
   */
  onLoadStormConfigs: function (data) {
    var nimbusHost = this.get('nimbusHost'),
      stormNimbusHosts = this.getStormNimbusHosts(),
      configs = {},
      attributes = {};

    data.items.forEach(function (item) {
      configs[item.type] = item.properties;
      attributes[item.type] = item.properties_attributes || {};
    }, this);

    this.updateZkConfigs(configs);

    configs['storm-site']['nimbus.seeds'] = JSON.stringify(stormNimbusHosts).replace(/"/g, "'");
    var groups = [
      {
        properties: {
          'storm-site': configs['storm-site']
        },
        properties_attributes: {
          'storm-site': attributes['storm-site']
        }
      }
    ];
    this.saveConfigsBatch(groups, 'NIMBUS', nimbusHost);
  },

  /**
   * Success callback for load configs request
   * @param {object} data
   * @method loadHiveConfigs
   */
  loadWebHCatConfigs: function (data) {
    return App.ajax.send({
      name: 'admin.get.all_configurations',
      sender: this,
      data: {
        webHCat: true,
        urlParams: [
          '(type=hive-site&tag=' + data.Clusters.desired_configs['hive-site'].tag + ')',
          '(type=webhcat-site&tag=' + data.Clusters.desired_configs['webhcat-site'].tag + ')',
          '(type=hive-env&tag=' + data.Clusters.desired_configs['hive-env'].tag + ')',
          '(type=core-site&tag=' + data.Clusters.desired_configs['core-site'].tag + ')'
        ].join('|')
      },
      success: 'onLoadHiveConfigs'
    });
  },

  /**
   * Success callback for load configs request
   * @param {object} data
   * @method loadHiveConfigs
   */
  loadHiveConfigs: function (data) {
    return App.ajax.send({
      name: 'admin.get.all_configurations',
      sender: this,
      data: {
        urlParams: [
          '(type=hive-site&tag=' + data.Clusters.desired_configs['hive-site'].tag + ')',
          '(type=webhcat-site&tag=' + data.Clusters.desired_configs['webhcat-site'].tag + ')',
          '(type=hive-env&tag=' + data.Clusters.desired_configs['hive-env'].tag + ')',
          '(type=core-site&tag=' + data.Clusters.desired_configs['core-site'].tag + ')'
        ].join('|')
      },
      success: 'onLoadHiveConfigs'
    });
  },

  /**
   * update and save Hive related configs to server
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method onLoadHiveConfigs
   */
  onLoadHiveConfigs: function (data, opt, params) {
    var
      hiveMetastoreHost = this.get('hiveMetastoreHost'),
      webhcatServerHost = this.get('webhcatServerHost'),
      hiveMSHosts = this.getHiveHosts(),
      hiveMasterHosts = hiveMSHosts.concat(App.HostComponent.find().filterProperty('componentName', 'HIVE_SERVER').mapProperty('hostName')).uniq().sort().join(','),
      configs = {},
      attributes = {},
      port = "",
      hiveUser = "",
      webhcatUser = "";

    data.items.forEach(function (item) {
      configs[item.type] = item.properties;
      attributes[item.type] = item.properties_attributes || {};
    }, this);

    port = configs['hive-site']['hive.metastore.uris'].match(/:[0-9]{2,4}/);
    port = port ? port[0].slice(1) : "9083";

    hiveUser = configs['hive-env']['hive_user'];
    webhcatUser = configs['hive-env']['webhcat_user'];

    for (var i = 0; i < hiveMSHosts.length; i++) {
      hiveMSHosts[i] = "thrift://" + hiveMSHosts[i] + ":" + port;
    }
    configs['hive-site']['hive.metastore.uris'] = hiveMSHosts.join(',');
    configs['webhcat-site']['templeton.hive.properties'] = configs['webhcat-site']['templeton.hive.properties'].replace(/thrift.+[0-9]{2,},/i, hiveMSHosts.join('\\,') + ",");
    if (params.webHCat) {
        configs['core-site']['hadoop.proxyuser.' + webhcatUser + '.hosts'] = hiveMasterHosts;
    } else {
        configs['core-site']['hadoop.proxyuser.' + hiveUser + '.hosts'] = hiveMasterHosts;
    }
    
    var groups = [
      {
        properties: {
          'hive-site': configs['hive-site'],
          'webhcat-site': configs['webhcat-site'],
          'hive-env': configs['hive-env']
        },
        properties_attributes: {
          'hive-site': attributes['hive-site'],
          'webhcat-site': attributes['webhcat-site'],
          'hive-env': attributes['hive-env']
        }
      },
      {
        properties: {
          'core-site': configs['core-site']
        },
        properties_attributes: {
          'core-site': attributes['core-site']
        }
      }
    ];
    var params = [groups];
    var componentName = this.get('addHiveServer') ? 'HIVE_SERVER' : (hiveMetastoreHost ? 'HIVE_METASTORE' : 'WEBHCAT_SERVER');
    var host = webhcatServerHost || hiveMetastoreHost;
    params.pushObjects([componentName, host]);
    this.saveConfigsBatch.apply(this, params);
    this.set('addHiveServer', false);
  },

  /**
   * save configs' sites in batch
   * @param groups
   * @param componentName
   * @param host
   */
  saveConfigsBatch: function (groups, componentName, host) {
    groups.forEach(function (group) {
      var desiredConfigs = [],
        tag = 'version' + (new Date).getTime(),
        properties = group.properties;

      for (var site in properties) {
        if (!properties.hasOwnProperty(site) || Em.isNone(properties[site])) continue;
        desiredConfigs.push({
          "type": site,
          "tag": tag,
          "properties": properties[site],
          "properties_attributes": group.properties_attributes[site],
          "service_config_version_note": Em.I18n.t('hosts.host.configs.save.note').format(App.format.role(componentName, false))
        });
      }
      if (desiredConfigs.length > 0) {
        App.ajax.send({
          name: 'common.service.configurations',
          sender: this,
          data: {
            desired_config: desiredConfigs,
            componentName: componentName,
            host: host
          },
          success: 'installHostComponent'
        });
      }
      //clear hive metastore host not to send second request to install component
      host = null;
    }, this);
  },

  /**
   * success callback for saveConfigsBatch method
   * @param data
   * @param opt
   * @param params
   */
  installHostComponent: function (data, opt, params) {
    if (App.router.get('location.location.hash').contains('configs')) {
      App.router.get('mainServiceInfoConfigsController').loadStep();
    }
    if (params.host) {
      this.installHostComponentCall(params.host, App.StackServiceComponent.find(params.componentName));
    }
  },
  /**
   * Delete Hive Metastore is performed
   * @type {bool}
   */
  deleteHiveMetaStore: false,

  /**
   * Delete WebHCat Server is performed
   *
   * @type {bool}
   */
  deleteWebHCatServer: false,

  getHiveHosts: function () {
    var
      hiveHosts = App.HostComponent.find().filterProperty('componentName', 'HIVE_METASTORE').mapProperty('hostName'),
      webhcatHosts = App.HostComponent.find().filterProperty('componentName', 'WEBHCAT_SERVER').mapProperty('hostName'),
      hiveMetastoreHost = this.get('hiveMetastoreHost'),
      webhcatServerHost = this.get('webhcatServerHost');

    hiveHosts = hiveHosts.concat(webhcatHosts).uniq();

    if (!!hiveMetastoreHost) {
      hiveHosts.push(hiveMetastoreHost);
      this.set('hiveMetastoreHost', '');
    }

    if (!!webhcatServerHost) {
      hiveHosts.push(webhcatServerHost);
      this.set('webhcatServerHost' ,'');
    }

    if (this.get('fromDeleteHost') || this.get('deleteHiveMetaStore') || this.get('deleteHiveServer') || this.get('deleteWebHCatServer')) {
      this.set('deleteHiveMetaStore', false);
      this.set('deleteHiveServer', false);
      this.set('deleteWebHCatServer', false);
      this.set('fromDeleteHost', false);
      hiveHosts = hiveHosts.without(this.get('content.hostName'));
    }
    return hiveHosts.sort();
  },

  /**
   * Success callback for load configs request
   * @param {object} data
   * @method loadHiveConfigs
   */
  loadRangerConfigs: function (data) {
    App.ajax.send({
      name: 'admin.get.all_configurations',
      sender: this,
      data: {
        urlParams: '(type=core-site&tag=' + data.Clusters.desired_configs['core-site'].tag + ')|(type=hdfs-site&tag=' + data.Clusters.desired_configs['hdfs-site'].tag + ')|(type=kms-env&tag=' + data.Clusters.desired_configs['kms-env'].tag + ')'
      },
      success: 'onLoadRangerConfigs'
    });
  },

  /**
   * update and save Hive hive.metastore.uris config to server
   * @param {object} data
   * @method onLoadHiveConfigs
   */
  onLoadRangerConfigs: function (data) {
    var hostToInstall = this.get('rangerKMSServerHost');
    var rkmsHosts = this.getRangerKMSServerHosts();
    var rkmsPort = data.items.findProperty('type', 'kms-env').properties['kms_port'];
    var coreSiteConfigs = data.items.findProperty('type', 'core-site');
    var hdfsSiteConfigs = data.items.findProperty('type', 'hdfs-site');
    var groups = [
      {
        properties: {
          'core-site': coreSiteConfigs.properties,
          'hdfs-site': hdfsSiteConfigs.properties
        },
        properties_attributes: {
          'core-site': coreSiteConfigs.properties_attributes,
          'hdfs-site': hdfsSiteConfigs.properties_attributes
        }
      }
    ];

    coreSiteConfigs.properties['hadoop.security.key.provider.path'] = 'kms://http@' + rkmsHosts.join(';') + ':' + rkmsPort + '/kms';
    hdfsSiteConfigs.properties['dfs.encryption.key.provider.uri'] = 'kms://http@' + rkmsHosts.join(';') + ':' + rkmsPort + '/kms';
    this.saveConfigsBatch(groups, 'RANGER_KMS_SERVER', hostToInstall);
  },

  /**
   * Delete Hive Metastore is performed
   * @type {bool}
   */
  deleteRangerKMSServer: false,

  getRangerKMSServerHosts: function () {
    var rkmsHosts = App.HostComponent.find().filterProperty('componentName', 'RANGER_KMS_SERVER').mapProperty('hostName');
    var rangerKMSServerHost = this.get('rangerKMSServerHost');

    if (!!rangerKMSServerHost) {
      rkmsHosts.push(rangerKMSServerHost);
      this.set('rangerKMSServerHost', '');
    }

    if (this.get('fromDeleteHost') || this.get('deleteRangerKMSServer')) {
      this.set('deleteRangerKMSServer', false);
      this.set('fromDeleteHost', false);
      return rkmsHosts.without(this.get('content.hostName'));
    }
    return rkmsHosts.sort();
  },

  /**
   * Delete Storm Nimbus is performed
   * @type {bool}
   */
  deleteNimbusHost: false,

  getStormNimbusHosts: function () {
    var
      stormNimbusHosts = App.HostComponent.find().filterProperty('componentName', 'NIMBUS').mapProperty('hostName'),
      nimbusHost = this.get('nimbusHost');

    if (!!nimbusHost) {
      stormNimbusHosts.push(nimbusHost);
      this.set('nimbusHost', '');
    }

    if (this.get('fromDeleteHost') || this.get('deleteNimbusHost')) {
      this.set('deleteNimbusHost', false);
      this.set('fromDeleteHost', false);
      return stormNimbusHosts.without(this.get('content.hostName'));
    }
    return stormNimbusHosts.sort();
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
   * Update storm config
   * @method updateStormConfigs
   */
  updateStormConfigs: function () {
    if (App.Service.find('STORM').get('isLoaded') && App.get('isHadoop23Stack')) {
      this.loadConfigs("loadStormConfigs");
    }
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
        self.updateStormConfigs();
        var callback =   function () {
          self.loadConfigs();
        };
        self.isServiceMetricsLoaded(callback);
      }, App.get('componentsUpdateInterval'));
    }
  },

  /**
   * This function should be always called from successcallback function of the promise `App.router.get('mainController').isLoading.call(App.router.get('clusterController'), 'isServiceContentFullyLoaded').done(promise)`
   * This is required to make sure that service metrics API determining the HA state of components is loaded
   * Load configs
   * @method loadConfigs
   */
  loadConfigs: function (callback) {
    App.ajax.send({
      name: 'config.tags',
      sender: this,
      success: callback ? callback : 'loadConfigsSuccessCallback',
      error: 'onLoadConfigsErrorCallback'
    });
  },

  /**
   * onLoadConfigsErrorCallback
   * @method onLoadConfigsErrorCallback
   */
  onLoadConfigsErrorCallback: function () {
    this.get('isOozieConfigLoaded').reject();
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
      urlParams.push('(type=zoo.cfg&tag=' + data.Clusters.desired_configs['zoo.cfg'].tag + ')');
    }
    if (services.someProperty('serviceName', 'ACCUMULO')) {
      urlParams.push('(type=accumulo-site&tag=' + data.Clusters.desired_configs['accumulo-site'].tag + ')');
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
    var attributes = {};
    data.items.forEach(function (item) {
      configs[item.type] = item.properties;
      attributes[item.type] = item.properties_attributes || {};
    }, this);

    this.updateZkConfigs(configs);

    var groups = [
      {
        properties: {
          'hive-site': configs['hive-site'],
          'webhcat-site': configs['webhcat-site']
        },
        properties_attributes: {
          'hive-site': attributes['hive-site'],
          'webhcat-site': attributes['webhcat-site']
        }
      }
    ];
    if ((App.Service.find().someProperty('serviceName', 'YARN') && App.get('isHadoop22Stack')) || App.get('isRMHaEnabled')) {
      groups.push(
        {
          properties: {
            'yarn-site': configs['yarn-site']
          },
          properties_attributes: {
            'yarn-site': attributes['yarn-site']
          }
        }
      );
    }
    if (App.Service.find().someProperty('serviceName', 'HBASE')) {
      groups.push(
        {
          properties: {
            'hbase-site': configs['hbase-site']
          },
          properties_attributes: {
            'hbase-site': attributes['hbase-site']
          }
        }
      );
    }
    if (App.Service.find().someProperty('serviceName', 'ACCUMULO')) {
      groups.push(
        {
          properties: {
            'accumulo-site': configs['accumulo-site']
          },
          properties_attributes: {
            'accumulo-site': attributes['accumulo-site']
          }
        }
      );
    }
    this.saveConfigsBatch(groups, 'ZOOKEEPER_SERVER');
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
    if (App.get('isHaEnabled') && configs['core-site']) {
      App.config.updateHostsListValue(configs['core-site'], 'ha.zookeeper.quorum', zksWithPort);
    }
    if (configs['hbase-site']) {
      App.config.updateHostsListValue(configs['hbase-site'], 'hbase.zookeeper.quorum', zks.join(','));
    }
    if (configs['accumulo-site']) {
      App.config.updateHostsListValue(configs['accumulo-site'], 'instance.zookeeper.host', zksWithPort);
    }
    if (configs['webhcat-site']) {
      App.config.updateHostsListValue(configs['webhcat-site'], 'templeton.zookeeper.hosts', zksWithPort);
    }
    if (configs['hive-site']) {
      App.config.updateHostsListValue(configs['hive-site'], 'hive.cluster.delegation.token.store.zookeeper.connectString', zksWithPort);
    }
    if (configs['storm-site']) {
      configs['storm-site']['storm.zookeeper.servers'] = JSON.stringify(zks).replace(/"/g, "'");
    }
    if (App.get('isRMHaEnabled') && configs['yarn-site']) {
      App.config.updateHostsListValue(configs['yarn-site'], 'yarn.resourcemanager.zk-address', zksWithPort);
    }
    if (App.get('isHadoop22Stack')) {
      if (configs['hive-site']) {
        App.config.updateHostsListValue(configs['hive-site'], 'hive.zookeeper.quorum', zksWithPort);
      }
      if (configs['yarn-site']) {
        App.config.updateHostsListValue(configs['yarn-site'], 'hadoop.registry.zk.quorum', zksWithPort);
        App.config.updateHostsListValue(configs['yarn-site'], 'yarn.resourcemanager.zk-address', zksWithPort);
      }
    }
    return true;
  },
  /**
   * concatenate URLs to ZOOKEEPER hosts with port "2181",
   * as value of config divided by comma
   * @param zks {array}
   * @param port {string}
   */
  concatZkNames: function (zks, port) {
    var zks_with_port = '';
    zks.forEach(function (zk) {
      zks_with_port += zk + ':' + port + ',';
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
      bodyClass: Em.View.extend({
        templateName: require('templates/main/host/details/installComponentPopup')
      }),
      onPrimary: function () {
        var _this = this;
        App.get('router.mainAdminKerberosController').getSecurityType(function () {
          App.get('router.mainAdminKerberosController').getKDCSessionState(function () {
            _this.hide();

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
          })
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
        return Em.I18n.t('hostPopup.recommendation.beforeDecommission').format(App.format.components["HBASE_REGIONSERVER"]);
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
            "exclusive": "true",
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
      id++;
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
            "exclusive": "true",
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
      batches.push({
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
      case "setRackId":
        this.setRackIdForHost();
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
    var popupInfo = Em.I18n.t('hosts.passiveMode.popup').format(context.active ? 'On' : 'Off', this.get('content.hostName'));
    if (state === 'OFF') {
      var hostVersion = this.get('content.stackVersions') && this.get('content.stackVersions').findProperty('isCurrent').get('repoVersion'),
        currentVersion = App.StackVersion.find().findProperty('isCurrent'),
        clusterVersion = currentVersion && currentVersion.get('repositoryVersion.repositoryVersion');
      if (hostVersion !== clusterVersion) {
        var msg = Em.I18n.t("hosts.passiveMode.popup.version.mismatch").format(this.get('content.hostName'), clusterVersion);
        popupInfo += '<br/><div class="alert alert-warning">' + msg + '</div>';
      }
    }
    return App.showConfirmationPopup(function () {
        self.hostPassiveModeRequest(state, message);
      }, popupInfo);
  },

  /**
   * Set rack id for host
   * @method setRackIdForHost
   */
  setRackIdForHost: function () {
    var hostNames = [{hostName: this.get('content.hostName')}];
    var rack = this.get('content.rack');
    var operationData = {message: Em.I18n.t('hosts.host.details.setRackId')};
    hostsManagement.setRackInfo(operationData, hostNames, rack);
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
    params.component.set('passiveState', params.passive_state);
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
      if (components.someProperty('componentName', 'NAMENODE') &&
        this.get('content.hostComponents').filterProperty('componentName', 'NAMENODE').someProperty('workStatus', App.HostComponentStatus.started)) {
        this.checkNnLastCheckpointTime(function () {
          return App.showConfirmationPopup(function () {
            self.sendComponentCommand(components, Em.I18n.t('hosts.host.maintainance.stopAllComponents.context'), App.HostComponentStatus.stopped);
          });
        });
      } else {
        return App.showConfirmationPopup(function () {
          self.sendComponentCommand(components, Em.I18n.t('hosts.host.maintainance.stopAllComponents.context'), App.HostComponentStatus.stopped);
        });
      }
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
      if (components.someProperty('componentName', 'NAMENODE') &&
        this.get('content.hostComponents').filterProperty('componentName', 'NAMENODE').someProperty('workStatus', App.HostComponentStatus.started)) {
        this.checkNnLastCheckpointTime(function () {
          return App.showConfirmationPopup(function () {
            batchUtils.restartHostComponents(components, Em.I18n.t('rollingrestart.context.allOnSelectedHost').format(self.get('content.hostName')), "HOST");
          });
        });
      } else {
        return App.showConfirmationPopup(function () {
          batchUtils.restartHostComponents(components, Em.I18n.t('rollingrestart.context.allOnSelectedHost').format(self.get('content.hostName')), "HOST");
        });
      }
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
    var self = this;
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
    });
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
          var remainingHosts = App.db.getSelectedHosts('mainHostController').removeObject(self.get('content.hostName'));
          App.db.setSelectedHosts('mainHostController', remainingHosts);
          popup.hide();
        };
        self.doDeleteHost(completeCallback);
      }
    });
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
        });
      }
      else {
        completeCallback();
        deleteError.xhr.responseText = "{\"message\": \"" + deleteError.xhr.statusText + "\"}";
        App.ajax.defaultErrorHandler(deleteError.xhr, deleteError.url, deleteError.method, deleteError.xhr.status);
      }
    });
  },
  deleteHostSuccessCallback: function (data, rq, requestBody) {
    var self = this;
    App.router.get('updateController').updateHost(function () {
      App.router.transitionTo('hosts.index');
    });
    if (!!(requestBody && requestBody.hostName))
      App.hostsMapper.deleteRecord(App.Host.find().findProperty('hostName', requestBody.hostName));
    App.router.get('clusterController').getAllHostNames();
  },
  deleteHostErrorCallback: function (xhr, textStatus, errorThrown, opt) {
    console.log('Error deleting host.');
    console.log(textStatus);
    console.log(errorThrown);
    xhr.responseText = "{\"message\": \"" + xhr.statusText + "\"}";
    var self = this;
    var callback =   function () {
      self.loadConfigs();
    };
    self.isServiceMetricsLoaded(callback);
    App.ajax.defaultErrorHandler(xhr, opt.url, 'DELETE', xhr.status);
  },

  /**
   * Send command to server to restart all host components with stale configs
   * @method restartAllStaleConfigComponents
   */
  restartAllStaleConfigComponents: function () {
    var self = this;
    var staleComponents = self.get('content.componentsWithStaleConfigs');
    if (staleComponents.someProperty('componentName', 'NAMENODE') &&
      this.get('content.hostComponents').filterProperty('componentName', 'NAMENODE').someProperty('workStatus', App.HostComponentStatus.started)) {
      this.checkNnLastCheckpointTime(function () {
        App.showConfirmationPopup(function () {
          batchUtils.restartHostComponents(staleComponents, Em.I18n.t('rollingrestart.context.allWithStaleConfigsOnSelectedHost').format(self.get('content.hostName')), "HOST");
        });
      });
    } else {
      return App.showConfirmationPopup(function () {
        batchUtils.restartHostComponents(staleComponents, Em.I18n.t('rollingrestart.context.allWithStaleConfigsOnSelectedHost').format(self.get('content.hostName')), "HOST");
      });
    }

  },

  /**
   * open Reassign Master Wizard with selected component
   * @param {object} event
   * @method moveComponent
   */
  moveComponent: function (event) {
    if ($(event.target).closest('li').hasClass('disabled')) {
      return;
    }
    return App.showConfirmationPopup(function () {
      var component = event.context;
      var reassignMasterController = App.router.get('reassignMasterController');
      reassignMasterController.saveComponentToReassign(component);
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
    var components = event.context;
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
    this.downloadClientConfigsCall({
      hostName: event.context.get('hostName'),
      componentName: event.context.get('componentName'),
      displayName: event.context.get('displayName')
    });
  },

  installClients: function (event) {
    var clientsToInstall = [],
      clientsToAdd = [],
      missedComponents = [],
      dependentComponents = [],
      self = this;
    event.context.forEach(function (component) {
      if (['INIT', 'INSTALL_FAILED'].contains(component.get('workStatus'))) {
        clientsToInstall.push(component);
      } else if (typeof component.get('workStatus') == 'undefined') {
        clientsToAdd.push(component);
      }
    });
    clientsToAdd.forEach(function (component, index, array) {
      var dependencies = this.checkComponentDependencies(component.get('componentName'), {
        scope: 'host',
        installedComponents: this.get('content.hostComponents').mapProperty('componentName')
      }).reject(function (componentName) {
        return array.mapProperty('componentName').contains(componentName);
      });
      if (dependencies.length) {
        missedComponents.pushObjects(dependencies);
        dependentComponents.push(component.get('displayName'));
      }
    }, this);
    missedComponents = missedComponents.uniq();
    if (missedComponents.length) {
      var popupMessage = Em.I18n.t('host.host.addComponent.popup.clients.dependedComponents.body').format(stringUtils.getFormattedStringFromArray(dependentComponents),
        stringUtils.getFormattedStringFromArray(missedComponents.map(function (componentName) {
          return App.StackServiceComponent.find(componentName).get('displayName');
        })));
      App.showAlertPopup(Em.I18n.t('host.host.addComponent.popup.dependedComponents.header'), popupMessage);
    } else {
      App.get('router.mainAdminKerberosController').getSecurityType(function () {
        App.get('router.mainAdminKerberosController').getKDCSessionState(function () {
          var sendInstallCommand = function () {
            if (clientsToInstall.length) {
              self.sendComponentCommand(clientsToInstall, Em.I18n.t('host.host.details.installClients'), 'INSTALLED');
            }
          };
          if (clientsToAdd.length) {
            var message = stringUtils.getFormattedStringFromArray(clientsToAdd.mapProperty('displayName'));
            var isManualKerberos = App.get('router.mainAdminKerberosController.isManualKerberos');
            self.showAddComponentPopup(message, isManualKerberos, function () {
              sendInstallCommand();
              clientsToAdd.forEach(function (component) {
                self.installHostComponentCall(self.get('content.hostName'), component);
              });
            });
          } else {
            sendInstallCommand();
          }
        });
      }.bind(this));
    }
  },

  /**
   * Check if all required components are installed on host.
   * Available options:
   *  scope: 'host' - dependency level `host`,`cluster` or `*`.
   *  hostName: 'example.com' - host name to search installed components
   *  installedComponents: ['A', 'B'] - names of installed components
   *
   * By default scope level is `*`
   * For host level dependency you should specify at least `hostName` or `installedComponents` attribute.
   *
   * @param {String} componentName
   * @param {Object} opt - options. Allowed options are `hostName`, `installedComponents`, `scope`.
   * @return {Array} - names of missed components
   */
  checkComponentDependencies: function (componentName, opt) {
    opt = opt || {};
    opt.scope = opt.scope || '*';
    var installedComponents;
    var dependencies = App.StackServiceComponent.find(componentName).get('dependencies');
    dependencies = opt.scope === '*' ? dependencies : dependencies.filterProperty('scope', opt.scope);
    if (dependencies.length == 0) return [];
    switch (opt.scope) {
      case 'host':
        Em.assert("You should pass at least `hostName` or `installedComponents` to options.", opt.hostName || opt.installedComponents);
        installedComponents = opt.installedComponents || App.HostComponent.find().filterProperty('hostName', opt.hostName).mapProperty('componentName').uniq();
        break;
      default:
        // @todo: use more appropriate value regarding installed components
        installedComponents = opt.installedComponents || App.HostComponent.find().mapProperty('componentName').uniq();
        break;
    }
    return dependencies.filter(function (dependency) {
      return !installedComponents.contains(dependency.componentName);
    }).mapProperty('componentName');
  },

  /**
   * On click handler for custom command from items menu
   * @param context
   */
  executeCustomCommand: function (event) {
    var controller = this;
    var context = event.context;
    return App.showConfirmationPopup(function () {
      App.ajax.send({
        name: 'service.item.executeCustomCommand',
        sender: controller,
        data: {
          command: context.command,
          context: context.context || Em.I18n.t('services.service.actions.run.executeCustomCommand.context').format(context.command),
          hosts: context.hosts,
          serviceName: context.service,
          componentName: context.component
        },
        success: 'executeCustomCommandSuccessCallback',
        error: 'executeCustomCommandErrorCallback'
      });
    });
  },

  executeCustomCommandSuccessCallback: function (data, ajaxOptions, params) {
    if (data.Requests.id) {
      App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
        if (initValue) {
          App.router.get('backgroundOperationsController').showPopup();
        }
      });
    } else {
      console.warn('Error during execution of ' + params.command + ' custom command on' + params.componentName);
    }
  },
  executeCustomCommandErrorCallback: function (data) {
    var error = Em.I18n.t('services.service.actions.run.executeCustomCommand.error');
    if (data && data.responseText) {
      try {
        var json = $.parseJSON(data.responseText);
        error += json.message;
      } catch (err) {
      }
    }
    App.showAlertPopup(Em.I18n.t('services.service.actions.run.executeCustomCommand.error'), error);
    console.warn('Error during executing custom command');
  },

  /**
   * Call callback after loading service metrics
   * @param callback
   */
  isServiceMetricsLoaded: function(callback) {
    App.router.get('mainController').isLoading.call(App.router.get('clusterController'), 'isServiceContentFullyLoaded').done(callback);
  }
});
