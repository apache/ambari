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

App.MainAlertInstancesController = Em.Controller.extend({

  name: 'mainAlertInstancesController',

  content: App.AlertInstance.find(),

  /**
   * @type {App.AlertInstance[]}
   */
  unhealthyAlertInstances: [],

  updateUnhealthyAlertInstances: function () {
    Em.run.once(this, this.updateUnhealthyAlertInstancesOnce);
  }.observes('content.[]'),

  updateUnhealthyAlertInstancesOnce: function() {
    var alertInstances = App.AlertInstance.find().filter(function (item) {
      return ['CRITICAL', 'WARNING'].contains(item.get('state'));
    });
    this.set('unhealthyAlertInstances', alertInstances);
  },

  /**
   * Are alertInstances loaded
   * @type {boolean}
   */
  isLoaded: false,

  /**
   * A flag to reload alert instances table every 10 seconds
   * @type {boolean}
   */
  reload: false,

  /**
   * Causes automatic updates of content if set to true
   * @type {boolean}
   */
  isUpdating: false,

  /**
   * Times for alert instances updater
   * Used in <code>scheduleUpdate</code>
   * @type {number|null}
   */
  updateTimer: null,

  /**
   * @type {string|null} sourceName - hostName or alertDefinitionId
   */
  sourceName: null,

  /**
   * @type {string|null} sourceType - 'HOST'|'ALERT_DEFINITION'
   */
  sourceType: null,

  /**
   * Load alert instances from server (all, for selected host, for selected alert definition)
   * @returns {$.ajax}
   * @method fetchAlertInstances
   */
  fetchAlertInstances: function () {
    var sourceType = this.get('sourceType'),
      sourceName = this.get('sourceName'),
      ajaxData = {
        sender: this,
        success: 'getAlertInstancesSuccessCallback',
        error: 'getAlertInstancesErrorCallback'
      };

    switch (sourceType) {
      case 'HOST':
        $.extend(ajaxData, {
          name: 'alerts.instances.by_host',
          data: {
            hostName: sourceName
          }
        });
        break;

      case 'ALERT_DEFINITION':
        $.extend(ajaxData, {
          name: 'alerts.instances.by_definition',
          data: {
            definitionId: sourceName
          }
        });
        break;

      default:
        $.extend(ajaxData, {
          name: 'alerts.instances'
        });
        break;
    }

    return App.ajax.send(ajaxData);
  },

  /**
   * Pseudo for <code>fetchAlertInstances</code>
   * Used to get all alert instances
   * @method loadAlertInstances
   */
  loadAlertInstances: function () {
    this.setProperties({
      isLoaded: false,
      sourceType: null,
      sourceName: null
    });
    this.fetchAlertInstances();
  },

  /**
   * Pseudo for <code>fetchAlertInstances</code>
   * Used to get alert instances for some host
   * @param {string} hostName
   * @method loadAlertInstancesByHost
   */
  loadAlertInstancesByHost: function (hostName) {
    this.setProperties({
      isLoaded: false,
      sourceType: 'HOST',
      sourceName: hostName
    });
    this.fetchAlertInstances();
  },

  /**
   * Pseudo for <code>fetchAlertInstances</code>
   * Used to get alert instances for some alert definition
   * @param {string} definitionId
   * @method loadAlertInstancesByAlertDefinition
   */
  loadAlertInstancesByAlertDefinition: function (definitionId) {
    this.setProperties({
      isLoaded: false,
      sourceType: 'ALERT_DEFINITION',
      sourceName: definitionId
    });
    this.fetchAlertInstances();
  },

  scheduleUpdate: function () {
    var self = this;
    if (this.get('isUpdating')) {
      this.set('updateTimer', setTimeout(function () {
        self.fetchAlertInstances().complete(function() {
          self.scheduleUpdate();
        });
      }, App.get('alertInstancesUpdateInterval')));
    }
    else {
      clearTimeout(this.get('updateTimer'));
    }
  }.observes('isUpdating'),

  /**
   * Success-callback for alert instances request
   * @param {object} json
   * @method getAlertInstancesSuccessCallback
   */
  getAlertInstancesSuccessCallback: function (json) {
    App.alertInstanceMapper.mapLocal(json);
    this.set('isLoaded', true);
    this.toggleProperty('reload');
  },

  /**
   * Error-callback for alert instances request
   * @method getAlertInstancesErrorCallback
   */
  getAlertInstancesErrorCallback: function () {
    this.set('isLoaded', true);
  },

  /**
   * Onclick handler for alerts number located right to bg ops number (see application.hbs)
   * @method showPopup
   * @return {App.ModalPopup}
   */
  showPopup: function () {

    var self = this;

    return App.ModalPopup.show({

      alertsNumberBinding: 'App.router.mainAlertDefinitionsController.unhealthyAlertInstancesCount',

      header: function () {
        return Em.I18n.t('alerts.fastAccess.popup.header').format(this.get('alertsNumber'));
      }.property('alertsNumber'),

      classNames: ['sixty-percent-width-modal', 'alerts-popup'],

      secondary: Em.I18n.t('alerts.fastAccess.popup.body.showmore'),

      autoHeight: false,

      isHideBodyScroll: true,

      onSecondary: function () {
        this._super();
        App.router.transitionTo('main.alerts.index');
      },

      bodyClass: App.TableView.extend(App.TableServerViewMixin, {

        updaterBinding: 'App.router.updateController',

        templateName: require('templates/common/modal_popups/alerts_popup'),

        controller: self,

        isPaginate: true,

        willInsertElement: function () {
          this._super();
          this.updateAlertInstances();
        },

        /**
         * Number of all critical and warning alert instances
         * @type {Boolean}
         */
        filteredCount: function () {
          return App.router.get('mainAlertDefinitionsController.unhealthyAlertInstancesCount');
        }.property('alertsNumber'),

        content: function () {
          return this.get('controller.unhealthyAlertInstances');
        }.property('controller.unhealthyAlertInstances.@each.state'),

        isLoaded: function () {
          return !!this.get('controller.unhealthyAlertInstances');
        }.property('controller.unhealthyAlertInstances'),

        isAlertEmptyList: function () {
          return !this.get('content.length');
        }.property('content.length'),

        /**
         * Update list of shown alert instances
         * @method updateAlertInstances
         */
        updateAlertInstances: function () {
          var self = this,
            displayLength = this.get('displayLength'),
            startIndex = this.get('startIndex');
          if (!displayLength) return; // wait while table-info is loaded
          this.get('updater').set('queryParamsForUnhealthyAlertInstances', {
            from: startIndex - 1,
            page_size: displayLength
          });
          this.set('filteringComplete', false);
          this.get('updater').updateUnhealthyAlertInstances(function() {
            self.set('filteringComplete', true);
          });
        }.observes('displayLength', 'startIndex'),

        /**
         * Show spinner when filter/sorting request is in processing
         * @method overlayObserver
         */
        overlayObserver: function() {
          var $tbody = this.$('#alert-info'),
            $overlay = this.$('.table-overlay'),
            $spinner = $($overlay).find('.spinner');
          if (!this.get('filteringComplete')) {
            if (!$tbody) return;
            var tbodyPos =  $tbody.position();
            if (!tbodyPos) return;
            $spinner.css('display', 'block');
            $overlay.css({
              top: tbodyPos.top + 1,
              left: tbodyPos.left + 1,
              width: $tbody.width() - 1,
              height: $tbody.height() - 1
            });
          }
        },

        /**
         * No filtering for alert definitions
         * @method filter
         */
        filter: function() {
          this.set('filteredContent', this.get('content'));
        }.observes('content.length'),

        /**
         * Router transition to alert definition details page
         * @param event
         */
        gotoAlertDetails: function (event) {
          if (event && event.context) {
            this.get('parentView').hide();
            var definition = App.AlertDefinition.find().findProperty('id', event.context.get('definitionId'));
            App.router.transitionTo('main.alerts.alertDetails', definition);
          }
        },

        /**
         * Router transition to service summary page
         * @param event
         */
        goToService: function (event) {
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
          this.filter();
          this.addObserver('filteringComplete', this, this.overlayObserver);
          this.overlayObserver();
          return this._super();
        }
      })
    });
  }

});
