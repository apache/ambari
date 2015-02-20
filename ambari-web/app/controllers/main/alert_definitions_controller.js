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

App.MainAlertDefinitionsController = Em.ArrayController.extend({

  name: 'mainAlertDefinitionsController',

  /**
   * Timestamp when <code>App.alertDefinitionsMapper</code> run last time
   * Current <code>content</code> is updated on when it changed
   * @type {number|null}
   */
  mapperTimestamp: null,

  /**
   * Define whether restore filter conditions from local db
   * @type {Boolean}
   */
  showFilterConditionsFirstLoad: false,

  /**
   * List of all <code>App.AlertDefinition</code>
   * Consists of:
   * <ul>
   *   <li>App.PortAlertDefinition</li>
   *   <li>App.MetricsAlertDefinition</li>
   *   <li>App.WebAlertDefinition</li>
   *   <li>App.AggregateAlertDefinition</li>
   *   <li>App.ScriptAlertDefinition</li>
   * </ul>
   * @type {App.AlertDefinition[]}
   */
  content: App.AlertDefinition.find(),

  /**
   * Enable/disable alertDefinition confirmation popup
   * @param {object} event
   * @method toggleState
   * @return {App.ModalPopup}
   */
  toggleState: function (event) {
    var alertDefinition = event.context;
    var self = this;
    var bodyMessage = Em.Object.create({
      confirmMsg: alertDefinition.get('enabled') ? Em.I18n.t('alerts.table.state.enabled.confirm.msg') : Em.I18n.t('alerts.table.state.disabled.confirm.msg'),
      confirmButton: alertDefinition.get('enabled') ? Em.I18n.t('alerts.table.state.enabled.confirm.btn') : Em.I18n.t('alerts.table.state.disabled.confirm.btn')
    });

    return App.showConfirmationFeedBackPopup(function (query) {
      self.toggleDefinitionState(alertDefinition);
    }, bodyMessage);
  },

  /**
   * Enable/disable alertDefinition
   * @param {object} alertDefinition
   * @returns {$.ajax}
   * @method toggleDefinitionState
   */
  toggleDefinitionState: function (alertDefinition) {
    var newState = !alertDefinition.get('enabled');
    alertDefinition.set('enabled', newState);
    return App.ajax.send({
      name: 'alerts.update_alert_definition',
      sender: this,
      data: {
        id: alertDefinition.get('id'),
        data: {
          "AlertDefinition/enabled": newState
        }
      }
    });
  },

  /**
   * Calculate critical count for each service, to show up the label on services menu
   * @method getCriticalAlertsCountForService
   * @return {number}
   */
  getCriticalAlertsCountForService: function (service) {
    return this.get('criticalAlertInstances').filterProperty('service', service).get('length');
  },

  /**
   * Calculate critical/warning count for each service, to show up the label on services menu
   * @method getCriticalOrWarningAlertsCountForService
   * @return {number}
   */
  getCriticalOrWarningAlertsCountForService: function (service) {
    return this.get('unhealthyAlertInstances').filterProperty('service', service).get('length');
  },

  /**
   *  ========================== alerts popup dialog =========================
   */

  /**
   * Alerts number to show up on top-nav bar: number of critical/warning alerts
   * @type {number}
   */
  allAlertsCount: function () {
    return this.get('unhealthyAlertInstances.length');
  }.property('unhealthyAlertInstances.length'),

  criticalAlertInstances: function () {
    return App.AlertInstance.find().toArray().filterProperty('state', 'CRITICAL');
  }.property('mapperTimestamp'),

  warningAlertInstances: function () {
    return App.AlertInstance.find().toArray().filterProperty('state', 'WARNING');
  }.property('mapperTimestamp'),

  unhealthyAlertInstances: function () {
    return this.get('criticalAlertInstances').concat(this.get('warningAlertInstances'));
  }.property('criticalAlertInstances', 'warningAlertInstances'),

  /**
   * if critical alerts exist, if true, the alert badge should be red.
   */
  isCriticalAlerts: function () {
    return this.get('unhealthyAlertInstances').someProperty('state', 'CRITICAL');
  }.property('unhealthyAlertInstances.@each.state'),

  /**
   * Onclick handler for alerts number located right to bg ops number (see application.hbs)
   * @method showPopup
   * @return {App.ModalPopup}
   */
  showPopup: function () {

    var self = this;

    return App.ModalPopup.show({

      header: function () {
        return Em.I18n.t('alerts.fastAccess.popup.header').format(this.get('definitionsController.allAlertsCount'));
      }.property('definitionsController.allAlertsCount'),

      definitionsController: this,

      classNames: ['sixty-percent-width-modal', 'alerts-popup'],

      secondary: Em.I18n.t('alerts.fastAccess.popup.body.showmore'),

      autoHeight: false,

      isHideBodyScroll: true,

      onSecondary: function () {
        this.hide();
        App.router.transitionTo('main.alerts.index');
      },

      bodyClass: Em.View.extend({

        templateName: require('templates/common/modal_popups/alerts_popup'),

        controller: self,

        contents: function () {
          return this.get('controller.unhealthyAlertInstances');
        }.property('controller.unhealthyAlertInstances.length', 'controller.unhealthyAlertInstances.@each.state'),

        isLoaded: function () {
          return !!this.get('controller.unhealthyAlertInstances');
        }.property('controller.unhealthyAlertInstances'),

        isAlertEmptyList: function () {
          return !this.get('contents.length');
        }.property('contents.length'),

        /**
         * Router transition to alert definition details page
         * @param event
         */
        gotoAlertDetails: function (event) {
          if (event && event.context) {
            this.get('parentView').hide();
            var definition = this.get('controller.content').findProperty('id', event.context.get('definitionId'));
            App.router.transitionTo('main.alerts.alertDetails', definition);
          }
        },

        /**
         * Router transition to service summary page
         * @param event
         */
        gotoService: function (event) {
          if (event && event.context) {
            this.get('parentView').hide();
            App.router.transitionTo('main.services.service.summary', event.context);
          }
        },

        /**
         * Router transition to host level alerts page
         * @param event
         */
        goToHostAlerts: function (event) {
          if (event && event.context) {
            this.get('parentView').hide();
            App.router.transitionTo('main.hosts.hostDetails.alerts', event.context);
          }
        },

        didInsertElement: function () {
          Em.run.next(this, function () {
            App.tooltip($(".timeago"));
          });
        }
      })
    });
  }
});
