# Statesman
Statesman is a Generic Workflow Service. 

# Local setup
```
git clone <statesman>
cd statesman
CODE=$(pwd)
mvn clean package 
cd statesman-server/
```

Change the line on Dockerfile (in the path $CODE/statesman-server/Dockerfile): 

ADD config/docker.yml config/docker.yml

To the following for local:

ADD config/local.yml config/docker.yml
```
docker-compose build
docker-compose up 
```

Incase you have ports conflicting (8080, 8081, 8082, 8083 will be used by this app), ports are mentioned in the following files:
```
$CODE/statesman-server/Dockerfile 
$CODE/statesman-server/config/local.yml
$CODE/statesman-server/docker-compose.yml
```


