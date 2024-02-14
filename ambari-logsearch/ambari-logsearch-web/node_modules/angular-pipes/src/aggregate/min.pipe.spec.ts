import { MinPipe } from './min.pipe';
    


describe('MinPipe', () => {
  
  let pipe: MinPipe;
  
  beforeEach(() => {
     pipe = new MinPipe(); 
  });
  
  it('Should return 1', () => {
    
    expect(pipe.transform([1,2,3,4])).toEqual(1);
  });

  it('Should return 2', () => {
    expect(pipe.transform([4,3,2,5])).toEqual(2);
  });
  
  it('Should return 1', () => {
    
    expect(pipe.transform([1])).toEqual(1);
  });
  
  it('Should return 1', () => {
    
    expect(pipe.transform([1,1])).toEqual(1);
  });
  
  it('Should return the value unchanged', () => {
     
     expect(pipe.transform(1)).toEqual(1); 
  });

  it('Should return undefined', () => {
    expect(pipe.transform([])).toBeUndefined();
  });
});