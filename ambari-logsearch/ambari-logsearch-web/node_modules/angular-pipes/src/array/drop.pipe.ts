import { Pipe, PipeTransform  } from '@angular/core';
import { isArray } from '../utils/utils';

@Pipe({
  name: 'drop'
})
export class DropPipe implements PipeTransform {
  
  transform (input: any, quantity?: number): any {
    
    if (!isArray(input)) {
      return input;
    }
    
    return input.slice(quantity || 1, input.lenth);
  }
}