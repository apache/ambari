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

App.MainAdminServiceAutoStartController = Em.Controller.extend({
  name: 'mainAdminServiceAutoStartController',

  /**
   * @type {?object}
   * @default null
   */
  clusterConfigs: null,

  /**
   * @type {Array}
   */
  componentsConfigs: [],

  /**
   * @type {boolean}
   * @default false
   */
  isSaveDisabled: true,

  /**
   * Current value of servicesAutoStart
   * @type {boolean}
   */
  servicesAutoStart: false,

  /**
   * Value of servicesAutoStart saved on server
   * @type {boolean}
   */
  servicesAutoStartSaved: false,

  /**
   * config version tag,
   * required to sync cluster configs locally without reload
   * @type {?object}
   */
  tag: null,

  /**
   * @type {boolean}
   */
  isServicesAutoStartChanged: Em.computed.notEqualProperties('servicesAutoStart', 'servicesAutoStartSaved'),

  valueChanged: function() {
    var isSaveDisabled = true;

    if (this.get('isServicesAutoStartChanged')) {
      this.set('clusterConfigs.recovery_enabled', this.get('servicesAutoStart') + '');
      isSaveDisabled = false;
    }
    this.get('tabs').forEach(function(tab) {
      if (isSaveDisabled) {
        isSaveDisabled = !tab.get('components').someProperty('isChanged');
      }
    });

    this.set('isSaveDisabled', isSaveDisabled);
  },

  load: function() {
    var self = this;
    var dfd = $.Deferred();

    return this.loadClusterConfig().done(function (data) {
      var tag = [
        {
          siteName: 'cluster-env',
          tagName: data.Clusters.desired_configs['cluster-env'].tag,
          newTagName: null
        }
      ];
      self.set('tag', tag);
      App.router.get('configurationController').getConfigsByTags(tag).done(function (data) {
        var servicesAutoStart = data[0].properties.recovery_enabled === 'true';
        self.set('clusterConfigs', data[0].properties);
        self.set('servicesAutoStart', servicesAutoStart);
        self.set('servicesAutoStartSaved', servicesAutoStart);
        self.loadComponentsConfigs().done(dfd.resolve).fail(dfd.reject);
      }).fail(dfd.reject);
    }).fail(dfd.reject);

    return dfd.promise();
  },

  loadClusterConfig: function () {
    return App.ajax.send({
      name: 'config.tags.site',
      sender: this,
      data: {
        site: 'cluster-env'
      }
    });
  },

  loadComponentsConfigs: function () {
    return App.ajax.send({
      name: 'components.get_category',
      sender: this,
      success: 'loadComponentsConfigsSuccess'
    });
  },

  loadComponentsConfigsSuccess: function (data) {
    this.set('componentsConfigs', data.items);
  },

  saveClusterConfigs: function (clusterConfigs) {
    return App.ajax.send({
      name: 'admin.save_configs',
      sender: this,
      data: {
        siteName: 'cluster-env',
        properties: clusterConfigs
      }
    });
  },

  saveComponentSettingsCall: function(recoveryEnabled, components) {
    return App.ajax.send({
      name: 'components.update',
      sender: this,
      data: {
        ServiceComponentInfo: {
          recovery_enabled: recoveryEnabled
        },
        query: 'ServiceComponentInfo/component_name.in(' + components.join(',') + ')'
      }
    });
  },

  tabs: function() {
    var services = {};
    var tabs = [];

    this.get('componentsConfigs').forEach(function (component) {
      var serviceComponentInfo = component.ServiceComponentInfo;
      if (serviceComponentInfo.total_count) {
        if (serviceComponentInfo.category === "MASTER" || serviceComponentInfo.category === "SLAVE") {
          var componentRecovery = this.createRecoveryComponent(serviceComponentInfo);
          var service = services[serviceComponentInfo.service_name];

          if (service) {
            service.get('components').pushObject(componentRecovery);
            service.set('enabledComponents', services[serviceComponentInfo.service_name].get('enabledComponents') + (componentRecovery.get('recovery_enabled') ? 1 : 0));
            service.set('totalComponents', services[serviceComponentInfo.service_name].get('totalComponents') + 1);
          } else {
            services[serviceComponentInfo.service_name] = this.createTab(serviceComponentInfo, componentRecovery);
          }
        }
      }
    }, this);
    for (var name in services ) {
      tabs.push(services[name]);
    }
    if (tabs.length) {
      tabs[0].set('isActive', true);
    }
    return tabs;
  }.property('componentsConfigs'),

  createRecoveryComponent: function(serviceComponentInfo) {
    return Ember.Object.create({
      displayName: App.format.role(serviceComponentInfo.component_name, false),
      componentName: serviceComponentInfo.component_name,
      recoveryEnabled: serviceComponentInfo.recovery_enabled === 'true',
      recoveryEnabledSaved: serviceComponentInfo.recovery_enabled === 'true',
      isChanged: Em.computed.notEqualProperties('recoveryEnabled', 'recoveryEnabledSaved'),
      serviceName: serviceComponentInfo.service_name
    });
  },

  createTab: function(serviceComponentInfo, componentRecovery) {
    return Ember.Object.create({
      service_name: serviceComponentInfo.service_name,
      display_name: App.format.role(serviceComponentInfo.service_name, true),
      headingClass: "." + serviceComponentInfo.service_name,
      isActive: false,
      tooltip: function () {
        var percentage = this.get('enabledComponents') / this.get('totalComponents');
        var text = Em.I18n.t('admin.serviceAutoStart.tooltip.text');
        if (percentage === 1) {
          return text.format("All");
        } else if (percentage === 0) {
          return text.format("No");
        } else {
          return text.format(this.get('enabledComponents') + "/" + this.get('totalComponents'));
        }
      }.property('enabledComponents', 'totalComponents'),
      components: Em.A([componentRecovery]),
      enabledComponents: componentRecovery.recoveryEnabled ? 1 : 0,
      totalComponents: 1,
      indicator: function () {
        var percentage = this.get('enabledComponents') / this.get('totalComponents');
        var indicator = "icon-adjust";
        if (percentage === 1) {
          indicator = "icon-circle";
        } else if (percentage === 0) {
          indicator = "icon-circle-blank";
        }
        return indicator;
      }.property('enabledComponents', 'totalComponents')
    });
  },

  filterTabsByRecovery: function(tabs, isRecoveryEnabled) {
    var components = [];

    tabs.forEach(function (service) {
      service.get('components').forEach(function (component) {
        if (component.get('isChanged') && component.get('recoveryEnabled') === isRecoveryEnabled) {
          components.push(component.get('componentName'));
        }
      });
    });
    return components;
  },

  syncStatus: function () {
    var servicesAutoStart = this.get('servicesAutoStart');
    this.set('servicesAutoStartSaved', servicesAutoStart);
    App.router.get('configurationController').getConfigsByTags(this.get('tag')).done(function(data) {
      data[0].properties.recovery_enabled = servicesAutoStart + '';
      App.router.get('configurationController').saveToDB(data);
    });
    this.get('tabs').forEach(function(tab) {
      tab.get('components').forEach(function(component) {
        component.set('recoveryEnabledSaved', component.get('recoveryEnabled'));
      });
    });
    this.valueChanged();
  },

  revertStatus: function () {
    this.set('servicesAutoStart', this.get('servicesAutoStartSaved'));
    this.get('tabs').forEach(function(tab) {
      tab.get('components').forEach(function(component) {
        component.set('recoveryEnabled', component.get('recoveryEnabledSaved'));
      });
    });
  },

  enableAll: function (event) {
    event.context.get('components').setEach('recoveryEnabled', true);
  },

  disableAll: function (event) {
    event.context.get('components').setEach('recoveryEnabled', false);
  },

  /**
   * If some configs are changed and user navigates away or select another config-group, show this popup with propose to save changes
   * @param {object} transitionCallback - callback with action to change configs view
   * @return {App.ModalPopup}
   * @method showSavePopup
   */
  showSavePopup: function (transitionCallback) {
    var self = this;
    var title = '';
    var body = '';
    if (typeof transitionCallback === 'function') {
      title = Em.I18n.t('admin.serviceAutoStart.save.popup.transition.title');
      body = Em.I18n.t('admin.serviceAutoStart.save.popup.transition.body');
    } else {
      title = Em.I18n.t('admin.serviceAutoStart.save.popup.title');
      body = Em.I18n.t('admin.serviceAutoStart.save.popup.body');
    }
    return App.ModalPopup.show({
      header: title,
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile(body)
      }),
      footerClass: Em.View.extend({
        templateName: require('templates/main/service/info/save_popup_footer')
      }),
      primary: Em.I18n.t('common.save'),
      secondary: Em.I18n.t('common.cancel'),
      onSave: function () {
        var clusterConfigsCall, enabledComponentsCall, disabledComponentsCall;

        if (self.get('isServicesAutoStartChanged')) {
          clusterConfigsCall = self.saveClusterConfigs(self.get('clusterConfigs'));
        }
        var enabledComponents = self.filterTabsByRecovery(self.get('tabs'), true);
        var disabledComponents = self.filterTabsByRecovery(self.get('tabs'), false);

        if (enabledComponents.length) {
          enabledComponentsCall = self.saveComponentSettingsCall('true', enabledComponents);
        }
        if (disabledComponents.length) {
          disabledComponentsCall = self.saveComponentSettingsCall('false', disabledComponents);
        }
        $.when(clusterConfigsCall, enabledComponentsCall, disabledComponentsCall).done(function () {
          if (typeof transitionCallback === 'function') {
            transitionCallback();
          }
          self.syncStatus();
        });
        this.hide();
      },
      onDiscard: function () {
        self.revertStatus();
        if (typeof transitionCallback === 'function') {
          transitionCallback();
        }
        this.hide();
      },
      onCancel: function () {
        this.hide();
      }
    });
  }
});
