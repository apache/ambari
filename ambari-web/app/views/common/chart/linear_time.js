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
var string_utils = require('utils/string_utils');

/**
 * @class
 * 
 * This is a view which GETs data from a URL and shows it as a time based line
 * graph. Time is shown on the X axis with data series shown on Y axis. It
 * optionally also has the ability to auto refresh itself over a given time
 * interval.
 * 
 * This is an abstract class which is meant to be extended.
 * 
 * Extending classes should override the following:
 * <ul>
 * <li>url - from where the data can be retrieved
 * <li>title - Title to be displayed when showing the chart
 * <li>id - which uniquely identifies this chart in any page
 * <li>#transformToSeries(jsonData) - function to map server data into graph
 * series
 * </ul>
 * 
 * Extending classes could optionally override the following:
 * <ul>
 * <li>#colorForSeries(series) - function to get custom colors per series
 * </ul>
 * 
 * @extends Ember.Object
 * @extends Ember.View
 */
App.ChartLinearTimeView = Ember.View.extend({
  templateName: require('templates/main/charts/linear_time'),

  /**
   * The URL from which data can be retrieved.
   *
   * This property must be provided for the graph to show properly.
   *
   * @type String
   * @default null
   */
  url: null,

  /**
   * A unique ID for this chart.
   *
   * @type String
   * @default null
   */
  id: null,

  /**
   * Title to be shown under the chart.
   *
   * @type String
   * @default null
   */
  title: null,

  /**
   * @private
   *
   * @type Rickshaw.Graph
   * @default null
   */
  _graph: null,

  _popupGraph: null,

  _seriesProperties: null,

  _seriesPropertiesWidget: null,

  renderer: 'area',

  popupSuffix: '-popup',

  isPopup: false,

  isReady: false,

  isPopupReady: false,

  hasData: true,
  /**
   * Current cluster name
   */
  clusterName: function() {
    return App.router.get('clusterController.clusterName');
  }.property('App.router.clusterController.clusterName'),
  /**
   * Url prefix common for all child views
   */
  urlPrefix: function() {
    return App.apiPrefix + "/clusters/" + this.get('clusterName');
  }.property('clusterName'),

  /**
   * Color palette used for this chart
   *
   * @private
   * @type String[]
   */
   /*
  _paletteScheme: [ 'rgba(181,182,169,0.4)', 'rgba(133,135,114,0.4)',
      'rgba(120,95,67,0.4)', 'rgba(150,85,126,0.4)',
      'rgba(70,130,180,0.4)', 'rgba(0,255,204,0.4)',
      'rgba(255,105,180,0.4)', 'rgba(101,185,172,0.4)',
      'rgba(115,192,58,0.4)', 'rgba(203,81,58,0.4)' ].reverse(),
  */

  selector: function () {
    return '#' + this.get('elementId');
  }.property('elementId'),

  didInsertElement: function () {
    this.loadData();
    this.registerGraph();
  },
  registerGraph: function(){
    var graph = {
      name: this.get('title'),
      id: this.get('elementId'),
      popupId: this.get('id')
    };
    App.router.get('updateController.graphs').push(graph);
  },

  loadData: function() {
    App.ajax.send({
      name: this.get('ajaxIndex'),
      sender: this,
      data: this.getDataForAjaxRequest(),
      success: '_refreshGraph',
      error: 'loadDataErrorCallback'
    });
  },

  getDataForAjaxRequest: function() {
    var toSeconds = Math.round(new Date().getTime() / 1000);
    var hostName = (this.get('content')) ? this.get('content.hostName') : "";

    var HDFSService = App.HDFSService.find().objectAt(0);
    var nameNodeName = HDFSService ? HDFSService.get('nameNode.hostName') : "";
    var MapReduceService = App.MapReduceService.find().objectAt(0);
    var jobTrackerNode = MapReduceService ? MapReduceService.get('jobTracker.hostName') : "";
    var YARNService = App.YARNService.find().objectAt(0);
    var resourceManager = YARNService ? YARNService.get('resourceManagerNode.hostName') : "";
    var timeUnit = this.get('timeUnitSeconds');
    return {
      toSeconds: toSeconds,
      fromSeconds: toSeconds - timeUnit,
      stepSeconds: 15,
      hostName: hostName,
      nameNodeName: nameNodeName,
      jobTrackerNode: jobTrackerNode,
      resourceManager: resourceManager
    };
  },
  loadDataErrorCallback: function(xhr, textStatus, errorThrown){
    this.set('isReady', true);
    if (xhr.readyState == 4 && xhr.status) {
      textStatus = xhr.status + " " + textStatus;
    }
    this._showMessage('warn', this.t('graphs.error.title'), this.t('graphs.error.message').format(textStatus, errorThrown));
    this.set('isPopup', false);
    this.set('hasData', false);
  },

  /**
   * Shows a yellow warning message in place of the chart.
   *
   * @param type  Can be any of 'warn', 'error', 'info', 'success'
   * @param title Bolded title for the message
   * @param message String representing the message
   * @type: Function
   */
  _showMessage: function(type, title, message){
    var chartOverlay = '#' + this.id;
    var chartOverlayId = chartOverlay + '-chart';
    var chartOverlayY = chartOverlay + '-yaxis';
    var chartOverlayX = chartOverlay + '-xaxis';
    var chartOverlayLegend = chartOverlay + '-legend';
    var chartOverlayTimeline = chartOverlay + '-timeline';
    if (this.get('isPopup')) {
      chartOverlayId += this.get('popupSuffix');
      chartOverlayY += this.get('popupSuffix');
      chartOverlayX += this.get('popupSuffix');
      chartOverlayLegend += this.get('popupSuffix');
      chartOverlayTimeline += this.get('popupSuffix');
    }
    var typeClass;
    switch (type) {
      case 'error':
        typeClass = 'alert-error';
        break;
      case 'success':
        typeClass = 'alert-success';
        break;
      case 'info':
        typeClass = 'alert-info';
        break;
      default:
        typeClass = '';
        break;
    }
    $(chartOverlayId+', '+chartOverlayY+', '+chartOverlayX+', '+chartOverlayLegend+', '+chartOverlayTimeline).html('');
    $(chartOverlayId).append('<div class=\"alert '+typeClass+'\"><strong>'+title+'</strong> '+message+'</div>');
  },

  /**
   * Transforms the JSON data retrieved from the server into the series
   * format that Rickshaw.Graph understands.
   *
   * The series object is generally in the following format: [ { name :
   * "Series 1", data : [ { x : 0, y : 0 }, { x : 1, y : 1 } ] } ]
   *
   * Extending classes should override this method.
   *
   * @param seriesData
   *          Data retrieved from the server
   * @param displayName
   *          Graph title
   * @type: Function
   *
   */
  transformData: function (seriesData, displayName) {
    var seriesArray = [];
    if (seriesData != null) {
      // Is it a string?
      if ("string" == typeof seriesData) {
        seriesData = JSON.parse(seriesData);
      }
      // Is it a number?
      if ("number" == typeof seriesData) {
        // Same number applies to all time.
        var number = seriesData;
        seriesData = [];
        seriesData.push([number, new Date().getTime()-(60*60)]);
        seriesData.push([number, new Date().getTime()]);
      }
      // We have valid data
      var series = {};
      series.name = displayName;
      series.data = [];
      for ( var index = 0; index < seriesData.length; index++) {
        series.data.push({
          x: seriesData[index][1],
          y: seriesData[index][0]
        });
      }
      return series;
    }
  },

  /**
   * Provides the formatter to use in displaying Y axis.
   *
   * Uses the App.ChartLinearTimeView.DefaultFormatter which shows 10K,
   * 300M etc.
   *
   * @type Function
   */
  yAxisFormatter: function(y) {
    return App.ChartLinearTimeView.DefaultFormatter(y);
  },

  /**
   * Provides the color (in any HTML color format) to use for a particular
   * series.
   *
   * @param series
   *          Series for which color is being requested
   * @return color String. Returning null allows this chart to pick a color
   *         from palette.
   * @default null
   * @type Function
   */
  colorForSeries: function (series) {
    return null;
  },

  /**
  * Check whether seriesData is correct data for chart drawing
  * @param seriesData
  * @return {Boolean}
  */
  checkSeries : function(seriesData){
    if(!seriesData || !seriesData.length){
      return false;
    }
    var result = true;
    seriesData.forEach(function(item){
      if(!item.data || !item.data.length || !item.data[0] || typeof item.data[0].x === 'undefined'){
        result = false;
      }
    });
    return result;
  },

  /**
   * @private
   *
   * Refreshes the graph with the latest JSON data.
   *
   * @type Function
   */
  _refreshGraph: function (jsonData) {
    if(this.get('isDestroyed')){
      return;
    }
    var seriesData = this.transformToSeries(jsonData);

      //if graph opened as modal popup
      var popup_path = $("#" + this.id + "-container" + this.get('popupSuffix'));
      var graph_container = $("#" + this.id + "-container");
      if(popup_path.length) {
        popup_path.children().each(function () {
          $(this).children().remove();
        });
        this.set('isPopup', true);
      }
      else {
        graph_container.children().each(function (index, value) {
          $(value).children().remove();
        });
      }
    if (this.checkSeries(seriesData)) {
      // Check container exists (may be not, if we go to another page and wait while graphs loading)
      if (graph_container.length) {
        this.draw(seriesData);
        this.set('hasData', true);
          //move yAxis value lower to make them fully visible
        $("#" + this.id + "-container").find('.y_axis text').attr('y',8);
      }
    }
    else {
      this.set('isReady', true);
      //if Axis X time interval is default(60 minutes)
      if(this.get('timeUnitSeconds') === 3600){
        this._showMessage('info', this.t('graphs.noData.title'), this.t('graphs.noData.message'));
        this.set('hasData', false);
      } else {
        this._showMessage('info', this.t('graphs.noData.title'), this.t('graphs.noDataAtTime.message'));
      }
      this.set('isPopup', false);
    }
  },

  /**
   * Returns a custom time unit, that depends on X axis interval length, for the graph's X axis.
   * This is needed as Rickshaw's default time X axis uses UTC time, which can be confusing
   * for users expecting locale specific time.
   *
   * If <code>null</code> is returned, Rickshaw's default time unit is used.
   *
   * @type Function
   * @return Rickshaw.Fixtures.Time
   */
  localeTimeUnit: function(timeUnitSeconds){
    var timeUnit = new Rickshaw.Fixtures.Time();
    switch (timeUnitSeconds){
      case 604800:
        timeUnit = timeUnit.unit('day');
        break;
      case 2592000:
        timeUnit = timeUnit.unit('week');
        break;
      case 31104000:
        timeUnit = timeUnit.unit('month');
        break;
      default:
        timeUnit = {
          name: timeUnitSeconds / 240 + ' minute',
          seconds: timeUnitSeconds / 4,
          formatter: function (d) {
            return d.toLocaleString().match(/(\d+:\d+):/)[1];
          }
        };
    }
    return timeUnit;
  },

  /**
   * @private
   *
   * When a graph is given a particular width and height,the lines are drawn
   * in a slightly bigger area thereby chopping off some of the UI. Hence
   * after the rendering, we adjust the SVGs size in the DOM to compensate.
   *
   * Opened https://github.com/shutterstock/rickshaw/issues/141
   *
   * @type Function
   */
  _adjustSVGHeight: function () {
    if (this._graph && this._graph.element
        && this._graph.element.firstChild) {
      var svgElement = this._graph.element.firstChild;
      svgElement.setAttribute('height', $(this._graph.element).height()
          + "px");
      svgElement.setAttribute('width', $(this._graph.element).width()
          + "px");
    }
  },
  /**
   * temporary fix for incoming data for graph
   * to shift data time to correct time point
   */
  dataShiftFix: function(data){
    var nowTime = Math.round(new Date().getTime() / 1000);
    data.forEach(function(series){
      var l = series.data.length;
      var shiftDiff = nowTime - series.data[l - 1].x;
      if(shiftDiff > 3600){
        for(var i = 0;i < l;i++){
          series.data[i].x = series.data[i].x + shiftDiff;
        }
        series.data.unshift({
          x: nowTime - this.get('timeUnitSeconds'),
          y: 0
        });
      }
    }, this);
  },

  draw: function(seriesData) {
    var isPopup = this.get('isPopup');
    var p = '';
    if (isPopup) {
      p = this.get('popupSuffix');
    }
    var palette = new Rickshaw.Color.Palette({ scheme: 'munin'});

    this.dataShiftFix(seriesData);

    // var palette = new Rickshaw.Color.Palette({
    //   scheme: this._paletteScheme
    // });

    var self = this;
    var series_min_length = 100000000;
    seriesData.forEach(function (series, index) {
      var seriesColor = self.colorForSeries(series);
      if (seriesColor == null) {
        seriesColor = palette.color();
      }
      series.color = seriesColor;
      series.stroke = 'rgba(0,0,0,0.3)';
      if (isPopup) {
        // calculate statistic data for popup legend
        var avg = 0;
        var min = Number.MAX_VALUE;
        var max = Number.MIN_VALUE;
        for (var i = 0; i < series.data.length; i++) {
          avg += series.data[i]['y'];
          if (series.data[i]['y'] < min) {
            min = series.data[i]['y'];
          }
          else {
            if (series.data[i]['y'] > max) {
              max = series.data[i]['y'];
            }
          }
        }
        series.name = string_utils.pad(series.name, 30, '&nbsp;', 2) + string_utils.pad('min', 5, '&nbsp;', 3) + string_utils.pad(this.get('yAxisFormatter')(min), 12, '&nbsp;', 3) + string_utils.pad('avg', 5, '&nbsp;', 3) + string_utils.pad(this.get('yAxisFormatter')(avg/series.data.length), 12, '&nbsp;', 3) + string_utils.pad('max', 12, '&nbsp;', 3) + string_utils.pad(this.get('yAxisFormatter')(max), 5, '&nbsp;', 3);
      }
      if (series.data.length < series_min_length) {
        series_min_length = series.data.length;
      }
    }.bind(this));
    seriesData.forEach(function(series, index) {
      if (series.data.length > series_min_length) {
        series.data.length = series_min_length;
      }
    });
    var chartId = "#" + this.id + "-chart" + p;
    var chartOverlayId = "#" + this.id + "-container" + p;
    var xaxisElementId = "#" + this.id + "-xaxis" + p;
    var yaxisElementId = "#" + this.id + "-yaxis" + p;
    var legendElementId = "#" + this.id + "-legend" + p;

    var chartElement = document.querySelector(chartId);
    var overlayElement = document.querySelector(chartOverlayId);
    var xaxisElement = document.querySelector(xaxisElementId);
    var yaxisElement = document.querySelector(yaxisElementId);
    var legendElement = document.querySelector(legendElementId);

    var strokeWidth = 1;
    if (this.get('renderer') != 'area') {
      strokeWidth = 2;
    }

    var height = 150;
    var diff = 32;
    if(this.get('inWidget')){
      height = 105; // for widgets view
      diff = 22;
    }
    var width = 400;
    if (isPopup) {
      height = 180;
      width = 670;
    } else {
      // If not in popup, the width could vary.
      // We determine width based on div's size.
      var thisElement = this.get('element');
      if (thisElement!=null) {
        var calculatedWidth = $(thisElement).width();
        if (calculatedWidth > diff) {
          width = calculatedWidth - diff;
        }
      }
    }
    var _graph = new Rickshaw.Graph({
      height: height,
      width: width,
      element: chartElement,
      series: seriesData,
      interpolation: 'step-after',
      stroke: true,
      renderer: this.get('renderer'),
      strokeWidth: strokeWidth
    });
    if (this.get('renderer') === 'area') {
      _graph.renderer.unstack = false;
    }

    xAxis = new Rickshaw.Graph.Axis.Time({
      graph: _graph,
      timeUnit: this.localeTimeUnit(this.get('timeUnitSeconds'))
    });

    var orientation = 'right';
    if (isPopup) {
      orientation = 'left';
    }
    yAxis = new Rickshaw.Graph.Axis.Y({
      tickFormat: this.yAxisFormatter,
      element: yaxisElement,
      orientation: orientation,
      graph: _graph
    });

    var legend = new Rickshaw.Graph.Legend({
      graph: _graph,
      element: legendElement
    });

    if (!isPopup) {
      overlayElement.addEventListener('mousemove', function () {
        $(xaxisElement).removeClass('hide');
        $(legendElement).removeClass('hide');
        $(chartElement).children("div").removeClass('hide');
      });
      overlayElement.addEventListener('mouseout', function () {
        $(legendElement).addClass('hide');
      });
      _graph.onUpdate(function () {
        $(legendElement).addClass('hide');
      });
    }

   var shelving = new Rickshaw.Graph.Behavior.Series.Toggle({
      graph: _graph,
      legend: legend
    });

    var order = new Rickshaw.Graph.Behavior.Series.Order({
      graph: _graph,
      legend: legend
    });
    //show the graph when it's loaded
    _graph.onUpdate(function(){
      self.set('isReady', true);
    });
    _graph.render();

    if (isPopup) {
      var self = this;
      var hoverDetail = new Rickshaw.Graph.HoverDetail({
        graph: _graph,
        yFormatter:function (y) {
          return self.yAxisFormatter(y);
        },
        xFormatter:function (x) {
          return (new Date(x * 1000)).toLocaleTimeString();
        },
        formatter:function (series, x, y, formattedX, formattedY, d) {
          return formattedY + '<br />' + formattedX;
        }
      });
    }

    if (isPopup) {
      var self = this;
      // In popup save selected metrics and show only them after data update
      _graph.series.forEach(function(series, index) {
        if (self.get('_seriesProperties') !== null && self.get('_seriesProperties')[index] !== null && self.get('_seriesProperties')[index] !== undefined ) {
          if(self.get('_seriesProperties')[self.get('_seriesProperties').length - index - 1].length > 1) {
            $('#'+self.get('id')+'-container'+self.get('popupSuffix')+' a.action:eq('+(self.get('_seriesProperties').length - index - 1)+')').parent('li').addClass('disabled');
            series.disable();
          }
        }
      });
      //show the graph when it's loaded
      _graph.onUpdate(function(){
        self.set('isPopupReady', true);
      });
      _graph.update();

      $('#'+self.get('id')+'-container'+self.get('popupSuffix')+' li.line').click(function() {
        var series = [];
        $('#'+self.get('id')+'-container'+self.get('popupSuffix')+' a.action').each(function(index, v) {
          series[index] = v.parentNode.classList;
        });
        self.set('_seriesProperties', series);
      });

      this.set('_popupGraph', _graph);
    }
    else {

      _graph.series.forEach(function(series, index) {
        if (self.get('_seriesPropertiesWidget') !== null && self.get('_seriesPropertiesWidget')[index] !== null && self.get('_seriesPropertiesWidget')[index] !== undefined ) {
          if(self.get('_seriesPropertiesWidget')[self.get('_seriesPropertiesWidget').length - index - 1].length > 1) {
            $('#'+self.get('id')+'-container'+' a.action:eq('+(self.get('_seriesPropertiesWidget').length - index - 1)+')').parent('li').addClass('disabled');
            series.disable();
          }
        }
      });
      _graph.update();

      $('#'+self.get('id')+'-container'+' li.line').click(function() {
        var series = [];
        $('#'+self.get('id')+'-container'+' a.action').each(function(index, v) {
          series[index] = v.parentNode.classList;
        });
        self.set('_seriesPropertiesWidget', series);
      });

      this.set('_graph', _graph);
    }
  },


  showGraphInPopup: function() {
    if(!this.get('hasData')){
      return;
    }

    this.set('isPopup', true);
    var self = this;
    App.ModalPopup.show({
      template: Ember.Handlebars.compile([
        '<div class="modal-backdrop"></div><div class="modal modal-graph-line" id="modal" tabindex="-1" role="dialog" aria-labelledby="modal-label" aria-hidden="true">',
        '<div class="modal-header">',
        '<a class="close" {{action onClose target="view"}}>x</a>',
        '<h3 id="modal-label">',
        '{{#if headerClass}}{{view headerClass}}',
        '{{else}}{{header}}{{/if}}',
        '</h3>',
        '</div>',
        '<div class="modal-body">',
        '{{#if bodyClass}}{{view bodyClass}}',
        '{{else}}',
          '<div class="screensaver no-borders chart-container" {{bindAttr class="view.isReady:hide"}} ></div>',
          '<div class="time-label" {{bindAttr class="view.isReady::hidden"}}>{{view.currentTimeState.name}}</div>',
          '{{#if view.isTimePagingEnable}}<div class="arrow-left" {{bindAttr class="view.leftArrowVisible:visibleArrow"}} {{action "switchTimeBack" target="view"}}></div>{{/if}}',
          '<div id="'+this.get('id')+'-container'+this.get('popupSuffix')+'" class="chart-container chart-container'+this.get('popupSuffix')+' hide" {{bindAttr class="view.isReady:show"}} >',
            '<div id="'+this.get('id')+'-yaxis'+this.get('popupSuffix')+'" class="'+this.get('id')+'-yaxis chart-y-axis"></div>',
            '<div id="'+this.get('id')+'-xaxis'+this.get('popupSuffix')+'" class="'+this.get('id')+'-xaxis chart-x-axis"></div>',
            '<div id="'+this.get('id')+'-legend'+this.get('popupSuffix')+'" class="'+this.get('id')+'-legend chart-legend"></div>',
            '<div id="'+this.get('id')+'-chart'+this.get('popupSuffix')+'" class="'+this.get('id')+'-chart chart"></div>',
            '<div id="'+this.get('id')+'-title'+this.get('popupSuffix')+'" class="'+this.get('id')+'-title chart-title">{{view.title}}</div>',
          '</div>',
        '{{#if view.isTimePagingEnable}}<div class="arrow-right" {{bindAttr class="view.rightArrowVisible:visibleArrow"}} {{action "switchTimeForward" "forward" target="view"}}></div>{{/if}}',
        '{{/if}}',
        '</div>',
        '<div class="modal-footer">',
        '{{#if view.primary}}<a class="btn btn-success" {{action onPrimary target="view"}}>{{view.primary}}</a>{{/if}}',
        '</div>',
        '</div>'
      ].join('\n')),

      header: this.get('title'),
      self: self,
      isReady: function(){
        return this.get('self.isPopupReady');
      }.property('self.isPopupReady'),
      primary: 'OK',
      onPrimary: function() {
        this.hide();
        self.set('isPopup', false);
        self.set('timeUnitSeconds', 3600);
      },
      onClose: function(){
        this.hide();
        self.set('isPopup', false);
        self.set('timeUnitSeconds', 3600);
      },
      /**
       * check is time paging feature is enable for graph
       */
      isTimePagingEnable: function(){
        return !self.get('isTimePagingDisable');
      }.property(),
      rightArrowVisible: function(){
        return (this.get('isReady') && (this.get('currentTimeIndex') != 0))? true : false;
      }.property('isReady', 'currentTimeIndex'),
      leftArrowVisible: function(){
        return (this.get('isReady') && (this.get('currentTimeIndex') != 7))? true : false;
      }.property('isReady', 'currentTimeIndex'),
      /**
       * move graph back by time
       * @param event
       */
      switchTimeBack: function(event){
        var index = this.get('currentTimeIndex');
        // 7 - number of last time state
        if(index < 7){
          this.reloadGraphByTime(++index);
        }
      },
      /**
       * move graph forward by time
       * @param event
       */
      switchTimeForward: function(event){
        var index = this.get('currentTimeIndex');
        if(index > 0){
          this.reloadGraphByTime(--index);
        }
      },
      /**
       * reload graph depending on the time
       * @param index
       */
      reloadGraphByTime: function(index){
        this.set('currentTimeIndex', index);
        self.set('timeUnitSeconds', this.get('timeStates')[index].seconds);
        self.loadData();
      },
      timeStates: [
        {name: Em.I18n.t('graphs.timeRange.hour'), seconds: 3600},
        {name: Em.I18n.t('graphs.timeRange.twoHours'), seconds: 7200},
        {name: Em.I18n.t('graphs.timeRange.fourHours'), seconds: 14400},
        {name: Em.I18n.t('graphs.timeRange.twelveHours'), seconds: 43200},
        {name: Em.I18n.t('graphs.timeRange.day'), seconds: 86400},
        {name: Em.I18n.t('graphs.timeRange.week'), seconds: 604800},
        {name: Em.I18n.t('graphs.timeRange.month'), seconds: 2592000},
        {name: Em.I18n.t('graphs.timeRange.year'), seconds: 31104000}
      ],
      currentTimeIndex: 0,
      currentTimeState: function(){
        return this.get('timeStates').objectAt(this.get('currentTimeIndex'));
      }.property('currentTimeIndex')
    });
    Ember.run.next(function() {
      self.loadData();
      self.set('isPopupReady', false);
    });
  },
  //60 minute interval on X axis.
  timeUnitSeconds: 3600
});

/**
 * A formatter which will turn a number into computer storage sizes of the
 * format '23 GB' etc.
 * 
 * @type Function
 */
App.ChartLinearTimeView.BytesFormatter = function (y) {
  if (y == 0) return '0 B';
  var value = Rickshaw.Fixtures.Number.formatBase1024KMGTP(y);
  if (!y || y.length < 1) {
    value = '0 B';
  }
  else {
    if ("number" == typeof value) {
      value = String(value);
    }
    if ("string" == typeof value) {
      value = value.replace(/\.\d(\d+)/, function($0, $1){ // Remove only 1-digit after decimal part
        return $0.replace($1, '');
      }); 
      // Either it ends with digit or ends with character
      value = value.replace(/(\d$)/, '$1 '); // Ends with digit like '120'
      value = value.replace(/([a-zA-Z]$)/, ' $1'); // Ends with character like
      // '120M'
      value = value + 'B'; // Append B to make B, MB, GB etc.
    }
  }
  return value;
};

/**
 * A formatter which will turn a number into percentage display like '42%'
 * 
 * @type Function
 */
App.ChartLinearTimeView.PercentageFormatter = function (percentage) {
  var value = percentage;
  if (!value || value.length < 1) {
    value = '0 %';
  } else {
    value = value.toFixed(3).replace(/0+$/, '').replace(/\.$/, '') + '%';
  }
  return value;
};

/**
 * A formatter which will turn elapsed time into display time like '50 ms',
 * '5s', '10 m', '3 hr' etc. Time is expected to be provided in milliseconds.
 * 
 * @type Function
 */
App.ChartLinearTimeView.TimeElapsedFormatter = function (millis) {
  var value = millis;
  if (!value || value.length < 1) {
    value = '0 ms';
  } else if ("number" == typeof millis) {
    var seconds = millis > 1000 ? Math.round(millis / 1000) : 0;
    var minutes = seconds > 60 ? Math.round(seconds / 60) : 0;
    var hours = minutes > 60 ? Math.round(minutes / 60) : 0;
    var days = hours > 24 ? Math.round(hours / 24) : 0;
    if (days > 0) {
      value = days + ' d';
    } else if (hours > 0) {
      value = hours + ' hr';
    } else if (minutes > 0) {
      value = minutes + ' m';
    } else if (seconds > 0) {
      value = seconds + ' s';
    } else if (millis > 0) {
      value = millis.toFixed(3).replace(/0+$/, '').replace(/\.$/, '') + ' ms';
    } else {
      value = millis.toFixed(3).replace(/0+$/, '').replace(/\.$/, '') + ' ms';
    }
  }
  return value;
};

/**
 * The default formatter which uses Rickshaw.Fixtures.Number.formatKMBT 
 * which shows 10K, 300M etc.
 *
 * @type Function
 */
App.ChartLinearTimeView.DefaultFormatter = function(y) {
  if(isNaN(y)){
    return 0;
  }
  var value = Rickshaw.Fixtures.Number.formatKMBT(y);
  if (value == '') return '0';
  value = String(value);
  var c = value[value.length - 1];
  if (!isNaN(parseInt(c))) {
    // c is digit
    value = parseFloat(value).toFixed(3).replace(/0+$/, '').replace(/\.$/, '');
  }
  else {
    // c in not digit
    value = parseFloat(value.substr(0, value.length - 1)).toFixed(3).replace(/0+$/, '').replace(/\.$/, '') + c;
  }
  return value;
};


/**
 * Creates and returns a formatter that can convert a 'value' 
 * to 'value units/s'. 
 * 
 * @param unitsPrefix Prefix which will be used in 'unitsPrefix/s'
 * @param valueFormatter  Value itself will need further processing 
 *        via provided formatter. Ex: '10M requests/s'. Generally
 *        should be App.ChartLinearTimeView.DefaultFormatter. 
 * @return Function
 */
App.ChartLinearTimeView.CreateRateFormatter = function (unitsPrefix, valueFormatter) {
  var suffix = " "+unitsPrefix+"/s";
  return function (value) {
    value = valueFormatter(value) + suffix;
    return value;
  };
};