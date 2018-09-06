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

import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'modal-dialog',
  templateUrl: './modal-dialog.component.html',
  styleUrls: ['./modal-dialog.component.less']
})
export class ModalDialogComponent implements OnInit {

  @Input()
  title: string;

  @Input()
  extraCssClass: string;

  @Input()
  showCloseBtn = true;

  @Input()
  showBackdrop = true;

  @Input()
  closeOnBackdropClick = true;

  @Input()
  visible = false;

  @Output()
  onCloseRequest: EventEmitter<MouseEvent> =  new EventEmitter();

  constructor() { }

  ngOnInit() {
  }

  onCloseBtnClick(event: MouseEvent) {
    this.onCloseRequest.emit(event);
  }

  onBackdropClick(event: MouseEvent) {
    if (this.closeOnBackdropClick) {
      this.onCloseRequest.emit(event);
    }
  }

}
