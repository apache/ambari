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
require('utils/helper');
require('controllers/main/apps_controller');

describe('MainAppsController', function () {


   describe('#iTotalDisplayRecordsObserver()', function () {
     it('should set number of filtered jobs when switching to all jobs', function () {
       var mainAppsController = App.MainAppsController.create();
       mainAppsController.set("paginationObject.iTotalDisplayRecords", 5);
       expect(mainAppsController.get('filterObject.filteredDisplayRecords')).to.equal(5);
     })
   });


   describe('#filterObject.onRunTypeChange()', function () {
     it('should set sSearch_2 of filterObject when changing value of filterObject.runType', function () {
       var mainAppsController = App.MainAppsController.create();
       mainAppsController.set("filterObject.runType", "MapReduce");
       expect(mainAppsController.get('filterObject.sSearch_2')).to.equal("mr");
       mainAppsController.set("filterObject.runType", "Hive");
       expect(mainAppsController.get('filterObject.sSearch_2')).to.equal("hive");
       mainAppsController.set("filterObject.runType", "Pig");
       expect(mainAppsController.get('filterObject.sSearch_2')).to.equal("pig");
     })
   });

   describe('#filterObject.onJobsChange()', function () {
     it('should set minJobs,maxJobs of filterObject when changing value of filterObject.jobs', function () {
       var mainAppsController = App.MainAppsController.create();
       mainAppsController.set("filterObject.jobs", ">3");
       expect(mainAppsController.get('filterObject.minJobs')).to.equal("3");
       expect(mainAppsController.get('filterObject.maxJobs')).to.equal("");
       mainAppsController.set("filterObject.jobs", "<3");
       expect(mainAppsController.get('filterObject.minJobs')).to.equal("");
       expect(mainAppsController.get('filterObject.maxJobs')).to.equal("3");
       mainAppsController.set("filterObject.jobs", "3");
       expect(mainAppsController.get('filterObject.minJobs')).to.equal("3");
       expect(mainAppsController.get('filterObject.maxJobs')).to.equal("3");
       mainAppsController.set("filterObject.jobs", "=3");
       expect(mainAppsController.get('filterObject.minJobs')).to.equal("3");
       expect(mainAppsController.get('filterObject.maxJobs')).to.equal("3");
     })
   });

   describe('#filterObject.onDurationChange()', function () {
     it('should set minDuration,maxDuration of filterObject when changing value of filterObject.duration', function () {
       var mainAppsController = App.MainAppsController.create();
       mainAppsController.set("filterObject.duration", ">3h");
       expect(mainAppsController.get('filterObject.minDuration')).to.equal(10799640);
       expect(mainAppsController.get('filterObject.maxDuration')).to.equal("");
       mainAppsController.set("filterObject.duration", "<6m");
       expect(mainAppsController.get('filterObject.minDuration')).to.equal("");
       expect(mainAppsController.get('filterObject.maxDuration')).to.equal(360060);
       mainAppsController.set("filterObject.duration", "10s");
       expect(mainAppsController.get('filterObject.minDuration')).to.equal(9990);
       expect(mainAppsController.get('filterObject.maxDuration')).to.equal(10010);
       mainAppsController.set("filterObject.duration", "1");
       expect(mainAppsController.get('filterObject.minDuration')).to.equal(990);
       expect(mainAppsController.get('filterObject.maxDuration')).to.equal(1010);
     })
   });

   describe('#filterObject.onRunDateChange()', function () {
     it('should set minStartTime,maxStartTime of filterObject when changing value of filterObject.runDate', function () {
       var mainAppsController = App.MainAppsController.create();
       mainAppsController.set("filterObject.runDate", "Any");
       expect(mainAppsController.get('filterObject.minStartTime')).to.equal("");
       mainAppsController.set("filterObject.runDate", "Past 1 Day");
       expect(mainAppsController.get('filterObject.minStartTime')).to.be.within(((new Date().getTime())-86400000)-1000,((new Date().getTime())-86400000)+1000);
       mainAppsController.set("filterObject.runDate", "Past 2 Days");
       expect(mainAppsController.get('filterObject.minStartTime')).to.be.within(((new Date().getTime())-172800000)-1000,((new Date().getTime())-172800000)+1000);
       mainAppsController.set("filterObject.runDate", "Past 7 Days");
       expect(mainAppsController.get('filterObject.minStartTime')).to.be.within(((new Date().getTime())-604800000)-1000,((new Date().getTime())-604800000)+1000);
       mainAppsController.set("filterObject.runDate", "Past 14 Days");
       expect(mainAppsController.get('filterObject.minStartTime')).to.be.within(((new Date().getTime())-1209600000)-1000,((new Date().getTime())-1209600000)+1000);
       mainAppsController.set("filterObject.runDate", "Past 30 Days");
       expect(mainAppsController.get('filterObject.minStartTime')).to.be.within(((new Date().getTime())-2592000000)-1000,((new Date().getTime())-2592000000)+1000);
     })
   });

   describe('#filterObject.createAppLink(), #filterObject.valueObserver()', function () {
     var mainAppsController = App.MainAppsController.create();
     mainAppsController.set('content.length', 20);
     it('should set runUrl of filterObject when changing value for any filter', function () {
       mainAppsController.set("filterObject.sSearch_0", "0");
       mainAppsController.set("filterObject.sSearch_1", "workflowName");
       mainAppsController.set("filterObject.sSearch_2", "pig");
       mainAppsController.set("filterObject.sSearch_3", "admin");
       mainAppsController.set("filterObject.minJobs", "1");
       mainAppsController.set("filterObject.maxJobs", "2");
       mainAppsController.set("filterObject.minDuration", "1000");
       mainAppsController.set("filterObject.maxDuration", "2000");
       mainAppsController.set("filterObject.minStartTime", "999");
       mainAppsController.set("filterObject.maxStartTime", "1000");
       mainAppsController.set("filterObject.sSearch", "searchTerm");
       mainAppsController.set("filterObject.iDisplayLength", "10");
       mainAppsController.set("filterObject.iDisplayStart", "10");
       mainAppsController.set("filterObject.iSortCol_0", "1");
       mainAppsController.set("filterObject.sSortDir_0", "ASC");
       expect(mainAppsController.get('runUrl')).to.equal("/jobhistory/datatable?" +
           "sSearch_0=0" +
           "&sSearch_1=workflowName" +
           "&sSearch_2=pig" +
           "&sSearch_3=admin" +
           "&minJobs=1" +
           "&maxJobs=2" +
           "&minDuration=1000" +
           "&maxDuration=2000" +
           "&minStartTime=999" +
           "&maxStartTime=1000" +
           "&sSearch=searchTerm" +
           "&iDisplayLength=10" +
           "&iDisplayStart=10" +
           "&iSortCol_0=1" +
           "&sSortDir_0=ASC");
       expect(mainAppsController.get('filterObject.viewType')).to.equal('filtered');
     });

     it('should set viewType to "all" when set iDisplayLength, iDisplayStart, iSortCol_0, sSortDir_0', function () {
       mainAppsController.set("filterObject.sSearch_0", "");
       mainAppsController.set("filterObject.sSearch_1", "");
       mainAppsController.set("filterObject.sSearch_2", "");
       mainAppsController.set("filterObject.sSearch_3", "");
       mainAppsController.set("filterObject.minJobs", "");
       mainAppsController.set("filterObject.maxJobs", "");
       mainAppsController.set("filterObject.minDuration", "");
       mainAppsController.set("filterObject.maxDuration", "");
       mainAppsController.set("filterObject.minStartTime", "");
       mainAppsController.set("filterObject.maxStartTime", "");
       mainAppsController.set("filterObject.sSearch", "");
       mainAppsController.set("filterObject.iDisplayLength", "10");
       mainAppsController.set("filterObject.iDisplayStart", "10");
       mainAppsController.set("filterObject.iSortCol_0", "1");
       mainAppsController.set("filterObject.sSortDir_0", "ASC");
       expect(mainAppsController.get('filterObject.viewType')).to.equal('all');
     });
   });




 });

