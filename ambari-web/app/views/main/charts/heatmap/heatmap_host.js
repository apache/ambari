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

var date = require('utils/date');

var App = require('app');

App.MainChartsHeatmapHostView = Em.View.extend({
  templateName: require('templates/main/charts/heatmap/heatmap_host'),
  /** @private */
  hostClass: 'hostBlock',

  /**
   * show Host details block and move it near the cursor
   * 
   * @param {Object}
   *          e
   * @this App.MainChartsHeatmapHostView
   */
  mouseEnter: function (e) {
    var host = this.get('content');
    var view = App.MainChartsHeatmapHostDetailView.create();
    $.each(view.get('details'), function(i){
      var val = host.get(i);
      if (i == 'diskUsage') {
        if (val == undefined || isNaN(val) || val == Infinity || val == -Infinity) {
          val = null;
        } else {
          val = val.toFixed(1);
        }
      } else if (i == 'cpuUsage') {
        if (val == undefined || isNaN(val)) {
          val = null;
        } else {
          val = val.toFixed(1);
        }
      } else if (i == 'memoryUsage') {
        if (val == undefined || isNaN(val) || val == Infinity || val == -Infinity) {
          val = null;
        } else {
          val = val.toFixed(1);
        }
      } else if (i == 'hostComponents') {
        if (val == undefined) {
          val = null;
        } else {
          val = val.filterProperty('isMaster').concat(val.filterProperty('isSlave')).mapProperty('displayName').join(', ');
        }
      }
      view.set('details.' + i, val);
    });
    var selectedMetric = this.get('controller.selectedMetric');
    if (selectedMetric) {
      var metricName = selectedMetric.get('name');
      var h2vMap = selectedMetric.get('hostToValueMap');
      if (h2vMap && metricName) {
        var value = h2vMap[host.get('hostName')];
        if (value == undefined || value == null) {
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
    var detailsBlock = $("#heatmapDetailsBlock");
    detailsBlock.css('top', e.pageY + 10);
    detailsBlock.css('left', e.pageX + 10);
    detailsBlock.show();
  },

  /**
   * hide Host details block
   * 
   * @param {Object}
   *          e
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