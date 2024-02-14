import { Pipe, PipeTransform  } from '@angular/core';
import { isArray } from '../utils/utils';

@Pipe({
  name: 'head'
})
export class HeadPipe implements PipeTransform {
  
  transform (input: any): any {
    
    if (!isArray(input)) {
      return input;
    }
    
    // Will return undefined if length is 0
    return input[0];
  }
}