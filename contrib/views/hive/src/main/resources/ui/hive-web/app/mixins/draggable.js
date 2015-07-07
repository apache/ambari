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
import dragWidget from 'hive/mixins/jquery-drag-widget'

export default Ember.Mixin.create(dragWidget, {
  uiDragOptions: ['disabled_drag', 'addClasses_drag', 'appendTo_drag', 'axis_drag', 'cancel_drag', 'connectToSortable_drag', 'containment_drag', 'cursor_drag',
    'delay_drag', 'distance_drag', 'grid_drag', 'handle_drag', 'helper_drag', 'iframeFix_drag', 'opacity_drag', 'revert_drag', 'scope_drag', 'snap_drag', 'snapMode_drag', 'stack_drag'],
  uiDragEvents: ['create_drag', 'start_drag', 'drag_drag', 'stop_drag'],
});
