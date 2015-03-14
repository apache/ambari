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

function _fetchTagged(adapter, store, type, sinceToken) {
  var promise = adapter.findAllTagged(store, type, sinceToken),
      serializer = store.serializerFor('queue'),
      label = "DS: Handle Adapter#findAllTagged of " + type;


  return Em.RSVP.Promise.cast(promise, label).then(function(adapterPayload) {
    var config = serializer.normalizePayload(adapterPayload.items[0].properties);
    var v = (store.get('current_tag') !== store.get('tag'))?adapterPayload.items[0].version:'';

    store.set('tag',store.get('current_tag'));

    if (!Em.isArray(config.queue)) {
      return;
    }

    store.all('queue').filterBy('isNewQueue',true).forEach(function (q) {
      q.store.get('nodeLabels').forEach(function (nl) {
        var label = q.store.getById('label',[q.get('id'),nl].join('.'));
        if (label) {
          label.unloadRecord();
        }
      });
      q.unloadRecord();
    });

    store.findAll('queue').then(function (queues) {
      queues.forEach(function  (queue) {
        var new_version = config.queue.findBy('id',queue.id);
        if (new_version) {
          new_version['isNewQueue'] = queue.get('isNewQueue');
          store.findByIds('label',new_version.labels).then(function(labels) {
            labels.forEach(function (label){
              label.setProperties(config.label.findBy('id',label.get('id')));
            });
            queue.updateHasMany('labels',labels);
            queue.set('version',v);
          });
          delete new_version.labels;
          queue.setProperties(new_version);
        } else {
          store.unloadRecord(queue);
        }
        config.queue.removeObject(new_version);
      });

      config.label.forEach(function (label) {
        if (!store.hasRecordForId('label',label.id)) {
          store.push('label',label);
        }
      });

      config.queue.setEach('isNewQueue',true);
      store.pushMany(type,config.queue);
      config.queue.forEach(function(item) {
        store.recordForId('queue',item.id).set('version',v);
      });
      store.didUpdateAll(type);
      return store.recordForId('scheduler','scheduler');
    }).then(function (scheduler) {
      scheduler.setProperties(config.scheduler.objectAt(0));
      scheduler.set('version',v);
    });

  }, null, "DS: Extract payload of findAll " + type);
}

App.ApplicationStore = DS.Store.extend({

  adapter: App.QueueAdapter,

  configNote:'',

  clusterName: '',

  tag: '',

  current_tag: '',

  nodeLabels: function () {
    var adapter = this.get('defaultAdapter');
    return Ember.ArrayProxy.extend(Ember.PromiseProxyMixin).create({
      promise: adapter.getNodeLabels()
    });
  }.property(),

  isInitialized: Ember.computed.and('tag', 'clusterName'),

  markForRefresh:function () {
    this.set('defaultAdapter.saveMark','saveAndRefresh');
  },

  markForRestart:function () {
    this.set('defaultAdapter.saveMark','saveAndRestart');
  },

  flushPendingSave: function() {
    var pending = this._pendingSave.slice(),
        newPending = [[]];

    if (pending.length == 1) {
      this._super();
      return;
    }

    pending.forEach(function (tuple) {
      var record = tuple[0], resolver = tuple[1];
      newPending[0].push(record);
      newPending[1] = resolver;
    });

    this._pendingSave = [newPending];
    this._super();
  },
  didSaveRecord: function(record, data) {
    if (Em.isArray(record)) {
      for (var i = 0; i < record.length; i++) {
        this._super(record[i],data.findBy('id',record[i].id));
      }
    } else {
      this._super(record, data);
    }
  },
  recordWasError: function(record) {
    if (Em.isArray(record)) {
      for (var i = 0; i < record.length; i++) {
        record[i].adapterDidError();
      }
    } else {
      record.adapterDidError();
    }
  },
  fetchTagged: function(type, tag) {
    var adapter = this.adapterFor(type),
        sinceToken = this.typeMapFor(type).metadata.since;

    this.set('tag',tag);

    return _fetchTagged(adapter, this, type, sinceToken);
  },
  checkOperator:function () {
    return this.get('defaultAdapter').getPrivilege();
  }
});
