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

import Ember from 'ember';
import STATUS from '../configs/service-check-status';

export default Ember.Service.extend({

  store: Ember.inject.service(),

  transitionToApplication: false,

  numberOfChecks: 4,
  hdfsCheckStatus: STATUS.notStarted,
  atsCheckStatus: STATUS.notStarted,
  userHomeCheckStatus: STATUS.notStarted,
  hiveCheckStatus: STATUS.notStarted,
  percentCompleted: Ember.computed('hdfsCheckStatus', 'atsCheckStatus', 'userHomeCheckStatus', 'hiveCheckStatus', function () {
    // if all skipped then 100%
    if(this.get("numberOfChecks") === 0){
      return 100;
    }

    let percent = 0;
    percent = this.getCompletedPercentage(percent, 'hdfsCheckStatus');
    percent = this.getCompletedPercentage(percent, 'atsCheckStatus');
    percent = this.getCompletedPercentage(percent, 'userHomeCheckStatus');
    percent = this.getCompletedPercentage(percent, 'hiveCheckStatus');

    return percent;
  }),

  getCompletedPercentage(currentPercent, checkStatus) {
    if(this.get(checkStatus) === STATUS.skipped) {
      return currentPercent;
    }

    return this.get(checkStatus) === STATUS.completed ? currentPercent + (100/this.get("numberOfChecks")) : currentPercent;
  },

  fetchServiceCheckPolicy(){
    return this._getServiceCheckAdapter().getServiceCheckPolicy();
  },

  checkCompleted: Ember.computed('percentCompleted', function () {
    return this.get('percentCompleted') <= 100 && this.get('percentCompleted') >= 95.5; // approximation for cases where odd number of checks are there.
  }),

  transitioner: Ember.observer('checkCompleted', function() {
    if(this.get('checkCompleted')) {
      Ember.run.later(() => {
        this.set('transitionToApplication', true);
      }, 2000);
    }
  }),

  check(serviceCheckPolicy) {
    let numberOfChecks = this.get("numberOfChecks");
    let hdfsPromise = null;
    if( serviceCheckPolicy.checkHdfs){
      hdfsPromise = this._doHdfsCheck();
    } else {
      this.set("numberOfChecks", this.get("numberOfChecks") - 1);
      this.set("hdfsCheckStatus", STATUS.skipped);
    }

    let atsPromise = null;
    if( serviceCheckPolicy.checkATS){
      atsPromise = this._doAtsCheck();
    }else {
      this.set("numberOfChecks", this.get("numberOfChecks") - 1);
      this.set("atsCheckStatus", STATUS.skipped);
    }

    let userHomePromise = null;
    if( serviceCheckPolicy.checkHomeDirectory){
      userHomePromise = this._doUserHomeCheck();
    }else {
      this.set("numberOfChecks", this.get("numberOfChecks") - 1);
      this.set("userHomeCheckStatus", STATUS.skipped);
    }

    let hivePromise = null;
    if( serviceCheckPolicy.checkHive == true){
      hivePromise = this._doHiveCheck();
    }else{
      this.set("numberOfChecks", this.get("numberOfChecks") - 1);
      this.set("hiveCheckStatus", STATUS.skipped);
    }

    let promises = {
      hdfsPromise: hdfsPromise,
      atsPromise: atsPromise,
      userHomePromise: userHomePromise,
      hivePromise: hivePromise
    };
    return Ember.RSVP.hashSettled(promises);
  },

  _getServiceCheckAdapter: function () {
    return this.get('store').adapterFor('service-check');
  },

  _doHdfsCheck() {
    return this._doCheck( 'hdfsCheckStatus',
      (adapter) => adapter.doHdfsSeriveCheck(),
      this._identity(), this._identity());
  },

  _doAtsCheck() {
    return this._doCheck( 'atsCheckStatus',
      (adapter) => adapter.doAtsCheck(),
      this._identity(), this._identity());
  },

  _doUserHomeCheck() {
    return this._doCheck( 'userHomeCheckStatus',
      (adapter) => adapter.doUserHomeCheck(),
      this._identity(), this._identity());
  },

  _doHiveCheck() {
    return this._doCheck( 'hiveCheckStatus',
      (adapter) => adapter.doHiveCheck(),
      this._identity(),
      (err) => {
        // TODO: things to take care of related to LDAP
        return err;
      }
    );
  },

  _doCheck(statusVar, checkFn, successFn, errorFn) {
    return new Ember.RSVP.Promise((resolve, reject) => {
      this.set(statusVar, STATUS.started);
      checkFn(this._getServiceCheckAdapter()).then((data) => {
        this.set(statusVar, STATUS.completed);
        let finalData = (typeof successFn === 'function') ? successFn(data) : data;
        resolve(finalData);
      }).catch((err) => {
        this.set(statusVar, STATUS.errored);
        let finalData = (typeof errorFn === 'function') ? errorFn(err) : err;
        reject(finalData);
      });
    });

  },

  _identity() {
    return (data) => data;
  }


});
