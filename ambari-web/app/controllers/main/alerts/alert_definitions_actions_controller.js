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

App.MainAlertDefinitionActionsController = Em.ArrayController.extend({

  name: 'mainAlertDefinitionActionsController',

  /**
   * List of available actions for alert definitions
   * @type {{title: string, icon: string, action: string, showDivider: boolean}[]}
   */
  content: [
    /*{
      title: Em.I18n.t('alerts.actions.create'),
      icon: 'icon-plus',
      action: 'createNewAlertDefinition',
      showDivider: true
    },*/
    {
      title: Em.I18n.t('alerts.actions.manageGroups'),
      icon: 'icon-th-large',
      action: 'manageAlertGroups',
      showDivider: false
    },
    {
      title: Em.I18n.t('alerts.actions.manageNotifications'),
      icon: 'icon-envelope-alt',
      action: 'manageNotifications',
      showDivider: false
    }
  ],

  /**
   * Common handler for menu item click
   * Call proper controller's method described in <code>action</code>-field (see <code>content</code>)
   * @param {object} event
   * @method actionHandler
   */
  actionHandler: function(event) {
    var menuElement = event.context,
      action = menuElement.action;
    if ('function' === Em.typeOf(Em.get(this, action))) {
      this[action]();
    }
    else {
      console.error('Invalid action provided - ', action);
    }
  },

  /**
   * Start "Create Alert Definition" wizard
   * @method createNewAlertDefinition
   */
  createNewAlertDefinition: function() {
    App.router.transitionTo('alertAdd');
  },

  /**
   * Handler when clicking on "Manage Alert Groups", a popup will show up
   * @method manageAlertGroups
   * @return {App.ModalPopup}
   */
  manageAlertGroups: function () {

    return App.ModalPopup.show({

      header: Em.I18n.t('alerts.actions.manage_alert_groups_popup.header'),

      bodyClass: App.MainAlertsManageAlertGroupView.extend({
        controllerBinding: 'App.router.manageAlertGroupsController'
      }),

      classNames: ['sixty-percent-width-modal', 'manage-alert-group-popup'],
      primary: Em.I18n.t('common.save'),

      onPrimary: function () {
        var modifiedAlertGroups = this.get('subViewController.defsModifiedAlertGroups');
        var dataForSuccessPopup = {
          created: modifiedAlertGroups.toCreate.length,
          deleted: modifiedAlertGroups.toDelete.length,
          updated: modifiedAlertGroups.toSet.length
        };
        var showSuccessPopup = dataForSuccessPopup.created + dataForSuccessPopup.deleted + dataForSuccessPopup.updated > 0;
        // Save modified Alert-groups
        console.log("manageAlertGroups(): Saving modified Alert groups: ", modifiedAlertGroups);
        var self = this;
        var errors = [];
        var deleteQueriesCounter = modifiedAlertGroups.toDelete.length;
        var createQueriesCounter = modifiedAlertGroups.toCreate.length;
        var deleteQueriesRun = false;
        var createQueriesRun = false;
        var runNextQuery = function () {
          if (!deleteQueriesRun && deleteQueriesCounter > 0) {
            deleteQueriesRun = true;
            modifiedAlertGroups.toDelete.forEach(function (group) {
              self.get('subViewController').removeAlertGroup(group, finishFunction, finishFunction);
            }, this);
          } else if (!createQueriesRun && deleteQueriesCounter < 1) {
            createQueriesRun = true;
            modifiedAlertGroups.toSet.forEach(function (group) {
              self.get('subViewController').updateAlertGroup(group, finishFunction, finishFunction);
            }, this);
            modifiedAlertGroups.toCreate.forEach(function (group) {
              self.get('subViewController').postNewAlertGroup(group, finishFunction);
            }, this);
          }
        };
        var finishFunction = function (xhr, text, errorThrown) {
          if (xhr && errorThrown) {
            var error = xhr.status + "(" + errorThrown + ") ";
            try {
              var json = $.parseJSON(xhr.responseText);
              error += json.message;
            } catch (err) {
            }
            console.error('Error updating Alert Group:', error);
            errors.push(error);
          }
          if (createQueriesRun) {
            createQueriesCounter--;
          } else {
            deleteQueriesCounter--;
          }
          if (deleteQueriesCounter + createQueriesCounter < 1) {
            if (errors.length > 0) {
              console.log(errors);
              self.get('subViewController').set('errorMessage', errors.join(". "));
            }
            else {
              self.hide();
              if (showSuccessPopup) {
                App.ModalPopup.show({
                  secondary: null,
                  header: Em.I18n.t('alerts.groups.successPopup.header'),
                  bodyClass: Em.View.extend({
                    dataForSuccessPopup: dataForSuccessPopup,
                    templateName: require('templates/main/alerts/alert_groups/success_popup_body')
                  })
                });
              }
              App.router.get('updateController').updateAlertGroups(function () {
                App.router.get('manageAlertGroupsController').toggleProperty('changeTrigger');
                App.router.get('updateController').updateAlertDefinitions(function() {
                  App.router.get('updateController').updateAlertNotifications(Em.K);
                });
              });
            }
          } else {
            runNextQuery();
          }
        };
        runNextQuery();
      },

      subViewController: function () {
        return App.router.get('manageAlertGroupsController');
      }.property('App.router.manageAlertGroupsController'),

      updateButtons: function () {
        var modified = this.get('subViewController.isDefsModified');
        this.set('disablePrimary', !modified);
      }.observes('subViewController.isDefsModified'),

      didInsertElement: function () {
        this.fitZIndex();
      }

    });
  },

  /**
   * "Manage Alert Notifications" handler
   * @method manageNotifications
   * @return {App.ModalPopup}
   */
  manageNotifications: function () {

    return App.ModalPopup.show({

      header: Em.I18n.t('alerts.actions.manage_alert_notifications_popup.header'),

      bodyClass: App.ManageAlertNotificationsView.extend({
        controllerBinding: 'App.router.manageAlertNotificationsController'
      }),

      classNames: ['sixty-percent-width-modal', 'manage-configuration-group-popup'],

      secondary: null,

      primary: Em.I18n.t('common.close'),

      autoHeight: false

    });
  }

});