import { PipeTransform } from '@angular/core';
import * as moment from 'moment';
export declare class AddPipe implements PipeTransform {
    transform(value: any, amount: moment.DurationInputArg1, unit?: moment.DurationInputArg2): any;
}
