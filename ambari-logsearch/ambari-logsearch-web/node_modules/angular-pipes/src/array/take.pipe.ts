import { Pipe, PipeTransform  } from '@angular/core';
import { isArray } from '../utils/utils';

@Pipe({
  name: 'take'
})
export class TakePipe implements PipeTransform {
  
  transform (input: any, quantity?: number): any {
    
    if (!isArray(input)) {
      return input;
    }
    
    return input.slice(0, quantity || 1);
  }
}