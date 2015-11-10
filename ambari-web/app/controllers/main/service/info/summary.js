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

var App = require('app');

App.MainServiceInfoSummaryController = Em.Controller.extend(App.WidgetSectionMixin, {
  name: 'mainServiceInfoSummaryController',

  selectedFlumeAgent: null,

  /**
   * Indicates whether Ranger plugins status update polling is active
   * @type {boolean}
   */
  isRangerUpdateWorking: false,

  /**
   * Indicates whether array with initial Ranger plugins data is set
   * @type {boolean}
   */
  isRangerPluginsArraySet: false,

  /**
   * Indicates whether previous AJAX request for Ranger plugins config properties has failed
   * @type {boolean}
   */
  isPreviousRangerConfigsCallFailed: false,

  layoutNameSuffix: "_dashboard",

  sectionNameSuffix: "_SUMMARY",

  /**
   * Ranger plugins data
   * @type {array}
   */
  rangerPlugins: [
    {
      serviceName: 'HDFS',
      type: 'ranger-hdfs-plugin-properties',
      propertyName: 'ranger-hdfs-plugin-enabled',
      valueForEnable: 'Yes'
    },
    {
      serviceName: 'YARN',
      type: 'ranger-yarn-plugin-properties',
      propertyName: 'ranger-yarn-plugin-enabled',
      valueForEnable: 'Yes'
    },
    {
      serviceName: 'HBASE',
      type: 'ranger-hbase-plugin-properties',
      propertyName: 'ranger-hbase-plugin-enabled',
      valueForEnable: 'Yes'
    },
    {
      serviceName: 'HIVE',
      type: 'hive-env',
      propertyName: 'hive_security_authorization',
      valueForEnable: 'Ranger'
    },
    {
      serviceName: 'KNOX',
      type: 'ranger-knox-plugin-properties',
      propertyName: 'ranger-knox-plugin-enabled',
      valueForEnable: 'Yes'
    },
    {
      serviceName: 'STORM',
      type: 'ranger-storm-plugin-properties',
      propertyName: 'ranger-storm-plugin-enabled',
      valueForEnable: 'Yes'
    },
    {
      serviceName: 'KAFKA',
      type: 'ranger-kafka-plugin-properties',
      propertyName: 'ranger-kafka-plugin-enabled',
      valueForEnable: 'Yes'
    }
  ],

  /**
   * @type {boolean}
   */
  showTimeRangeControl: function () {
    return !this.get('isServiceWithEnhancedWidgets') || this.get('widgets').filterProperty('widgetType', 'GRAPH').length > 0;
  }.property('isServiceWithEnhancedWidgets', 'widgets.length'),

  /**
   * Set initial Ranger plugins data
   * @method setRangerPlugins
   */
  setRangerPlugins: function () {
    if (App.get('router.clusterController.isLoaded') && !this.get('isRangerPluginsArraySet')) {
      // Display order of ranger plugin for services should be decided from  App.StackService.displayOrder to keep consistency
      // with display order of services at other places in the application like `select service's page` and `service menu bar`
      var displayOrderLength = App.StackService.displayOrder.length;
      var rangerPlugins = this.get('rangerPlugins').map(function (item, index) {
        var displayOrderIndex = App.StackService.displayOrder.indexOf(item.serviceName);
        return $.extend(item, {
          index: displayOrderIndex == -1 ? displayOrderLength + index : displayOrderIndex
        });
      }).sortProperty('index');

      this.setProperties({
        rangerPlugins: rangerPlugins.map(function (item) {
          var stackService = App.StackService.find().findProperty('serviceName', item.serviceName);
          var displayName = (stackService) ? stackService.get('displayName') : item.serviceName;
          return $.extend(item, {
            pluginTitle: Em.I18n.t('services.service.summary.ranger.plugin.title').format(displayName),
            isDisplayed: App.Service.find().someProperty('serviceName', item.serviceName) &&
              stackService.get('configTypes').hasOwnProperty(item.type),
            status: Em.I18n.t('services.service.summary.ranger.plugin.loadingStatus')
          });
        }),
        isRangerPluginsArraySet: true
      });
    }
  }.observes('App.router.clusterController.isLoaded'),

  /**
   * Get latest config tags
   * @method updateRangerPluginsStatus
   * @param callback
   */
  updateRangerPluginsStatus: function (callback) {
    App.ajax.send({
      name: 'config.tags',
      sender: this,
      success: 'getRangerPluginsStatus',
      callback: callback
    });
  },

  /**
   * Get latest Ranger plugins config properties
   * @method getRangerPluginsStatus
   * @param data
   */
  getRangerPluginsStatus: function (data) {
    var urlParams = [];
    this.get('rangerPlugins').forEach(function (item) {
      if (App.Service.find().someProperty('serviceName', item.serviceName) && data.Clusters.desired_configs.hasOwnProperty(item.type)) {
        var currentTag = data.Clusters.desired_configs[item.type].tag;
        var isTagChanged = item.tag != currentTag;
        Em.set(item, 'isDisplayed', true);
        //Request for properties should be sent either if configs have changed or if previous Ranger plugins config properties has failed
        if (isTagChanged || this.get('isPreviousRangerConfigsCallFailed')) {
          Em.set(item, 'tag', currentTag);
          urlParams.push('(type=' + item.type + '&tag=' + currentTag + ')');
        }
      } else {
        Em.set(item, 'isDisplayed', false);
      }
    }, this);
    if (urlParams.length) {
      App.ajax.send({
        name: 'reassign.load_configs',
        sender: this,
        data: {
          urlParams: urlParams.join('|')
        },
        success: 'getRangerPluginsStatusSuccess',
        error: 'getRangerPluginsStatusError'
      });
    }
  },

  /**
   * Set Ranger plugins statuses
   * @method getRangerPluginsStatusSuccess
   * @param data
   */
  getRangerPluginsStatusSuccess: function (data) {
    this.set('isPreviousRangerConfigsCallFailed', false);
    data.items.forEach(function (item) {
      var serviceName = this.get('rangerPlugins').findProperty('type', item.type).serviceName;
      var propertyName = this.get('rangerPlugins').findProperty('type', item.type).propertyName;
      var propertyValue = this.get('rangerPlugins').findProperty('type', item.type).valueForEnable;
      var statusString;

      if (item.properties[propertyName]) {
        statusString = item.properties[propertyName] == propertyValue ? 'alerts.table.state.enabled' : 'alerts.table.state.disabled';
      }
      else {
        statusString = 'common.unknown';
      }
      Em.set(this.get('rangerPlugins').findProperty('serviceName', serviceName), 'status', Em.I18n.t(statusString));
    }, this);
  },

  /**
   * Method executed if Ranger plugins config properties request has failed
   * @method getRangerPluginsStatusError
   */
  getRangerPluginsStatusError: function () {
    this.set('isPreviousRangerConfigsCallFailed', true);
  },

  /**
   * Send start command for selected Flume Agent
   * @method startFlumeAgent
   */
  startFlumeAgent: function () {
    var selectedFlumeAgent = arguments[0].context;
    if (selectedFlumeAgent && selectedFlumeAgent.get('status') === 'NOT_RUNNING') {
      var self = this;
      App.showConfirmationPopup(function () {
        var state = 'STARTED';
        var context = Em.I18n.t('services.service.summary.flume.start.context').format(selectedFlumeAgent.get('name'));
        self.sendFlumeAgentCommandToServer(state, context, selectedFlumeAgent);
      });
    }
  },

  /**
   * Send stop command for selected Flume Agent
   * @method stopFlumeAgent
   */
  stopFlumeAgent: function () {
    var selectedFlumeAgent = arguments[0].context;
    if (selectedFlumeAgent && selectedFlumeAgent.get('status') === 'RUNNING') {
      var self = this;
      App.showConfirmationPopup(function () {
        var state = 'INSTALLED';
        var context = Em.I18n.t('services.service.summary.flume.stop.context').format(selectedFlumeAgent.get('name'));
        self.sendFlumeAgentCommandToServer(state, context, selectedFlumeAgent);
      });
    }
  },

  /**
   * Send command for Flume Agent to server
   * @param {string} state
   * @param {string} context
   * @param {Object} agent
   * @method sendFlumeAgentCommandToServer
   */
  sendFlumeAgentCommandToServer: function (state, context, agent) {
    App.ajax.send({
      name: 'service.flume.agent.command',
      sender: this,
      data: {
        state: state,
        context: context,
        agentName: agent.get('name'),
        host: agent.get('hostName')
      },
      success: 'commandSuccessCallback'
    });
  },

  /**
   * Callback, that shows Background operations popup if request was successful
   */
  commandSuccessCallback: function () {
    console.log('Send request for refresh configs successfully');
    // load data (if we need to show this background operations popup) from persist
    App.router.get('userSettingsController').dataLoading('show_bg').done(function (showPopup) {
      if (showPopup) {
        App.router.get('backgroundOperationsController').showPopup();
      }
    });
  },

  gotoConfigs: function () {
    App.router.get('mainServiceItemController').set('routeToConfigs', true);
    App.router.transitionTo('main.services.service.configs', this.get('content'));
    App.router.get('mainServiceItemController').set('routeToConfigs', false);
  },

  showServiceAlertsPopup: function (event) {
    var service = event.context;
    return App.ModalPopup.show({
      header: Em.I18n.t('services.service.summary.alerts.popup.header').format(service.get('displayName')),
      autoHeight: false,
      classNames: ['forty-percent-width-modal'],
      bodyClass: Em.View.extend({
        templateName: require('templates/main/service/info/service_alert_popup'),
        classNames: ['service-alerts'],
        controllerBinding: 'App.router.mainAlertDefinitionsController',
        didInsertElement: function () {
          Em.run.next(this, function () {
            App.tooltip(this.$(".timeago"));
          });
        },
        willDestroyElement:function () {
          this.$(".timeago").tooltip('destroy');
        },
        alerts: function () {
          var serviceDefinitions = this.get('controller.content').filterProperty('service', service);
          // definitions should be sorted in order: critical, warning, ok, unknown, other
          var criticalDefinitions = [], warningDefinitions = [], okDefinitions = [], unknownDefinitions = [];
          serviceDefinitions.forEach(function (definition) {
            if (definition.get('isCritical')) {
              criticalDefinitions.push(definition);
              serviceDefinitions = serviceDefinitions.without(definition);
            } else if (definition.get('isWarning')) {
              warningDefinitions.push(definition);
              serviceDefinitions = serviceDefinitions.without(definition);
            } else if (definition.get('isOK')) {
              okDefinitions.push(definition);
              serviceDefinitions = serviceDefinitions.without(definition);
            } else if (definition.get('isUnknown')) {
              unknownDefinitions.push(definition);
              serviceDefinitions = serviceDefinitions.without(definition);
            }
          });
          serviceDefinitions = criticalDefinitions.concat(warningDefinitions, okDefinitions, unknownDefinitions, serviceDefinitions);
          return serviceDefinitions;
        }.property('controller.content'),
        gotoAlertDetails: function (event) {
          if (event && event.context) {
            this.get('parentView').hide();
            App.router.transitionTo('main.alerts.alertDetails', event.context);
          }
        },
        closePopup: function () {
          this.get('parentView').hide();
        }
      }),
      isHideBodyScroll: false,
      primary: Em.I18n.t('common.close'),
      secondary: null
    });
  },


  /**
   * @type {boolean}
   */
  isWidgetLayoutsLoaded: false,

  /**
   * @type {boolean}
   */
  isAllSharedWidgetsLoaded: false,

  /**
   * @type {boolean}
   */
  isMineWidgetsLoaded: false,


  /**
   * load widget layouts across all users in CLUSTER scope
   * @returns {$.ajax}
   */
  loadWidgetLayouts: function () {
    this.set('isWidgetLayoutsLoaded', false);
    return App.ajax.send({
      name: 'widgets.layouts.get',
      sender: this,
      data: {
        sectionName: this.get('sectionName')
      },
      success: 'loadWidgetLayoutsSuccessCallback'
    });
  },

  loadWidgetLayoutsSuccessCallback: function (data) {
    App.widgetLayoutMapper.map(data);
    this.set('isWidgetLayoutsLoaded', true);
  },


  /**
   * load all shared widgets to show on widget browser
   * @returns {$.ajax}
   */
  loadAllSharedWidgets: function () {
    this.set('isAllSharedWidgetsLoaded', false);
    return App.ajax.send({
      name: 'widgets.all.shared.get',
      sender: this,
      success: 'loadAllSharedWidgetsSuccessCallback'
    });
  },

  /**
   * success callback of <code>loadAllSharedWidgets</code>
   * @param {object|null} data
   */
  loadAllSharedWidgetsSuccessCallback: function (data) {
    var widgetIds = this.get('widgets').mapProperty('id');
    if (data.items[0] && data.items.length) {
      this.set("allSharedWidgets",
        data.items.filter(function (widget) {
          return widget.WidgetInfo.widget_type != "HEATMAP";
        }).map(function (widget) {
          var widgetType = widget.WidgetInfo.widget_type;
          var widgetName = widget.WidgetInfo.widget_name;
          var widgetId =  widget.WidgetInfo.id;
          return Em.Object.create({
            id: widgetId,
            widgetName: widgetName,
            description: widget.WidgetInfo.description,
            widgetType: widgetType,
            iconPath: "/img/widget-" + widgetType.toLowerCase() + ".png",
            serviceName: JSON.parse(widget.WidgetInfo.metrics).mapProperty('service_name').uniq().join('-'),
            added: widgetIds.contains(widgetId),
            isShared: widget.WidgetInfo.scope == "CLUSTER"
          });
        })
      );
    }
    this.set('isAllSharedWidgetsLoaded', true);
  },

  allSharedWidgets: [],
  mineWidgets: [],

  /**
   * load all mine widgets of current user to show on widget browser
   * @returns {$.ajax}
   */
  loadMineWidgets: function () {
    this.set('isMineWidgetsLoaded', false);
    return App.ajax.send({
      name: 'widgets.all.mine.get',
      sender: this,
      data: {
        loginName: App.router.get('loginName')
      },
      success: 'loadMineWidgetsSuccessCallback'
    });
  },

  /**
   * success callback of <code>loadMineWidgets</code>
   * @param {object|null} data
   */
  loadMineWidgetsSuccessCallback: function (data) {
    var widgetIds = this.get('widgets').mapProperty('id');
    if (data.items[0] && data.items.length) {
      this.set("mineWidgets",
        data.items.filter(function (widget) {
          return widget.WidgetInfo.widget_type != "HEATMAP";
        }).map(function (widget) {
          var widgetType = widget.WidgetInfo.widget_type;
          var widgetName = widget.WidgetInfo.widget_name;
          var widgetId =  widget.WidgetInfo.id;
          return Em.Object.create({
            id: widget.WidgetInfo.id,
            widgetName: widgetName,
            description: widget.WidgetInfo.description,
            widgetType: widgetType,
            iconPath: "/img/widget-" + widgetType.toLowerCase() + ".png",
            serviceName: JSON.parse(widget.WidgetInfo.metrics).mapProperty('service_name').uniq().join('-'),
            added: widgetIds.contains(widgetId),
            isShared: widget.WidgetInfo.scope == "CLUSTER"
          });
        })
      );
    } else {
      this.set("mineWidgets", []);
    }
    this.set('isMineWidgetsLoaded', true);
  },

  /**
   * add widgets, on click handler for "Add"
   */
  addWidget: function (event) {
    var widgetToAdd = event.context;
    var activeLayout = this.get('activeWidgetLayout');
    var widgetIds = activeLayout.get('widgets').map(function(widget) {
      return {
        "id": widget.get("id")
      }
    });
    widgetIds.pushObject({
      "id": widgetToAdd.id
    });
    var data = {
      "WidgetLayoutInfo": {
        "display_name": activeLayout.get("displayName"),
        "id": activeLayout.get("id"),
        "layout_name": activeLayout.get("layoutName"),
        "scope": activeLayout.get("scope"),
        "section_name": activeLayout.get("sectionName"),
        "widgets": widgetIds
      }
    };

    widgetToAdd.set('added', !widgetToAdd.added);
    return App.ajax.send({
      name: 'widget.layout.edit',
      sender: this,
      data: {
        layoutId: activeLayout.get("id"),
        data: data
      },
      success: 'updateActiveLayout'
    });
  },

  /**
   * hide widgets, on click handler for "Added"
   */
  hideWidget: function (event) {
    var widgetToHide = event.context;
    var activeLayout = this.get('activeWidgetLayout');
    var widgetIds = activeLayout.get('widgets').map(function (widget) {
      return {
        "id": widget.get("id")
      }
    });
    var data = {
      "WidgetLayoutInfo": {
        "display_name": activeLayout.get("displayName"),
        "id": activeLayout.get("id"),
        "layout_name": activeLayout.get("layoutName"),
        "scope": activeLayout.get("scope"),
        "section_name": activeLayout.get("sectionName"),
        "widgets": widgetIds.filter(function (widget) {
          return widget.id !== widgetToHide.id;
        })
      }
    };

    widgetToHide.set('added', !widgetToHide.added);
    return App.ajax.send({
      name: 'widget.layout.edit',
      sender: this,
      data: {
        layoutId: activeLayout.get("id"),
        data: data
      },
      success: 'hideWidgetSuccessCallback'
    });

  },

  /**
   * @param {object|null} data
   * @param {object} opt
   * @param {object} params
   */
  hideWidgetSuccessCallback: function (data, opt, params) {
    params.data.WidgetLayoutInfo.widgets = params.data.WidgetLayoutInfo.widgets.map(function (widget) {
      return {
        WidgetInfo: {
          id: widget.id
        }
      }
    });
    App.widgetLayoutMapper.map({items: [params.data]});
    this.propertyDidChange('widgets');
  },

  /**
   * update current active widget layout
   */
  updateActiveLayout: function () {
    this.getActiveWidgetLayout();
  },

  /**
   * delete widgets, on click handler for "Delete"
   */
  deleteWidget: function (event) {
    var widget = event.context;
    var self = this;
    var confirmMsg =  widget.get('isShared') ? Em.I18n.t('dashboard.widgets.browser.action.delete.shared.bodyMsg').format(widget.widgetName) :  Em.I18n.t('dashboard.widgets.browser.action.delete.mine.bodyMsg').format(widget.widgetName);
    var bodyMessage = Em.Object.create({
      confirmMsg: confirmMsg,
      confirmButton: Em.I18n.t('dashboard.widgets.browser.action.delete.btnMsg')
    });
    return App.showConfirmationFeedBackPopup(function (query) {
      return App.ajax.send({
        name: 'widget.action.delete',
        sender: self,
        data: {
          id: widget.id
        },
        success: 'updateWidgetBrowser'
      });

    }, bodyMessage);
  },

  /**
   * update widget browser content after deleted some widget
   */
  updateWidgetBrowser: function () {
    this.loadAllSharedWidgets();
    this.loadMineWidgets();
  },

  /**
   * Share widgets, on click handler for "Share"
   */
  shareWidget: function (event) {
    var widget = event.context;
    var self = this;
    var bodyMessage = Em.Object.create({
      confirmMsg: Em.I18n.t('dashboard.widgets.browser.action.share.confirmation'),
      confirmButton: Em.I18n.t('dashboard.widgets.browser.action.share')
    });
    return App.showConfirmationFeedBackPopup(function (query) {
      return App.ajax.send({
        name: 'widgets.wizard.edit',
        sender: self,
        data: {
          data: {
            "WidgetInfo": {
              "widget_name": widget.get("widgetName"),
              "scope": "CLUSTER"
            }
          },
          widgetId: widget.get("id")
        },
        success: 'updateWidgetBrowser'
      });
    }, bodyMessage);
  },

  /**
   * create widget
   */
  createWidget: function () {
    App.router.send('createServiceWidget', Em.Object.create({
      layout: this.get('activeWidgetLayout'),
      serviceName: this.get('content.serviceName')
    }));
  },

  /**
   * edit widget
   * @param {App.Widget} content
   */
  editWidget: function (content) {
    content.set('serviceName', this.get('content.serviceName'));
    App.router.send('editServiceWidget', content);
  },

  /**
   * launch Widgets Browser popup
   * @method showPopup
   * @return {App.ModalPopup}
   */
  goToWidgetsBrowser: function () {
    var self = this;

    return App.ModalPopup.show({
      header: function () {
        return Em.I18n.t('dashboard.widgets.browser.header');
      }.property(''),

      classNames: ['sixty-percent-width-modal', 'widgets-browser-popup'],
      onPrimary: function () {
        this.hide();
        self.set('isAllSharedWidgetsLoaded', false);
        self.set('allSharedWidgets', []);
        self.set('isMineWidgetsLoaded', false);
        self.set('mineWidgets', []);
      },
      autoHeight: false,
      isHideBodyScroll: false,
      footerClass: Ember.View.extend({
        templateName: require('templates/common/modal_popups/widget_browser_footer'),
        isShowMineOnly: false,
        onPrimary: function() {
          this.get('parentView').onPrimary();
        }
      }),
      isShowMineOnly: false,
      bodyClass: Ember.View.extend({
        templateName: require('templates/common/modal_popups/widget_browser_popup'),
        controller: self,
        willInsertElement: function () {
          this.get('controller').loadAllSharedWidgets();
          this.get('controller').loadMineWidgets();
        },

        isLoaded: function () {
          return !!(this.get('controller.isAllSharedWidgetsLoaded') && this.get('controller.isMineWidgetsLoaded'));
        }.property('controller.isAllSharedWidgetsLoaded', 'controller.isMineWidgetsLoaded'),

        isWidgetEmptyList: function () {
          return !this.get('filteredContent.length');
        }.property('filteredContent.length'),

        activeService: '',
        activeStatus: '',

        content: function () {
          if (this.get('parentView.isShowMineOnly')) {
            return this.get('controller.mineWidgets');
          } else {
            // merge my widgets and all shared widgets, no duplicated is allowed
            var content = [];
            var widgetMap = {};
            var allWidgets = this.get('controller.allSharedWidgets').concat(this.get('controller.mineWidgets'));
            allWidgets.forEach(function(widget) {
              if (!widgetMap[widget.get("id")]) {
                content.pushObject(widget);
                widgetMap[widget.get("id")] = true;
              }
            });
            return content;
          }
        }.property('controller.allSharedWidgets.length', 'controller.isAllSharedWidgetsLoaded',
          'controller.mineWidgets.length', 'controller.isMineWidgetsLoaded', 'parentView.isShowMineOnly'),

        /**
         * displaying content filtered by service name and status.
         */
        filteredContent: function () {
          var activeService = this.get('activeService') ? this.get('activeService') : this.get('controller.content.serviceName');
          var result = [];
          this.get('content').forEach(function (widget) {
            if (widget.get('serviceName').indexOf(activeService) >= 0) {
              result.pushObject(widget);
            }
          });
          return result;
        }.property('content', 'activeService', 'activeStatus'),

        /**
         * service name filter
         */
        services: function () {
          var view = this;
          var services = App.Service.find().filter(function(item){
            var stackService =  App.StackService.find().findProperty('serviceName', item.get('serviceName'));
            return stackService.get('isServiceWithWidgets');
          });
          return services.map(function (service) {
            return Em.Object.create({
              value: service.get('serviceName'),
              label: service.get('displayName'),
              isActive: function () {
                var activeService = view.get('activeService') ? view.get('activeService') : view.get('controller.content.serviceName');
                return this.get('value') == activeService;
              }.property('value', 'view.activeService')
            })
          });
        }.property('activeService'),

        filterByService: function (event) {
          this.set('activeService', event.context);
        },

        createWidget: function () {
          this.get('parentView').onPrimary();
          this.get('controller').createWidget();
        },

        ensureTooltip: function () {
          Em.run.later(this, function () {
            App.tooltip($("[rel='shared-icon-tooltip']"));
          }, 1000);
        }.observes('activeService', 'parentView.isShowMineOnly'),

        didInsertElement: function () {
          this.ensureTooltip();
        }
      })
    });
  }

});