import { Pipe, PipeTransform  } from '@angular/core';
import { isArray, isFunction, getProperty } from '../utils/utils';

@Pipe({
  name: 'where'
})
export class WherePipe implements PipeTransform {
  
  /** 
  * Support a function or a value or the shorthand ['key', value] like the lodash shorthand.
  */
  transform (input: any, fn: any): any {
    
    if (!isArray(input)) {
      return input
    }
    
    if (isFunction(fn)) {
      return input.filter(fn);
    }
    else if (isArray(fn)) {
      const [key, value] = fn;
      return input.filter((item: any) => getProperty(item, key) === value);
    }
    else if (fn) {
      return input.filter((item: any) => item === fn);
    }
    else {
      return input;
    }
    
  }
}