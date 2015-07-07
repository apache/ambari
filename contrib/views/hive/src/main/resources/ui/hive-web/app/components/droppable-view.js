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
import droppable from 'hive/mixins/droppable';

export default Ember.Component.extend(droppable, {
  classNames: ['drop-target'],
  activeClass_drop: 'active',
  hoverClass_drop: 'ui-state-hover',
  self: this,

  drop_drop: function(event, ui) {
    var draggable_name = this.$(ui.draggable).attr('data-name');
    console.log(draggable_name + " dropped");
    console.log(ui);
    console.log(this.$(ui.draggable).first());
    this.$().html("");
    this.$().addClass("disabled");
    //this.$().append( this.$(ui.draggable).first().clone() );
    var field_name =
    this.$("<div/>")
      .addClass( "pull-left btn-text truncate" )
      .html(draggable_name);
    var remove_field =
    this.$("<div/>")
      .addClass("pull-right no-padding close-container")
      .html("<i class='fa fa-close field-remove pull-right'></i>");
    this.$("<div/>")
      .append(field_name)
      .append(remove_field)
      .css("padding", "3px 10px")
      .appendTo( this.$() );

    this.set('disabled_drop', true);
  },

});
