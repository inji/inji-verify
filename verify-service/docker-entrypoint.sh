#!/bin/sh

exec java -jar \
  -Dspring.cloud.config.label="${spring_config_label_env}" \
  -Dspring.profiles.active="${active_profile_env}" \
  -Dspring.cloud.config.uri="${spring_config_url_env}" \
  verify-service.jar
