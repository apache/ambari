import { Pipe, PipeTransform } from '@angular/core';
import { wrapDeep, isDeepObject } from '../utils/utils';

@Pipe({
  name: 'deep'
})
export class DeepPipe implements PipeTransform {
  
  transform(value: any) : any {
    
    if (isDeepObject(value)) {
      return value;
    }
    
    return wrapDeep(value);
  }
}