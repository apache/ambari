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

export default Ember.Service.extend({
  tabsInfo : {},
  workInProgress : {},
  setLastActiveTab(tabId){
    console.log("setting last active tabId "+tabId);
    localStorage.setItem('lastActiveTab', tabId);
  },
  getLastActiveTab(){
    console.log("get last active "+localStorage.getItem('lastActiveTab'));
    return localStorage.getItem('lastActiveTab');
  },
  restoreTabs(){
    var tabs = localStorage.getItem('tabsInfo');
    console.log("Restoring tabs "+tabs);
    return JSON.parse(tabs);
  },
  saveTabs(tabs){
    if(!tabs){
      return;
    }
    var tabArray = [];
    tabs.forEach((tab)=>{
      tabArray.push({
        type : tab.type,
        id : tab.id,
        name : tab.name,
        filePath : tab.filePath
      });
    });
    console.log("Saving tabs "+JSON.stringify(tabArray));
    localStorage.setItem('tabsInfo', JSON.stringify(tabArray));
  },
  restoreWorkInProgress(id){
    console.log("Restoring workInProgress "+id);
    return localStorage.getItem(id);
  },
  saveWorkInProgress(id, workInProgress){
    console.log("Restoring workInProgress "+id);
    localStorage.setItem(id, workInProgress);
  },
  deleteWorkInProgress(id){
    localStorage.removeItem(id);
  }
});
