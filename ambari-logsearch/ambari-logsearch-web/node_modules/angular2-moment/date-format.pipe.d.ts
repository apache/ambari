import { PipeTransform } from '@angular/core';
import * as moment from 'moment';
export declare class DateFormatPipe implements PipeTransform {
    transform(value: Date | moment.Moment | string | number, ...args: any[]): string;
}
