# Briar Mailbox

This project aims to develop an easy way for Briar users to increase their 
reachability and lower the battery drain of their phone.

In Briar messages are exchanged directly between contacts (peer-to-peer). 
This kind of synchronous message exchange requires contacts to be online and 
connected to each other.  
While this is great for privacy (no central server which
can log things or be censored) it's bad for reachability, especially in
mobile networks where connectivity can be limited.

```mermaid
graph LR
 A[Alice]
 B[Bob]
 A1[Alice]
 B1[Bob]
 style B fill:#8db600
 style A1 fill:#8db600
 subgraph Alice offline
 B-. can't send message .-> A
 end
 subgraph Bob offline
 B1-. can't send message .-> A1
 end
```

Message delivery could be delayed for an arbitrary time (or even indefinitely) 
until both Bob and Alice are online at the same time.  
The mailbox solves this problem by providing 
a message buffer where contacts can leave messages for the owner 
of the mailbox and which is connected to a stable internet connection 
(e.g. the wifi at home, cable internet) and a power source.
 

```mermaid
graph LR
  A[Alice]
  A1[Alice]
  B[Bob]
  B1[Bob]
  RA["Mailbox (always online)"]
  style B fill:#8db600
  style RA fill:#8db600
  style A1 fill:#8db600
  subgraph Alice offline
  B-. can't send message .-> A
  end
  subgraph Alice's Mailbox
  B-- send message --> RA
  end
  subgraph Alice online
  B1-. can't send message .-> A1
  A1-- get message --> RA
  end
```

## Hardware

We want the mailbox to be as easy to deploy as possible. The target for this project
will come as Android application since it will be easy to setup and besides a 
spare phone, no special hardware is required. Once this is done support for
any hardware supporting Java (e.g. unix server, raspberry pi) could be added.

## Features

### Core features

* Allow contacts to store messages for the owner of the mailbox
* Allow the owner to store messages for her contacts. Contacts can pick them up
  when syncing with the mailbox.
* Owner and contacts connect to the mailbox via Tor.

### Extended features/components

* The mailbox can sync group messages (from groups the owner is part of) with 
  other group members (increases message circulation)
* Contacts and the owner can connect to the mailbox via other transports (Bluetooth, Wifi-Direct, Lan)
* Push-like message notification for the owner to decrease battery drain

## Useful links 
[Source code](https://code.briarproject.org/briar/briar-mailbox/-/tree/main) 

[API documentation](https://code.briarproject.org/briar/briar-mailbox/-/blob/main/API.md)

[Mailbox Architecture](https://code.briarproject.org/briar/briar/-/wikis/Mailbox-Architecture)

[briarproject.org](https://briarproject.org/)

## Server CLI version

A fat JAR for running on a x86_64 GNU/Linux server can be compiled with

    ./gradlew x86LinuxJar

And also ARM64 with

    ./gradlew aarch64LinuxJar

As well as a docker container can be built with

    docker build -t briar/mailbox .

And run with

    docker volume create briar-mailbox-1
    docker run --name briar-mailbox --volume briar-mailbox-1:/root briar/mailbox

## Donate 
[![Donate using Liberapay](https://briarproject.org/img/liberapay.svg)](https://liberapay.com/Briar/donate) [![Flattr this](https://briarproject.org/img/flattr-badge-large.png "Flattr this")](https://flattr.com/t/592836/)   
Bitcoin and BCH: 1NZCKkUCtJV2U2Y9hDb9uq8S7ksFCFGR6K

## License

This program is free software: you can redistribute it and/or modify it
under the terms of the [GNU Affero General Public License](LICENSES/AGPL-3.0-or-later.txt)
as published by the Free Software Foundation, either version 3 of the License,
or (at your option) any later version.

Compliant with version 3.0 of the [REUSE Specification](https://reuse.software).
