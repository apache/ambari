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
import {Component, Input, AfterViewInit, ElementRef, ViewChild, OnChanges, SimpleChanges, HostListener, ChangeDetectorRef} from '@angular/core';

/**
 * This is a simple UI component to display the log message. The goal is to be able to show one line and be collapsile
 * to show the full log message with new lines.
 * @class LogMessageComponent
 */
@Component({
  selector: 'log-message',
  templateUrl: './log-message.component.html',
  styleUrls: ['./log-message.component.less']
})
export class LogMessageComponent implements AfterViewInit, OnChanges {

  /**
   * This is the element reference to the message log container element. So that we can calculate if the caret should be
   * displayed or not.
   * @type ElementRef
   */
  @ViewChild('content') content: ElementRef;

  /**
   * This is the flag property to indicate if the content container is open or not.
   * @type {boolean}
   */
  @Input()
  isOpen: boolean = false;

  /**
   * This is a helper property to handle the changes on the parent component. The goal of this input is to be able to
   * react when the parent component (currently the log-list component) has changed (its size) in a way that the
   * LogMessageComponent should check if the caret should be visible or not.
   */
  @Input()
  listenChangesOn: any;

  /**
   * This is a private flag to check if it should display the caret or not, it depends on the size of the size of
   * the content container element. Handled by the @checkAddCaret method
   * @type {boolean}
   */
  private addCaret: boolean = false;

  /**
   * This is a primary check if the message content does contain new line (/n) characters. If so than we display the
   * caret to give a possibility to the user to see the message as it is (pre-wrapped).
   * @type {boolean}
   */
  private isMultiLineMessage: boolean = false;

  constructor(private cdRef:ChangeDetectorRef) {}

  /**
   * This change handler's goal is to check if we should add the caret or not. Mainly it is because currently we have
   * the LogListComponent where columns can be added or removed and we have to recheck the visibility of the caret every
   * changes of the displayed columns.
   * @param {SimpleChanges} changes
   */
  ngOnChanges(changes: SimpleChanges): void {
    if (changes.listenChangesOn !== undefined) {
      this.checkAddCaret();
    }
  }

  /**
   * The goal is to perform a initial caret display check when the component has been initialized.
   */
  ngAfterViewInit(): void {
    let text = this.content.nativeElement.textContent;
    let newLinePos = text.indexOf('\n');
    this.isMultiLineMessage = ((text.length - 1) > newLinePos) && (newLinePos > 0);
    this.checkAddCaret();
  }

  /**
   * Since the size of the column is depends on the window size we have to listen the resize event and show/hide the
   * caret corresponding the new size of the content container element.
   * Using the arrow function will keep the instance scope.
   */
  @HostListener('window:resize', ['$event'])
  onWindowResize = (): void => {
    this.isMultiLineMessage || this.checkAddCaret();
  };

  /**
   * The goal is to perform a height check on the content container element. It is based on the comparison of the
   * scrollHeight and the clientHeight.
   */
  checkAddCaret = (): void =>  {
    let el = this.content.nativeElement;
    this.addCaret = this.isMultiLineMessage || (el.scrollHeight > el.clientHeight);
    this.cdRef.detectChanges();
  };

  /**
   * This is the click event handler of the caret button element. It will only toggle the isOpen property so that the
   * component element css classes will follow its state.
   * @param ev {MouseEvent}
   */
  onCaretClick(ev:MouseEvent) {
    ev.preventDefault();
    this.toggleOpen();
  }

  /**
   * This is a simple property toggle method of the @isOpen property.
   * The goal is to separate this logic from the event handling and give a way to call it from anywhere.
   */
  toggleOpen():void {
    this.isOpen = !this.isOpen;
  }

}
