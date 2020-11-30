# Xariot
Xariot is a system that synchronizes files between collaborators, remotely, in real time.

## Features
Xariot provides  multiple features that allow the creation of a collaborative environment between users.
Such features are: 
- Access control
- Reliable and secure transfer of data between users
- Conflict resolution
- Client server model
- Cross platform compatibility
- User interface

## Technologies used

### Spring boot
Inviting users via e-mail and probing for http requests on the server.

### Apache Derby
Database support for keeping track of the users and their access rights on a group, file names, the groups that are collaborating on these files and how many users are editing a specific file at any given time.

### Secure sockets
The transfer of data between machines is done by SSL sockets created by exchanging the public keys of the client and the server.

### Java FX
GUI support



