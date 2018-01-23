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

import {Component, OnInit, OnDestroy, HostListener, Input, ViewChild, ElementRef, forwardRef} from '@angular/core';
import {ControlValueAccessor, NG_VALUE_ACCESSOR} from '@angular/forms';
import {Subject} from 'rxjs/Subject';
import {SearchBoxParameter, SearchBoxParameterProcessed, SearchBoxParameterTriggered} from '@app/classes/filtering';
import {ListItem} from '@app/classes/list-item';
import {HomogeneousObject} from '@app/classes/object';
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

  constructor(private utils: UtilsService) {
  }

  ngOnInit(): void {
    this.parameterInput = this.parameterInputRef.nativeElement;
    this.valueInput = this.valueInputRef.nativeElement;
    this.parameterNameChangeSubject.subscribe(this.onParameterNameChange);
    this.parameterAddSubject.subscribe(this.onParameterAdd);
    this.updateValueSubject.subscribe(this.updateValue);
  }

  ngOnDestroy(): void {
    this.parameterNameChangeSubject.unsubscribe();
    this.parameterAddSubject.unsubscribe();
    this.updateValueSubject.unsubscribe();
  }

  private currentId: number = 0;

  private isExclude: boolean = false;

  /**
   * Indicates whether search box is currently active
   * @type {boolean}
   */
  isActive: boolean = false;

  /**
   * Indicates whether search query parameter value is currently typed
   * @type {boolean}
   */
  isValueInput: boolean = false;

  currentValue: string;

  /**
   * Indicates whether there's no autocomplete matches in preset options for search query parameter name
   * @type {boolean}
   */
  private noMatchingParameterName: boolean = true;

  /**
   * Indicates whether there's no autocomplete matches in preset options for search query parameter value
   * @type {boolean}
   */
  private noMatchingParameterValue: boolean = true;

  @Input()
  items: ListItem[] = [];

  @Input()
  itemsOptions: HomogeneousObject<ListItem[]> = {};

  /**
   * Name of parameter to be used if there are no matching values
   * @type {string}
   */
  @Input()
  defaultParameterName?: string;

  @Input()
  parameterNameChangeSubject: Subject<SearchBoxParameterTriggered> = new Subject();

  @Input()
  parameterAddSubject: Subject<SearchBoxParameter> = new Subject();

  @Input()
  updateValueSubject: Subject<void> = new Subject();

  /**
   * Indicates whether form should receive updated value immediately after user adds new search parameter, without
   * explicit actions like pressing Submit button or Enter key
   * @type {boolean}
   */
  @Input()
  updateValueImmediately: boolean = true;

  @ViewChild('parameterInput')
  parameterInputRef: ElementRef;

  @ViewChild('valueInput')
  valueInputRef: ElementRef;

  private parameterInput: HTMLInputElement;

  private valueInput: HTMLInputElement;

  /**
   * Currently active search query parameter
   * @type {ListItem | null}
   */
  activeItem: ListItem | null = null;

  /**
   * Search query parameters that are already specified by user
   * @type {SearchBoxParameterProcessed[]}
   */
  parameters: SearchBoxParameterProcessed[] = [];

  /**
   * Available options for value of currently active search query parameter
   * @returns {ListItem[]}
   */
  get activeItemValueOptions(): ListItem[] {
    return this.itemsOptions && this.activeItem && this.itemsOptions[this.activeItem.value] ?
      this.itemsOptions[this.activeItem.value] : [];
  }

  private onChange: (fn: any) => void;

  @HostListener('click')
  private onRootClick(): void {
    if (!this.isActive) {
      this.parameterInput.focus();
    }
  }

  @HostListener('keydown', ['$event'])
  private onRootKeyDown(event: KeyboardEvent): void {
    if (this.utils.isEnterPressed(event)) {
      event.preventDefault();
    }
  };

  @HostListener('blur')
  private onRootBlur(): void {
    this.clear();
  };

  onParameterInputFocus(): void {
    this.isActive = true;
  }

  private switchToParameterInput = (): void => {
    this.clear();
    this.isActive = true;
    this.isValueInput = false;
    setTimeout(() => this.parameterInput.focus(), 0);
  };

  private getItemByValue(name: string): ListItem {
    return this.items.find((field: ListItem): boolean => field.value === name);
  }

  clear(): void {
    this.isActive = false;
    this.activeItem = null;
    this.currentValue = '';
    this.parameterInput.value = '';
    this.valueInput.value = '';
  }

  changeParameterName(options: SearchBoxParameterTriggered): void {
    this.parameterNameChangeSubject.next(options);
  }

  onParameterNameChange = (options: SearchBoxParameterTriggered): void => {
    this.activeItem = options.item.label ? options.item : this.getItemByValue(options.item.value);
    this.isExclude = options.isExclude;
    this.isActive = true;
    this.isValueInput = true;
    this.currentValue = '';
    this.valueInput.focus();
  };

  onParameterValueKeyDown(event: KeyboardEvent): void {
    if (this.utils.isBackSpacePressed(event) && !this.currentValue) {
      this.switchToParameterInput();
    }
  }

  onParameterValueKeyUp(event: KeyboardEvent): void {
    if (this.utils.isEnterPressed(event) && this.currentValue && this.noMatchingParameterValue) {
      this.onParameterValueChange(this.currentValue);
    }
  }

  onParameterValueChange(value: string): void {
    this.parameters.push({
      id: this.currentId++,
      name: this.activeItem.value,
      label: this.activeItem.label,
      value: value,
      isExclude: this.isExclude
    });
    if (this.updateValueImmediately) {
      this.updateValueSubject.next();
    }
    this.switchToParameterInput();
  }

  /**
   * Adding the new parameter to search query
   * @param parameter {SearchBoxParameter}
   */
  onParameterAdd = (parameter: SearchBoxParameter): void => {
    const item = this.getItemByValue(parameter.name);
    this.parameters.push({
      id: this.currentId++,
      name: parameter.name,
      label: item.label,
      value: parameter.value,
      isExclude: parameter.isExclude
    });
    if (this.updateValueImmediately) {
      this.updateValueSubject.next();
    }
    this.switchToParameterInput();
  };

  onParameterKeyUp(event: KeyboardEvent): void {
    if (this.utils.isEnterPressed(event)) {
      if (!this.currentValue && !this.updateValueImmediately) {
        this.updateValueSubject.next();
      } else if (this.currentValue && this.noMatchingParameterName && this.defaultParameterName) {
        this.parameterAddSubject.next({
          name: this.defaultParameterName,
          value: this.currentValue,
          isExclude: false
        });
      }
    }
  }

  /**
   * Removing parameter from search query
   * @param event {MouseEvent} - event that triggered this action
   * @param id {number} - id of parameter
   */
  removeParameter(event: MouseEvent, id: number): void {
    this.parameters = this.parameters.filter((parameter: SearchBoxParameterProcessed): boolean => parameter.id !== id);
    if (this.updateValueImmediately) {
      this.updateValueSubject.next();
    }
    event.stopPropagation();
  }

  updateValue = (): void => {
    this.currentValue = '';
    if (this.onChange) {
      this.onChange(this.parameters);
    }
  };

  /**
   * Update flag that indicates presence of autocomplete matches in preset options for search query parameter name
   * @param hasNoMatches {boolean}
   */
  setParameterNameMatchFlag(hasNoMatches: boolean): void {
    this.noMatchingParameterName = hasNoMatches;
  }

  /**
   * Update flag that indicates presence of autocomplete matches in preset options for search query parameter value
   * @param hasNoMatches {boolean}
   */
  setParameterValueMatchFlag(hasNoMatches: boolean): void {
    this.noMatchingParameterValue = hasNoMatches;
  }

  writeValue(parameters: SearchBoxParameterProcessed[] = []): void {
    this.parameters = parameters;
    this.updateValueSubject.next();
  }

  registerOnChange(callback: any): void {
    this.onChange = callback;
  }

  registerOnTouched(): void {
  }

}
