# Real-time Notification Server

This service provides publishing, retrieval, and notification APIs for real-time notifications.

It is a [JAX-RS](http://en.wikipedia.org/wiki/Java_API_for_RESTful_Web_Services) service written in Java using the [Dropwizard](http://dropwizard.io) framework.

Notifications are persisted in [Redis](http://redis.io/) and sent to interested consumers over [Redis PubSub](http://redis.io/topics/pubsub).

A WebSocket bridge is coming soon.

## Usage

### Sending notifications

Sending a notification to all subscribers of the "test" topic:

    curl -H 'Content-Type: application/json' -X PUT -d '{"title":"This is a test notification.", "payload": ["Foo", "Bar", "Baz"]}' http://localhost:8080/notifications?topic=test

## Building

    mvn package
    java -jar target/shuttle-notifications-0.0.1-SNAPSHOT.jar server config/configuration.sample.yml

## Setting up a Redis server for development

## Mac OS X

    brew install redis
    redis-server /usr/local/etc/redis.conf
