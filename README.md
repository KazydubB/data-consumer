# About
A simple application that gathers items from https://hacker-news.firebaseio.com/v0/item/{id}.json and then
persists them to MongoDB storage according to their type: there are two possible types - `story` and `comment`. A 
`story` can have many `comment`s as children; each `comment` can a `comment` children as well. 

After data is persisted, it is possible to query list (see **Available Commands** below) of stored `stories` with its 
`externalId` (the `id` from the service) and its `title`. It is also possible to list a specific entry, given an 
`externalId`. In case when given `externalId` corresponds to a `story`, the `story` is output with all of its' children
(`comments`). In the case when `externalId` corresponds to a `comment` - related `story` will be output with all of its 
children as well.

**Note**: this application is using Spring Boot which makes it easier to setup.

Also, be free to tune parameters found in `./src/main/resources/application.properties` to see what works the best for you.

# Prerequisites
To run the application you need to start RabbitMQ broker and MongoDB.

### To start RabbitMQ
If you have Docker installed, you can create `docker-compose.yml` file containing:
```
rabbitmq:
image: rabbitmq:management
ports:
- "5672:5672"
- "15672:15672"
```
go to the directory containing the file and run `docker-compose up` which will start RabbitMQ in a container.

Otherwise you can download it from https://www.rabbitmq.com/download.html (or install it by any other means, such as `brew install rabbitmq` on Mac), unpack it and run `rabbitmq-server`.

### To start MongoDB
If you don't have MongoDB installed, you may install it by following instructions on https://docs.mongodb.org/manual/installation/ depending on your OS. 
After the installation start it with `mongod` command.

In case when you use Mac, you can install it using
```
brew tap mongodb/brew
brew install mongodb-community
```

after install is complete run it with `brew services start mongodb-community`.

# To start the application
To start the application, you can simply build the jar, with `mvn clean install`, and then run ` java -jar target/data-consumer-0.0.1-SNAPSHOT.jar consumer.ConsumerApplication`.

Before running any commands, make sure that RabbitMQ and MongoDB are running (see above).

### Available commands
- `consume` -- consumes data from endpoint and stores it into local DB;
- `list` -- shows all Story entities stored in the DB after `consume`;
- `list {id}` -- shows an entity with its parents and kids identified by id param (with id being integer value). If ID corresponds to story, then the story will be output in a json. If ID corresponds to comment - the story, to which this comment corresponds to, will be output as well as all of its comments as json;
- `exit`/`quit` -- exit the application.