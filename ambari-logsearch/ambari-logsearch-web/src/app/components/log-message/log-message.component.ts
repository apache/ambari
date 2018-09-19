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
import {
  Component,
  Input,
  AfterViewInit,
  ElementRef,
  ViewChild,
  OnChanges,
  OnInit,
  OnDestroy,
  SimpleChanges,
  ChangeDetectorRef
} from '@angular/core';
import {Subject} from 'rxjs/Subject';
import {Subscription} from 'rxjs/Subscription';
import 'rxjs/add/operator/auditTime';

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
export class LogMessageComponent implements AfterViewInit, OnChanges, OnInit, OnDestroy {

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
  isOpen = false;

  /**
   * This is a helper property to handle the changes on the parent component. The goal of this input is to be able to
   * react when the parent component (currently the log-list component) has changed (its size) in a way that the
   * LogMessageComponent should check if the caret should be visible or not.
   */
  @Input()
  refreshOn$: Subject<any>;

  /**
   * This will be shown as log message in the component
   */
  @Input()
  message: string;

  /**
   * This is a private flag to check if it should display the caret or not, it depends on the size of the size of
   * the content container element. Handled by the @checkAddCaret method
   * @type {boolean}
   */
  addCaret = false;

  private scrollWidth: number;

  /**
   * This is a regexp tester to check if the log message is multiline text or single line. Doing by checking the new
   * line characters.
   * @type {RegExp}
   */
  private readonly multiLineTestRegexp = /\r?\n|\r/;

  /**
   * This is a primary check if the message content does contain new line (/n) characters. If so than we display the
   * caret to give a possibility to the user to see the message as it is (pre-wrapped).
   * @type {boolean}
   */
  isMultiLineMessage = false;

  /**
   * The array to collect all the subscriptions created by the instance in order to unsubscribe when the component
   * destroyed
   */
  protected subscriptions: Subscription[] = [];

  constructor(private cdRef: ChangeDetectorRef) {}

  /**
   * This change handler's goal is to check if we should add the caret or not. Mainly it is because currently we have
   * the LogListComponent where columns can be added or removed and we have to recheck the visibility of the caret every
   * changes of the displayed columns.
   * @param {SimpleChanges} changes
   */
  ngOnChanges(changes: SimpleChanges): void {
    if (changes.message !== undefined) {
      this.message = this.message.trim();
      this.reCalculateOnChange();
      this.checkAddCaret();
    }
  }

  ngOnInit() {
    if (this.refreshOn$) {
      this.subscriptions.push(this.refreshOn$.subscribe(this.checkAddCaret));
    }
  }

  ngOnDestroy() {
    this.subscriptions.forEach((subscription: Subscription) => subscription.unsubscribe());
  }

  /**
   * The goal is to perform a initial caret display check when the component has been initialized.
   */
  ngAfterViewInit(): void {
    this.reCalculateOnChange();
    this.checkAddCaret();
  }

  reCalculateOnChange() {
    this.isMultiLineMessage = this.multiLineTestRegexp.test(this.message);
    this.scrollWidth = this.content.nativeElement.scrollWidth;
  }

  /**
   * The goal is to perform a height check on the content container element. It is based on the comparison of the
   * scrollHeight and the clientHeight.
   */
  checkAddCaret = (): void =>  {
    this.addCaret = this.isMultiLineMessage || (this.scrollWidth > this.content.nativeElement.clientWidth);
    this.cdRef.detectChanges();
  }

  /**
   * This is the click event handler of the caret button element. It will only toggle the isOpen property so that the
   * component element css classes will follow its state.
   * @param ev {MouseEvent}
   */
  onCaretClick(ev: MouseEvent) {
    ev.preventDefault();
    this.toggleOpen();
  }

  /**
   * This is a simple property toggle method of the @isOpen property.
   * The goal is to separate this logic from the event handling and give a way to call it from anywhere.
   */
  toggleOpen(): void {
    this.isOpen = !this.isOpen;
  }

}
