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

import {Component, OnInit, OnDestroy, Input, ViewChild, ElementRef, forwardRef} from '@angular/core';
import {ControlValueAccessor, NG_VALUE_ACCESSOR} from '@angular/forms';
import {Subject} from 'rxjs/Subject';
import {UtilsService} from '@app/services/utils.service';

@Component({
  selector: 'search-box',
  templateUrl: './search-box.component.html',
  styleUrls: ['./search-box.component.less'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SearchBoxComponent),
      multi: true
    }
  ]
})
export class SearchBoxComponent implements OnInit, OnDestroy, ControlValueAccessor {

  constructor(private element: ElementRef, private utils: UtilsService) {
    this.rootElement = element.nativeElement;
    this.rootElement.addEventListener('click', this.onRootClick);
    this.rootElement.addEventListener('keydown', this.onRootKeyDown);
  }

  ngOnInit() {
    this.parameterInput = this.parameterInputRef.nativeElement;
    this.valueInput = this.valueInputRef.nativeElement;
    this.parameterInput.addEventListener('focus', this.onParameterInputFocus);
    this.parameterInput.addEventListener('blur', this.onParameterInputBlur);
    this.valueInput.addEventListener('blur', this.onValueInputBlur);
    this.parameterNameChangeSubject.subscribe(this.onParameterNameChange);
  }

  ngOnDestroy() {
    this.rootElement.removeEventListener('click', this.onRootClick);
    this.rootElement.removeEventListener('keydown', this.onRootKeyDown);
    this.parameterInput.removeEventListener('focus', this.onParameterInputFocus);
    this.parameterInput.removeEventListener('blur', this.onParameterInputBlur);
    this.valueInput.removeEventListener('blur', this.onValueInputBlur);
    this.parameterNameChangeSubject.unsubscribe();
  }

  private currentId: number = 0;

  private isExclude: boolean = false;

  private defaultSubject: Subject<any> = new Subject();

  isActive: boolean = false;

  isParameterInput: boolean = false;

  isValueInput: boolean = false;

  currentValue: string;

  @Input()
  items: any[] = [];

  @Input()
  parameterNameChangeSubject: Subject<any> = this.defaultSubject;

  @ViewChild('parameterInput')
  parameterInputRef: ElementRef;

  @ViewChild('valueInput')
  valueInputRef: ElementRef;

  rootElement: HTMLElement;

  parameterInput: HTMLElement;

  valueInput: HTMLElement;

  activeItem?: any;

  parameters: any[] = [];

  private onChange: (fn: any) => void;

  private onRootClick = (): void => {
    if (!this.isActive) {
      this.parameterInput.focus();
    }
  };

  private onRootKeyDown = (event: KeyboardEvent): void => {
    if (this.utils.isEnterPressed(event)) {
      event.preventDefault();
    }
  };

  private onParameterInputFocus = (): void => {
    this.isActive = true;
    this.isValueInput = false;
    this.isParameterInput = true;
  };

  private onParameterInputBlur = (): void => {
    if (!this.isValueInput) {
      this.clear();
    }
  };

  private onValueInputBlur = (): void => {
    if (!this.isParameterInput) {
      this.clear();
    }
  };

  clear(): void {
    this.isActive = false;
    this.activeItem = null;
    this.currentValue = null;
  }

  itemsListFormatter(item: any): string {
    return item.name;
  }

  changeParameterName(item: any): void {
    this.parameterNameChangeSubject.next(item);
  }

  onParameterNameChange = (options: any): void => {
    this.activeItem = typeof options.item === 'string' ?
      this.items.find(field => field.value === options.item) : options.item;
    this.isExclude = options.isExclude;
    this.isActive = true;
    this.isParameterInput = false;
    this.isValueInput = true;
    this.currentValue = '';
    setTimeout(() => this.valueInput.focus(), 0);
  }

  onParameterValueChange(event: KeyboardEvent): void {
    if (this.utils.isEnterPressed(event) && this.currentValue) {
      this.parameters.push({
        id: this.currentId++,
        name: this.activeItem.value,
        label: this.activeItem.name,
        value: this.currentValue,
        isExclude: this.isExclude
      });
      this.currentValue = '';
      this.activeItem = null;
      this.isValueInput = false;
      this.updateValue();
    }
  }

  removeParameter(event: MouseEvent, id: number): void {
    this.parameters = this.parameters.filter(parameter => parameter.id !== id);
    this.updateValue();
    event.stopPropagation();
  }

  updateValue() {
    this.onChange(this.parameters);
  }

  writeValue() {
  }

  registerOnChange(callback: any): void {
    this.onChange = callback;
  }

  registerOnTouched() {
  }

}
