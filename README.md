﻿#ZIO Rite of Passage
This repository is based on the Rock the JVM ZIO Rite of Passage course, showcasing a full-stack application built with ZIO, ScalaJS, Laminar, and related libraries.

Setup Instructions
Backend
Start the database using Docker:
docker-compose up -d
Compile and run the backend:
sbt ~compile
Frontend
Compile the frontend and start the application:
sbt ~fastOptJS
npm run start
For macOS M1 Users (Colima + TestContainers)
Install dependencies:
brew install --HEAD colima docker docker-compose
sudo ln -s $HOME/.colima/default/docker.sock /var/run/docker.sock
Start Colima:
colima start --cpu 2 --memory 8 --disk 60 --arch aarch64 --vm-type vz --vz-rosetta --mount-type virtiofs --network-address
Export necessary environment variables:
export TESTCONTAINERS_HOST_OVERRIDE=$(colima ls -j | jq -r '.address' | head -n 1)
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
export DOCKER_HOST=unix://$HOME/.colima/default/docker.sock
Create a .env file with the following:

DOCKER_HOST=unix://$HOME/.colima/default/docker.sock
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
TESTCONTAINERS_HOST_OVERRIDE=<colima-host>
Example API Requests
Create a user:
http post localhost:8080/users email='user@example.com' password='test'
Login:
http post localhost:8080/users/login email='user@example.com' password='test'
Add a company:
http post localhost:8080/companies name='Company' url='company.com' country='USA' location='City' industry='Tech' tags:='["IT"]' 'Authorization: Bearer <token>'
Testing and Deploying
Add sample data using SQL:
INSERT INTO reviews(...) VALUES(...);
INSERT INTO invites(...) VALUES(...);
Test invites and reviews:
http get localhost:8080/invite/all "Authorization: Bearer <token>"
http get localhost:8080/reviews/company/1/summary
Build and deploy Docker image:
sbt stagingBuild / Docker / publishLocal
docker save -o server.tar <image-name>:<tag>
