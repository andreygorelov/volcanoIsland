# Island Reservation
Fun volcano campsite reservation project

## About the Service
This service contains REST API endpoints to Reserve/Book, Edit, and Cancel a campsite. 
Other functionalities are to retrieve all available dates for reservation and to view existing reservation by supplying reservation id.

Reservation Rules:

* The campsite can be booked for 1 and up to 3 days
* The campsite can be booked 1 day ahead and up to 30 days in advance
* Check-in & check-out time is 12:00am
* Reservation can be cancelled any time supplying reservation id and user email


This project does not use any external databases, it uses in memory map structure to store campsite reservations.
Occasional backup of existing reservation is done via cron job that runs  every hour. Cron timing is configurable via application.properties. 
If the service stops or crushes for any reason it will restore all the data from the last backup on startup.
Since a campsite can only be booked a month in advance the service only stores 30 days. 
The service removes passed days and reservations every day at midnight and add new date(s).
If service stopped and restarted few days after, during start up the service reloads all the data from the file system and purges all the past dates and expired reservations.
### NOTE: accepted date format for all REST endpoints is The most common ISO Date Format ```yyyy-MM-dd```

## Getting Started
IslandReservation service is a Spring Boot application.

## Prerequsites
* Java 11
* Maven 3.x

## Spring-boot properties
Two properties are used to enable/disable backup campsite reservation and restore from backup. Both flags are enabled by default for production. Flags are disabled in testing, however there tests that test backup and restpre functionality not using cron jobs.

## Steps to build and run the service

* Clone this repository
* Make sure you are using JDK 11 and Maven 3.x
* You can build the project and run the tests by running ``mvn clean install``
* After successful build the service can be run in 3 ways:
 ``` 
 * java -jar target/IslandReservation-1.0-SNAPSHOT.jar
  or
 * mvn spring-boot:run
  or
 * In IDE/Intellij, find Application.java execute run
```

## REST Endpoints

### Note all endpoints that accpet request body should be in JSON format with the following restrictions:
* Names must be provided
* Email shoud have valid email format
* Start and End dates should have ISO Date Format ```yyyy-MM-dd```


### Retrieve available for reservation dates
```
GET /api/campsite/reservation/availableDates

Response: HTTP 200
Body:

[
    "2021-12-30",
    "2021-12-29",
    "2021-12-28",
…
…
]

```

### Create / reserve a campsite

```
POST /api/campsite/reservations
Accept: application/json
Content-Type: application/json

{
"firstName": "Nichael",
"lastName": "Kackson",
"email": "nkackson@domain.net",
"startDate": "2021-12-21",
"endDate": "2021-12-23"
}

Response HTTP 201:
{
"reservationId": " kPSQsuUi",
"firstName": "Nichael",
"lastName": "Kackson",
"email": "nkackson@domain.net",
"startDate": "2021-12-21",
"endDate": "2021-12-23"
}

```

### Retrieve existing reservation by reservation id

```
GET /api/campsite/reservation/{reservationId}
http://localhost:8080/api/campsite/reservation/ZvDHF5UH

Response HTTP 200
Body:

{
    "reservationId": "ZvDHF5UH",
    "firstName": "Buck",
    "lastName": "Up",
    "email": "buest@domain.net",
    "startDate": "2021-12-15",
    "endDate": "2021-12-17"
}


```

### Update campsite reservation 

When updating existing resrvation, the service checkes if new days available for reservation, then cancels old reservation (by id) and create a new reservation with new reservationId

```
PUT  /api/campsite/reservation/{reservationId}
Accept: application/json
Content-Type: application/json

Example using previous POST request registrationId kPSQsuUi
http://localhost:8080/api/campsite/reservation/kPSQsuUi
Body:
{
    "firstName": "Nichael",
    "lastName": "Kackson",
    "email": "nkackson@domain.net",
    "startDate": "2021-12-23",
    "endDate": "2021-12-24"
}

Response HTTP 200

{
    "reservationId": "KHNBXAPH",
    "firstName": "Nichael",
    "lastName": "Kackson",
    "email": "nkackson@domain.net",
    "startDate": "2021-12-23",
    "endDate": "2021-12-24"
}

```

### Cancel reservation 

```
DELETE /api/campsite/reservation/{reservationId}/{userId}

http://localhost:8080/api/campsite/reservation/KHNBXAPH/nkackson@domain.net

Response HTTP 200

```
 




