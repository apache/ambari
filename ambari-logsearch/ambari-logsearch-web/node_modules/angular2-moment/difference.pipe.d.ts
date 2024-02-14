import { PipeTransform } from '@angular/core';
import * as moment from 'moment';
export declare class DifferencePipe implements PipeTransform {
    transform(value: Date | moment.Moment, otherValue: Date | moment.Moment, unit?: moment.unitOfTime.Diff, precision?: boolean): number;
}
