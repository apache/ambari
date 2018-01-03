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

import {Component, Input, ViewChild, ElementRef} from '@angular/core';
import {ListItem} from '@app/classes/list-item';
import {ServiceInjector} from '@app/classes/service-injector';
import {ComponentActionsService} from '@app/services/component-actions.service';

@Component({
  selector: 'menu-button',
  templateUrl: './menu-button.component.html',
  styleUrls: ['./menu-button.component.less']
})
export class MenuButtonComponent {

  constructor() {
    this.actions = ServiceInjector.injector.get(ComponentActionsService);
  }

  @ViewChild('dropdown')
  dropdown: ElementRef;

  @Input()
  label?: string;

  @Input()
  action: string;

  @Input()
  iconClass: string;

  @Input()
  labelClass?: string;

  @Input()
  subItems?: ListItem[];

  @Input()
  isMultipleChoice: boolean = false;

  @Input()
  hideCaret: boolean = false;

  @Input()
  isRightAlign: boolean = false;

  @Input()
  additionalLabelComponentSetter?: string;

  @Input()
  badge: string;

  @Input()
  caretClass: string = 'fa-caret-down';

  /**
   * The minimum time to handle a mousedown as a longclick. Default is 500 ms (0.5sec)
   * @default 500
   * @type {number}
   */
  @Input()
  minLongClickDelay: number = 500;

  /**
   * The maximum milliseconds to wait for longclick ends. The default is 0 which means no upper limit.
   * @default 0
   * @type {number}
   */
  @Input()
  maxLongClickDelay: number = 0;

  private actions: ComponentActionsService;

  /**
   * This is a private property to indicate the mousedown timestamp, so that we can check it when teh click event
   * has been triggered.
   */
  private mouseDownTimestamp: number;

  /**
   * Indicates if the dropdown list is open or not. So that we use internal state to display or hide the dropdown.
   * @type {boolean}
   */
  private dropdownIsOpen: boolean = false;

  get hasSubItems(): boolean {
    return Boolean(this.subItems && this.subItems.length);
  }

  get hasCaret(): boolean {
    return this.hasSubItems && !this.hideCaret;
  }

  /**
   * Handling the click event on the component element.
   * Two goal:
   * - check if we have a 'longclick' event and open the dropdown (if any) when longclick event happened
   * - trigger the action or the dropdown open depending on the target element (caret will open the dropdown otherwise
   * trigger the action.
   * @param {MouseEvent} event
   */
  onMouseClick(event: MouseEvent): void {
    let el = <HTMLElement>event.target;
    let now = Date.now();
    let mdt = this.mouseDownTimestamp; // mousedown time
    let isLongClick = mdt && mdt + this.minLongClickDelay <= now && (
      !this.maxLongClickDelay || mdt + this.maxLongClickDelay >= now
    );
    let openDropdown = this.hasSubItems && (
      el.classList.contains(this.caretClass) || isLongClick || !this.actions[this.action]
    );
    if (openDropdown && this.dropdown) {
      if (this.toggleDropdown()) {
        this.listenToClickOut();
      }
    } else if (this.action) {
      this.actions[this.action]();
    }
    this.mouseDownTimestamp = 0;
    event.preventDefault();
  }

  /**
   * Listening the click event on the document so that we can hide our dropdown list if the event source is not the
   * component.
   */
  private listenToClickOut = (): void => {
    this.dropdownIsOpen && document.addEventListener('click', this.onDocumentMouseClick);
  };

  /**
   * Handling the click event on the document to hide the dropdown list if it needs.
   * @param {MouseEvent} event
   */
  private onDocumentMouseClick = (event: MouseEvent): void => {
    let el = <HTMLElement>event.target;
    if (!this.dropdown.nativeElement.contains(el)) {
      this.closeDropdown();
      this.removeDocumentClickListener()
    }
  };

  /**
   * Handling the mousedown event, so that we can check the long clicks and open the dropdown if any.
   * @param {MouseEvent} event
   */
  onMouseDown = (event: MouseEvent): void => {
    if (this.hasSubItems) {
      let el = <HTMLElement>event.target;
      if (!el.classList.contains(this.caretClass)) {
        this.mouseDownTimestamp = Date.now();
      }
    }
  };

  /**
   * The goal is to have one and only one place where we open the dropdown. So that later if we need to change the way
   * how we do, it will be easier.
   */
  private openDropdown():void {
    this.dropdownIsOpen = true;
  }

  /**
   * The goal is to have one and only one place where we close the dropdown. So that later if we need to change the way
   * how we do, it will be easier.
   */
  private closeDropdown():void {
    this.dropdownIsOpen = false;
  }

  /**
   * Just a simple helper method to make the dropdown toggle more easy.
   * @returns {boolean} It will return the open state of the dropdown;
   */
  private toggleDropdown(): boolean {
    this[this.dropdownIsOpen ? 'closeDropdown' : 'openDropdown']();
    return this.dropdownIsOpen;
  }

  /**
   * The goal is to simply remove the click event listeners from the document.
   */
  private removeDocumentClickListener(): void {
    document.removeEventListener('click', this.onDocumentMouseClick);
  }

  /**
   * The main goal if this function is tho handle the item change event on the child dropdown list.
   * Should update the value and close the dropdown if it is not multiple choice type.
   * @param {ListItem} options The selected item(s) from the dropdown list.
   */
  onDropdownItemChange(options: ListItem) {
    this.updateSelection(options);
    !this.isMultipleChoice && this.closeDropdown();
  }

  updateSelection(options: ListItem) {
    // TODO implement value change behaviour
  }

}
