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

App.RecurceQueuesComponent = Em.View.extend({
  templateName: "components/queueListItem",
  depth:0,
  parent:'',
  leaf:function () {
    return this.get('controller.arrangedContent')
      .filterBy('depth',this.get('depth'))
      .filterBy('parentPath',this.get('parent'));
  }.property('depth','parent','controller.content.length','controller.content.@each.name'),
  childDepth:function () {
    return this.get('leaf.firstObject.depth')+1;
  }.property('depth'),
  didInsertElement:function () {
    Ember.run.scheduleOnce('afterRender',null, this.setFirstAndLast, this);
  },
  setFirstAndLast:function (item) {
    var items = item.$().parents('.queue-list').find('.list-group-item');
    items.first().addClass('first');
    items.last().addClass('last');
  }

});

App.DiffTooltipComponent = Em.Component.extend({
  classNames:'fa fa-fw fa-lg blue fa-pencil'.w(),
  tagName:'i',
  queue:null,
  initTooltip:function  () {
    var queue = this.get('queue');
    this.$().tooltip({
        title:function () {
          var caption = '',
              fmtString = '<span>%@: %@ -> %@</span>\n',
              emptyValue = '<small><em>not set</em></small>',
              changes = queue.changedAttributes(),
              idsToNames = function (l) {
                return l.split('.').get('lastObject');
              },
              formatChangedAttributes = function (prefix,item) {
                // don't show this to user.
                if (item == '_accessAllLabels') return;

                var oldV = this[item].objectAt(0),
                    newV = this[item].objectAt(1);

                caption += fmtString.fmt(
                    [prefix,item].compact().join('.'),
                    (oldV != null && '\'%@\''.fmt(oldV)) || emptyValue,
                    (newV != null && '\'%@\''.fmt(newV)) || emptyValue
                  );
              },
              initialLabels,
              currentLabels,
              isAllChanged,
              oldV,
              newV;

          Em.keys(changes).forEach(Em.run.bind(changes,formatChangedAttributes,null));

          if (queue.constructor.typeKey === 'queue') {
            //cpmpare labels
            isAllChanged = changes.hasOwnProperty('_accessAllLabels');
            initialLabels = queue.get('initialLabels').sort();
            currentLabels = queue.get('labels').mapBy('id').sort();

            if (queue.get('isLabelsDirty') || isAllChanged) {

              oldV = ((isAllChanged && changes._accessAllLabels.objectAt(0)) || (!isAllChanged && queue.get('_accessAllLabels')))?'*':initialLabels.map(idsToNames).join(',') || emptyValue;
              newV = ((isAllChanged && changes._accessAllLabels.objectAt(1)) || (!isAllChanged && queue.get('_accessAllLabels')))?'*':currentLabels.map(idsToNames).join(',') || emptyValue;

              caption += fmtString.fmt('accessible-node-labels', oldV, newV);
            }

            queue.get('labels').forEach(function (label) {
              var labelsChanges = label.changedAttributes(),
                  prefix = ['accessible-node-labels',label.get('name')].join('.');
              Em.keys(labelsChanges).forEach(Em.run.bind(labelsChanges,formatChangedAttributes,prefix));
            });
          }

          return caption;
        },
        html:true,
        placement:'bottom'
      });
  }.on('didInsertElement')
});
