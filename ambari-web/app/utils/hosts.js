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
   * @param popupDescription {Object} Consist header and message for popup
   *   Example: {header: 'header', dialogMessage: 'message'}
   *  is closed, cancelled or OK is pressed.
   */
  launchHostsSelectionDialog : function(availableHosts, selectedHosts, 
      selectAtleastOneHost, validComponents, callback, popupDescription) {
    // set default popup description
    var defaultPopupDescription = {
      header: Em.I18n.t('hosts.selectHostsDialog.title'),
      dialogMessage: Em.I18n.t('hosts.selectHostsDialog.message')
    };
    if (popupDescription !== null) {
      popupDescription = $.extend(true, defaultPopupDescription, popupDescription);
    }
    App.ModalPopup.show({
      classNames: [ 'sixty-percent-width-modal' ],
      header: popupDescription.header,
      dialogMessage: popupDescription.dialogMessage,
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
          this.set('warningMessage', Em.I18n.t('hosts.selectHostsDialog.message.warning'));
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
        filterColumn: null,
        filterColumns: Ember.A([
          Ember.Object.create({id: 'ip', name: 'IP Address', selected: true}),
          Ember.Object.create({id: 'cpu', name: 'CPU', selected: false}),
          Ember.Object.create({id: 'memory', name: 'RAM', selected: false}),
          Ember.Object.create({id: 'osArch', name: 'OS Architecture', selected: false}),
          Ember.Object.create({id: 'osType', name: 'OS Type', selected: false}),
          Ember.Object.create({id: 'diskTotal', name: 'Total Disks Capacity', selected: false}),
          Ember.Object.create({id: 'disksMounted', name: '# of Disk Mounts', selected: false})
        ]),
        showOnlySelectedHosts: false,
        filterComponents: validComponents,
        filterComponent: null,
        didInsertElement: function(){
          var defaultFilterColumn = this.get('filterColumns').findProperty('selected');
          this.set('filterColumn', defaultFilterColumn);
        },
        filterHosts: function () {
          var filterText = this.get('filterText');
          var showOnlySelectedHosts = this.get('showOnlySelectedHosts');
          var filterComponent = this.get('filterComponent');
          var filterColumn = this.get('filterColumn');

          this.get('availableHosts').forEach(function (host) {
            var skip = showOnlySelectedHosts && !host.get('selected');
            var value = host.get('host').get(filterColumn.id);
            var hostComponentNames = host.get('host.hostComponents').mapProperty('componentName');

            host.set('filterColumnValue', value);

            if (!skip && filterText) {
              if ((value == null || !value.toString().match(filterText)) && !host.get('host.publicHostName').match(filterText)) {
                skip = true;
              }
            }
            if (!skip && filterComponent) {
              if (hostComponentNames.length > 0) {
                skip = !hostComponentNames.contains(filterComponent.get('componentName'));
              }
            }
            host.set('filtered', !skip);
          }, this);
        }.observes('availableHosts', 'filterColumn', 'filterText', 'filterComponent', 'filterComponent.componentName', 'showOnlySelectedHosts'),
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
          this.get('availableHosts').filterProperty('filtered').setEach('selected', this.get('allHostsSelected'));
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
