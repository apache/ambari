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

App.MapReduceSlotsView = App.DashboardWidgetView.extend({

  templateName: require('templates/main/dashboard/widgets/mapreduce_slots'),
  title: Em.I18n.t('dashboard.widgets.MapReduceSlots'),
  id:'10',

  isProgressBar: true,
  model_type: 'mapreduce',
  hiddenInfo: function () {
    var result = [];
    if(this.get('isViewExist')) {
      var line1 = "Map: " + this.get('model.mapSlotsOccupied') + " Occupied / " + this.get('model.mapSlotsReserved') + " Reserved / " + this.get('model.mapSlots') + " Total";
      result.pushObject(line1);
      var line2 = "Reduce: " + this.get('model.reduceSlotsOccupied') + " Occupied / " + this.get('model.reduceSlotsReserved') + " Reserved / " + this.get('model.reduceSlots') + " Total";
      result.pushObject(line2);
    }
    else {
      result.pushObject('MapReduce Not Started');
    }
    return result;
  }.property('isViewExist', 'map_display_text', 'reduce_display_text'),
  isViewExist: function () {
    return this.get('model.mapSlotsOccupied') != null && this.get('model.mapSlotsReserved') != null && this.get('model.reduceSlotsOccupied') != null && this.get('model.reduceSlotsReserved') != null;
  }.property('model.mapSlotsReserved', 'model.mapSlotsOccupied', 'model.reduceSlotsReserved', 'model.reduceSlotsOccupied'),

  map_occupied: function () {
    if (this.get('model.mapSlotsOccupied')) {
      return "width: " + ((this.get('model.mapSlotsOccupied'))*100/(this.get('model.mapSlots'))).toString() + "%";
    } else {
      return "width: 0%";
    }
  }.property('model.mapSlotsOccupied','model.mapSlots'),
  map_reserved: function () {
    if (this.get('model.mapSlotsReserved')) {
      return "width: " + ((this.get('model.mapSlotsReserved'))*100/(this.get('model.mapSlots'))).toString() + "%";
    } else {
      return "width: 0%";
    }
  }.property('model.mapSlotsReserved','model.mapSlots'),
  map_display_text: function () {
    return this.get('model.mapSlotsOccupied') + "/" + this.get('model.mapSlotsReserved') + "/" + this.get('model.mapSlots');
  }.property('model.mapSlotsReserved','model.mapSlotsOccupied','model.mapSlots'),


  reduce_occupied: function () {
    if (this.get('model.reduceSlotsOccupied')) {
      return "width: " + ((this.get('model.reduceSlotsOccupied'))*100/(this.get('model.reduceSlots'))).toString() + "%";
    } else {
      return "width: 0%";
    }
  }.property('model.reduceSlotsOccupied','model.reduceSlots'),
  reduce_reserved: function () {
    if (this.get('model.reduceSlotsReserved')) {
      return "width: " + ((this.get('model.reduceSlotsReserved'))*100/(this.get('model.reduceSlots'))).toString() + "%";
    } else {
      return "width: 0%";
    }
  }.property('model.reduceSlotsReserved','model.reduceSlots'),
  reduce_display_text: function () {
    return this.get('model.reduceSlotsOccupied') + "/" + this.get('model.reduceSlotsReserved') + "/" + this.get('model.reduceSlots');
  }.property('model.reduceSlotsReserved','model.reduceSlotsOccupied','model.reduceSlots')

});

App.MapReduceSlotsView.reopenClass({
  class: 'span4p8'
});