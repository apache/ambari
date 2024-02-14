import { Pipe, PipeTransform } from '@angular/core';
import { isString, upperFirst } from '../utils/utils';

@Pipe({
  name: 'capitalize'
})
export class CapitalizePipe implements PipeTransform {
  transform(input: any, all: boolean = false) : any {
    
    if (!isString(input)) {
      return input;
    }
    
    if (!all) {
      return upperFirst(input.toLowerCase());
    }
    else {
      return input.toLowerCase()
        .split(' ')
        .map((value: string) => upperFirst(value))
        .join(' ');
    }
  }
}