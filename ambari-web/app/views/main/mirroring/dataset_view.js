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
var filters = require('views/common/filter_view');
var sort = require('views/common/sort_view');
var date = require('utils/date');

App.MainMirroringDataSetView = Em.View.extend({
  name: 'mainMirroringDataSetView',
  templateName: require('templates/main/mirroring/dataset'),
  repeatNumberSelected: null,
  repeatOptions: Ember.Object.create({
    repeatOptionSelected: function (key, value) {
      if (value) {
        // setter
        var content = this.get('content');
        for (var i = 0; i < content.length; i++) {
          if (content.objectAt(i).get('value').toLowerCase() === value.get('value').toLowerCase())
            return content.objectAt(i);
        }
      }
      return this.get('content').objectAt(0);
    }.property(),
    content: function () {
      return [
        Ember.Object.create({ value: "Days"}),
        Ember.Object.create({ value: "Weeks"}),
        Ember.Object.create({ value: "Hours"}),
        Ember.Object.create({ value: "Minutes"})
      ];
    }.property('controller.model.newDataSet')
  }),
  rawDataset: null,

  targetClusterSelect: App.MainMirroringDropdownView.extend({

    selected: function (key, manuallySelectedElement) {
      // getter
      if (arguments.length == 1) {
        var targetClusterSelected = this.get('controller.model.newDataSet.targetCluster.clusterName');

        if (targetClusterSelected && targetClusterSelected.trim() !== '') {
          var newClusterOption = Ember.Object.create({title: targetClusterSelected, value: ''});
          return newClusterOption;
        }
        else {
          var defaultSelect = Ember.Object.create({title: Em.I18n.t('mirroring.dataset.selectTargetClusters'), value: ''});
          return defaultSelect;
        }
      } else {
        //setter ( if we manually change the dropdown option )
        return manuallySelectedElement;

      }
    }.property('controller.model.newDataSet.targetCluster'),

    controller: App.get('router.mainMirroringDataSetController'),

    listOfOptions: function () {
      var listOfTargetClusterNames = this.get('controller.model.listOfTargetClusterNames');
      var returnValue = [];
      if (listOfTargetClusterNames) {
        listOfTargetClusterNames.forEach(
          function (targetClusterName) {
            returnValue.push({
              title: targetClusterName,
              value: ''
            });

          }, this);

        returnValue.push({
          title: Em.I18n.t('mirroring.dataset.addTargetCluster'),
          value: ''
        });

      }
      return returnValue;
    }.property('controller.model.listOfTargetClusterNames'),

    select: function (event) {
      if (event.currentTarget.innerText === Em.I18n.t('mirroring.dataset.addTargetCluster')) {
        this.get('controller').createTargetCluster();
      }
      else {
        var selected = event.context;
        this.set('selected', selected);
        // var targetClusterRecord = App.TargetCluster.find({clusterName : selected.title});  NOT WORKING. Not sure. Why.
        var targetClusterRecord = App.TargetCluster.find().findProperty("clusterName", selected.title); // WORKING
        this.set('controller.model.newDataSet.targetCluster', targetClusterRecord);
      }
    }

  }),

  dayOrNightOptions: function (key, value) {
    var obj = Ember.Object.create({
      content: [
        Ember.Object.create({name: "AM", value: 'AM'}),
        Ember.Object.create({name: "PM", value: 'PM'})
      ]
    });

    if (value) {
      // setter
      obj.set('selectedForStart', Ember.Object.create({name: value.selectedForStart, value: value.selectedForStart}));
      obj.set('selectedForEnd', Ember.Object.create({name: value.selectedForEnd, value: value.selectedForEnd}));
    } else {
      obj.set('selectedForStart', obj.content.objectAt(0));
      obj.set('selectedForEnd', obj.content.objectAt(0));
    }

    return obj;

  }.property('controller.model.newDataSet'),

  hourOptions: Ember.Object.create({
    selectedForStart: function (key, value) {
      if (value) {
        var content = this.get('content');
        for (var i = 0; i < content.length; i++) {
          if (parseInt(content.objectAt(i).get('value')) === parseInt(value.get('value')))
            return content.objectAt(i);
        }
      }
      return this.get('content').objectAt(0);
    }.property('controller.model.newDataSet'),

    selectedForEnd: function (key, value) {
      if (value) {
        var content = this.get('content');
        for (var i = 0; i < content.length; i++) {
          if (parseInt(content.objectAt(i).get('value')) === parseInt(value.get('value')))
            return content.objectAt(i);
        }
      }
      return this.get('content').objectAt(0);
    }.property('controller.model.newDataSet'),

    content: function () {
      var values = [];
      for (var i = 0; i < 12; i++) {
        if (i == 0)
          values.push(Ember.Object.create({value: '12'}));
        else
          values.push(Ember.Object.create({value: (i < 10 ? '0' + i : '' + i)}));
      }
      return values;
    }.property()
  }),

  minuteOptions: Ember.Object.create({
    selectedForStart: function (key, value) {
      if (value) {
        var content = this.get('content');
        for (var i = 0; i < content.length; i++) {
          if (parseInt(content.objectAt(i).get('value')) === parseInt(value.get('value')))
            return content.objectAt(i);
        }
      }
      return this.get('content').objectAt(0);
    }.property('controller.model.newDataSet'),

    selectedForEnd: function (key, value) {
      if (value) {
        var content = this.get('content');
        for (var i = 0; i < content.length; i++) {
          if (parseInt(content.objectAt(i).get('value')) === parseInt(value.get('value')))
            return content.objectAt(i);
        }
      }
      return this.get('content').objectAt(0);
    }.property('controller.model.newDataSet'),

    content: function () {
      var values = [];
      for (var i = 0; i < 60; i += 5) {
        values.push(Ember.Object.create({value: (i < 10 ? '0' + i : '' + i)}));
      }
      return values;
    }.property()
  }),

  /*
   // May be used in future
   timeOptions: Ember.Object.create({

   selectedForStart: 1,
   selectedForEnd: 1,
   content: function () {
   var values = [];
   for (var i = 0; i < 24; i++) {
   var j = i;
   if (j == 0) {
   j = 12;
   }
   else if (j > 12) {
   j = j % 12;
   }

   values.push((j < 10 ? '0' + j : '' + j) + (i < 12 ? ':00am' : ':00pm'));
   values.push((j < 10 ? '0' + j : '' + j) + (i < 12 ? ':30am' : ':30pm'));

   }
   return values;
   }.property()
   }),

   */

  /*
   idChanged: function() {
   var newDataSet = this.get('newDataSet');
   if( newDataSet && newDataSet.name ){
   var re = new RegExp(" ", "g");
   newDataSet.id = newDataSet.name.replace(re, "_");
   var schedule = newDataSet.getSchedule();
   schedule.set('id',newDataSet.id);
   }

   }.observes('newDataSet.name'),
   */
  scheduleChangedOnUI: function () {

    var isPleaseIgnoreListener = this.get('isPleaseIgnoreListener');
    if (isPleaseIgnoreListener) {
      return;
    }

    var isPleaseIgnoreListener = this.get('isPleaseIgnoreListener');


    var newDataSet = this.get('controller.model.newDataSet');

    var schedule = newDataSet.get('schedule');

    var startTime = this.get('hourOptions.selectedForStart.value') + ':' + this.get('minuteOptions.selectedForStart.value') + ':' + this.get('dayOrNightOptions.selectedForStart.value');
    var endTime = this.get('hourOptions.selectedForEnd.value') + ':' + this.get('minuteOptions.selectedForEnd.value') + ':' + this.get('dayOrNightOptions.selectedForEnd.value');
    var timezone = 'UTC'; // TODO : Need to set this correctly
    var frequency = this.get('repeatOptions.repeatOptionSelected.value') + '(' + this.get('repeatNumberSelected') + ')';

    schedule.set('startTime', startTime);
    schedule.set('endTime', endTime);
    schedule.set('timezone', timezone);
    schedule.set('frequency', frequency);
    schedule.set('dataset', newDataSet);

    //1. We need to find start time, end time
    //2. We need to
  }.observes('hourOptions.selectedForStart.value', 'hourOptions.selectedForEnd.value', 'minuteOptions.selectedForStart.value', 'minuteOptions.selectedForEnd.value', 'dayOrNightOptions.selectedForStart.value', 'dayOrNightOptions.selectedForEnd.value', 'repeatOptions.repeatOptionSelected.value', 'repeatNumberSelected'),


  isPleaseIgnoreListener: false,

  updateScheduleOnUI: function () {
    this.set('isPleaseIgnoreListener', true);
    var newDataSet = this.get('controller.model.newDataSet');

    var schedule = newDataSet.get('schedule');

    // this.notifyPropertyChange("hourOptions.selectedForStart.value");
    var startTime = schedule.get('startTime');
    var endTime = schedule.get('endTime');
    var timezone = schedule.get('timezone'); // TODO : Need to handle this correctly
    var frequency = schedule.get('frequency');
    var repeatNumberSelected = frequency.substring(frequency.indexOf('(') + 1, frequency.indexOf(')'));
    var repeatOptionSelected = frequency.substring(0, frequency.indexOf('('));

    this.set('repeatNumberSelected', repeatNumberSelected);
    this.set('repeatOptions.repeatOptionSelected', Ember.Object.create({value: repeatOptionSelected}));

    var startTimeOptions = startTime.split(':');
    var endTimeOptions = endTime.split(':');

    var startHour = startTimeOptions[0];
    var startMinute = startTimeOptions[1];
    var startAMPM = startTimeOptions[2];

    var endHour = endTimeOptions[0];
    var endMinute = endTimeOptions[1];
    var endAMPM = endTimeOptions[2];

    this.set('dayOrNightOptions',
      {
        selectedForStart: startAMPM,
        selectedForEnd: endAMPM
      });

    this.set('hourOptions.selectedForStart', Ember.Object.create({value: startHour}));
    this.set('hourOptions.selectedForEnd', Ember.Object.create({value: endHour}));
    this.set('minuteOptions.selectedForStart', Ember.Object.create({value: startMinute}));
    this.set('minuteOptions.selectedForEnd', Ember.Object.create({value: endMinute}));
    this.set('isPleaseIgnoreListener', false);
  },

  /**
   * When View is displayed, it asks the controller "Give me the Model".
   */
  didInsertElement: function () {
    var controller = this.get('controller');

    // Load model and expose to view
    var ds = controller.get('model.newDataSet');

    if (this.get('controller.isPopupForEdit'))
      this.updateScheduleOnUI();


    // copy dataset for "undo" ability
    var props = Em.Object.create();
    for (var prop in ds) {
      if (ds.hasOwnProperty(prop)
        && prop.indexOf('__ember') < 0
        && prop.indexOf('_super') < 0
        && Em.typeOf(ds.get(prop)) !== 'function'
        ) {
        props.set(prop, ds.get(prop));
      }
    }
    this.set('controller.rawDataSet', props);

    $('.datepicker').datepicker({
      format: 'mm/dd/yyyy'
    });
  }



});
