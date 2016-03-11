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

  clusterConfigs: null,
  componentsConfigs: [],
  isSaveDisabled: true,
  valueChanged: false,

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

  tabs: function() {
    var services = {};
    var tabs = [];
    this.get('componentsConfigs').forEach(function(component) {
      var serviceComponentInfo = component.ServiceComponentInfo;
      if (serviceComponentInfo.total_count) {
        if (serviceComponentInfo.category === "MASTER" || serviceComponentInfo.category === "SLAVE") {
          var componentRecovery = Ember.Object.create({
            display_name: App.format.role(serviceComponentInfo.component_name, false),
            component_name: serviceComponentInfo.component_name,
            recovery_enabled: serviceComponentInfo.recovery_enabled === 'true',
            valueChanged: false,
            service_name: serviceComponentInfo.service_name
          });
          if (services[serviceComponentInfo.service_name]) {
            services[serviceComponentInfo.service_name].get('componentRecovery').push(componentRecovery);
            services[serviceComponentInfo.service_name].set('enabledComponents', services[serviceComponentInfo.service_name].get('enabledComponents') + (componentRecovery.get('recovery_enabled') ? 1 : 0));
            services[serviceComponentInfo.service_name].set('totalComponents', services[serviceComponentInfo.service_name].get('totalComponents') + 1);
          } else {
            services[serviceComponentInfo.service_name] = Ember.Object.create({
              service_name: serviceComponentInfo.service_name,
              display_name: App.format.role(serviceComponentInfo.service_name, true),
              headingClass: "." + serviceComponentInfo.service_name,
              isActive: false,
              tooltip: function () {
                var percentage = this.get('enabledComponents') / this.get('totalComponents');
                var tooltip = '';
                if (percentage === 1) {
                  tooltip = Em.I18n.t('admin.serviceAutoStart.tooltip.text').format("All");
                } else if (percentage === 0) {
                  tooltip = Em.I18n.t('admin.serviceAutoStart.tooltip.text').format("No");
                } else {
                  tooltip = Em.I18n.t('admin.serviceAutoStart.tooltip.text').format(this.get('enabledComponents') + "/" + this.get('totalComponents'));
                }
                return tooltip;
              }.property('enabledComponents', 'totalComponents'),
              componentRecovery: [componentRecovery],
              enabledComponents: componentRecovery.recovery_enabled ? 1 : 0,
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
          }
        }
      }
    });
    for (var service in services ) {
      tabs.push(services[service]);
    }
    if (tabs.length) {
      tabs[0].set('isActive', true);
    }
    return tabs;
  }.property('componentsConfigs'),

  checkValuesChange: function () {
    var valuesChanged = this.get('valueChanged');
    this.get('tabs').forEach(function (service) {
      service.get('componentRecovery').forEach(function (component) {
        valuesChanged = valuesChanged || component.get('valueChanged');
      });
    });
    this.set('isSaveDisabled', !valuesChanged);
  }.observes('valueChanged'),

  enableAll: function (event) {
    event.context.get('componentRecovery').forEach(function (component) {
      component.set('recoveryEnabled', true);
    });
  },

  disableAll: function (event) {
    event.context.get('componentRecovery').forEach(function (component) {
      component.set('recoveryEnabled', false);
    });
  },

  doReload: function () {
    window.location.reload();
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
        //save cluster setting
        if (self.get('valueChanged')) {
          self.saveClusterConfigs(self.get('clusterConfigs'));
        }
        //save component settings
        var enabledComponents = [];
        var disabledComponents = [];
        self.get('tabs').forEach(function (service) {
          service.get('componentRecovery').forEach(function (component) {
            if (component.get('valueChanged')) {
              if (component.get('recovery_enabled')) {
                enabledComponents.push(component.get('component_name'));
              } else {
                disabledComponents.push(component.get('component_name'));
              }
            }
          });
        });
        if (enabledComponents.length){
          App.ajax.send({
            name: 'components.update',
            sender: this,
            data: {
              ServiceComponentInfo: {
                recovery_enabled: "true"
              },
              query: 'ServiceComponentInfo/component_name.in(' + enabledComponents.join(',') + ')'
            }
          });
        }
        if (disabledComponents.length){
          App.ajax.send({
            name: 'components.update',
            sender: this,
            data: {
              ServiceComponentInfo: {
                recovery_enabled: "false"
              },
              query: 'ServiceComponentInfo/component_name.in(' + disabledComponents.join(',') + ')'
            }
          });
        }
        if (typeof transitionCallback === 'function') {
          transitionCallback();
        } else {
          self.doReload();
        }
        this.hide();
      },
      onDiscard: function () {
        if (typeof transitionCallback === 'function') {
          transitionCallback();
        } else {
          self.doReload();
        }
        this.hide();
      },
      onCancel: function () {
        this.hide();
      }
    });
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
  }
});
