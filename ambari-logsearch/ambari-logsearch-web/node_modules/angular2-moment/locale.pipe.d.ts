import { PipeTransform } from '@angular/core';
import * as moment from 'moment';
export declare class LocalePipe implements PipeTransform {
    transform(value: string, locale: string): moment.Moment;
}
