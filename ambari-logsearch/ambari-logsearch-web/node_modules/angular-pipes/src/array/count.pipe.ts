import { Pipe, PipeTransform  } from '@angular/core';
import { count } from '../utils/utils';


@Pipe({ 
  name: 'count' 
})
export class CountPipe implements PipeTransform {
  
  transform (input: any): any {
    
    return count(input);
  }
}

