# FCM Mock for Docker

Mocked FCM server crafted with Wiremock, shipped inside a Docker container.

## Usage
To build the image run:

```
$ docker build -t fcm-wiremock .
```
And to run it:
```
$ docker run -p 3000:3000 fcm-wiremock
```