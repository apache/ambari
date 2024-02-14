import { Pipe, PipeTransform  } from '@angular/core';
import { isArray, isDeepObject, unwrapDeep, deepIndexOf } from '../utils/utils';

@Pipe({
  name: 'union'
})
export class UnionPipe implements PipeTransform {
  
  transform (a?: any, b?: any): any {
    
    if ((!isArray(a) && !isDeepObject(a)) || !isArray(b)) {
      return [];
    }
    
    if (isDeepObject(a)) {
      const unwrapped = unwrapDeep(a);
      if (!isArray(unwrapped)) {
        return [];
      }
      
      return []
        .concat(unwrapped)
        .concat(b)
        .filter((value: any, index: number, input: any[]) => deepIndexOf(input, value) === index);
    }

    return [].concat(a).concat(b).filter((value: any, index: number, input: any[]) => input.indexOf(value) === index);
  }
}