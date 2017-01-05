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
import MediaService from 'ember-responsive/media';

const { K, getOwner } = Ember;
const { classify } = Ember.String;

MediaService.reopen({
  // Change this if you want a different default breakpoint in tests.
  _defaultBreakpoint: 'desktop',

  _breakpointArr: Ember.computed('breakpoints', function() {
    return Object.keys(this.get('breakpoints')) || Ember.A([]);
  }),

  _forceSetBreakpoint(breakpoint) {
    let found = false;

    const props = {};
    this.get('_breakpointArr').forEach(function(bp) {
      const val = bp === breakpoint;
      if (val) {
        found = true;
      }

      props[`is${classify(bp)}`] = val;
    });

    if (found) {
      this.setProperties(props);
    } else {
      throw new Error(
        `You tried to set the breakpoint to ${breakpoint}, which is not in your app/breakpoint.js file.`
      );
    }
  },

  match: K, // do not set up listeners in test

  init() {
    this._super(...arguments);

    this._forceSetBreakpoint(this.get('_defaultBreakpoint'));
  }
});

export default Ember.Test.registerAsyncHelper('setBreakpoint', function(app, breakpoint) {
  // this should use getOwner once that's supported
  const mediaService = app.__deprecatedInstance__.lookup('service:media');
  mediaService._forceSetBreakpoint(breakpoint);
});

export function setBreakpointForIntegrationTest(container, breakpoint) {
  const mediaService = getOwner(container).lookup('service:media');
  mediaService._forceSetBreakpoint(breakpoint);
  container.set('media', mediaService);

  return mediaService;
}
