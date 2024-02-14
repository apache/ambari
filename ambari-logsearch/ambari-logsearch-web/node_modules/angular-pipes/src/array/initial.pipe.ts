import { Pipe, PipeTransform  } from '@angular/core';
import { isArray } from '../utils/utils';

@Pipe({
  name: 'initial'
})
export class InitialPipe implements PipeTransform {
  
  transform (input: any): any {
    
    if (!isArray(input)) {
      return input;
    }
    
    return input.slice(0, input.length - 1);
  }
}