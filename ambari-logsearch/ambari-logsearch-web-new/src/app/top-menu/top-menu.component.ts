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

import {Component, OnInit} from '@angular/core';

@Component({
  selector: 'top-menu',
  templateUrl: './top-menu.component.html',
  styleUrls: ['./top-menu.component.less']
})
export class TopMenuComponent implements OnInit {

  constructor() {
  }

  ngOnInit() {
  }

  //TODO implement loading of real data into subItems
  readonly items = [
    {
      iconClassNames: ['fa', 'fa-arrow-left'],
      title: 'topMenu.undo',
      action: 'undo',
      subItems: [
        {
          title: 'Item 1'
        },
        {
          title: 'Item 2'
        }
      ]
    },
    {
      iconClassNames: ['fa', 'fa-arrow-right'],
      title: 'topMenu.redo',
      action: 'redo',
      subItems: [
        {
          title: 'Item 1'
        },
        {
          title: 'Item 2'
        }
      ]
    },
    {
      iconClassNames: ['fa', 'fa-refresh'],
      title: 'topMenu.refresh',
      action: 'refresh'
    },
    {
      iconClassNames: ['fa', 'fa-clock-o'],
      title: 'topMenu.history',
      action: 'openHistory',
      subItems: [
        {
          title: 'Item 1'
        },
        {
          title: 'Item 2'
        }
      ]
    },
    {
      iconClassNames: ['fa', 'fa-bell'],
      action: 'openAlerts'
    },
    {
      iconClassNames: ['fa', 'fa-user'],
      action: 'openUserDetails'
    }
  ];

}
