FROM eclipse-temurin:25.0.1_8-jre-alpine

ARG SOURCE
ARG COMMIT_HASH
ARG COMMIT_ID
ARG BUILD_TIME
LABEL source=${SOURCE}
LABEL commit_hash=${COMMIT_HASH}
LABEL commit_id=${COMMIT_ID}
LABEL build_time=${BUILD_TIME}

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG spring_config_label

# can be passed during Docker build as build time environment for spring profiles active
ARG active_profile

# can be passed during Docker build as build time environment for config server URL
ARG spring_config_url

# environment variable to pass active profile such as DEV, QA etc at docker runtime
ENV active_profile_env=${active_profile}

# environment variable to pass github branch to pickup configuration from, at docker runtime
ENV spring_config_label_env=${spring_config_label}

# environment variable to pass github branch to pickup configuration from, at docker runtime
ENV spring_config_label_env=${spring_config_label}

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG container_user=inji

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG container_user_group=inji

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG container_user_uid=1002

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG container_user_gid=1001

# create the non-root container user
RUN addgroup -g "${container_user_gid}" "${container_user_group}" \
    && adduser -s /bin/sh -u "${container_user_uid}" -G "${container_user_group}" \
        -h "/home/${container_user}" --disabled-password "${container_user}"

# set working directory for the user
WORKDIR /home/${container_user}

ENV work_dir=/home/${container_user}

COPY ./target/verify-service-*-exec.jar ./verify-service.jar
COPY ./docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh

# change permissions of file inside working dir
RUN chown -R "${container_user}:${container_user}" "/home/${container_user}" \
    && chmod +x /usr/local/bin/docker-entrypoint.sh

# select container user for all tasks
USER ${container_user_uid}:${container_user_gid}

EXPOSE 8080

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
