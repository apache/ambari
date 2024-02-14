import { PipeTransform } from '@angular/core';
import * as moment from 'moment';
export declare class UtcPipe implements PipeTransform {
    transform(value: Date | moment.Moment | string | number): moment.Moment;
}
