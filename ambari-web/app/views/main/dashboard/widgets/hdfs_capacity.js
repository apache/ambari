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

App.NameNodeCapacityPieChartView = App.DashboardWidgetView.extend({

  title: Em.I18n.t('dashboard.widgets.NameNodeCapacity'),
  id: '2',

  isPieChart: true,
  isText: false,
  isProgressBar: false,
  model_type: 'hdfs',

  hiddenInfo: function () {
    var text = this.t("dashboard.services.hdfs.capacityUsed");
    var total = this.get('model.capacityTotal') + 0;
    var remaining = this.get('model.capacityRemaining') + 0;
    var used = total - remaining;
    var percent = total > 0 ? ((used * 100) / total).toFixed(1) : 0;
    if (percent == "NaN" || percent < 0) {
      percent = Em.I18n.t('services.service.summary.notAvailable') + " ";
    }
    if (used < 0) {
      used = 0;
    }
    if (total < 0) {
      total = 0;
    }
    var result = [];
    result.pushObject(used.bytesToSize(1, 'parseFloat') + ' of ' + total.bytesToSize(1, 'parseFloat'));
    result.pushObject(percent + '% used');
    return result;
  }.property('model.capacityUsed', 'model.capacityTotal'),

  thresh1: 40,// can be customized
  thresh2: 70,
  maxValue: 100,

  isPieExist: function () {
    var total = this.get('model.capacityTotal') + 0;
    return total > 0 ;
  }.property('model.capacityTotal'),

  template: Ember.Handlebars.compile([

    '<div class="has-hidden-info">',
    '<li class="thumbnail row">',
      '<a class="corner-icon" href="#" {{action deleteWidget target="view"}}>','<i class="icon-remove-sign icon-large"></i>','</a>',
    '<div class="caption span10">', '{{view.title}}','</div>',
    '<a class="corner-icon span1" href="#" {{action editWidget target="view"}}>','<i class="icon-edit"></i>','</a>',
    '<div class="hidden-info">', '<table align="center">{{#each line in view.hiddenInfo}}', '<tr><td>{{line}}</td></tr>','{{/each}}</table>','</div>',

    '{{#if view.isPieExist}}',
      '<div class="widget-content" >','{{view view.content modelBinding="view.model" thresh1Binding="view.thresh1" thresh2Binding="view.thresh2"}}','</div>',
    '{{else}}',
      '<div class="widget-content-isNA" >','{{t services.service.summary.notAvailable}}','</div>',
    '{{/if}}',
    '</li>',
    '</div>'
  ].join('\n')),

  content: App.ChartPieView.extend({

    model: null,  //data bind here
    id: 'widget-nn-capacity', // html id
    stroke: '#D6DDDF', //light grey
    thresh1: null,  // can be customized later
    thresh2: null,
    innerR: 25,

    existCenterText: true,
    centerTextColor: function () {
      return this.get('contentColor');
    }.property('contentColor'),

    palette: new Rickshaw.Color.Palette({
      scheme: [ '#FFFFFF', '#D6DDDF'].reverse()
    }),

    data: function () {
      var total = this.get('model.capacityTotal') + 0;
      var remaining = this.get('model.capacityRemaining') + 0;
      var used = total - remaining;
      var percent = total > 0 ? ((used * 100) / total).toFixed() : 0;
      if (percent == "NaN" || percent < 0) {
        percent = Em.I18n.t('services.service.summary.notAvailable') + " ";
      }
      return [ percent, 100 - percent];
    }.property('model.capacityUsed', 'model.capacityTotal'),

    contentColor: function () {
      var used = parseFloat(this.get('data')[0]);
      var thresh1 = parseFloat(this.get('thresh1'));
      var thresh2 = parseFloat(this.get('thresh2'));
      var color_green = '#95A800';
      var color_red = '#B80000';
      var color_orange = '#FF8E00';
      if (used <= thresh1) {
        this.set('palette', new Rickshaw.Color.Palette({
          scheme: [ '#FFFFFF', color_green  ].reverse()
        }))
        return color_green;
      } else if (used <= thresh2) {
        this.set('palette', new Rickshaw.Color.Palette({
          scheme: [ '#FFFFFF', color_orange  ].reverse()
        }))
        return color_orange;
      } else {
        this.set('palette', new Rickshaw.Color.Palette({
          scheme: [ '#FFFFFF', color_red  ].reverse()
        }))
        return color_red;
      }
    }.property('data', 'this.thresh1', 'this.thresh2'),

    // refresh text and color when data in model changed
    refreshSvg: function () {
      // remove old svg
      var old_svg =  $("#" + this.id);
      old_svg.remove();

      // draw new svg
      this.appendSvg();
    }.observes('model.capacityUsed', 'model.capacityTotal', 'this.thresh1', 'this.thresh2')
  })

})
