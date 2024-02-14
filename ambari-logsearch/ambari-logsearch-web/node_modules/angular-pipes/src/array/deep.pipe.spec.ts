import { DeepPipe } from './deep.pipe';
import { wrapDeep } from '../utils/utils';


describe('DropPipe', () => {
  
  let pipe: DeepPipe;
  
  beforeEach(() => {
    pipe = new DeepPipe(); 
  });
  
  it('Should return the deep object', () => {
    const object = wrapDeep({ x: 1 });
    expect(pipe.transform(object)).toBe(object);
  });
  
})