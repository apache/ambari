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

import {
  Component, OnChanges, AfterViewChecked, SimpleChanges, Input, Output, EventEmitter, ViewChildren, ViewContainerRef,
  QueryList, ChangeDetectorRef, ElementRef, ViewChild, OnInit
} from '@angular/core';
import {ListItem} from '@app/classes/list-item';
import {ComponentGeneratorService} from '@app/services/component-generator.service';

@Component({
  selector: 'ul[data-component="dropdown-list"]',
  templateUrl: './dropdown-list.component.html',
  styleUrls: ['./dropdown-list.component.less']
})
export class DropdownListComponent implements OnInit, OnChanges, AfterViewChecked {

  private shouldRenderAdditionalComponents: boolean = false;

  @Input()
  items: ListItem[] = [];

  private itemsSelected: ListItem[] = [];

  private itemsUnSelected: ListItem[] = [];

  @Input()
  isMultipleChoice?: boolean = false;

  @Input()
  additionalLabelComponentSetter?: string;

  @Input()
  actionArguments: any[] = [];

  @Output()
  selectedItemChange: EventEmitter<ListItem | ListItem[]> = new EventEmitter();

  @ViewChildren('additionalComponent', {
    read: ViewContainerRef
  })
  containers: QueryList<ViewContainerRef>;

  @Input()
  useLocalFilter = false;

  @ViewChild('filter')
  filterRef: ElementRef;

  @Input()
  filterStr = '';


  @ViewChild('selectAll')
  selectAllRef: ElementRef;

  private filterRegExp: RegExp;

  constructor(
    private componentGenerator: ComponentGeneratorService,
    private changeDetector: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.separateSelections();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.hasOwnProperty('items')) {
      this.separateSelections();
      this.shouldRenderAdditionalComponents = true;
    }
  }

  ngAfterViewChecked() {
    this.renderAdditionalComponents();
  }

  private separateSelections() {
    this.itemsSelected = this.items ? this.items.filter((item: ListItem) => item.isChecked) : [];
    this.itemsUnSelected = this.items ? this.items.filter((item: ListItem) => !item.isChecked) : [];
    this.shouldRenderAdditionalComponents = true;
  }

  private clearSelection() {
    this.unSelectAll();
    this.separateSelections();
  }

  private onClearSelectionClick = (event): void => {
    event.preventDefault();
    event.stopPropagation();
    this.clearSelection();
  }

  private changeAllSelection(event) {
    event.stopPropagation();
    if (!this.selectAllRef.nativeElement.checked) {
      this.selectAll();
    } else {
      this.unSelectAll();
    }
    this.separateSelections();
  }

  selectAll() {
    this.items.forEach((item: ListItem) => {
      item.isChecked = true;
      if (item.onSelect) {
        item.onSelect(...this.actionArguments);
      }
    });
    this.selectedItemChange.emit(this.items);
  }

  unSelectAll() {
    this.items.forEach((item: ListItem) => {
      item.isChecked = false;
      if (item.onSelect) {
        item.onSelect(...this.actionArguments);
      }
    });
    this.selectedItemChange.emit(this.items);
  }

  private onFilterInputKeyUp(event) {
    if (this.useLocalFilter) {
      this.filterRegExp = event.target.value ? new RegExp(`${event.target.value}`, 'gi') : null;
      this.filterStr = event.target.value;
    }
  }

  private isFiltered = (item: ListItem): boolean => {
    return this.useLocalFilter && this.filterRegExp && (
      !this.filterRegExp.test(item.value)
      &&
      !this.filterRegExp.test(item.label)
    );
  }

  private clearFilter = (event: MouseEvent): void => {
    this.filterRegExp = null;
    this.filterStr = '';
  }

  private renderAdditionalComponents(): void {
    const setter = this.additionalLabelComponentSetter;
    const containers = this.containers;
    if (this.shouldRenderAdditionalComponents && setter && containers) {
      containers.forEach((container, index) => this.componentGenerator[setter](this.items[index].value, container));
      this.shouldRenderAdditionalComponents = false;
      this.changeDetector.detectChanges();
    }
  }

  changeSelectedItem(item: ListItem, event?: MouseEvent): void {
    if (item.onSelect) {
      item.onSelect(...this.actionArguments);
    }
    this.separateSelections();
    this.selectedItemChange.emit(item);
  }

  doItemsCheck() {
    this.separateSelections();
  }

}
