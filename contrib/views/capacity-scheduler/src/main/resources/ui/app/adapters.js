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

App.QueueAdapter = DS.Adapter.extend({
  PREFIX: "yarn.scheduler.capacity",
  queues: [],
  clusterName: '',
  tag: '',
  createRecord: function(store, type, record) {
    var data = this.serialize(record, { includeId: true });
    return new Ember.RSVP.Promise(function(resolve, reject) {
      return store.filter('queue',function (q) {
        return q.id === record.id;
      }).then(function (queues) {
        var message;
        if (record.get('name.length')==0) {
          message = "Field can not be empty";
        } else if (queues.get('length') > 1) {
          message = "Queue already exists";
        };
        if (message) {
          var error = new DS.InvalidError({path:[message]});
          store.recordWasInvalid(record, error.errors);
          return;
        };

        Ember.run(record, resolve, {'queue':data})

      });
    });
  },

  saveMark:'',

  updateRecord:function (store,type,record) {
    var adapter = this,
        saveMark = this.get('saveMark'),
        uri = _getCapacitySchedulerUri(adapter.clusterName, adapter.tag),
        serializer = store.serializerFor('queue'),
        props = serializer.serializeConfig(record);

    if (saveMark) {
      uri = [uri,saveMark].join('/');
      this.set('saveMark','');
    };

    return new Ember.RSVP.Promise(function(resolve, reject) {
      adapter.ajax(uri,'PUT',{data:props}).then(function(data) {
        Ember.run(null, resolve, data);
      }, function(jqXHR) {
        jqXHR.then = null;
        Ember.run(null, reject, jqXHR);
      });
    });
  },
  
  /**
   * Finds queue by id.
   *
   */
  findQueue: function(id) {
    var result = $.grep(App.Adapter.queues, function(e){ return e.get('id') == id; });
    return result[0];
  },

  /**
   * Finds queue by id in store.
   *
   */
  find: function(store, type, id) {
    return store.findAll('queue').then(function (queues) {
      return {"queue":queues.findBy('id',id).toJSON({includeId:true})};
    });
  },

  /**
   * Finds all queues.
   *
   */
  findAll: function(store, type) {
    var adapter = this;
    var uri = _getCapacitySchedulerUri(adapter.clusterName, adapter.tag);
    return new Ember.RSVP.Promise(function(resolve, reject) {
      adapter.ajax(uri).then(function(data) {
        Ember.run(null, resolve, data);
      }, function(jqXHR) {
        jqXHR.then = null;
        Ember.run(null, reject, jqXHR);
      });
    });

  },
     
  /**
   * Finds the cluster name.
   *
   */
  findClusterName: function() {
    var uri = _getClusterNameUri();
        
    return this.ajax(uri).then(function(data) {
      var clusterName = data.ViewInstanceInfo.properties.clusterName;
      console.log("found cluster name: "+clusterName);
      App.Adapter.clusterName = clusterName;
      return clusterName;
    });
  },

  /**
   * Finds the capacity scheduler tag.
   */
  findTag: function(clusterName) {
    var uri = _getTagVersionUri(clusterName);
    
    return this.ajax(uri).then(function(data) {
      var tag = data.Clusters.desired_configs["capacity-scheduler"].tag;
      console.log("found tag version: "+tag);
      App.Adapter.tag = tag;
      return  tag;
    })
  },
  
  ajax: function(url, type, hash) {
    var adapter = this;

    return new Ember.RSVP.Promise(function(resolve, reject) {
      hash = adapter.ajaxOptions(url, type, hash);

      hash.success = function(json) {
        Ember.run(null, resolve, json);
      };

      hash.error = function(jqXHR, textStatus, errorThrown) {
        Ember.run(null, reject, adapter.ajaxError(jqXHR));
      };

      Ember.$.ajax(hash);
    }, "DS: RestAdapter#ajax " + type + " to " + url);
  },

  ajaxOptions: function(url, type, hash) {
    hash = hash || {};
    hash.url = url;
    hash.type = type;
    hash.dataType = 'json';
    hash.context = this;

    if (hash.data && type !== 'GET') {
      hash.contentType = 'application/json; charset=utf-8';
      hash.data = JSON.stringify(hash.data);
    }
    hash.beforeSend = function (xhr) {
      xhr.setRequestHeader('X-Requested-By', 'ambari');
    };
    return hash;
  },

  ajaxError: function(jqXHR) {
    if (jqXHR && typeof jqXHR === 'object') {
      jqXHR.then = null;
    }

    return jqXHR;
  },
});

App.SchedulerAdapter = App.QueueAdapter.extend({
  find: function(store, type, id) {
    return store.findAll('scheduler').then(function (scheduler) {
      return {"scheduler":scheduler.findBy('id',id).toJSON({includeId:true})};
    });
  },
});

App.ApplicationStore = DS.Store.extend({

  adapter: App.QueueAdapter,

  markForRefresh:function (mark) {
    this.set('defaultAdapter.saveMark','saveAndRefresh');
  },

  markForRestart:function (mark) {
    this.set('defaultAdapter.saveMark','saveAndRestart');
  },

  flushPendingSave: function() {
    var pending = this._pendingSave.slice(),
        newPending = [[]],scheduler;

    if (pending.length == 1) {
      this._super();
      return;
    };
    
    pending.forEach(function (tuple) {
      var record = tuple[0], resolver = tuple[1],
          operation;
      newPending[0].push(record)
      newPending[1] = resolver;
    });

    this._pendingSave = [newPending];
    this._super();
  },
  didSaveRecord: function(record, data) {
    if (Em.isArray(record)) {
      for (var i = 0; i < record.length; i++) {
        this._super(record[i],data.findBy('id',record[i].id));
      };
    } else {
      this._super(record, data);
    };
  },
  recordWasError: function(record) {
    if (Em.isArray(record)) {
      for (var i = 0; i < record.length; i++) {
        record[i].adapterDidError();
      };
    } else {
      record.adapterDidError();
    }
  },
});

App.ApplicationSerializer = DS.RESTSerializer.extend({

  PREFIX:"yarn.scheduler.capacity",

  serializeConfig:function (records) {
    var config = {},
        prefix = this.PREFIX;
    records.forEach(function (record) {
      if (record.id == 'scheduler') {
        config[prefix + ".maximum-am-resource-percent"] = record.get('maximum_am_resource_percent');
        config[prefix + ".maximum-applications"] = record.get('maximum_applications');
        config[prefix + ".node-locality-delay"] = record.get('node_locality_delay');
      } else {
        config[prefix + "." + record.get('path') + ".unfunded.capacity"] = record.get('unfunded_capacity');
        config[prefix + "." + record.get('path') + ".acl_administer_queue"] = record.get('acl_administer_queue');
        config[prefix + "." + record.get('path') + ".acl_administer_jobs"] = record.get('acl_administer_jobs');
        config[prefix + "." + record.get('path') + ".acl_submit_applications"] = record.get('acl_submit_applications');
        config[prefix + "." + record.get('path') + ".minimum-user-limit-percent"] = record.get('minimum_user_limit_percent');
        config[prefix + "." + record.get('path') + ".maximum-capacity"] = record.get('maximum_capacity');
        config[prefix + "." + record.get('path') + ".user-limit-factor"] = record.get('user_limit_factor');
        config[prefix + "." + record.get('path') + ".state"] = record.get('state');
        config[prefix + "." + record.get('path') + ".capacity"] = record.get('capacity');
        config[prefix + "." + record.get('path') + ".queues"] = record.get('queueNames')||'';
      };
    });

    for (var i in config) {
      if (config[i] === null || config[i] === undefined) {
        delete config[i];
      }
    }

    return {properties : config};

  },
  normalizeConfig:function (store, payload) {
    var queues = [];
    var props = payload.items[0].properties;

    var scheduler = [{
      id:'scheduler',
      maximum_am_resource_percent:props[this.PREFIX + ".maximum-am-resource-percent"],
      maximum_applications:props[this.PREFIX + ".maximum-applications"],
      node_locality_delay:props[this.PREFIX + ".node-locality-delay"]
    }];
    queues = _recurseQueues(null, "root", 0, props, queues, store);

    return {'queue':queues,'scheduler':scheduler};
  },

  extractFindAll: function(store, type, payload){
    var queues = [];
    var props = payload.items[0].properties;

    var scheduler = {
      id:'scheduler',
      maximum_am_resource_percent:props[this.PREFIX + ".maximum-am-resource-percent"],
      maximum_applications:props[this.PREFIX + ".maximum-applications"],
      node_locality_delay:props[this.PREFIX + ".node-locality-delay"]
    };
    queues = _recurseQueues(null, "root", 0, props, queues, store);

    var config = this.normalizeConfig(store, payload);
    
    return this.extractArray(store, type, config);
  },

  extractUpdateRecord:function (store, type, payload, id, requestType) {
    var queues = [];
    var props = payload.items[0].properties;

    var scheduler = {
      id:'scheduler',
      maximum_am_resource_percent:props[this.PREFIX + ".maximum-am-resource-percent"],
      maximum_applications:props[this.PREFIX + ".maximum-applications"],
      node_locality_delay:props[this.PREFIX + ".node-locality-delay"]
    };
    queues = _recurseQueues(null, "root", 0, props, queues, store);
    
    var config = this.normalizeConfig(store, payload);

    return this.extractConfig(store, App.Queue, {'queue':queues,'scheduler':[scheduler]});
  },

  extractConfig: function(store, primaryType, payload) {
      payload = this.normalizePayload(payload);

      var primaryTypeName = primaryType.typeKey,
          primaryArray = [],
          scheduler,queues;

      for (var prop in payload) {
        var typeKey = prop,
            forcedSecondary = false;

        if (prop.charAt(0) === '_') {
          forcedSecondary = true;
          typeKey = prop.substr(1);
        }

        var typeName = this.typeForRoot(typeKey),
            type = store.modelFor(typeName),
            typeSerializer = store.serializerFor(type),
            isPrimary = (!forcedSecondary && (type.typeKey === primaryTypeName));

        var normalizedArray = Ember.ArrayPolyfills.map.call(payload[prop], function(hash) {
          return typeSerializer.normalize(type, hash, prop);
        }, this);

        if (typeKey == App.Scheduler.typeKey) {
          scheduler = normalizedArray;
        } else {
          queues = normalizedArray;
        }
      }

      return scheduler.concat(queues);
    },

  extractQueue: function(data, props) {
    var q = { name: data.name, parentPath: data.parentPath, depth: data.depth };
    var prefix = this.PREFIX;

    if (q.parentPath == null || q.parentPath.length == 0){
        q.path = q.name;
      } else {
        q.path = q.parentPath + '.' + q.name;
      }
      q.id = q.path.dasherize();

    q.unfunded_capacity = props[prefix + "." + q.path + ".unfunded.capacity"];
    q.acl_administer_queue = props[prefix + "." + q.path + ".acl_administer_queue"];
    q.acl_administer_jobs = props[prefix + "." + q.path + ".acl_administer_jobs"];
    q.acl_submit_applications = props[prefix + "." + q.path + ".acl_submit_applications"];
    q.minimum_user_limit_percent = props[prefix + "." + q.path + ".minimum-user-limit-percent"];
    q.maximum_capacity = props[prefix + "." + q.path + ".maximum-capacity"];
    q.user_limit_factor = props[prefix + "." + q.path + ".user-limit-factor"];
    q.state = props[prefix + "." + q.path + ".state"];

    q.capacity = props[prefix + "." + q.path + ".capacity"];
    q.queueNames = props[prefix + "." + q.path + ".queues"];

    return q;
  }
});

/**
 * Recursively builds the list of queues.
 *
 */
function _recurseQueues(parentQueue, queueName, depth, props, queues, store) {
  var serializer = store.serializerFor('queue');
  var prefix = serializer.PREFIX;
  var parentPath = '';
  if (parentQueue != null) {
    parentPath = parentQueue.path;
    prefix += ".";
  }

  var queue = serializer.extractQueue({ name: queueName, parentPath: parentPath, depth: depth}, props);
  queues.push(queue);

  var queueProp = prefix + parentPath + "." + queueName + ".queues";
  if (props[queueProp]) {
    var qs = props[queueProp].split(',');
    for (var i=0; i < qs.length; i++) {
      queues = _recurseQueues(queue, qs[i], depth+1, props, queues, store)
    }
  }
  
  return queues;
}

/**
 * Gets the cluster name URI based on test mode
 *
 * @return  cluster name URI
 */
function _getCapacitySchedulerUri() {
  if (App.testMode)
    return  "/data/scheduler-configuration.json"

  var parts = window.location.pathname.match(/\/[^\/]*/g);
  var view = parts[1];
  var version = '/versions' + parts[2];
  var instance = parts[3];
  if (parts.length == 4) { // version is not present
    instance = parts[2];
    version = '';
  }
  return '/api/v1/views' + view + version + '/instances' + instance+'/resources/scheduler/configuration';
}

/**
 * Gets the cluster name URI based on test mode
 *
 * @return  cluster name URI
 */
function _getClusterNameUri() {
  if (App.testMode)
    return  "/data/view-instance.json"

  var parts = window.location.pathname.match(/\/[^\/]*/g);
  var view = parts[1];
  var version = '/versions' + parts[2];
  var instance = parts[3];
  if (parts.length == 4) { // version is not present
    instance = parts[2];
    version = '';
  }
  return '/api/v1/views' + view + version + '/instances' + instance;
}

/**
 * Gets the capacity scheduler URI based on test mode.
 *
 * @param   clusterName     the cluster name
 * @param   tagVersion      the tag version
 * @return  the capacity scheduler URI
 */
function _getCapacitySchedulerUri_old(clusterName, tagVersion) {
  console.log("_getCapacitySchedulerUri.clusterName = "+clusterName);
  console.log("_getCapacitySchedulerUri.tagVersion = "+tagVersion);
  if (App.testMode)
    return  "/data/capacity-scheduler.json"
    
  return "/api/v1/clusters/" + clusterName + "/configurations?type=capacity-scheduler&tag="+tagVersion;
}

/**
 * Gets the tag version URI based on test mode.
 *
 * @param   clusterName     the cluster name
 * @return  the capacity scheduler tag URI
 */
function _getTagVersionUri(clusterName) {
  if (App.testMode)
    return  "/data/cs-tag.json"
    
  return "/api/v1/clusters/" + clusterName + "?fields=Clusters/desired_configs/capacity-scheduler";
}

/**
 * Setup claster name and version tag for adapter
 */

/*App.deferReadiness();

(function () {

  var clusterNameUri = _getClusterNameUri();
    
  return Em.$.ajax({url: clusterNameUri, dataType: "json"}).then(function(data) {
    var clusterName = data.ViewInstanceInfo.properties.clusterName;
    console.log("found cluster name: "+clusterName);

    App.QueueAdapter.reopen({clusterName:clusterName});

    var tagVersionUri = _getTagVersionUri(clusterName);
  
    return Em.$.ajax({url: tagVersionUri, dataType: "json"}).then(function(data) {
      var tag = data.Clusters.desired_configs["capacity-scheduler"].tag;
      console.log("found tag version: "+tag);

      App.QueueAdapter.reopen({tag:tag});

      App.advanceReadiness();

    });
  });
})();*/
