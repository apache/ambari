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
var batchUtils = require('utils/batch_scheduled_requests');
var misc = require('utils/misc');
require('views/main/service/service');
require('data/service_graph_config');

App.MainServiceInfoSummaryView = Em.View.extend(App.UserPref, App.TimeRangeMixin, {
  templateName: require('templates/main/service/info/summary'),
  /**
   * @property {Number} chunkSize - number of columns in Metrics section
   */
  chunkSize: 5,
  attributes:null,

  /**
   * Contain array with list of master components from <code>App.Service.hostComponets</code> which are
   * <code>App.HostComponent</code> models.
   * @type {App.HostComponent[]}
   */
  mastersObj: [],
  mastersLength: 0,

  /**
   * Contain array with list of slave components models <code>App.SlaveComponent</code>.
   * @type {App.SlaveComponent[]}
   */
  slavesObj: [],
  slavesLength: 0,

  /**
   * Contain array with list of client components models <code>App.ClientComponent</code>.
   * @type {App.ClientComponent[]}
   */
  clientObj: [],
  clientsLength: 0,

  /**
   *  @property {String} templatePathPrefix - base path for custom templates
   *    if you want to add custom template, add <service_name>.hbs file to
   *    templates/main/service/info/summary folder.
   */
  templatePathPrefix: 'templates/main/service/info/summary/',
  /** @property {Ember.View} serviceSummaryView - view to embed, computed in
   *  <code>loadServiceSummary()</code>
   */
  serviceSummaryView: null,
  /**
   * @property {Object} serviceCustomViewsMap - custom views to embed
   *
   */
  serviceCustomViewsMap: function() {
    return {
      HBASE: App.MainDashboardServiceHbaseView,
      HDFS: App.MainDashboardServiceHdfsView,
      STORM: App.MainDashboardServiceStormView,
      YARN: App.MainDashboardServiceYARNView,
      RANGER: App.MainDashboardServiceRangerView,
      FLUME: App.MainDashboardServiceFlumeView
    }
  }.property('serviceName'),
  /** @property collapsedMetrics {object[]} - metrics list for collapsed section
   *    structure of element from list:
   *      @property {string} header - title for section
   *      @property {string} id - id of section for toggling, like: metric1
   *      @property {string} toggleIndex - passed to `data-parent` attribute, like: #metric1
   *      @property {Em.View} metricView - metric view class
   */
  collapsedSections: null,

  servicesHaveClients: function() {
    return App.get('services.hasClient');
  }.property('App.services.hasClient'),

  hasManyServers: function () {
    return this.get('servers').length > 1;
  }.property('servers'),

  clientsHostText: function () {
    if (this.get('controller.content.installedClients').length == 0) {
      return '';
    } else if (this.get("hasManyClients")) {
      return Em.I18n.t('services.service.summary.viewHosts');
    } else {
      return Em.I18n.t('services.service.summary.viewHost');
    }
  }.property("hasManyClients"),

  hasManyClients: function () {
    return this.get('controller.content.installedClients').length > 1;
  }.property('service.installedClients'),

  servers: function () {
    var result = [];
    var service = this.get('controller.content');
    if (service.get("id") == "ZOOKEEPER" || service.get("id") == "FLUME") {
      var servers = service.get('hostComponents').filterProperty('isMaster');
      if (servers.length > 0) {
        result = [
          {
            'host': servers[0].get('displayName'),
            'isComma': false,
            'isAnd': false
          }
        ];
      }
      if (servers.length > 1) {
        result[0].isComma = true;
        result.push({
          'host': servers[1].get('displayName'),
          'isComma': false,
          'isAnd': false
        });
      }
      if (servers.length > 2) {
        result[1].isAnd = true;
        result.push({
          'host': Em.I18n.t('services.service.info.summary.serversHostCount').format(servers.length - 2),
          'isComma': false,
          'isAnd': false
        });
      }
    }
    return result;
  }.property('controller.content'),

  historyServerUI: function(){
    var service=this.get('controller.content');
    return (App.singleNodeInstall ? "http://" + App.singleNodeAlias + ":19888" : "http://" + service.get("hostComponents").findProperty('isMaster', true).get("host").get("publicHostName")+":19888");
  }.property('controller.content'),

  /**
   * Property related to ZOOKEEPER service, is unused for other services
   * @return {Object}
   */
  serversHost: function() {
    var service = this.get('controller.content');
    if (service.get("id") == "ZOOKEEPER" || service.get("id") == "FLUME") {
      var servers = service.get('hostComponents').filterProperty('isMaster');
      if (servers.length > 0) {
        return servers[0];
      }
    }
    return {};
  }.property('controller.content'),


  componentsLengthDidChange: function() {
    var self = this;
    if (!this.get('service')) return;
    Em.run.once(self, 'setComponentsContent');
  }.observes('service.hostComponents.length', 'service.slaveComponents.@each.totalCount', 'service.clientComponents.@each.totalCount'),

  setComponentsContent: function() {
    Em.run.next(function() {
      var masters = this.get('service.hostComponents').filterProperty('isMaster');
      var slaves = this.get('service.slaveComponents').toArray();
      var clients = this.get('service.clientComponents').toArray();

      if (this.get('mastersLength') != masters.length) {
        this.updateComponentList(this.get('mastersObj'), masters);
        this.set('mastersLength', masters.length);
      }
      if (this.get('slavesLength') != slaves.length) {
        this.updateComponentList(this.get('slavesObj'), slaves);
        this.set('slavesLength', slaves.length);
      }
      if (this.get('clientsLength') != clients.length) {
        this.updateComponentList(this.get('clientObj'), clients);
        this.set('clientsLength', clients.length);
      }
    }.bind(this));
  },


  updateComponentList: function(source, data) {
    var sourceIds = source.mapProperty('id');
    var dataIds = data.mapProperty('id');
    if (sourceIds.length == 0) {
      source.pushObjects(data);
    }
    if (source.length > data.length) {
      sourceIds.forEach(function(item, index) {
        if (!dataIds.contains(item)) {
          source.removeAt(index);
        }
      });
    } else if (source.length < data.length) {
      dataIds.forEach(function(item, index) {
        if (!sourceIds.contains(item)) {
          source.pushObject(data.objectAt(index));
        }
      });
    }
  },

  data:{
    hive:{
      "database":"PostgreSQL",
      "databaseName":"hive",
      "user":"hive"
    }
  },

  /**
   * Wrapper for displayName. used to render correct display name for mysql_server
   */
  componentNameView: Ember.View.extend({
    template: Ember.Handlebars.compile('{{view.displayName}}'),
    comp : null,
    displayName: function(){
      if(this.get('comp.componentName') == 'MYSQL_SERVER'){
        return this.t('services.hive.databaseComponent');
      }
      return this.get('comp.displayName');
    }.property('comp')
  }),

  service: null,

  getServiceModel: function (serviceName) {
    var svc = App.Service.find(serviceName);
    var svcName = svc.get('serviceName');
    if (svcName) {
      switch (svcName.toLowerCase()) {
        case 'hdfs':
          svc = App.HDFSService.find().objectAt(0);
          break;
        case 'yarn':
          svc = App.YARNService.find().objectAt(0);
          break;
        case 'hbase':
          svc = App.HBaseService.find().objectAt(0);
          break;
        case 'flume':
          svc = App.FlumeService.find().objectAt(0);
          break;
        case 'storm':
          svc = App.StormService.find().objectAt(0);
          break;
        default:
          break;
      }
    }
    return svc;
  },

  isHide:true,
  moreStatsView:Em.View.extend({
    tagName:"a",
    template:Ember.Handlebars.compile('{{t services.service.summary.moreStats}}'),
    attributeBindings:[ 'href' ],
    classNames:[ 'more-stats' ],
    click:function (event) {
      this._parentView._parentView.set('isHide', false);
      this.remove();
    },
    href:'javascript:void(null)'
  }),

  serviceName:function () {
    return this.get('service.serviceName');
  }.property('service'),

  oldServiceName:'',

  /*
   * 'Restart Required bar' start
   */
  componentsCount: null,
  hostsCount: null,

  alertsCount: function () {
    return this.get('controller.content.alertsCount');
  }.property('controller.content.alertsCount'),

  hasCriticalAlerts: function () {
    return this.get('controller.content.hasCriticalAlerts');
  }.property('controller.content.alertsCount'),

  /**
   * Define if service has alert definitions defined
   * @type {Boolean}
   */
  hasAlertDefinitions: function () {
    return App.AlertDefinition.find().someProperty('serviceName', this.get('controller.content.serviceName'));
  }.property('controller.content.serviceName'),

  updateComponentInformation: function() {
    var hc = this.get('controller.content.restartRequiredHostsAndComponents');
    var hostsCount = 0;
    var componentsCount = 0;
    for (var host in hc) {
      hostsCount++;
      componentsCount += hc[host].length;
    }
    this.set('componentsCount', componentsCount);
    this.set('hostsCount', hostsCount);
  }.observes('controller.content.restartRequiredHostsAndComponents'),

  rollingRestartSlaveComponentName : function() {
    return batchUtils.getRollingRestartComponentName(this.get('serviceName'));
  }.property('serviceName'),
  rollingRestartActionName : function() {
    var label = null;
    var componentName = this.get('rollingRestartSlaveComponentName');
    if (componentName) {
      label = Em.I18n.t('rollingrestart.dialog.title').format(App.format.role(componentName, false));
    }
    return label;
  }.property('rollingRestartSlaveComponentName'),

  restartAllStaleConfigComponents: function () {
    var self = this;
    var serviceDisplayName = this.get('service.displayName');
    var bodyMessage = Em.Object.create({
      confirmMsg: Em.I18n.t('services.service.restartAll.confirmMsg').format(serviceDisplayName),
      confirmButton: Em.I18n.t('services.service.restartAll.confirmButton'),
      additionalWarningMsg: this.get('service.passiveState') === 'OFF' ? Em.I18n.t('services.service.restartAll.warningMsg.turnOnMM').format(serviceDisplayName) : null
    });

    var isNNAffected = false;
    var restartRequiredHostsAndComponents = this.get('controller.content.restartRequiredHostsAndComponents');
    for (var hostName in restartRequiredHostsAndComponents) {
      restartRequiredHostsAndComponents[hostName].forEach(function (hostComponent) {
        if (hostComponent == 'NameNode')
          isNNAffected = true;
      })
    }
    if (serviceDisplayName == 'HDFS' && isNNAffected &&
      this.get('controller.content.hostComponents').filterProperty('componentName', 'NAMENODE').someProperty('workStatus', App.HostComponentStatus.started)) {
      App.router.get('mainServiceItemController').checkNnLastCheckpointTime(function () {
        return App.showConfirmationFeedBackPopup(function (query) {
          var selectedService = self.get('service.id');
          batchUtils.restartAllServiceHostComponents(selectedService, true, query);
        }, bodyMessage);
      });
    } else {
      return App.showConfirmationFeedBackPopup(function (query) {
        var selectedService = self.get('service.id');
        batchUtils.restartAllServiceHostComponents(selectedService, true, query);
      }, bodyMessage);
    }
  },
  rollingRestartStaleConfigSlaveComponents: function (componentName) {
    batchUtils.launchHostComponentRollingRestart(componentName.context, this.get('service.displayName'), this.get('service.passiveState') === "ON", true);
  },

   /*
   * Find the graph class associated with the graph name, and split
   * the array into sections of 5 for displaying on the page
   * (will only display rows with 5 items)
   */
  constructGraphObjects: function(graphNames) {
    var result = [], graphObjects = [], chunkSize = this.get('chunkSize');
    var self = this;
    var serviceName = this.get('controller.content.serviceName');
    var stackService = App.StackService.find().findProperty('serviceName', serviceName);

    if (!graphNames && !stackService.get('isServiceWithWidgets')) {
      self.get('serviceMetricGraphs').clear();
      self.set('isServiceMetricLoaded', false);
      return;
    }

    // load time range for current service from server
    self.getUserPref(self.get('persistKey')).complete(function () {
      var index = self.get('currentTimeRangeIndex');
      if (graphNames) {
        graphNames.forEach(function(graphName) {
          graphObjects.push(App["ChartServiceMetrics" + graphName].extend({
            setCurrentTimeIndex: function () {
              this.set('currentTimeIndex', this.get('parentView.currentTimeRangeIndex'));
            }.observes('parentView.currentTimeRangeIndex')
          }));
        });
      }
      while(graphObjects.length) {
        result.push(graphObjects.splice(0, chunkSize));
      }
      self.set('serviceMetricGraphs', result);
      self.set('isServiceMetricLoaded', true);
    });
  },

  /**
   * Contains graphs for this particular service
   */
  serviceMetricGraphs: [],
  isServiceMetricLoaded: false,

  /**
   * Key-name to store time range in Persist
   * @type {string}
   */
  persistKey: function () {
    return 'time-range-service-' + this.get('service.serviceName');
  }.property(),

  getUserPrefSuccessCallback: function (response, request, data) {
    if (response) {
      console.log('Got persist value from server with key ' + data.key + '. Value is: ' + response);
      this.set('currentTimeRangeIndex', response);
    }
  },

  getUserPrefErrorCallback: function (request) {
    if (request.status == 404) {
      console.log('Persist did NOT find the key');
      this.postUserPref(this.get('persistKey'), 0);
      this.set('currentTimeRangeIndex', 0);
    }
  },

  /**
   * list of static actions of widget
   * @type {Array}
   */
  staticGeneralWidgetActions: [
    Em.Object.create({
      label: Em.I18n.t('dashboard.widgets.actions.browse'),
      class: 'icon-th',
      action: 'goToWidgetsBrowser',
      isAction: true
    })
  ],

  /**
   *list of static actions of widget accessible to Admin/Operator privelege
   * @type {Array}
   */

  staticAdminPrivelegeWidgetActions: [
    Em.Object.create({
      label: Em.I18n.t('dashboard.widgets.create'),
      class: 'icon-plus',
      action: 'createWidget',
      isAction: true
    })
  ],

  /**
   * List of static actions related to widget layout
   */
  staticWidgetLayoutActions: [
    Em.Object.create({
      label: Em.I18n.t('dashboard.widgets.layout.save'),
      class: 'icon-download-alt',
      action: 'saveLayout',
      isAction: true
    }),
    Em.Object.create({
      label: Em.I18n.t('dashboard.widgets.layout.import'),
      class: 'icon-file',
      isAction: true,
      layouts: App.WidgetLayout.find()
    })
  ],

  /**
   * @type {Array}
   */
  widgetActions: function() {
    var options = [];
    if (App.isAccessible('MANAGER')) {
      if (App.supports.customizedWidgetLayout) {
        options.pushObjects(this.get('staticWidgetLayoutActions'));
      }
      options.pushObjects(this.get('staticAdminPrivelegeWidgetActions'));
    }
    options.pushObjects(this.get('staticGeneralWidgetActions'));
    return options;
  }.property(''),

  /**
   * call action function defined in controller
   * @param event
   */
  doWidgetAction: function(event) {
    if($.isFunction(this.get('controller')[event.context])) {
      this.get('controller')[event.context].apply(this.get('controller'));
    }
  },

  /**
   * onclick handler for a time range option
   * @param {object} event
   */
  setTimeRange: function (event) {
    var graphs = this.get('controller.widgets').filterProperty('widgetType', 'GRAPH'),
      callback = function () {
        graphs.forEach(function (widget) {
          widget.set('properties.time_range', event.context.value);
        });
      };
    this._super(event, callback);

    // Preset time range is specified by user
    if (event.context.value !== '0') {
      callback();
    }
  },

  loadServiceSummary: function () {
    var serviceName = this.get('serviceName');
    var serviceSummaryView = null;

    if (!serviceName) {
      return;
    }

    if (this.get('oldServiceName')) {
      // do not delete it!
      return;
    }

    var customServiceView = this.get('serviceCustomViewsMap')[serviceName];
    if (customServiceView) {
      serviceSummaryView  = customServiceView.extend({
        service: this.get('service')
      });
    } else  {
      serviceSummaryView = Em.View.extend(App.MainDashboardServiceViewWrapper, {
        templateName: this.get('templatePathPrefix') + 'base'
      });
    }
    this.set('serviceSummaryView', serviceSummaryView);
    this.set('oldServiceName', serviceName);
  }.observes('serviceName'),


  /**
   * Service metrics panel not displayed when metrics service (ex:Ganglia) is not in stack definition.
   */
  isNoServiceMetricsService: function() {
    return !App.get('services.serviceMetrics').length;
  }.property('App.services.serviceMetrics'),

  didInsertElement: function () {
    this._super();
    var svcName = this.get('controller.content.serviceName');
    this.set('service', this.getServiceModel(svcName));
    var isMetricsSupported = svcName != 'STORM' || App.get('isStormMetricsSupported');

    this.get('controller').getActiveWidgetLayout();
    if (App.get('supports.customizedWidgetLayout')) {
      this.get('controller').loadWidgetLayouts();
    }

    if (svcName && isMetricsSupported) {
      var allServices =  require('data/service_graph_config');
      this.constructGraphObjects(allServices[svcName.toLowerCase()]);
    }
    // adjust the summary table height
    var summaryTable = document.getElementById('summary-info');
    if (summaryTable) {
      var rows = $(summaryTable).find('tr');
      if (rows != null && rows.length > 0) {
        var minimumHeightSum = 20;
        var summaryActualHeight = summaryTable.clientHeight;
        // for summary window
        if (summaryActualHeight <= minimumHeightSum) {
          $(summaryTable).attr('style', "height:" + minimumHeightSum + "px;");
        }
      }
    }
    this.makeSortable();
    Em.run.later(this, function () {
      App.tooltip($("[rel='add-widget-tooltip']"));
      // enalble description show up on hover
      $('.thumbnail').hoverIntent(function() {
        if ($(this).is(':hover')) {
          $(this).find('.hidden-description').delay(1000).fadeIn(200).end();
        }
      }, function() {
        $(this).find('.hidden-description').stop().hide().end();
      });
    }, 1000);
    App.loadTimer.finish('Service Summary Page');
  },

  willDestroyElement: function() {
    $("[rel='add-widget-tooltip']").tooltip('destroy');
    $('.thumbnail').off();
    $('#widget_layout').sortable('destroy');
    $('.widget.span2p4').detach().remove();
    this.get('serviceMetricGraphs').clear();
    this.set('service', null);
    this.get('mastersObj').clear();
    this.get('slavesObj').clear();
    this.get('clientObj').clear();
  },

  /**
   * Define if some widget is currently moving
   * @type {boolean}
   */
  isMoving: false,

  /**
   * Make widgets' list sortable on New Dashboard style
   */
  makeSortable: function () {
    var self = this;
    $('html').on('DOMNodeInserted', '#widget_layout', function () {
      $(this).sortable({
        items: "> div",
        cursor: "move",
        tolerance: "pointer",
        scroll: false,
        update: function () {
          var widgets = misc.sortByOrder($("#widget_layout .widget").map(function () {
            return this.id;
          }), self.get('controller.widgets'));
          self.get('controller').saveWidgetLayout(widgets);
        },
        activate: function (event, ui) {
          self.set('isMoving', true);
        },
        deactivate: function (event, ui) {
          self.set('isMoving', false);
        }
      }).disableSelection();
      $('html').off('DOMNodeInserted', '#widget_layout');
    });
  }
});
