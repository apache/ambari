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
require('views/common/timeline_view');

describe('App.Timeline', function() {
  describe('#sortedEvents', function () {
    it('Sorts events by date', function () {
      var view = App.Timeline.create();
      view.set('events', [
        {
          date: new Date(2017, 4, 1)
        },
        {
          date: new Date(2017, 1, 1)
        },
        {
          date: new Date(2017, 5, 1)
        },
        {
          date: new Date(2017, 3, 1)
        },
        {
          date: new Date(2017, 2, 1)
        }
      ]);

      var expected = [
        {
          date: new Date(2017, 1, 1)
        },
        {
          date: new Date(2017, 2, 1)
        },
        {
          date: new Date(2017, 3, 1)
        },
        {
          date: new Date(2017, 4, 1)
        },
        {
          date: new Date(2017, 5, 1)
        }
      ];
      
      var sortedEvents = view.get('sortedEvents');
      var actual = [
        sortedEvents.objectAt(0),
        sortedEvents.objectAt(1),
        sortedEvents.objectAt(2),
        sortedEvents.objectAt(3),
        sortedEvents.objectAt(4),
      ];

      expect(actual).to.deep.equal(expected);
    });

    it('Sorts events using the provided datePath', function () {
      var view = App.Timeline.create();
      view.set('datePath', 'path.to.date');
      view.set('events', [
        {
          path: {
            to: {
              date: new Date(2017, 4, 1)
            }
          }
        },
        {
          path: {
            to: {
              date: new Date(2017, 1, 1)
            }
          }
        },
        {
          path: {
            to: {
              date: new Date(2017, 5, 1)
            }
          }
        },
        {
          path: {
            to: {
              date: new Date(2017, 3, 1)
            }
          }
        },
        {
          path: {
            to: {
              date: new Date(2017, 2, 1)
            }
          }
        }
      ]);

      var expected = [
        {
          path: {
            to: {
              date: new Date(2017, 1, 1)
            }
          }
        },
        {
          path: {
            to: {
              date: new Date(2017, 2, 1)
            }
          }
        },
        {
          path: {
            to: {
              date: new Date(2017, 3, 1)
            }
          }
        },
        {
          path: {
            to: {
              date: new Date(2017, 4, 1)
            }
          }
        },
        {
          path: {
            to: {
              date: new Date(2017, 5, 1)
            }
          }
        }
      ];
      
      var sortedEvents = view.get('sortedEvents');
      var actual = [
        sortedEvents.objectAt(0),
        sortedEvents.objectAt(1),
        sortedEvents.objectAt(2),
        sortedEvents.objectAt(3),
        sortedEvents.objectAt(4),
      ];

      expect(actual).to.deep.equal(expected);
    });

    it('Sorts events using the provided function', function () {
      var view = App.Timeline.create();
      var sort = function (a, b) {
        return a.id - b.id;
      };

      view.set('sort', sort);
      view.set('events', [
        {
          id: 3
        },
        {
          id: 2
        },
        {
          id: 5
        },
        {
          id: 1
        },
        {
          id: 4
        }
      ]);

      var expected = [
        {
          id: 1
        },
        {
          id: 2
        },
        {
          id: 3
        },
        {
          id: 4
        },
        {
          id: 5
        }
      ];
      
      var sortedEvents = view.get('sortedEvents');
      var actual = [
        sortedEvents.objectAt(0),
        sortedEvents.objectAt(1),
        sortedEvents.objectAt(2),
        sortedEvents.objectAt(3),
        sortedEvents.objectAt(4),
      ];

      expect(actual).to.deep.equal(expected);
    });
  });
});