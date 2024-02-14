import { EncodeURIPipe } from './encode-uri.pipe';
        


describe('EncodeURIPipe', () => {
    
    let pipe: EncodeURIPipe;
    
    beforeEach(() => {
       pipe = new EncodeURIPipe(); 
    });

    it('Should return the value encoded', () => {
        const word = 'éà';
        expect(pipe.transform(word)).toEqual(encodeURI(word));
    });
    
    it('Should return the value unchanged', () => {
       
       expect(pipe.transform(1)).toEqual(1); 
    });
   
});