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

App.DatabasesTabOnStep7View = Em.View.extend({

  templateName: require('templates/wizard/step7/databases_tab'),

  tabs: function () {
    return App.Tab.find().filterProperty('themeName', 'database');
  }.property(),

  configsView: null,

  didInsertElement: function () {
    var controllerRoute = 'App.router.' + this.get('controller.name');
    this.set('configsView', App.ServiceConfigView.extend({
      templateName: require('templates/common/configs/service_config_wizard'),
      supportsHostOverrides: false,
      controllerBinding: controllerRoute,
      isNotEditableBinding: controllerRoute + '.isNotEditable',
      filterBinding: controllerRoute + '.filter',
      columnsBinding: controllerRoute + '.filterColumns',
      selectedServiceBinding: controllerRoute + '.selectedService',
      serviceConfigsByCategoryView: Em.ContainerView.create(),
      supportsConfigLayout: true,
      willDestroyElement: function () {
        $('.loading').append(Em.I18n.t('app.loadingPlaceholder'));
      },
      didInsertElement: function () {
        $('.loading').empty();
        this._super();
      },
      tabs: function () {
        var tabs = this.get('tabModels');
        this.processTabs(tabs);
        return tabs;
      }.property('tabModels'),
      hideTabs: function () {
        this.get('tabs').forEach(function (tab) {
          tab.set('isHidden', tab.get('isConfigsPrepared') && tab.get('isHiddenByFilter'));
        });
      }.observes('tabs.@each.isHiddenByFilter'),
      setActiveTab: function (event) {
        if (event.context.get('isHiddenByFilter')) return false;
        this.get('tabs').forEach(function (tab) {
          tab.set('isActive', false);
        });
        var currentTab = event.context;
        currentTab.set('isActive', true);
      }
    }));
  }

});
