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

import {Component, OnInit, AfterViewInit, ElementRef, Input, Output, ContentChild, TemplateRef, EventEmitter} from '@angular/core';
import * as $ from 'jquery';

@Component({
  selector: 'modal',
  templateUrl: './modal.component.html'
})
export class ModalComponent implements OnInit, AfterViewInit {

  constructor(private element: ElementRef) {
    this.rootElement = $(element.nativeElement);
  }

  ngOnInit() {
    this.modalElements = this.rootElement.find('.in');
    this.show();
  }

  ngAfterViewInit() {
    this.init.emit();
  }

  private rootElement: JQuery;

  private modalElements: JQuery;

  @Input()
  showHeader: boolean = true;

  @Input()
  title: string = '';

  @Input()
  showCloseButton: boolean = true;

  @Input()
  bodyText: string = '';

  @Input()
  showFooter: boolean = true;

  @Input()
  showSubmitButton: boolean = true;

  @Input()
  submitButtonLabel: string = 'modal.submit';

  @Input()
  submitButtonClassName: string = 'btn-success';

  @Input()
  showCancelButton: boolean = true;

  @Input()
  cancelButtonLabel: string = 'modal.cancel';

  @Input()
  cancelButtonClassName: string = 'btn-default';

  @Input()
  isSmallModal: boolean = false;

  @Input()
  isLargeModal: boolean = false;

  @ContentChild(TemplateRef)
  bodyTemplate;

  @Output()
  init: EventEmitter<any> = new EventEmitter();

  @Output()
  submit: EventEmitter<any> = new EventEmitter();

  @Output()
  cancel: EventEmitter<any> = new EventEmitter();

  @Output()
  close: EventEmitter<any> = new EventEmitter();

  show(): void {
    this.modalElements.show();
  }

  hide(): void {
    this.modalElements.hide();
  }

  onSubmit(): void {
    this.hide();
    this.submit.emit();
  }

  onCancel(): void {
    this.hide();
    this.cancel.emit();
  }

  onClose(): void {
    this.hide();
    this.close.emit();
  }

}
