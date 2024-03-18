/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {Component, Input} from '@angular/core';

enum Side {
  LEFT = "left",
  RIGHT = "right"
}

/**
 * The goal of this component to have a simple BS panel with a collapse link in the panel heading. So that adding
 * components/content into the body of the panel we can hide and show the its content.
 * @class CollapsiblePanelComponent
 */
@Component({
  selector: 'collapsible-panel',
  templateUrl: './collapsible-panel.component.html',
  styleUrls: ['./collapsible-panel.component.less']
})
export class CollapsiblePanelComponent {

  /**
   * This is for the common title of the panel. If the openTitle or the collapsedTitle not set this will be displayed.
   * @type {string}
   */
  @Input()
  commonTitle: string = '';

  /**
   * The panel's title for the opened state
   * @type {string}
   */
  @Input()
  openTitle?: string;

  /**
   * The panel's title fo the closed/collapsed state
   * @type {string}
   */
  @Input()
  collapsedTitle?: string;

  /**
   * This property indicates the position of the caret. It can be 'left' or 'right'
   * @type {Side}
   */
  @Input()
  caretSide: Side = Side.LEFT;

  /**
   * The flag to indicate the collapsed state.
   * @type {boolean}
   */
  @Input()
  isCollapsed: boolean = false;

  /**
   * The goal is to handle the click event of the collapse link/button. It will simply call the inside logic to toggle
   * the collapsed state. The goal is to separate the functions by responsibility.
   * @param {MouseEvent} ev
   */
  handleCollapseBtnClick(ev: MouseEvent): void {
    this.toggleCollapsed();
    ev.preventDefault();
  }

  /**
   * The goal is to simply negate the current collapse state.
   */
  toggleCollapsed(): void {
    this.isCollapsed = !this.isCollapsed;
  }

}
