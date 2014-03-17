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

String.prototype.trim = function () {
  return this.replace(/^\s\s*/, '').replace(/\s\s*$/, '');
};

String.prototype.endsWith = function(suffix) {
  return this.indexOf(suffix, this.length - suffix.length) !== -1;
};

String.prototype.startsWith = function (prefix){
  return this.indexOf(prefix) == 0;
};

String.prototype.contains = function(substring) {
  return this.indexOf(substring) != -1;
};

String.prototype.capitalize = function () {
  return this.charAt(0).toUpperCase() + this.slice(1);
};

/**
 * Replace {i} with argument. where i is number of argument to replace with
 * @return {String}
 */
String.prototype.format = function () {
  var args = arguments;
  return this.replace(/{(\d+)}/g, function (match, number) {
    return typeof args[number] != 'undefined' ? args[number] : match;
  });
};

String.prototype.highlight = function (words, highlightTemplate) {
  var self = this;
  highlightTemplate = highlightTemplate ? highlightTemplate : "<b>{0}</b>";

  words.forEach(function (word) {
    var searchRegExp = new RegExp("\\b" + word + "\\b", "gi");
    self = self.replace(searchRegExp, function (found) {
      return highlightTemplate.format(found);
    });
  });

  return self;
};

Number.prototype.toDaysHoursMinutes = function () {
  var formatted = {},
    dateDiff = this,
    secK = 1000, //ms
    minK = 60 * secK, // sec
    hourK = 60 * minK, // sec
    dayK = 24 * hourK;

  dateDiff = parseInt(dateDiff);
  formatted.d = Math.floor(dateDiff / dayK);
  dateDiff -= formatted.d * dayK;
  formatted.h = Math.floor(dateDiff / hourK);
  dateDiff -= formatted.h * hourK;
  formatted.m = (dateDiff / minK).toFixed(2);

  return formatted;
};

/**
 Sort an array by the key specified in the argument.
 Handle only native js objects as element of array, not the Ember's object.

 Can be used as alternative to sortProperty method of Ember library
 in order to speed up executing on large data volumes

 @method sortBy
 @param {String} path name(s) to sort on
 @return {Array} The sorted array.
 */
Array.prototype.sortPropertyLight = function (path) {
  var realPath = (typeof path === "string") ? path.split('.') : [];
  this.sort(function (a, b) {
    var aProperty = a;
    var bProperty = b;
    realPath.forEach(function (key) {
      aProperty = aProperty[key];
      bProperty = bProperty[key];
    });
    if (aProperty > bProperty) return 1;
    if (aProperty < bProperty) return -1;
    return 0;
  });
  return this;
};

Em.CoreObject.reopen({
  t:function (key, attrs) {
    return Em.I18n.t(key, attrs)
  }
});

Em.Handlebars.registerHelper('log', function (variable) {
  console.log(variable);
});

Em.Handlebars.registerHelper('warn', function (variable) {
  console.warn(variable);
});

Em.Handlebars.registerHelper('highlight', function (property, words, fn) {
  var context = (fn.contexts && fn.contexts[0]) || this;
  property = Em.Handlebars.getPath(context, property, fn);

  words = words.split(";");

//  if (highlightTemplate == undefined) {
  var highlightTemplate = "<b>{0}</b>";
//  }

  words.forEach(function (word) {
    var searchRegExp = new RegExp("\\b" + word + "\\b", "gi");
    property = property.replace(searchRegExp, function (found) {
      return highlightTemplate.format(found);
    });
  });

  return new Em.Handlebars.SafeString(property);
});

App = require('app');

/**
 * Certain variables can have JSON in string
 * format, or in JSON format itself.
 */
App.parseJSON = function (value) {
  if (typeof value == "string") {
    return jQuery.parseJSON(value);
  }
  return value;
};
/**
 * Check for empty <code>Object</code>, built in Em.isEmpty()
 * doesn't support <code>Object</code> type
 *
 * @params obj {Object}
 *
 * @return {Boolean}
 */
App.isEmptyObject = function(obj) {
  var empty = true;
  for (var prop in obj) { if (obj.hasOwnProperty(prop)) {empty = false; break;} }
  return empty;
}

App.format = {

  /**
   * @type Object
   */
  components: {
    'APP_TIMELINE_SERVER': 'App Timeline Server',
    'DATANODE': 'DataNode',
    'DECOMMISSION_DATANODE': 'Update Exclude File',
    'DRPC_SERVER': 'DRPC Server',
    'FALCON': 'Falcon',
    'FALCON_CLIENT': 'Falcon Client',
    'FALCON_SERVER': 'Falcon Server',
    'FALCON_SERVICE_CHECK': 'Falcon Service Check',
    'FLUME_SERVER': 'Flume Agent',
    'GANGLIA_MONITOR': 'Ganglia Monitor',
    'GANGLIA_SERVER': 'Ganglia Server',
    'GLUSTERFS_CLIENT': 'GLUSTERFS Client',
    'GLUSTERFS_SERVICE_CHECK': 'GLUSTERFS Service Check',
    'GMETAD_SERVICE_CHECK': 'Gmetad Service Check',
    'GMOND_SERVICE_CHECK': 'Gmond Service Check',
    'HADOOP_CLIENT': 'Hadoop Client',
    'HBASE_CLIENT': 'HBase Client',
    'HBASE_MASTER': 'HBase Master',
    'HBASE_REGIONSERVER': 'HBase RegionServer',
    'HBASE_SERVICE_CHECK': 'HBase Service Check',
    'HCAT': 'HCat',
    'HCAT_SERVICE_CHECK': 'HCat Service Check',
    'HDFS_CLIENT': 'HDFS Client',
    'HDFS_SERVICE_CHECK': 'HDFS Service Check',
    'HISTORYSERVER': 'History Server',
    'HIVE_CLIENT': 'Hive Client',
    'HIVE_METASTORE': 'Hive Metastore',
    'HIVE_SERVER': 'HiveServer2',
    'HIVE_SERVICE_CHECK': 'Hive Service Check',
    'HUE_SERVER': 'Hue Server',
    'JAVA_JCE': 'Java JCE',
    'JOBTRACKER': 'JobTracker',
    'JOBTRACKER_SERVICE_CHECK': 'JobTracker Service Check',
    'JOURNALNODE': 'JournalNode',
    'KERBEROS_ADMIN_CLIENT': 'Kerberos Admin Client',
    'KERBEROS_CLIENT': 'Kerberos Client',
    'KERBEROS_SERVER': 'Kerberos Server',
    'LOGVIEWER_SERVER': 'Logviewer Server',
    'MAPREDUCE2_CLIENT': 'MapReduce2 Client',
    'MAPREDUCE2_SERVICE_CHECK': 'MapReduce2 Service Check',
    'MAPREDUCE_CLIENT': 'MapReduce Client',
    'MAPREDUCE_SERVICE_CHECK': 'MapReduce Service Check',
    'MYSQL_SERVER': 'MySQL Server',
    'NAGIOS_SERVER': 'Nagios Server',
    'NAMENODE': 'NameNode',
    'NAMENODE_SERVICE_CHECK': 'NameNode Service Check',
    'NIMBUS': 'Nimbus',
    'NODEMANAGER': 'NodeManager',
    'OOZIE_CLIENT': 'Oozie Client',
    'OOZIE_SERVER': 'Oozie Server',
    'OOZIE_SERVICE_CHECK': 'Oozie Service Check',
    'PIG': 'Pig',
    'PIG_SERVICE_CHECK': 'Pig Service Check',
    'RESOURCEMANAGER': 'ResourceManager',
    'SECONDARY_NAMENODE': 'SNameNode',
    'SQOOP': 'Sqoop',
    'SQOOP_SERVICE_CHECK': 'Sqoop Service Check',
    'STORM_REST_API': 'Storm REST API Server',
    'STORM_SERVICE_CHECK': 'Storm Service Check',
    'STORM_UI_SERVER': 'Storm UI Server',
    'SUPERVISOR': 'Supervisor',
    'TASKTRACKER': 'TaskTracker',
    'TEZ_CLIENT': 'Tez Client',
    'WEBHCAT_SERVER': 'WebHCat Server',
    'WEBHCAT_SERVICE_CHECK': 'WebHCat Service Check',
    'YARN_CLIENT': 'YARN Client',
    'YARN_SERVICE_CHECK': 'YARN Service Check',
    'ZKFC': 'ZKFailoverController',
    'ZOOKEEPER_CLIENT': 'ZooKeeper Client',
    'ZOOKEEPER_QUORUM_SERVICE_CHECK': 'ZK Quorum Service Check',
    'ZOOKEEPER_SERVER': 'ZooKeeper Server',
    'ZOOKEEPER_SERVICE_CHECK': 'ZooKeeper Service Check',
    'CLIENT': 'client'
  },

  /**
   * @type Object
   */
  command: {
    'INSTALL': 'Install',
    'UNINSTALL': 'Uninstall',
    'START': 'Start',
    'STOP': 'Stop',
    'EXECUTE': 'Execute',
    'ABORT': 'Abort',
    'UPGRADE': 'Upgrade',
    'RESTART': 'Restart',
    'SERVICE_CHECK': 'Check',
    'Excluded:': 'Decommission:',
    'Included:': 'Recommission:'
  },

  /**
   * convert role to readable string
   * @param role
   */
  role:function (role) {
    return this.components[role] ? this.components[role] : '';
  },

  /**
   * convert command_detail to readable string, show the string for all tasks name
   * @param command_detail
   */
  commandDetail: function (command_detail) {
    var detailArr = command_detail.split(' ');
    var self = this;
    var result = '';
    detailArr.forEach( function(item) {
      // if the item has the pattern SERVICE/COMPONENT, drop the SERVICE part
      if (item.contains('/')) {
        item = item.split('/')[1];
      }
      // ignore 'DECOMMISSION', command came from 'excluded/included'
      if (item == 'DECOMMISSION,') {
        item = '';
      }
      if (self.components[item]) {
        result = result + ' ' + self.components[item];
      } else if (self.command[item]) {
        result = result + ' ' + self.command[item];
      } else {
        result = result + ' ' + item;
      }
    });
    if (result === ' nagios_update_ignore ACTIONEXECUTE') {
       result = Em.I18n.t('common.maintenance.task');
    }
    return result;
  },

  /**
   * PENDING - Not queued yet for a host
   * QUEUED - Queued for a host
   * IN_PROGRESS - Host reported it is working
   * COMPLETED - Host reported success
   * FAILED - Failed
   * TIMEDOUT - Host did not respond in time
   * ABORTED - Operation was abandoned
   */
  taskStatus:function (_taskStatus) {
    return _taskStatus.toLowerCase();
  }
};

/**
 * wrapper to bootstrap popover
 * fix issue when popover stuck on view routing
 * @param self
 * @param options
 */
App.popover = function(self, options) {
  self.popover(options);
  self.on("remove", function () {
    $(this).trigger('mouseleave');
  });
};

/**
 * wrapper to bootstrap tooltip
 * fix issue when tooltip stuck on view routing
 * @param self - DOM element
 * @param options
 */
App.tooltip = function(self, options) {
  self.tooltip(options);
  self.on("remove", function () {
    $(this).trigger('mouseleave');
  });
};

/**
 * wrapper to Date().getTime()
 * fix issue when client clock and server clock not sync
 * @return timeStamp of current server clock
 */
App.dateTime = function() {
  return new Date().getTime() + App.clockDistance;
};

/*
 * Helper function for bound property helper registration
 * @params name {String} - name of helper
 * @params view {Em.View} - view
 */
App.registerBoundHelper = function(name, view) {
  Em.Handlebars.registerHelper(name, function(property, options) {
    options.hash.contentBinding = property;
    return Em.Handlebars.helpers.view.call(this, view, options);
  });
};

/*
 * Return singular or plural word based on Em.I18n property key.
 *
 *  Example: {{pluralize hostsCount singular="t:host" plural="t:hosts"}}
 */
App.registerBoundHelper('pluralize', Em.View.extend({
  tagName: 'span',
  template: Em.Handlebars.compile('{{view.wordOut}}'),

  wordOut: function() {
    var count, singular, plural;
    count = this.get('content');
    singular = this.get('singular');
    plural = this.get('plural');
    return this.getWord(count, singular, plural);
  }.property('content'),

  getWord: function(count, singular, plural) {
    singular = this.tDetect(singular);
    plural = this.tDetect(plural);
    if (singular && plural) {
      if (count > 1) {
        return plural;
      } else {
        return singular;
      }
    }
    return '';
  },
  /*
   * Detect for Em.I18n.t reference call
   * @params word {String}
   * return {String}
  */
  tDetect: function(word) {
    var splitted = word.split(':');
    if (splitted.length > 1 && splitted[0] == 't') {
      return Em.I18n.t(splitted[1]);
    } else {
      return splitted[0];
    }
  }
  })
);
/**
 * Return defined string instead of empty if value is null/undefined
 * by default is `n/a`.
 *
 * @param empty {String} - value instead of empty string (not required)
 *  can be used with Em.I18n pass value started with't:'
 *
 * Examples:
 *
 * default value will be returned
 * {{formatNull service.someValue}}
 *
 * <code>empty<code> will be returned
 * {{formatNull service.someValue empty="I'm empty"}}
 *
 * Em.I18n translation will be returned
 * {{formatNull service.someValue empty="t:my.key.to.translate"
 */
App.registerBoundHelper('formatNull', Em.View.extend({
  tagName: 'span',
  template: Em.Handlebars.compile('{{view.result}}'),

  result: function() {
    var emptyValue = this.get('empty') ? this.get('empty') : Em.I18n.t('services.service.summary.notAvailable');
    emptyValue = emptyValue.startsWith('t:') ? Em.I18n.t(emptyValue.substr(2, emptyValue.length)) : emptyValue;
    return (this.get('content') || this.get('content') == 0) ? this.get('content') : emptyValue;
  }.property('content')
}));

/**
 * Return formatted string with inserted <code>wbr</code>-tag after each dot
 *
 * @param {String} content
 *
 * Examples:
 *
 * returns 'apple'
 * {{formatWordBreak 'apple'}}
 *
 * returns 'apple.<wbr />banana'
 * {{formatWordBreak 'apple.banana'}}
 *
 * returns 'apple.<wbr />banana.<wbr />uranium'
 * {{formatWordBreak 'apple.banana.uranium'}}
 */
App.registerBoundHelper('formatWordBreak', Em.View.extend({
  tagName: 'span',
  template: Em.Handlebars.compile('{{{view.result}}}'),

  /**
   * @type {string}
   */
  result: function() {
    return this.get('content').replace(/\./g, '.<wbr />');
  }.property('content')
}));

/**
 * Ambari overrides the default date transformer.
 * This is done because of the non-standard data
 * sent. For example Nagios sends date as "12345678".
 * The problem is that it is a String and is represented
 * only in seconds whereas Javascript's Date needs
 * milliseconds representation.
 */
DS.attr.transforms.date = {
  from: function (serialized) {
    var type = typeof serialized;
    if (type === Em.I18n.t('common.type.string')) {
      serialized = parseInt(serialized);
      type = typeof serialized;
    }
    if (type === Em.I18n.t('common.type.number')) {
      if (!serialized ){  //serialized timestamp = 0;
        return 0;
      }
      // The number could be seconds or milliseconds.
      // If seconds, then the length is 10
      // If milliseconds, the length is 13
      if (serialized.toString().length < 13) {
        serialized = serialized * 1000;
      }
      return new Date(serialized);
    } else if (serialized === null || serialized === undefined) {
      // if the value is not present in the data,
      // return undefined, not null.
      return serialized;
    } else {
      return null;
    }
  },
  to: function (deserialized) {
    if (deserialized instanceof Date) {
      return deserialized.getTime();
    } else if (deserialized === undefined) {
      return undefined;
    } else {
      return null;
    }
  }
};

DS.attr.transforms.object = {
  from: function(serialized) {
    return Ember.none(serialized) ? null : Object(serialized);
  },

  to: function(deserialized) {
    return Ember.none(deserialized) ? null : Object(deserialized);
  }
};

/**
 * Allows EmberData models to have array properties.
 *
 * Declare the property as <code>
 *  operations: DS.attr('array'),
 * </code> and
 * during load provide a JSON array for value.
 *
 * This transform simply assigns the same array in both directions.
 */
DS.attr.transforms.array = {
  from : function(serialized) {
    return serialized;
  },
  to : function(deserialized) {
    return deserialized;
  }
};
