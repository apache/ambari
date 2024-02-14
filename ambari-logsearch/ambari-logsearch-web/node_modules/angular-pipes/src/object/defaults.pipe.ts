import { Pipe, PipeTransform  } from '@angular/core';
import { isObject, isArray, isNil } from '../utils/utils';

@Pipe({ name: 'defaults' })
export class DefaultsPipe implements PipeTransform {
  
  transform (input: any, defaults: any): any {
    
    if (!isObject(defaults)) {
      return input;
    }
    
    if (isNil(input)) {
      return defaults;
    }
    
    if (isArray(input)) {
      return input.map((item: any) => {
        
        if (isObject(item)) {
          return Object.assign({}, defaults, item);
        }
        
        if (isNil(item)) {
          return defaults;
        }
        
        return item;
      });
    }
    
    if (isObject(input)) {
      return Object.assign({}, defaults, input);
    }
    
    return input;
  }
}