package edu.rpi.shuttles.notifications.health;

import com.codahale.metrics.health.HealthCheck;

public class NotificationHealthCheck extends HealthCheck {
  @Override
  protected Result check() throws Exception {
    return Result.healthy();
  }
}
