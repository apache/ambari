import { PipeTransform } from '@angular/core';
import * as moment from 'moment';
export declare class ParsePipe implements PipeTransform {
    transform(value: string, format: string): moment.Moment;
}
