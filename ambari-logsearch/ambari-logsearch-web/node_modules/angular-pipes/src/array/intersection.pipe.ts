import { Pipe, PipeTransform  } from '@angular/core';
import { isArray, isDeepObject, unwrapDeep, deepIndexOf } from '../utils/utils';

@Pipe({
  name: 'intersection'
})
export class IntersectionPipe implements PipeTransform {
  
  transform (a?: any, b?: any): any {
    
    if ((!isArray(a) && !isDeepObject(a)) || !isArray(b)) {
      return [];
    }
    
    if (isDeepObject(a)) {
      const unwrapped = unwrapDeep(a);
      if (!isArray(unwrapped)) {
        return [];
      }
      
      return unwrapped.reduce((intersection: any[], value: any) => intersection.concat(
        (deepIndexOf(b, value) !== -1 && deepIndexOf(intersection, value) === -1) ? value : []
      ), []);
    }

    return a.reduce((intersection: any[], value: any) => intersection.concat(
      (b.indexOf(value) !== -1 && intersection.indexOf(value) === -1) ? value : []
    ), [])
  }
}