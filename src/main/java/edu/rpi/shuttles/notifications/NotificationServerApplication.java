package edu.rpi.shuttles.notifications;

import com.bendb.dropwizard.redis.JedisBundle;
import com.bendb.dropwizard.redis.JedisFactory;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import edu.rpi.shuttles.notifications.resources.NotificationResource;

public class NotificationServerApplication extends Application<NotificationServerConfiguration> {
  public static void main(String[] args) throws Exception {
    new NotificationServerApplication().run(args);
  }

  @Override
  public String getName() {
    return "shuttle-notification-server";
  }

  @Override
  public void initialize(Bootstrap<NotificationServerConfiguration> bootstrap) {
    bootstrap.addBundle(new JedisBundle<NotificationServerConfiguration>() {
      @Override
      public JedisFactory getJedisFactory(NotificationServerConfiguration configuration) {
        return configuration.getJedisFactory();
      }
    });
  }

  @Override
  public void run(NotificationServerConfiguration configuration,
    Environment environment) {
    final NotificationResource notificationResource = new NotificationResource();
    environment.jersey().register(notificationResource);
  }
}
