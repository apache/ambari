import { Pipe, PipeTransform  } from '@angular/core';
import { shuffle } from '../utils/utils';

@Pipe({
  name: 'shuffle'
})
export class ShufflePipe implements PipeTransform {
  
  transform (input: any): any {
    
    return shuffle(input);
  }
}
