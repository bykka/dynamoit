##Overview
It's a simple DynamoDB graphical client written on JavaFX.

This client allows easily to view, edit, create, and delete data.

The main idea of the client is to reach the required data in a few clicks.

## Features
 * No login required. The client uses AWS cli profiles.
 * Group tables by keywords
 * Automatically detects when to use scan or query requests
 * Supports pagination to view data
 * Fast data filtering
 * Edit or delete the selected records
 * Search (Ctrl+F) in the edit dialog 

## System requirements, building and running
Java 11 and maven are required to build and run the application.

Execute the next command to build
```
mvn clean package
```
A jar file with all dependencies will be created in the _target_ directory.

To run the application simply execute the command
```
java -jar target/fatJar/DynamoIt-<version>.jar 
```

## Screenshots:
![main screen](./images/main_screen.png)

![edit screen](./images/edit_screen.png)
