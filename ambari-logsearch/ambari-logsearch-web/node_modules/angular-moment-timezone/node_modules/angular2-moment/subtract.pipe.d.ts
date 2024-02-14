import { PipeTransform } from '@angular/core';
import * as moment from 'moment';
export declare class SubtractPipe implements PipeTransform {
    transform(value: any, amount: moment.DurationInputArg1, unit?: moment.DurationInputArg2): any;
}
