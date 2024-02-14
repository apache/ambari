import { Pipe, PipeTransform } from '@angular/core';
import { isArray, sum } from '../utils/utils';

@Pipe({ name: 'sum' })
export class SumPipe implements PipeTransform {
  transform(input: any): any {
    return !isArray(input) ? input : sum(input);
  }
}
