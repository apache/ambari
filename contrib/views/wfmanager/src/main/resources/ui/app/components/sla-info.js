/*
*    Licensed to the Apache Software Foundation (ASF) under one or more
*    contributor license agreements.  See the NOTICE file distributed with
*    this work for additional information regarding copyright ownership.
*    The ASF licenses this file to You under the Apache License, Version 2.0
*    (the "License"); you may not use this file except in compliance with
*    the License.  You may obtain a copy of the License at
*
*        http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS,
*    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*    See the License for the specific language governing permissions and
*    limitations under the License.
*/
import Ember from 'ember';
import EmberValidations from 'ember-validations';

export default Ember.Component.extend(EmberValidations, {
  alertEvents: Ember.A([]),
  timeUnitOptions : Ember.A([]),
  nominalTime : '',
  initialize : function(){
    this.set('alertEvents', Ember.A([]));
    this.get('alertEvents').pushObject({eventType:'start_miss', alertEnabled:false, displayName :'Start Miss'});
    this.get('alertEvents').pushObject({eventType:'end_miss', alertEnabled:false, displayName : 'End Miss'});
    this.get('alertEvents').pushObject({eventType:'duration_miss', alertEnabled:false, displayName:'Duration Miss'});

    Ember.addObserver(this, 'alertEvents.@each.alertEnabled', this, this.alertEventsObserver);

    this.set('timeUnitOptions',Ember.A([]));
    this.get('timeUnitOptions').pushObject({value:'',displayName:'Select'});
    this.get('timeUnitOptions').pushObject({value:'MINUTES',displayName:'Minutes'});
    this.get('timeUnitOptions').pushObject({value:'HOURS',displayName:'Hours'});
    this.get('timeUnitOptions').pushObject({value:'DAYS',displayName:'Days'});

    if(this.get('slaInfo.alertEvents')){
      var alertsFor = this.get('slaInfo.alertEvents').split(",");
      alertsFor.forEach((alert)=>{
        Ember.set(this.get('alertEvents').findBy('eventType', alert),'alertEnabled', true);
      });
    }
    if(this.get('slaEnabled') === undefined){
      this.set('slaEnabled', false);
    }
    Ember.addObserver(this, 'slaEnabled', this, this.slaObserver);
    if(this.get('slaInfo.nominalTime')){
      var date = new Date(this.get('slaInfo.nominalTime'));
      if(date && !isNaN(date.getTime())){
        var utcDate = new Date(date.getTime() + date.getTimezoneOffset()*60*1000);
        this.set('nominalTime', moment(utcDate).format("MM/DD/YYYY hh:mm A"));
      }
    }
    Ember.addObserver(this, 'nominalTime', this, this.nominalTimeObserver);
  }.on('init'),
  alertEventsObserver : function(){
    var alerts = this.get('alertEvents').filterBy('alertEnabled',true).mapBy('eventType');
    this.set('slaInfo.alertEvents', alerts.join());
  },
  onDestroy : function(){
    Ember.removeObserver(this, 'alertEvents.@each.alertEnabled', this, this.alertEventsObserver);
    Ember.removeObserver(this, 'slaEnabled', this, this.slaObserver);
    Ember.removeObserver(this, 'nominalTime', this, this.nominalTimeObserver);
  }.on('willDestroyElement'),
  elementsInserted : function() {
    this.$('#nominalTime').datetimepicker({
      useCurrent: false,
      showClose : true,
      defaultDate : this.get('slaInfo.nominalTime')
    });
    this.sendAction('register','slaInfo', this);
    if(this.get('slaEnabled')){
      this.$('#slaCollapse').collapse('show');
    }
  }.on('didInsertElement'),
  nominalTimeObserver : function(){
    var date = new Date(this.get('nominalTime'));
    this.set('slaInfo.nominalTime',moment(date).format("YYYY-MM-DDTHH:mm")+'Z');
  },
  shouldEnd : Ember.computed.alias('slaInfo.shouldEnd'),
  shouldStart : Ember.computed.alias('slaInfo.shouldStart'),
  maxDuration : Ember.computed.alias('slaInfo.maxDuration'),
  validations : {
    'nominalTime': {
      presence: {
        'if': 'slaEnabled',
        'message' : 'Required',
      }
    },
    'shouldEnd.time': {
      presence: {
        'if': 'slaEnabled',
        'message' : 'Required',
      },
      numericality: {
        'if': 'slaEnabled',
        onlyInteger: true,
        greaterThan: 0,
        'message' : 'Number Only'
      }
    },
    'shouldStart.time': {
      numericality: {
        'if': 'slaEnabled',
        allowBlank :true,
        onlyInteger: true,
        greaterThan: 0,
        message : 'Number Only'
      }
    },
    'maxDuration.time': {
      numericality: {
        'if': 'slaEnabled',
        allowBlank :true,
        onlyInteger: true,
        greaterThan: 0,
        message : 'Number Only'
      }
    },
    'shouldStart.unit': {
      presence: {
        'if': 'shouldStart.time',
        'message' : 'Required',
      }
    },
    'shouldEnd.unit': {
      presence: {
        'if': 'slaEnabled',
        'message' : 'Required',
      }
    },
    'maxDuration.unit': {
      presence: {
        'if': 'maxDuration.time',
        'message' : 'Required',
      }
    }
  },
  slaObserver : function(){
    if(this.get('slaEnabled')){
      this.$('#slaCollapse').collapse('show');
    }else{
      this.$('#slaCollapse').collapse('hide');
    }
  }
});
