# From the course [Rock the JVM](https://rockthejvm.com/) course [ZIO Rite of Passage](https://rockthejvm.com/courses/enrolled/2132116)
This project demonstrates a full-stack Scala application using ZIO, Laminar, and Scala.js, focused on building a production-ready system with features like authentication, REST APIs, and real-time updates. The website serves as a platform for reviewing companies, showcasing skills in functional programming and real-world application development.

### Front End
```bash
sbt: ~fastOptJS
npm run start
docker exec -it zio-rite-of-passage-db-1 psql -U docker
\c reviewboard
\d invites
```

### Back End
```bash
sbt: ~compile
sbt: ~Test / compile
```


#### Create a User
```bash
http post localhost:8080/users email='something@something.com' password='test'
```
#### Login
```bash
http post localhost:8080/users/login email='user@example.com' password='test'
```
#### Add a Company
```bash
http post localhost:8080/companies name='Company' url='company.com' country='USA' location='City' industry='Tech' tags:='["IT"]' 'Authorization: Bearer <token>'
```
#### Search
```bash
http post localhost:8080/companies/search countries:='["Nepal"]' locations:='[]' industries:='[]' tags:='[]'
```


### Get Stripe secret for local testing of webhook
```bash
stripe listen --forward-to http://localhost:8080/invite/webhook
<key>
```

### sbt commands
stagingBuild / Docker / publishLocal

docker save -o server.tar rockthejvm-reviewboard-staging:1.0.1
scp server.tar root@139.59.146.144:/staging
docker load -i server.tar
