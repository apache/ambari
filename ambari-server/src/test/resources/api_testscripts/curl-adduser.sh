curl -i -o - -X GET http://localhost:8080/api/users
curl -i -o - -X GET http://localhost:8080/api/users/admin
#POST - to create a user test3 with password ambari belonging to both roles user and admin, the roles can just be admin
curl -i -X POST -d '{"Users": {"password" : "ambari", "roles" : "user,admin"}}' http://localhost:8080/api/users/test3
#PUT -
#similar to post for update, for password change you will have to do something like:
curl -i -X PUT -d '{"Users": {"password" : "ambari2", "old_password" : "ambari"}}' http://localhost:8080/api/users/test3
curl -i -o - -X GET http://localhost:8080/api/users/
curl -i -o - -X GET http://localhost:8080/api/users/admin
