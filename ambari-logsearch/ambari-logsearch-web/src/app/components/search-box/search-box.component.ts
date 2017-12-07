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
import {SearchBoxParameter, SearchBoxParameterProcessed, SearchBoxParameterTriggered} from '@app/classes/filtering';
import {CommonEntry} from '@app/classes/models/common-entry';
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

  ngOnInit(): void {
    this.parameterInput = this.parameterInputRef.nativeElement;
    this.valueInput = this.valueInputRef.nativeElement;
    this.parameterInput.addEventListener('focus', this.onParameterInputFocus);
    this.parameterInput.addEventListener('blur', this.onParameterInputBlur);
    this.valueInput.addEventListener('blur', this.onValueInputBlur);
    this.parameterNameChangeSubject.subscribe(this.onParameterNameChange);
    this.parameterAddSubject.subscribe(this.onParameterAdd);
  }

  ngOnDestroy(): void {
    this.rootElement.removeEventListener('click', this.onRootClick);
    this.rootElement.removeEventListener('keydown', this.onRootKeyDown);
    this.parameterInput.removeEventListener('focus', this.onParameterInputFocus);
    this.parameterInput.removeEventListener('blur', this.onParameterInputBlur);
    this.valueInput.removeEventListener('blur', this.onValueInputBlur);
    this.parameterNameChangeSubject.unsubscribe();
    this.parameterAddSubject.unsubscribe();
  }

  private readonly messageParameterName: string = 'log_message';

  private currentId: number = 0;

  private isExclude: boolean = false;

  private defaultSubject: Subject<any> = new Subject();

  isActive: boolean = false;

  isParameterInput: boolean = false;

  isValueInput: boolean = false;

  currentValue: string;

  @Input()
  items: CommonEntry[] = [];

  @Input()
  itemsOptions: {[key: string]: CommonEntry[]};

  @Input()
  parameterNameChangeSubject: Subject<SearchBoxParameterTriggered> = this.defaultSubject;

  @Input()
  parameterAddSubject: Subject<SearchBoxParameter> = this.defaultSubject;

  @ViewChild('parameterInput')
  parameterInputRef: ElementRef;

  @ViewChild('valueInput')
  valueInputRef: ElementRef;

  private rootElement: HTMLElement;

  private parameterInput: HTMLInputElement;

  private valueInput: HTMLInputElement;

  activeItem: CommonEntry | null = null;

  parameters: SearchBoxParameterProcessed[] = [];

  get activeItemValueOptions(): CommonEntry[] {
    return this.itemsOptions && this.activeItem && this.itemsOptions[this.activeItem.value] ?
      this.itemsOptions[this.activeItem.value] : [];
  }

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

  private switchToParameterInput = (): void => {
    this.activeItem = null;
    this.isValueInput = false;
    setTimeout(() => this.parameterInput.focus());
  };

  private getItemByValue(name: string): CommonEntry {
    return this.items.find((field: CommonEntry): boolean => field.value === name);
  }

  private getItemByName(name: string): CommonEntry {
    return this.items.find((field: CommonEntry): boolean => field.name === name);
  }

  clear(): void {
    this.isActive = false;
    this.activeItem = null;
    this.currentValue = '';
    this.parameterInput.value = '';
    this.valueInput.value = '';
  }

  itemsListFormatter(item: CommonEntry): string {
    return item.name;
  }

  itemsValueFormatter(item: CommonEntry): string {
    return item.value;
  }

  changeParameterName(options: SearchBoxParameterTriggered): void {
    this.parameterNameChangeSubject.next(options);
  }

  onParameterNameChange = (options: SearchBoxParameterTriggered): void => {
    if (options.value) {
      this.activeItem = this.getItemByValue(options.value);
      this.isExclude = options.isExclude;
      this.isActive = true;
      this.isParameterInput = false;
      this.isValueInput = true;
      this.currentValue = '';
      setTimeout(() => this.valueInput.focus(), 0);
    }
  };

  onParameterValueKeyDown(event: KeyboardEvent): void {
    if (this.utils.isBackSpacePressed(event) && !this.currentValue) {
      this.switchToParameterInput();
    }
  }

  onParameterValueKeyUp(event: KeyboardEvent): void {
    if (this.utils.isEnterPressed(event) && this.currentValue) {
      this.onParameterValueChange(this.currentValue);
    }
  }

  onParameterValueChange(value: string): void {
    if (value) {
      this.parameters.push({
        id: this.currentId++,
        name: this.activeItem.value,
        label: this.activeItem.name,
        value: value,
        isExclude: this.isExclude
      });
      this.updateValue();
    }
    this.switchToParameterInput();
  }

  onParameterAdd = (options: SearchBoxParameter): void => {
    const item = this.getItemByValue(options.name);
    this.parameters.push({
      id: this.currentId++,
      name: options.name,
      label: item.name,
      value: options.value,
      isExclude: options.isExclude
    });
    this.updateValue();
  };

  onParameterKeyUp = (event: KeyboardEvent): void => {
    if (this.utils.isEnterPressed(event) && this.currentValue) {
      const existingItem = this.getItemByName(this.currentValue);
      if (existingItem) {
        this.changeParameterName({
          value: this.currentValue,
          isExclude: false
        });
      } else {
        this.parameterAddSubject.next({
          name: this.messageParameterName,
          value: this.currentValue,
          isExclude: false
        });
      }
    }
  };

  removeParameter(event: MouseEvent, id: number): void {
    this.parameters = this.parameters.filter((parameter: SearchBoxParameterProcessed): boolean => parameter.id !== id);
    this.updateValue();
    event.stopPropagation();
  }

  updateValue(): void {
    this.currentValue = '';
    if (this.onChange) {
      this.onChange(this.parameters);
    }
  }

  writeValue(parameters: SearchBoxParameterProcessed[] = []): void {
    this.parameters = parameters;
    this.updateValue();
  }

  registerOnChange(callback: any): void {
    this.onChange = callback;
  }

  registerOnTouched(): void {
  }

}
