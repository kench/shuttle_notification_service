package edu.rpi.shuttles.notifications;

import javax.validation.constraints.NotNull;

import com.bendb.dropwizard.redis.JedisFactory;
import lombok.Getter;

import io.dropwizard.Configuration;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

public class NotificationServerConfiguration extends Configuration {

  @JsonProperty
  @NotNull
  private JedisFactory redis;

  public JedisFactory getJedisFactory() {
    return redis;
  }
}
