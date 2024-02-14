import { Pipe, PipeTransform  } from '@angular/core';
import { isArray } from '../utils/utils';

@Pipe({ 
  name: 'min' 
})
export class MinPipe implements PipeTransform {
  
  transform (input: any): any {
    
    if (!isArray(input)) {
      return input;
    }
    
    if (input.length === 0) {
      return undefined;
    }
    
    let min = input[0];
    
    input.forEach((value: any) => {
      
      if (min > value) {
        min = value;
      }
    });
    
    return min;
  }
}