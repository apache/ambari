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
      queues = _recurseQueues(queue, qs[i], depth+1, props, queues, store);
    }
  }

  return queues;
}

App.SerializerMixin = Em.Mixin.create({

  PREFIX:"yarn.scheduler.capacity",

  serializeConfig:function (records) {
    var config = {},
        note = this.get('store.configNote');

    Em.EnumerableUtils.forEach(records,function (record) {
      this.serializeIntoHash(config, record.constructor, record);
    },this);

    for (var i in config) {
      if (config[i] === null || config[i] === undefined) {
        delete config[i];
      }
    }

    this.set('store.configNote','');

    return {properties : config, service_config_version_note: note};

  },

  serializeIntoHash: function(hash, type, record, options) {
    Em.merge(hash,this.store.serializerFor(type).serialize(record, options));
  },

  extractUpdateRecord: function(store, type, payload) {
    return this.extractArray(store, App.Queue, payload);
  },

  extractCreateRecord: function(store, type, payload) {
    this._setupLabels({},[payload.queue],payload.label);
    return this.extractSave(store, type, payload);
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

    var base_path = prefix + "." + q.path;

    var labelsPath =                base_path + ".accessible-node-labels";
    var qLabels;

    q.unfunded_capacity =           props[base_path + ".unfunded.capacity"] || null;

    q.state =                       props[base_path + ".state"] || null;
    q.acl_administer_queue =        props[base_path + ".acl_administer_queue"] || null;
    q.acl_submit_applications =     props[base_path + ".acl_submit_applications"] || null;

    q.capacity =                    (props[base_path + ".capacity"])?+props[base_path + ".capacity"]:null;
    q.maximum_capacity =            (props[base_path + ".maximum-capacity"])?+props[base_path + ".maximum-capacity"]:null;

    q.user_limit_factor =           (props[base_path + ".user-limit-factor"])?+props[base_path + ".user-limit-factor"]:null;
    q.minimum_user_limit_percent =  (props[base_path + ".minimum-user-limit-percent"])?+props[base_path + ".minimum-user-limit-percent"]:null;
    q.maximum_applications =        (props[base_path + ".maximum-applications"])?+props[base_path + ".maximum-applications"]:null;
    q.maximum_am_resource_percent = (props[base_path + ".maximum-am-resource-percent"])?+props[base_path + ".maximum-am-resource-percent"]:null;

    //TODO what if didn't set??

    switch ((props.hasOwnProperty(labelsPath))?props[labelsPath]:'') {
      case '*':
        q.labels = this.get('store.nodeLabels.content').map(function(item) {
          return [q.id,item.name].join('.');
        });
        q._accessAllLabels = true;
        break;
      case '':
        q.labels = [];
        q._accessAllLabels = false;
        break;
      default:
        q._accessAllLabels = false;
        q.labels = props[labelsPath].split(',').map(function(labelName) {
          if (!this.get('store.nodeLabels.content').isAny('name',labelName)) {
            this.get('store.nodeLabels.content').pushObject({ name:labelName, notExist:true });
          }
          return [q.id,labelName].join('.');
        }.bind(this)).compact();
        break;

    }

    if (q.maximum_am_resource_percent)
      q.maximum_am_resource_percent = q.maximum_am_resource_percent*100; // convert to percent

    q.queues = props[prefix + "." + q.path + ".queues"] || null;

    return q;
  },
  normalizePayload: function (properties) {
    if (properties.hasOwnProperty('queue')) {
      return properties;
    }
    var labels = [], queues = [];

    var scheduler = [{
      id:'scheduler',
      maximum_am_resource_percent:properties[this.PREFIX + ".maximum-am-resource-percent"]*100, // convert to percent
      maximum_applications:properties[this.PREFIX + ".maximum-applications"],
      node_locality_delay:properties[this.PREFIX + ".node-locality-delay"],
      resource_calculator:properties[this.PREFIX + ".resource-calculator"]
    }];
    _recurseQueues(null, "root", 0, properties, queues, this.get('store'));
    this._setupLabels(properties,queues,labels,this.PREFIX);

    return {'queue':queues,'scheduler':scheduler,'label':labels};
  },
  _setupLabels :function (properties, queues, labels) {
    var prefix = this.PREFIX;
    var nodeLabels = this.get('store.nodeLabels.content');
    queues.forEach(function(queue) {
      nodeLabels.forEach(function(label) {
        var labelId = [queue.id,label.name].join('.'),
            cp =  [prefix, queue.path, 'accessible-node-labels',label.name,'capacity'].join('.'),
            mcp = [prefix, queue.path, 'accessible-node-labels',label.name,'maximum-capacity'].join('.');
        labels.push({
          id:labelId,
          capacity:properties.hasOwnProperty(cp)?+properties[cp]:0,
          maximum_capacity:properties.hasOwnProperty(mcp)?+properties[mcp]:100
        });
      });

      if (queue._accessAllLabels) {
        queue.labels = nodeLabels.map(function(label) {
            return [queue.id,label.name].join('.');
        }.bind(this)).compact();
      }
	  });
	  return labels;
	}
});

App.SchedulerSerializer = DS.RESTSerializer.extend(App.SerializerMixin,{
  serialize:function (record, options) {
    var json = {};

    json[this.PREFIX + ".maximum-am-resource-percent"] = record.get('maximum_am_resource_percent')/100; // convert back to decimal
    json[this.PREFIX + ".maximum-applications"] = record.get('maximum_applications');
    json[this.PREFIX + ".node-locality-delay"] = record.get('node_locality_delay');
    json[this.PREFIX + ".resource-calculator"] = record.get('resource_calculator');

    return json;
  }
});

App.QueueSerializer = DS.RESTSerializer.extend(App.SerializerMixin,{
  serialize:function (record, options) {
    var json = {};

    if (options && options.clone) {
      Em.merge(json,record.toJSON({ includeId:true }));

      record.eachRelationship(function(key, relationship) {
        if (relationship.kind === 'belongsTo') {
          //TODO will implement if need
        } else if (relationship.kind === 'hasMany') {
          json[key] = record.get(key).mapBy('id');
        }
      }, this);

      return json;
    }

    json[this.PREFIX + "." + record.get('path') + ".unfunded.capacity"] = record.get('unfunded_capacity');
    json[this.PREFIX + "." + record.get('path') + ".acl_administer_queue"] = record.get('acl_administer_queue');
    json[this.PREFIX + "." + record.get('path') + ".acl_submit_applications"] = record.get('acl_submit_applications');
    json[this.PREFIX + "." + record.get('path') + ".minimum-user-limit-percent"] = record.get('minimum_user_limit_percent');
    json[this.PREFIX + "." + record.get('path') + ".maximum-capacity"] = record.get('maximum_capacity');
    json[this.PREFIX + "." + record.get('path') + ".user-limit-factor"] = record.get('user_limit_factor');
    json[this.PREFIX + "." + record.get('path') + ".state"] = record.get('state');
    json[this.PREFIX + "." + record.get('path') + ".capacity"] = record.get('capacity');
    json[this.PREFIX + "." + record.get('path') + ".queues"] = record.get('queues')||null;

    // do not set property if not set
    var ma = record.get('maximum_applications')||'';
    if (ma) {
      json[this.PREFIX + "." + record.get('path') + ".maximum-applications"] = ma;
    }

    // do not set property if not set
    var marp = record.get('maximum_am_resource_percent')||'';
    if (marp) {
      marp = marp/100; // convert back to decimal
      json[this.PREFIX + "." + record.get('path') + ".maximum-am-resource-percent"] = marp;
    }

    record.eachRelationship(function(key, relationship) {
      if (relationship.kind === 'belongsTo') {
        this.serializeBelongsTo(record, json, relationship);
      } else if (relationship.kind === 'hasMany') {
        this.serializeHasMany(record, json, relationship);
      }
    }, this);

    return json;
  },
  serializeHasMany:function (record, json, relationship) {
    var key = relationship.key;
    record.get(key).map(function (l,idx,labels) {
      json[[this.PREFIX, record.get('path'), 'accessible-node-labels'].join('.')] = (record.get('accessAllLabels'))?'*':labels.mapBy('name').join(',');
      if (!record.get('store.nodeLabels').findBy('name',l.get('name')).notExist) {
        json[[this.PREFIX, record.get('path'), 'accessible-node-labels', l.get('name'), 'capacity'].join('.')] = l.get('capacity');
        json[[this.PREFIX, record.get('path'), 'accessible-node-labels', l.get('name'), 'maximum-capacity'].join('.')] = l.get('maximum_capacity');
      }
    },this);
  }
});

App.LabelSerializer = DS.RESTSerializer.extend({
  serialize:function () {
    return {};
  }
});

App.TagSerializer = DS.RESTSerializer.extend({
  extractFindAll: function(store, type, payload){
    return this.extractArray(store, type, {'tag':payload.items});
  },
  normalizeHash: {
    tag: function(hash) {
      hash.id = hash.version;
      delete hash.version;
      delete hash.href;
      delete hash.Config;
      delete hash.type;
      return hash;
    }
  }
});
