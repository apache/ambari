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
  OnInit,
  OnChanges,
  Input,
  ViewChild,
  ElementRef,
  SimpleChanges,
  SimpleChange
} from '@angular/core';

@Component({
  selector: 'circle-progress-bar',
  templateUrl: './circle-progress-bar.component.html',
  styleUrls: ['./circle-progress-bar.component.less']
})
export class CircleProgressBarComponent implements OnInit, OnChanges {

  @Input()
  radius: number;

  @Input()
  strokeColor = 'white';

  @Input()
  strokeWidth: number;

  @Input()
  fill = 'transparent';

  @Input()
  percent = 0;

  @Input()
  label: string;

  @ViewChild('circle')
  circleRef: ElementRef;

  get normalizedRadius(): number {
    return this.radius - this.strokeWidth;
  }

  get circumference(): number {
    return this.normalizedRadius * 2 * Math.PI;
  }

  get strokeDashoffset(): number {
    return this.circumference - (this.percent / 100 * this.circumference);
  }

  constructor() { }

  ngOnInit() {
    this.setProgress(this.percent);
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.percent) {
      this.setProgress(this.percent);
    }
  }

  setProgress(percent = this.percent) {
    if (this.circleRef) {
      this.circleRef.nativeElement.style.strokeDashoffset = this.strokeDashoffset;
    }
  }

}
