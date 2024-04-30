#!/usr/bin/bash

CONTAINER_NAME=tickr-db
ROOT_PASSWORD=rootpw
DATABASE_PORT=3306
DATABASE_NAME=tickr

TICKR_USER=tickr
TICKR_PASSWORD=tickr-pw

LOCK_FILE=.db.lock

if [[ -f "$LOCK_FILE" ]]; then
  echo "Database is already started!"
  exit 1
fi


echo "Initialising database..."
docker run --name $CONTAINER_NAME --health-cmd="mysqladmin ping --silent" -e MYSQL_ROOT_PASSWORD=$ROOT_PASSWORD -p $DATABASE_PORT:3306 -d mysql:latest
docker cp schema.sql $CONTAINER_NAME:/schema.sql
echo "Waiting for mysqld to start up..."
while STATUS=$(docker inspect --format "{{.State.Health.Status}}" $CONTAINER_NAME); [ "$STATUS" != "healthy" ]; do
  #echo "$STATUS"
  if [ "$STATUS" == "unhealthy" ]; then
    echo "Failed to initialise database!"
    docker stop $CONTAINER_NAME
    docker rm $CONTAINER_NAME
    exit 1
  fi
  sleep 1
done

echo "Initialising database schema..."
docker exec $CONTAINER_NAME mysql --user=root --password=$ROOT_PASSWORD -e \
    "create database $DATABASE_NAME;
    create USER '$TICKR_USER' identified by '$TICKR_PASSWORD';
    grant INSERT, UPDATE, DELETE, SELECT, REFERENCES, INDEX on $DATABASE_NAME.* to '$TICKR_USER';
    flush PRIVILEGES;
    use $DATABASE_NAME;
    source schema.sql;"
#docker exec -t $CONTAINER_NAME mysql --user=root --password=$ROOT_PASSWORD
echo "Successfully started up database!"
touch $LOCK_FILE