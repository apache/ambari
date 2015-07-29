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

App.ComponentActionsMixin = Em.Mixin.create({

  /**
   * Is ZooKeeper Server being deleted from host
   * @type {bool}
   */
  fromDeleteZkServer: false,

  /**
   * Delete Hive Metastore is performed
   * @type {bool}
   */
  deleteHiveMetaStore: false,

  /**
   * Delete Storm Nimbus is performed
   * @type {bool}
   */
  deleteNimbusHost: false,

  /**
   * Delete Hive Metastore is performed
   * @type {bool}
   */
  deleteRangerKMSServer: false,

  /**
   * Is deleteHost action id fired
   * @type {bool}
   */
  fromDeleteHost: false,

  isHostDetails: function () {
    return this.get('name') == 'mainHostDetailsController';
  }.property('name'),

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
          hostName: event.context.get('host.hostName'),
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
   * send command to server to stop selected host component
   * @param {object} event
   * @method stopComponent
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
   * PUTs a command to server to start/stop a component. If no
   * specific component is provided, all components are started.
   * @param {object} component  When <code>null</code> all startable components are started.
   * @param {String} context  Context under which this command is beign sent.
   * @param {String} state - desired state of component can be 'STARTED' or 'STOPPED'
   * @method sendComponentCommand
   */
  sendComponentCommand: function (component, context, state) {
    var data = {
      hostName: component.get('host.hostName'),
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
        this.hide();

        App.ajax.send({
          name: 'common.host.host_component.update',
          sender: self,
          data: {
            hostName: component.get('host.hostName'),
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

  toggleMaintenanceMode: function (event) {
    var self = this;
    var state = event.context.get('passiveState') === "ON" ? "OFF" : "ON";
    var message = Em.I18n.t('passiveState.turn' + state.toCapital() + 'For').format(event.context.get('displayName'));
    return App.showConfirmationPopup(function () {
      self.updateComponentPassiveState(event.context, state, message);
    });
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
        hostName: component.get('host.hostName'),
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
      isHiveMetastore: function () {
        return componentName == 'HIVE_METASTORE';
      }.property(),
      deleteHiveMetastoreMsg: Em.View.extend({
        template: Em.Handlebars.compile(Em.I18n.t('hosts.host.deleteComponent.popup.deleteHiveMetastore'))
      }),
      isNimbus: function () {
        return componentName == 'NIMBUS';
      }.property(),
      deleteNimbusMsg: Em.View.extend({
        template: Em.Handlebars.compile(Em.I18n.t('hosts.host.deleteComponent.popup.deleteNimbus'))
      }),
      isRangerKMSServer: function () {
        return componentName == 'RANGER_KMS_SERVER';
      }.property(),
      deleteRangerKMSServereMsg: Em.View.extend({
        template: Em.Handlebars.compile(Em.I18n.t('hosts.host.deleteComponent.popup.deleteRangerKMSServer'))
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
      deleteComponentMsg: function () {
        return Em.I18n.t('hosts.host.deleteComponent.popup.msg1').format(displayName);
      }.property(),
      deleteZkServerMsg: Em.View.extend({
        template: Em.Handlebars.compile(Em.I18n.t('hosts.host.deleteComponent.popup.deleteZooKeeperServer'))
      }),
      onPrimary: function () {
        var popup = this;
        self._doDeleteHostComponent(component, function () {
          if (self.get('isHostDetails')) {
            self.set('redrawComponents', true);
          }
          popup.hide();
        });
      }
    });
  },

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
        hostName: (component) ? component.get('host.hostName') : this.get('content.hostName')
      },
      success: '_doDeleteHostComponentSuccessCallback',
      error: '_doDeleteHostComponentErrorCallback'
    }).then(callback, callback);
  },

  /**
   * Success callback for delete host component request
   * @method _doDeleteHostComponentSuccessCallback
   */
  _doDeleteHostComponentSuccessCallback: function (response, request, data) {
    if (this.get('isHostDetails')) {
      this.set('_deletedHostComponentResult', null);
    }
    this.removeHostComponentModel(data.componentName, data.hostName);
    if (data.componentName == 'ZOOKEEPER_SERVER') {
      this.set('fromDeleteZkServer', true);
      this.loadConfigs(null, data.hostName);
    } else if (data.componentName == 'HIVE_METASTORE') {
      this.set('deleteHiveMetaStore', true);
      this.loadConfigs('loadHiveConfigs', data.hostName);
    } else if(data.componentName == 'NIMBUS') {
      this.set('deleteNimbusHost', true);
      this.loadConfigs('loadStormConfigs', data.hostName);
    } else if(data.componentName == 'RANGER_KMS_SERVER') {
      this.set('deleteRangerKMSServer', true);
      this.loadConfigs('loadRangerConfigs', data.hostName);
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
  removeHostComponentModel: function(componentName, hostName) {
    var component = App.HostComponent.find().filterProperty('componentName', componentName).findProperty('hostName', hostName);
    App.serviceMapper.deleteRecord(component);
  },

  /**
   * Load configs
   * This function when used without a callback should be always used from successcallback function of the promise `App.router.get('mainController').isLoading.call(App.router.get('clusterController'), 'isServiceContentFullyLoaded').done(promise)`
   * This is required to make sure that service metrics API determining the HA state of components is loaded
   * @method loadConfigs
   */
  loadConfigs: function (callback, hostName) {
    App.ajax.send({
      name: 'config.tags',
      sender: this,
      data: {
        hostName: hostName
      },
      success: callback ? callback : 'loadConfigsSuccessCallback',
      error: 'onLoadConfigsErrorCallback'
    });
  },

  /**
   * onLoadConfigsErrorCallback
   * @method onLoadConfigsErrorCallback
   */
  onLoadConfigsErrorCallback: Em.K,

  /**
   * Success callback for load configs request
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method adConfigsSuccessCallback
   */
  loadConfigsSuccessCallback: function (data, opt, params) {
    var urlParams = this.constructConfigUrlParams(data);
    if (urlParams.length > 0) {
      App.ajax.send({
        name: 'reassign.load_configs',
        sender: this,
        data: {
          hostName: params? params.hostName: '',
          urlParams: urlParams.join('|')
        },
        success: 'saveZkConfigs'
      });
      return true;
    }
    return false;
  },

  /**
   * Success callback for load configs request
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method loadHiveConfigs
   */
  loadHiveConfigs: function (data, opt, params) {
    App.ajax.send({
      name: 'admin.get.all_configurations',
      sender: this,
      data: {
        hostName: params.hostName,
        urlParams: '(type=hive-site&tag=' + data.Clusters.desired_configs['hive-site'].tag + ')|(type=webhcat-site&tag=' +
          data.Clusters.desired_configs['webhcat-site'].tag + ')|(type=hive-env&tag=' + data.Clusters.desired_configs['hive-env'].tag +
          ')|(type=core-site&tag=' + data.Clusters.desired_configs['core-site'].tag + ')'
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
      hiveMetastoreHost = App.get('router.mainHostDetailsController.hiveMetastoreHost'),
      hiveMSHosts = this.getHiveHosts(params.hostName),
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
    configs['core-site']['hadoop.proxyuser.' + hiveUser + '.hosts'] = hiveMasterHosts;
    configs['core-site']['hadoop.proxyuser.' + webhcatUser + '.hosts'] = hiveMasterHosts;
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
    this.saveConfigsBatch(groups, 'HIVE_METASTORE', hiveMetastoreHost);
  },

  /**
   * Success callback for Storm load configs request
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method loadStormConfigs
   */
  loadStormConfigs: function (data, opt, params) {
    App.ajax.send({
      name: 'admin.get.all_configurations',
      sender: this,
      data: {
        hostName: params.hostName,
        urlParams: '(type=storm-site&tag=' + data.Clusters.desired_configs['storm-env'].tag +')'
      },
      success: 'onLoadStormConfigs'
    });
  },

  /**
   * update and save Storm related configs to server
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method onLoadStormConfigs
   */
  onLoadStormConfigs: function (data, opt, params) {
    var nimbusHost = App.get('router.mainHostDetailsController.nimbusHost'),
      stormNimbusHosts = this.getStormNimbusHosts(params.hostName),
      configs = {},
      attributes = {};

    data.items.forEach(function (item) {
      configs[item.type] = item.properties;
      attributes[item.type] = item.properties_attributes || {};
    }, this);

    configs['storm-site']['nimbus.seeds'] = JSON.stringify(stormNimbusHosts).replace(/"/g, "'");
    var groups = [
      {
        properties: {
          'storm-site': configs['storm-site'],
          'storm-env': configs['storm-env']
        },
        properties_attributes: {
          'storm-site': attributes['storm-site'],
          'storm-env': attributes['storm-env']
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
    this.saveConfigsBatch(groups, 'NIMBUS', nimbusHost);
  },

  /**
   * Success callback for load configs request
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method loadRangerConfigs
   */
  loadRangerConfigs: function (data, opt, params) {
    App.ajax.send({
      name: 'admin.get.all_configurations',
      sender: this,
      data: {
        hostName: params.hostName,
        urlParams: '(type=core-site&tag=' + data.Clusters.desired_configs['core-site'].tag + ')|(type=hdfs-site&tag=' + data.Clusters.desired_configs['hdfs-site'].tag + ')|(type=kms-env&tag=' + data.Clusters.desired_configs['kms-env'].tag + ')'
      },
      success: 'onLoadRangerConfigs'
    });
  },

  /**
   * update and save Hive hive.metastore.uris config to server
   * @param {object} data
   *    * @param {object} opt
   * @param {object} params
   * @method onLoadRangerConfigs
   */
  onLoadRangerConfigs: function (data, opt, params) {
    var hostToInstall = this.get('rangerKMSServerHost');
    var rkmsHosts = this.getRangerKMSServerHosts(params? params.hostName: '');
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

  getHiveHosts: function (hostName) {
    var
      hiveHosts = App.HostComponent.find().filterProperty('componentName', 'HIVE_METASTORE').mapProperty('hostName'),
      hiveMetastoreHost = this.get('hiveMetastoreHost'),
      deletedHiveMetastoreHost = hostName || this.get('content.hostName');

    if(!!hiveMetastoreHost){
      hiveHosts.push(hiveMetastoreHost);
      this.set('hiveMetastoreHost', '');
    }

    if (this.get('fromDeleteHost') || this.get('deleteHiveMetaStore')) {
      this.set('deleteHiveMetaStore', false);
      this.set('fromDeleteHost', false);
      return hiveHosts.without(deletedHiveMetastoreHost);
    }
    return hiveHosts.sort();
  },

  getStormNimbusHosts: function (hostName) {
    var
      stormNimbusHosts = App.HostComponent.find().filterProperty('componentName', 'NIMBUS').mapProperty('hostName'),
      nimbusHost = App.get('router.mainHostDetailsController.nimbusHost'),
      deletedNimbusHost = hostName || this.get('content.hostName');

    if(!!nimbusHost){
      stormNimbusHosts.push(nimbusHost);
      App.set('router.mainHostDetailsController.nimbusHost', '');
    }

    if (this.get('fromDeleteHost') || this.get('deleteNimbusHost')) {
      this.set('deleteNimbusHost', false);
      this.set('fromDeleteHost', false);
      return stormNimbusHosts.without(deletedNimbusHost);
    }
    return stormNimbusHosts.sort();
  },

  getRangerKMSServerHosts: function (hostName) {
    var rkmsHosts = App.HostComponent.find().filterProperty('componentName', 'RANGER_KMS_SERVER').mapProperty('hostName'),
      rangerKMSServerHost = App.get('router.mainHostDetailsController.rangerKMSServerHost'),
      deletedRangerKMSServerHost = hostName || this.get('content.hostName');

    if(!!rangerKMSServerHost){
      rkmsHosts.push(rangerKMSServerHost);
      App.set('router.mainHostDetailsController.rangerKMSServerHost', '');
    }

    if (this.get('fromDeleteHost') || this.get('deleteRangerKMSServer')) {
      this.set('deleteRangerKMSServer', false);
      this.set('fromDeleteHost', false);
      return rkmsHosts.without(deletedRangerKMSServerHost);
    }
    return rkmsHosts.sort();
  },

  /**
   * save new ZooKeeper configs to server
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method saveZkConfigs
   */
  saveZkConfigs: function (data, opt, params) {
    var configs = {};
    var attributes = {};
    data.items.forEach(function (item) {
      configs[item.type] = item.properties;
      attributes[item.type] = item.properties_attributes || {};
    }, this);

    var zks = this.getZkServerHosts(params? params.hostName: '');
    var portValue = configs['zoo.cfg'] && Em.get(configs['zoo.cfg'], 'clientPort');
    var zkPort = typeof portValue === 'udefined' ? '2181' : portValue;
    var zksWithPort = this.concatZkNames(zks, zkPort);
    this.setZKConfigs(configs, zksWithPort, zks);
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
    this.saveConfigsBatch(groups, 'ZOOKEEPER_SERVER');
  },

  /**
   * Get list of hostnames where ZK Server is installed
   * @param hostName {string}
   * @returns {string[]}
   * @method getZkServerHosts
   */
  getZkServerHosts: function (hostName) {
    var zks = App.HostComponent.find().filterProperty('componentName', 'ZOOKEEPER_SERVER').mapProperty('hostName'),
      deletedZkServerHost = hostName || this.get('content.hostName');
    if (this.get('fromDeleteHost') || this.get('fromDeleteZkServer')) {
      this.set('fromDeleteHost', false);
      this.set('fromDeleteZkServer', false);
      return zks.without(deletedZkServerHost);
    }
    return zks;
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
    if (configs['accumulo-site']) {
      configs['accumulo-site']['instance.zookeeper.host'] = zksWithPort;
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
      configs['yarn-site']['yarn.resourcemanager.zk-address'] = zksWithPort;
    }
    if (App.get('isHadoop22Stack')) {
      if (configs['hive-site']) {
        configs['hive-site']['hive.zookeeper.quorum'] = zksWithPort;
      }
      if (configs['yarn-site']) {
        configs['yarn-site']['hadoop.registry.zk.quorum'] = zksWithPort;
        configs['yarn-site']['yarn.resourcemanager.zk-address'] = zksWithPort;
      }
    }
    return true;
  },

  /**
   * save configs' sites in batch
   * @param host
   * @param groups
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
          "service_config_version_note": Em.I18n.t('hosts.host.configs.save.note').format(App.format.role(componentName))
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
  installHostComponent: function(data, opt, params) {
    if (params.host) {
      componentsUtils.installHostComponent(params.host, App.StackServiceComponent.find(params.componentName));
    }
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
    return urlParams;
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
   * On click handler for custom command from items menu
   * @param context
   */
  executeCustomCommand: function (event) {
    var controller = this;
    var context = event.context;
    return App.showConfirmationPopup(function() {
      App.ajax.send({
        name : 'service.item.executeCustomCommand',
        sender: controller,
        data : {
          command : context.command,
          context : context.context || Em.I18n.t('services.service.actions.run.executeCustomCommand.context').format(context.command),
          hosts : context.hosts,
          serviceName : context.service,
          componentName : context.component
        },
        success : 'executeCustomCommandSuccessCallback',
        error : 'executeCustomCommandErrorCallback'
      });
    });
  },

  executeCustomCommandSuccessCallback: function (data, ajaxOptions, params) {
    if (data.Requests.id) {
      App.router.get('backgroundOperationsController').showPopup();
    } else {
      console.warn('Error during execution of ' + params.command + ' custom command on' + params.componentName);
    }
  },
  executeCustomCommandErrorCallback : function (data) {
    var error = Em.I18n.t('services.service.actions.run.executeCustomCommand.error');
    if(data && data.responseText){
      try {
        var json = $.parseJSON(data.responseText);
        error += json.message;
      } catch (err) {}
    }
    App.showAlertPopup(Em.I18n.t('services.service.actions.run.executeCustomCommand.error'), error);
    console.warn('Error during executing custom command');
  },

  /**
   *  Check that service metrics is loaded
   * @param callback
   */
  isServiceMetricsLoaded: function(callback) {
    App.router.get('mainController').isLoading.call(App.router.get('clusterController'), 'isServiceContentFullyLoaded').done(callback);
  }
});
