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
'use strict';
var assert = require('assert'), fs = require('fs'), vm = require('vm'), path = require('path');
var root = path.resolve(__dirname, '../..'), plan = require(root + '/app/utils/namenode_role_switch');
var count = 0;
function test(name, fn) { fn(); count++; console.log('PASS ' + name); }
function components() {
  var result = [];
  ['a', 'b', 'c'].forEach(function (h, i) {
    result.push({componentName:'NAMENODE',hostName:h,workStatus:'STARTED',staleConfigs:false,haStatus:i ? 'standby':'active'});
    result.push({componentName:'ZKFC',hostName:h,workStatus:'STARTED',staleConfigs:false});
  }); return result;
}
test('three-node target selection', function () { assert.deepEqual(plan(components()), {source:'a',targets:['b','c']}); });
test('two nodes supported', function () { assert.deepEqual(plan(components().slice(0,4)), {source:'a',targets:['b']}); });
test('multiple active rejected', function () { var c=components();c[2].haStatus='active';assert.throws(function(){plan(c);}); });
test('unknown peer rejected', function () {var c=components();c[4].haStatus='unknown';assert.throws(function(){plan(c);});});
test('stale NN rejected', function () {var c=components();c[2].staleConfigs=true;assert.throws(function(){plan(c);});});
test('stale ZKFC rejected', function () {var c=components();c[3].staleConfigs=true;assert.throws(function(){plan(c);});});
test('stopped ZKFC rejected', function () {var c=components();c[3].workStatus='INSTALLED';assert.throws(function(){plan(c);});});
test('missing ZKFC rejected', function () {assert.throws(function(){plan(components().slice(0,5));});});
// Exercise the actual controller method using a minimal modal/Ember adapter.
var text=fs.readFileSync(root+'/app/controllers/main/service/item.js','utf8');
var method=text.slice(text.indexOf('  switchActiveNameNode: function () {'),text.indexOf('  rebalanceHdfsNodes: function () {'));
var data, popup, sent, alerts, authorized;
var context={require:function(n){return n==='utils/namenode_role_switch'?plan:{};},
 App:{isAuthorized:function(){return authorized;},get:function(){return true;},
 Service:{find:function(){return {get:function(){return data.map(function(c){return {get:function(k){return c[k];}};});}};}},
 showAlertPopup:function(){alerts++;},ModalPopup:{show:function(p){popup=p;}},ajax:{send:function(p){sent.push(p);}}},
 Em:{I18n:{t:function(k){return k;}},View:{extend:function(x){return x;}}}};
vm.createContext(context);vm.runInContext('Function.prototype.property=function(){return this;}; var controller={'+method+'};',context);
function open() { data=components();popup=null;sent=[];alerts=0;authorized=true;context.controller.switchActiveNameNode();
 if(popup){popup.get=function(k){return k==='disablePrimary'?this.disablePrimary():this[k];};popup.set=function(k,v){this[k]=v;};popup.hide=function(){};} }
test('no mutation when dialog opens',function(){open();assert.equal(sent.length,0);assert.equal(popup.get('disablePrimary'),true);});
test('selection and confirmation required',function(){open();popup.targetHost='c';popup.onPrimary();assert.equal(sent.length,0);});
test('confirmed choice sends once to reviewed Active',function(){open();popup.targetHost='c';popup.confirmed=true;popup.onPrimary();popup.onPrimary();assert.equal(sent.length,1);assert.equal(sent[0].data.sourceHost,'a');assert.equal(sent[0].data.targetHost,'c');});
test('role drift before submit blocks request',function(){open();popup.targetHost='c';popup.confirmed=true;data[0].haStatus='standby';data[2].haStatus='active';popup.onPrimary();assert.equal(sent.length,0);assert.ok(popup.errorMessage);});
test('unauthorized handler cannot submit',function(){open();popup=null;authorized=false;context.controller.switchActiveNameNode();assert.equal(popup,null);assert.equal(sent.length,0);});
test('mapper selects NN metric without changing other HA components',function(){
  var mapper=fs.readFileSync(root+'/app/mappers/service_metrics_mapper.js','utf8');
  var expression=mapper.match(/this\.config3\.ha_status = ([\s\S]*?);/)[1];
  function field(component){return vm.runInNewContext(expression,{host_component:{HostRoles:{component_name:component}}});}
  assert.equal(field('NAMENODE'),'metrics.dfs.FSNamesystem.HAState');
  assert.equal(field('HBASE_MASTER'),'metrics.hbase.master.IsActiveMaster');
  assert.equal(field('RESOURCEMANAGER'),'HostRoles.ha_state');
});
console.log(count+' tests passed');
