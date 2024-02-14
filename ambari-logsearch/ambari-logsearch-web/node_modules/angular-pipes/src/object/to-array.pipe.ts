import { Pipe, PipeTransform  } from '@angular/core';
import { isObject } from '../utils/utils';


@Pipe({ name: 'toArray' })
export class ToArrayPipe implements PipeTransform {
  
  transform (input: any): any {
    
    if (!isObject(input)) {
      return input;
    }
    
    return Object.keys(input).map((value) => input[value]);
  }
}