import { Pipe, PipeTransform  } from '@angular/core';
import { isObject } from '../utils/utils';


@Pipe({ name: 'keys' })
export class KeysPipe implements PipeTransform {
  
  transform (input: any): any {
    
    if (!isObject(input)) {
      return input;
    }
    
    return Object.keys(input);
  }
}