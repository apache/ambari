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
var numberUtils = require('utils/number_utils');

App.ManageAlertGroupsController = Em.Controller.extend({
  name: 'manageAlertGroupsController',

  isLoaded: false,

  alertGroups: [],

  originalAlertGroups: [],

  selectedAlertGroup: null,

  selectedDefinitions: [],

  alertDefinitions: [],

  // load alert definitions for all services
  loadAlertDefinitions: function () {
    App.ajax.send({
      name: 'alerts.load_all_alert_definitions',
      sender: this,
      success: 'onLoadAlertDefinitionsSuccess',
      error: 'onLoadAlertDefinitionsError'
    });
  },

  onLoadAlertDefinitionsSuccess: function (data) {
    var alertDefinitions = [];
    if (data && data.items) {
      data.items.forEach( function(def) {
        if (def && def.AlertDefinition) {
          alertDefinitions.pushObject (Em.Object.create({
            name: def.AlertDefinition.name,
            serviceName: def.AlertDefinition.service_name,
            serviceNameDisplay:function() {
              return App.format.role(this.get('serviceName'));
            }.property('serviceName'),
            componentName: def.AlertDefinition.component_name,
            componentNameDisplay: function() {
              return App.format.role(this.get('componentName'));
            }.property('componentName'),
            label: def.AlertDefinition.label,
            id: def.AlertDefinition.id
          }));
        }
      });
    }
    this.set('alertDefinitions', alertDefinitions);
  },

  onLoadAlertDefinitionsError: function () {
    console.error('Unable to load all alert definitions.');
  },

  loadAlertGroups: function () {
    this.set('isLoaded', false);
    this.set('alertGroups.length', 0);
    this.set('originalAlertGroups.length', 0);
    App.ajax.send({
      name: 'alerts.load_alert_groups',
      sender: this,
      success: 'onLoadAlertGroupsSuccess',
      error: 'onLoadAlertGroupsError'
    });
  },

  onLoadAlertGroupsSuccess: function (data) {
    var self = this;
    if (data && data.items) {
      this.set('alertGroupsCount', data.items.length);
      data.items.forEach(function(alert_group) {
        App.ajax.send({
          name: 'alerts.load_an_alert_group',
          sender: self,
          data: {
            "group_id": alert_group.AlertGroup.id
          },
          success: 'onLoadAlertGroupSuccess',
          error: 'onLoadAlertGroupError'
        });
      }, this);
    }
  },

  onLoadAlertGroupSuccess: function (data) {
    var alertGroups = this.get('alertGroups');
    if (data && data.AlertGroup) {
      alertGroups.pushObject ( App.AlertGroupComplex.create({
        name: data.AlertGroup.name,
        default: data.AlertGroup.default,
        id: data.AlertGroup.id,
        definitions: data.AlertGroup.definitions,
        targets: data.AlertGroup.targets
      }));
    }
    if (this.get('alertGroupsCount') == alertGroups.length) {
      this.set('isLoaded', true);
      this.set('originalAlertGroups', this.copyAlertGroups(alertGroups));
    }
  },


  onLoadAlertGroupsError: function () {
    console.error('Unable to load all alert groups.');
  },
  onLoadAlertGroupError: function () {
    console.error('Unable to load an alert group.');
  },

  resortAlertGroup: function() {
    var alertGroups = Ember.copy(this.get('alertGroups'));
    if(alertGroups.length < 2){
      return;
    }
    var defaultGroups = alertGroups.filterProperty('default');
    defaultGroups.forEach( function(defaultGroup) {
      alertGroups.removeObject(defaultGroup);
    });
    var sorted = defaultGroups.concat(alertGroups.sortProperty('name'));
   // var sorted = alertGroups.sortProperty('name');

    this.removeObserver('alertGroups.@each.name', this, 'resortAlertGroup');
    this.set('alertGroups', sorted);
    this.addObserver('alertGroups.@each.name', this, 'resortAlertGroup');
  }.observes('alertGroups.@each.name'),

  /**
   * remove definitions from group
   */
  deleteDefinitions: function () {
    if (this.get('isDeleteDefinitionsDisabled')) {
      return;
    }
    var groupDefinitions = this.get('selectedAlertGroup.definitions');
    this.get('selectedDefinitions').slice().forEach(function (defObj) {
      groupDefinitions.removeObject(defObj);
    }, this);
    this.set('selectedDefinitions', []);
  },

  isDeleteDefinitionsDisabled: function () {
    var selectedGroup = this.get('selectedAlertGroup');
    if (selectedGroup) {
      return selectedGroup.default || this.get('selectedDefinitions').length === 0;
    }
    return true;
  }.property('selectedAlertGroup', 'selectedAlertGroup.definitions.length', 'selectedDefinitions.length'),

  /**
   * add alert definitions to a group
   */
  addDefinitions: function () {
    if (this.get('selectedAlertGroup.isAddDefinitionsDisabled')){
      return false;
    }
    var availableDefinitions = this.get('selectedAlertGroup.availableDefinitions');
    var popupDescription = {
      header: Em.I18n.t('alerts.actions.manage_alert_groups_popup.selectDefsDialog.title'),
      dialogMessage: Em.I18n.t('alerts.actions.manage_alert_groups_popup.selectDefsDialog.message').format(this.get('selectedAlertGroup.displayName'))
    };
    var validComponents = App.StackServiceComponent.find().map(function (component) {
      return Em.Object.create({
        componentName: component.get('componentName'),
        displayName: App.format.role(component.get('componentName')),
        selected: false
      });
    });
    var validServices = App.Service.find().map(function (service) {
      return Em.Object.create({
        serviceName: service.get('serviceName'),
        displayName: App.format.role(service.get('serviceName')),
        selected: false
      });
    });
    this.launchDefsSelectionDialog (availableDefinitions, [], validServices, validComponents, this.addDefinitionsCallback.bind(this), popupDescription);
  },

  /**
   * lauch a table view of all availavle definitions to choose
   */
  launchDefsSelectionDialog : function(initialDefs, selectedDefs, validServices, validComponents, callback, popupDescription) {

    App.ModalPopup.show({
      classNames: [ 'sixty-percent-width-modal' ],
      header: popupDescription.header,
      dialogMessage: popupDescription.dialogMessage,
      warningMessage: null,
      availableDefs: [],
      onPrimary: function () {
        this.set('warningMessage', null);
        var arrayOfSelectedDefs = this.get('availableDefs').filterProperty('selected', true);
        if (arrayOfSelectedDefs.length < 1) {
          this.set('warningMessage', Em.I18n.t('alerts.actions.manage_alert_groups_popup.selectDefsDialog.message.warning'));
          return;
        }
        callback(arrayOfSelectedDefs);
        console.debug('(new-selectedDefs)=', arrayOfSelectedDefs);
        this.hide();
      },
      disablePrimary: function () {
        return !this.get('isLoaded');
      }.property('isLoaded'),
      onSecondary: function () {
        callback(null);
        this.hide();
      },
      bodyClass: App.TableView.extend({
        templateName: require('templates/main/alerts/add_definition_to_group_popup'),
        controllerBinding: 'App.router.manageAlertGroupsController',
        isPaginate: true,
        filteredContent: function() {
          return this.get('parentView.availableDefs').filterProperty('filtered') || [];
        }.property('parentView.availableDefs.@each.filtered'),

        showOnlySelectedDefs: false,
        filterComponents: validComponents,
        filterServices: validServices,
        filterComponent: null,
        filterService: null,
        isDisabled: function () {
          return !this.get('parentView.isLoaded');
        }.property('parentView.isLoaded'),
        didInsertElement: function(){
          initialDefs.setEach('filtered', true);
          this.set('parentView.availableDefs', initialDefs);
          this.set('parentView.isLoaded', true);
        },
        filterDefs: function () {
          var showOnlySelectedDefs = this.get('showOnlySelectedDefs');
          var filterComponent = this.get('filterComponent');
          var filterService = this.get('filterService');
          this.get('parentView.availableDefs').forEach(function (defObj) {
            var componentOnObj = true;
            var serviceOnObj = true;
            if (filterComponent) {
              componentOnObj = (defObj.componentName == filterComponent.get('componentName'));
            }
            if (defObj.serviceName && filterService) {
              serviceOnObj = (defObj.serviceName == filterService.get('serviceName'));
            }
            if (showOnlySelectedDefs) {
              defObj.set('filtered', componentOnObj && serviceOnObj && defObj.get('selected'));
            } else {
              defObj.set('filtered', componentOnObj && serviceOnObj);
            }
          }, this);
          this.set('startIndex', 1);
        }.observes('parentView.availableDefs', 'filterService', 'filterService.serviceName', 'filterComponent', 'filterComponent.componentName', 'showOnlySelectedDefs'),
        defSelectMessage: function () {
          var defs = this.get('parentView.availableDefs');
          var selectedDefs = defs.filterProperty('selected', true);
          return this.t('alerts.actions.manage_alert_groups_popup.selectDefsDialog.selectedDefsLink').format(selectedDefs.get('length'), defs.get('length'));
        }.property('parentView.availableDefs.@each.selected'),

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
        selectFilterService: function (event) {
          if (event != null && event.context != null && event.context.serviceName != null) {
            var currentFilter = this.get('filterService');
            if (currentFilter != null) {
              currentFilter.set('selected', false);
            }
            if (currentFilter != null && currentFilter.serviceName === event.context.serviceName) {
              // selecting the same filter deselects it.
              this.set('filterService', null);
            } else {
              this.set('filterService', event.context);
              event.context.set('selected', true);
            }
          }
        },
        allDefsSelected: false,
        toggleSelectAllDefs: function (event) {
          this.get('parentView.availableDefs').filterProperty('filtered').setEach('selected', this.get('allDefsSelected'));
        }.observes('allDefsSelected'),
        toggleShowSelectedDefs: function () {
          var filter1 = this.get('filterComponent');
          if (filter1 != null) {
            filter1.set('selected', false);
          }
          var filter2 = this.get('filterService');
          if (filter2 != null) {
            filter2.set('selected', false);
          }
          this.set('filterComponent', null);
          this.set('filterService', null);
          this.set('showOnlySelectedDefs', !this.get('showOnlySelectedDefs'));
        }
      })
    });
  },

  /**
   * add alert definitions callback
   */
  addDefinitionsCallback: function (selectedDefs) {
    var group = this.get('selectedAlertGroup');
    if (selectedDefs) {
      var alertGroupDefs = group.get('definitions');
      selectedDefs.forEach(function (defObj) {
        alertGroupDefs.pushObject(defObj);
      }, this);
    }
  },

  /**
   * observes if any group changed including: group name, newly created group, deleted group, group with definitions changed
   */
  defsModifiedAlertGroups: function () {
    if (!this.get('isLoaded')) {
      return false;
    }
    var groupsToDelete = [];
    var groupsToSet = [];
    var groupsToCreate = [];
    var groups = this.get('alertGroups'); //current alert groups
    var originalGroups = this.get('originalAlertGroups'); // original alert groups
    // remove default group
    originalGroups = originalGroups.filterProperty('default', false);
    var originalGroupsIds = originalGroups.mapProperty('id');
    groups.forEach(function (group) {
      if (!group.get('default')) {
        var originalGroup = originalGroups.findProperty('id', group.get('id'));
        if (originalGroup) {
          if (!(JSON.stringify(group.get('definitions').slice().sort()) === JSON.stringify(originalGroup.get('definitions').slice().sort()))
           || !(JSON.stringify(group.get('targets').slice().sort()) === JSON.stringify(originalGroup.get('targets').slice().sort()))) {
            groupsToSet.push(group.set('id', originalGroup.get('id')));
          } else if (group.get('name') !== originalGroup.get('name') ) {
            // should update name
            groupsToSet.push(group.set('id', originalGroup.get('id')));
          }
          originalGroupsIds = originalGroupsIds.without(group.get('id'));
        } else {
          groupsToCreate.push(group);
        }
      }
    });
    originalGroupsIds.forEach(function (id) {
      groupsToDelete.push(originalGroups.findProperty('id', id));
    }, this);
    return {
      toDelete: groupsToDelete,
      toSet: groupsToSet,
      toCreate: groupsToCreate
    };
  }.property('selectedAlertGroup.definitions.@each', 'selectedAlertGroup.definitions.length', 'alertGroups', 'isLoaded'),

  isDefsModified: function () {
    var modifiedGroups = this.get('defsModifiedAlertGroups');
    if (!this.get('isLoaded')) {
      return false;
    }
    return !!(modifiedGroups.toSet.length || modifiedGroups.toCreate.length || modifiedGroups.toDelete.length);
  }.property('defsModifiedAlertGroups'),

  /**
   * copy alert groups for backup, to compare with current alert groups, so will know if some groups changed/added/deleted
   * @param originGroups
   * @return {Array}
   */
  copyAlertGroups: function (originGroups) {
    var alertGroups = [];
    originGroups.forEach(function (alertGroup) {
      var copiedGroup =  App.AlertGroupComplex.create($.extend(true, {}, alertGroup));
      alertGroups.pushObject(copiedGroup);
    });
    return alertGroups;
  },

  /**
   * ==============on API side: following are four functions to an alert group: create, delete, update definitions/targets/label of a group ===========
   */
  /**
   * Create a new alert group
   * @param newAlertGroupData
   * @param callback    Callback function for Success or Error handling
   * @return  Returns the created alert group
   */
  postNewAlertGroup: function (newAlertGroupData, callback) {
    // create a new group with name , definition and target
    var data = {
      'name': newAlertGroupData.get('name')
    };
    if (newAlertGroupData.get('definitions').length > 0) {
      data.definitions = newAlertGroupData.get('definitions').mapProperty('id');
    }
    if (newAlertGroupData.get('targets').length > 0) {
      data.targets = newAlertGroupData.get('targets').mapProperty('id');
    }
    var sendData = {
      name: 'alert_groups.create',
      data: data,
      success: 'successFunction',
      error: 'errorFunction',
      successFunction: function () {
        if (callback) {
          callback();
        }
      },
      errorFunction: function (xhr, text, errorThrown) {
        if (callback) {
          callback(xhr, text, errorThrown);
        }
        console.error('Error in creating new Alert Group');
      }
    };
    sendData.sender = sendData;
    App.ajax.send(sendData);
    return newAlertGroupData;
  },

  /**
   * PUTs the new alert group information on the server.
   * Changes possible here are the name, definitions, targets
   *
   * @param {App.ConfigGroup} alertGroup
   * @param {Function} successCallback
   * @param {Function} errorCallback
   */
  updateAlertGroup: function (alertGroup, successCallback, errorCallback) {
    var sendData = {
      name: 'alert_groups.update',
      data: {
        "group_id": alertGroup.id,
        'name': alertGroup.get('name'),
        'definitions': alertGroup.get('definitions').mapProperty('id'),
        'targets': alertGroup.get('targets').mapProperty('id')
      },
      success: 'successFunction',
      error: 'errorFunction',
      successFunction: function () {
        if (successCallback) {
          successCallback();
        }
      },
      errorFunction: function (xhr, text, errorThrown) {
        if (errorCallback) {
          errorCallback(xhr, text, errorThrown);
        }
      }
    };
    sendData.sender = sendData;
    App.ajax.send(sendData);
  },

  removeAlertGroup: function (alertGroup, successCallback, errorCallback) {
    var sendData = {
      name: 'alert_groups.delete',
      data: {
        "group_id": alertGroup.id
      },
      success: 'successFunction',
      error: 'errorFunction',
      successFunction: function () {
        if (successCallback) {
          successCallback();
        }
      },
      errorFunction: function (xhr, text, errorThrown) {
        if (errorCallback) {
          errorCallback(xhr, text, errorThrown);
        }
      }
    };
    sendData.sender = sendData;
    App.ajax.send(sendData);
  },

  /**
   *  =============on UI side: following are four operations to an alert group: add, remove, rename and duplicate==================
   */

  /**
   * confirm delete alert group
   */
  confirmDelete : function () {
    var self = this;
    App.showConfirmationPopup(function() {
      self.deleteAlertGroup();
    });
  },

  /**
   * delete selected alert group
   */
  deleteAlertGroup: function () {
    var selectedAlertGroup = this.get('selectedAlertGroup');
    if (this.get('isDeleteAlertDisabled')) {
      return;
    }
    this.get('alertGroups').removeObject(selectedAlertGroup);
    this.set('selectedAlertGroup', this.get('alertGroups')[0]);
  },

  /**
   * rename non-default alert group
   */
  renameAlertGroup: function () {
    if(this.get('selectedAlertGroup.default')) {
      return;
    }
    var self = this;
    this.renameGroupPopup = App.ModalPopup.show({
      header: Em.I18n.t('alerts.actions.manage_alert_groups_popup.renameButton'),
      bodyClass: Ember.View.extend({
        templateName: require('templates/main/alerts/create_new_alert_group')
      }),
      alertGroupName: self.get('selectedAlertGroup.name'),
      warningMessage: null,
      validate: function () {
        var warningMessage = '';
        var originalGroup = self.get('selectedAlertGroup');
        var groupName = this.get('alertGroupName').trim();

        if (originalGroup.get('name').trim() === groupName) {
          warningMessage = Em.I18n.t("alerts.actions.manage_alert_groups_popup.addGroup.exist");
        } else {
          if (self.get('alertGroups').mapProperty('displayName').contains(groupName)) {
            warningMessage = Em.I18n.t("alerts.actions.manage_alert_groups_popup.addGroup.exist");
          }
          else if (groupName && !validator.isValidAlertGroupName(groupName)) {
            warningMessage = Em.I18n.t("form.validator.alertGroupName");
          }
        }
        this.set('warningMessage', warningMessage);
      }.observes('alertGroupName'),
      disablePrimary: function () {
        return !(this.get('alertGroupName').trim().length > 0 && (this.get('warningMessage') !== null && !this.get('warningMessage')));
      }.property('warningMessage', 'alertGroupName'),
      onPrimary: function () {
        self.set('selectedAlertGroup.name', this.get('alertGroupName'));
        this.hide();
      }
    });
  },

  /**
   * create new alert group
   */
  addAlertGroup: function (duplicated) {
    duplicated = (duplicated === true);
    var self = this;
    this.addGroupPopup = App.ModalPopup.show({
      header: Em.I18n.t('alerts.actions.manage_alert_groups_popup.addButton'),
      bodyClass: Ember.View.extend({
        templateName: require('templates/main/alerts/create_new_alert_group')
      }),
      alertGroupName: duplicated ? self.get('selectedAlertGroup.name') + ' Copy' : "",
      warningMessage: '',
      didInsertElement: function(){
        this.validate();
        this.$('input').focus();
      },
      validate: function () {
        var warningMessage = '';
        var groupName = this.get('alertGroupName').trim();
        if (self.get('alertGroups').mapProperty('displayName').contains(groupName)) {
          warningMessage = Em.I18n.t("alerts.actions.manage_alert_groups_popup.addGroup.exist");
        }
        else if (groupName && !validator.isValidAlertGroupName(groupName)) {
          warningMessage = Em.I18n.t("form.validator.alertGroupName");
        }
        this.set('warningMessage', warningMessage);
      }.observes('alertGroupName'),
      disablePrimary: function () {
        return !(this.get('alertGroupName').trim().length > 0 && !this.get('warningMessage'));
      }.property('warningMessage', 'alertGroupName'),
      onPrimary: function () {
        var newAlertGroup = App.AlertGroupComplex.create({
          name: this.get('alertGroupName').trim(),
          definitions: [],
          targets: []
        })    ;
        self.get('alertGroups').pushObject(newAlertGroup);
        this.hide();
      }
    });
  },

  duplicateAlertGroup: function() {
    this.addAlertGroup(true);
  }

});
