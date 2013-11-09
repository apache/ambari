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
module.exports = {

  /**
   * Launches a dialog to select hosts from the provided available hosts. 
   * 
   * Once the user clicks OK or Cancel, the callback is called with the 
   * array of hosts (App.Host[]) selected. If the dialog was cancelled
   * or closed, <code>null</code> is provided to the callback. Else
   * an array (maybe empty) will be provided to the callback.
   * 
   * @param availableHosts  {App.Host[]} List of hosts to pick from
   * @param selectedHosts {App.Host[]} List of hosts already selected from the available hosts
   * @param selectAtleastOneHost  {boolean} If true atleast one host has to be selected
   * @param validComponents {App.HostComponent[]} List of host-component types to pick from.
   * @param callback  Callback function which is invoked when dialog 
   *  is closed, cancelled or OK is pressed.
   */
  launchHostsSelectionDialog : function(availableHosts, selectedHosts, 
      selectAtleastOneHost, validComponents, callback) {
    App.ModalPopup.show({
      classNames: [ 'sixty-percent-width-modal' ],
      header: Em.I18n.t('hosts.selectHostsDialog.title'),
      primary: Em.I18n.t('ok'),
      secondary: Em.I18n.t('common.cancel'),
      warningMessage: null,
      onPrimary: function () {
        console.debug('(old-selectedHosts)=', selectedHosts);
        this.set('warningMessage', null);
        var arrayOfSelectedHosts = [];
        var selectedHosts = availableHosts.filterProperty('selected', true);
        selectedHosts.forEach(function (host) {
          arrayOfSelectedHosts.push(host.get('host.id'));
        });
        if (selectAtleastOneHost && arrayOfSelectedHosts.length<1) {
          this.set('warningMessage', 'Atleast one host needs to be selected.');
          return;
        }
        callback(arrayOfSelectedHosts);
        console.debug('(new-selectedHosts)=', arrayOfSelectedHosts);
        this.hide();
      },
      onSecondary: function () {
        callback(null);
        this.hide();
      },
      bodyClass: Ember.View.extend({
        templateName: require('templates/common/configs/overrideWindow'),
        controllerBinding: 'App.router.mainServiceInfoConfigsController',
        filterText: '',
        filterTextPlaceholder: Em.I18n.t('hosts.selectHostsDialog.filter.placeHolder'),
        availableHosts: availableHosts,
        filterColumn: Ember.Object.create({id: 'ip', name: 'IP Address', selected: false}),
        filterColumns: Ember.A([
          Ember.Object.create({id: 'ip', name: 'IP Address', selected: false}),
          Ember.Object.create({id: 'cpu', name: 'CPU', selected: false}),
          Ember.Object.create({id: 'memory', name: 'RAM', selected: false}),
          Ember.Object.create({id: 'diskUsage', name: 'Disk Usage', selected: false}),
          Ember.Object.create({id: 'loadAvg', name: 'Load Average', selected: false}),
          Ember.Object.create({id: 'osArch', name: 'OS Architecture', selected: false}),
          Ember.Object.create({id: 'osType', name: 'OS Type', selected: false})
        ]),
        showOnlySelectedHosts: false,
        filterComponents: validComponents,
        filterComponent: null,
        filteredHosts: function () {
          var hosts = this.get('availableHosts');
          var filterText = this.get('filterText');
          var showOnlySelectedHosts = this.get('showOnlySelectedHosts');
          var filteredHosts = Ember.A([]);
          var self = this;
          hosts.forEach(function (host) {
            var skip = false;
            var ahost = host.get('host');
            var filterColumn = self.get('filterColumn');
            if (filterColumn == null) {
              filterColumn = self.get('filterColumns').objectAt(0);
            }
            var value = ahost.get(filterColumn.id);
            host.set('filterColumnValue', value);
            if (filterText != null && filterText.length > 0) {
              if ((value == null || !value.match(filterText)) && !host.get('host.publicHostName').match(filterText)) {
                skip = true;
              }
            }
            var filterComponent = self.get('filterComponent');
            if (!skip && filterComponent != null) {
              var componentFound = false;
              var fcn = filterComponent.get('componentName');
              var hcs = ahost.get('hostComponents');
              if (hcs != null) {
                hcs.forEach(function (hc) {
                  if (fcn === hc.get('componentName')) {
                    componentFound = true;
                  }
                });
              }
              if (!componentFound) {
                skip = true;
              }
            }
            if (!skip && showOnlySelectedHosts && !host.get('selected')) {
              skip = true;
            }
            if (!skip) {
              filteredHosts.pushObject(host);
            }
          });
          return filteredHosts;
        }.property('availableHosts', 'filterText', 'filterColumn', 'filterComponent', 'filterComponent.componentName', 'showOnlySelectedHosts'),
        hostColumnValue: function (host, column) {
          return host.get(column.id);
        },
        hostSelectMessage: function () {
          var hosts = this.get('availableHosts');
          var selectedHosts = hosts.filterProperty('selected', true);
          return this.t('hosts.selectHostsDialog.selectedHostsLink').format(selectedHosts.get('length'), hosts.get('length'))
        }.property('availableHosts.@each.selected'),
        selectFilterColumn: function (event) {
          if (event != null && event.context != null && event.context.id != null) {
            var filterColumn = this.get('filterColumn');
            if (filterColumn != null) {
              filterColumn.set('selected', false);
            }
            event.context.set('selected', true);
            this.set('filterColumn', event.context);
          }
        },
        selectFilterComponent: function (event) {
          if (event != null && event.context != null && event.context.componentName != null) {
            var currentFilter = this.get('filterComponent');
            if (currentFilter != null) {
              currentFilter.set('selected', false);
            }
            if (currentFilter != null && currentFilter.componentName === event.context.componentName) {
              // selecting the same filter deselects it.
              this.set('filterComponent', null);
            } else {
              this.set('filterComponent', event.context);
              event.context.set('selected', true);
            }
          }
        },
        allHostsSelected: false,
        toggleSelectAllHosts: function (event) {
          if (this.get('allHostsSelected')) {
            // Select all hosts
            this.get('availableHosts').setEach('selected', true);
          } else {
            // Deselect all hosts
            this.get('availableHosts').setEach('selected', false);
          }
        }.observes('allHostsSelected'),
        toggleShowSelectedHosts: function () {
          var currentFilter = this.get('filterComponent');
          if (currentFilter != null) {
            currentFilter.set('selected', false);
          }
          this.set('filterComponent', null);
          this.set('filterText', null);
          this.set('showOnlySelectedHosts', !this.get('showOnlySelectedHosts'));
        }
      })
    });
  }
};
