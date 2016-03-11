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

var date = require('utils/date/date');

var App = require('app');

App.MainChartsHeatmapHostView = Em.View.extend({
  templateName: require('templates/main/charts/heatmap/heatmap_host'),

  didInsertElement: function() {
    $("#heatmapDetailsBlock").hide();
  },

  /** @private */
  hostClass: 'hostBlock',

  /**
   * link to host record in App.Host model
   */
  hostModelLink: function () {
    return App.Host.find(this.get('content.hostName'));
  }.property('content.hostName'),

  /**
   * show Host details block and move it near the cursor
   *
   * @param {Object} event
   * @this App.MainChartsHeatmapHostView
   */
  mouseEnter: function (event) {
    var host = this.get('content');
    var view = App.MainChartsHeatmapHostDetailView.create();
    var self = this;
    var nonClientComponents = App.get('components.slaves').concat(App.get('components.masters'));

    $.each(view.get('details'), function (i) {
      var val = host[i];

      switch (i) {
        case 'diskUsage':
          val = self.getUsage(host, 'diskTotal', 'diskFree');
          break;
        case 'cpuUsage':
          val = 0;
          if (Number.isFinite(host.cpuSystem) && Number.isFinite(host.cpuUser)) {
            val = host.cpuSystem + host.cpuUser;
          }
          val = val.toFixed(1);
          break;
        case 'memoryUsage':
          val = self.getUsage(host, 'memTotal', 'memFree');
          break;
        case 'hostComponents':
          val = [];
          host[i].forEach(function (componentName) {
            if (nonClientComponents.contains(componentName)) {
              val.push(App.format.role(componentName, false));
            }
          }, this);
          val = val.join(', ')
      }

      view.set('details.' + i, val);
    });
    this.setMetric(view, host);
    this.openDetailsBlock(event);
  },

  /**
   * show tooltip with host's details
   */
  openDetailsBlock: function (event) {
    var detailsBlock = $("#heatmapDetailsBlock");

    detailsBlock.css('top', event.pageY + 10);
    detailsBlock.css('left', event.pageX + 10);
    detailsBlock.show();
  },

  /**
   * set name and value of selected metric
   * @param view
   * @param host
   */
  setMetric: function (view, host) {
    var selectedMetric = this.get('controller.selectedMetric');

    if (selectedMetric) {
      var metricName = selectedMetric.get('name');
      var h2vMap = selectedMetric.get('hostToValueMap');
      if (h2vMap && metricName) {
        var value = h2vMap[host.hostName];
        if (Em.isNone(value)) {
          value = this.t('charts.heatmap.unknown');
        } else {
          if (metricName == 'Garbage Collection Time') {
            value = date.timingFormat(parseInt(value));
          } else {
            if (isNaN(value)) {
              value = this.t('charts.heatmap.unknown');
            } else {
              value = value + selectedMetric.get('units');
            }
          }
        }
        view.set('details.metricName', metricName);
        view.set('details.metricValue', value);
      }
    }
  },
  /**
   * get relative usage of metric in percents
   * @param item
   * @param totalProperty
   * @param freeProperty
   * @return {String}
   */
  getUsage: function (item, totalProperty, freeProperty) {
    var result = 0;
    var total = item[totalProperty];

    if (Number.isFinite(total) && Number.isFinite(item[freeProperty]) && total > 0) {
      result = ((total - item[freeProperty]) / total) * 100;
    }
    return result.toFixed(1);
  },

  /**
   * hide Host details block
   *
   * @param {Object} e
   * @this App.MainChartsHeatmapHostView
   */
  mouseLeave: function (e) {
    $("#heatmapDetailsBlock").hide();
  },

  hostTemperatureStyle: function () {
    var controller = this.get('controller');
    var h2sMap = controller.get('hostToSlotMap');
    if (h2sMap) {
      var hostname = this.get('content.hostName');
      if (hostname) {
        var slot = h2sMap[hostname];
        if (slot > -1) {
          var slotDefs = controller.get('selectedMetric.slotDefinitions');
          if (slotDefs && slotDefs.length > slot) {
            return slotDefs[slot].get('cssStyle');
          }
        }
      }
    }
    return '';
  }.property('controller.hostToSlotMap')
});