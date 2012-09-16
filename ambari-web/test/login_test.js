var App = require('app');
require('controllers/login_controller');

describe('App.LoginController', function () {

  describe('#validateCredentials()', function () {
    it('should return false if no username is present', function () {
      var loginController = App.LoginController.create();
      loginController.set('loginName', '');
      expect(loginController.validateCredentials()).to.equal(false);
    })
    it('should return false if no password is present', function () {
      var loginController = App.LoginController.create();
      loginController.set('password', '');
      expect(loginController.validateCredentials()).to.equal(false);
    })
    it('should return true if username and password are the same (dummy until actual integration)', function () {
      var loginController = App.LoginController.create();
      loginController.set('loginName', 'abc');
      loginController.set('password', 'abc');
      expect(loginController.validateCredentials()).to.equal(true);
    })
  })
})