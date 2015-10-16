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
var date = require('utils/date/date');

var heatmap = require('utils/heatmap');

/**
 * Base class for any heatmap metric.
 * 
 * This class basically provides the following for each heatmap metric.
 * <ul>
 * <li> Provides number of slots in which temperature can fall.
 * <li> Maintains the maximum value so as to scale slot ranges.
 * <li> Gets JSON data from server and maps response for all hosts into above
 * slots.
 * </ul>
 * 
 */
App.MainChartHeatmapMetric = Em.Object.extend({
  /**
   * Name of this metric
   */
  name: null,

  /**
   * Number of slots this metric will be mapped into. When changing this value,
   * the color count in 'slotColors' should also be changed.
   */
  numberOfSlots: 5,

  /**
   * Colors for the each of the number of slots defined above. When changing the
   * number of slots, the number of colors also should be updated.
   * 
   * @type {Array}
   */
  slotColors: [ {
    r: 0x00,
    g: 0xcc,
    b: 0x00
  }, // Green
  {
    r: 0x9f,
    g: 0xee,
    b: 0x00
  }, {
    r: 0xff,
    g: 0xff,
    b: 0x00
  }, // Yellow
  {
    r: 0xff,
    g: 0xc0,
    b: 0x00
  }, // Orange
  {
    r: 0xff,
    g: 0x00,
    b: 0x00
  } ],// Red

  /**
   * Minimum value of this metric. Default is 0.
   */
  minimumValue: 0,

  /**
   * Maximum value of this metric. This has to be specified by extending classes
   * so that the range from 'minimumValue' to 'maximumValue' can be split among
   * 'numberOfSlots'. It is recommended that this value be a multiple of
   * 'numberOfSlots'.
   */
  maximumValue: 100,

  /**
   * Units of the maximum value which is shown in UI {String}
   */
  units: '',


  /**
   * Provides following information about slots in an array of objects.
   * <ul>
   * <li> from: {number} Slot starts from this value
   * <li> to: {number} Slot ends at this value (inclusive)
   * <li> label: {String} Slot name to be shown
   * <li> cssStyle: {String} style to be embedded on hosts which fall into this
   * slot.
   * </ul>
   * 
   * Slot count will be the same as specified in 'numberOfSlots'. Slot
   * definitions will be given in increasing temperature from 'minimumValue' to
   * 'maximumValue'.
   * 
   */
  slotDefinitions: function () {
    var max = parseFloat(this.get('maximumValue'));
    var slotCount = this.get('numberOfSlots');
    var labelSuffix = this.get('slotDefinitionLabelSuffix');
    var delta = (max - this.get('minimumValue')) / slotCount;
    var defs = [];
    var slotColors = this.get('slotColors');
    var slotColorIndex = 0;
    for ( var c = 0; c < slotCount - 1; c++) {
      var slotColor = slotColors[slotColorIndex++];
      var start = (c * delta);
      var end = ((c + 1) * delta);
      defs.push(this.generateSlot(start, end, labelSuffix, slotColor));
    }
    slotColor = slotColors[slotColorIndex++];
    start = ((slotCount - 1) * delta);
    defs.push(this.generateSlot(start, max, labelSuffix, slotColor));

    defs.push(Em.Object.create({
      from: NaN,
      to: NaN,
      label: Em.I18n.t('charts.heatmap.label.invalidData'),
      cssStyle: this.getHatchStyle()
    }));
    defs.push(Em.Object.create({
      from: -1,
      to: -1,
      label: Em.I18n.t('charts.heatmap.label.notApplicable'),
      cssStyle: "background-color:rgb(200, 200, 200)"
    }));
    return defs;
  }.property('minimumValue', 'maximumValue', 'numberOfSlots'),
  /**
   * genarate slot by given parameters
   *
   * @param min
   * @param max
   * @param labelSuffix
   * @param slotColor
   * @return {object}
   * @method generateSlot
   */
  generateSlot: function (min, max, labelSuffix, slotColor) {
    var from = this.formatLegendNumber(min);
    var to = this.formatLegendNumber(max);
    var label;
    if ($.trim(labelSuffix) == 'ms') {
      label = date.timingFormat(from, 'zeroValid') + " - " + date.timingFormat(to, 'zeroValid');
    } else {
      label = from + labelSuffix + " - " + to + labelSuffix;
    }
    return Em.Object.create({
      from: from,
      to: to,
      label: label,
      cssStyle: "background-color:rgb(" + slotColor.r + "," + slotColor.g + "," + slotColor.b + ")"
    });
  },

  /**
   * calculate hatch style of slot according to browser version used
   * @return {String}
   */
  getHatchStyle: function () {
    var hatchStyle = "background-color:rgb(135, 206, 250)";
    if (jQuery.browser.webkit) {
      hatchStyle = "background-image:-webkit-repeating-linear-gradient(-45deg, #FF1E10, #FF1E10 3px, #ff6c00 3px, #ff6c00 6px)";
    } else if (jQuery.browser.mozilla) {
      hatchStyle = "background-image:repeating-linear-gradient(-45deg, #FF1E10, #FF1E10 3px, #ff6c00 3px, #ff6c00 6px)";
    } else if (jQuery.browser.msie && jQuery.browser.version) {
      var majorVersion = parseInt(jQuery.browser.version.split('.')[0]);
      if (majorVersion > 9) {
        hatchStyle = "background-image:repeating-linear-gradient(-45deg, #FF1E10, #FF1E10 3px, #ff6c00 3px, #ff6c00 6px)";
      }
    }
    return hatchStyle;
  },

  /**
   * In slot definitions this value is used to construct the label by appending
   * it to slot min-max values. For example giving '%' here would result in slot
   * definition label being '0% - 10%'.
   */
  slotDefinitionLabelSuffix: '',

  hostToValueMap: null,

  hostToSlotMap: function () {
    var hostToValueMap = this.get('hostToValueMap');
    var hostNames = this.get('hostNames');
    var hostToSlotMap = {};
    if (hostToValueMap && hostNames) {
      hostNames.forEach(function (hostName) {
        var slot = this.calculateSlot(hostToValueMap, hostName);
        if (slot > -1) {
          hostToSlotMap[hostName] = slot;
        }
      }, this);
    }
    return hostToSlotMap;
  }.property('hostToValueMap', 'slotDefinitions'),

  /**
   * calculate slot position in hostToSlotMap by hostname
   * @param hostToValueMap
   * @param hostName
   * @return {Number}
   */
  calculateSlot: function (hostToValueMap, hostName) {
    var slot = -1;
    var slotDefs = this.get('slotDefinitions');

    if (hostName in hostToValueMap) {
      var value = hostToValueMap[hostName];
      if (isNaN(value)) {
        slot = slotDefs.length - 2;
      } else {
        for (var slotIndex = 0; slotIndex < slotDefs.length - 2; slotIndex++) {
          var slotDef = slotDefs[slotIndex];
          if (value >= slotDef.from && value <= slotDef.to) {
            slot = slotIndex;
          }
        }
        if (slot < 0) {
          // Assign it to the last legend
          slot = slotDefs.length - 3;
        }
      }
    } else {
      slot = slotDefs.length - 1;
    }
    return slot;
  },

  /**
   * Turns numbers into displayable values. For example 24.345432425 into 24.3
   * etc.
   * 
   * @private
   */
  formatLegendNumber: function (num) {
    var fraction = num % 1;
    if (fraction > 0) {
      return parseFloat(num.toFixed(1));
    }
    return num;
  }
});