FROM eclipse-temurin:21-jdk

USER root
RUN apt-get update && apt-get install -y git
# Define build-time arguments
ARG SOURCE
ARG COMMIT_HASH
ARG COMMIT_ID
ARG BUILD_TIME

# Set labels for metadata
LABEL source=${SOURCE}
LABEL commit_hash=${COMMIT_HASH}
LABEL commit_id=${COMMIT_ID}
LABEL build_time=${BUILD_TIME}

# Define build-time arguments for user and group
ARG container_user=mosip
ARG container_user_group=mosip
ARG container_user_uid=1001
ARG container_user_gid=1001

# Set working directory for the user
ENV work_dir=/home/${container_user}/

ARG KUBECTL_VERSION=1.22.9

# install packages and create user
RUN groupadd -g ${container_user_gid} ${container_user_group} \
    && useradd -u ${container_user_uid} -g ${container_user_group} -s /bin/bash -m ${container_user} -d ${work_dir} \
    && chown -R ${container_user}:${container_user} /home/${container_user}/  \
    && curl -LO "https://storage.googleapis.com/kubernetes-release/release/v${KUBECTL_VERSION}/bin/linux/amd64/kubectl" \
    && chmod +x kubectl \
    && mv kubectl /usr/local/bin/

# Switch to the specified user for the subsequent commands
USER ${container_user_uid}:${container_user_gid}

RUN mkdir -p /home/${container_user}/src/test/resources/config/

RUN mkdir -p /home/${container_user}/test-output/SparkReport/ \
    && chmod -R 777 /home/${container_user}/test-output

RUN mkdir -p /home/${container_user}/screenshots/ \
    && chmod -R 777 /home/${container_user}/screenshots/

# Copy files
COPY --chown=${container_user_uid}:${container_user} --chmod=771 ./entrypoint.sh ${work_dir}/entrypoint.sh
COPY --chown=${container_user}:${container_user} ./application.properties ${work_dir}
COPY --chown=${container_user}:${container_user} ./target ${work_dir}/target
COPY --chown=${container_user}:${container_user} ./src/test/resources/featurefiles/ /home/${container_user}/featurefiles/
COPY --chown=${container_user}:${container_user} ./src/main/resources/ /home/${container_user}/src/test/resources/
COPY --chown=${container_user}:${container_user} ./src/test/resources/config.properties /home/${container_user}/src/test/resources/config.properties
COPY --chown=${container_user}:${container_user} ./src/main/java/utils/ /home/${container_user}/src/main/java/utils/
COPY --chown=${container_user}:${container_user} ./screenshots/ /home/${container_user}/screenshots/
COPY --chown=${container_user}:${container_user} ./resource/ /home/${container_user}/resource/
COPY --chown=${container_user}:${container_user} ./testNgXmlFiles/ /home/${container_user}/testNgXmlFiles/
COPY --chown=${container_user}:${container_user} ./InsuranceCredential.pdf /home/${container_user}/InsuranceCredential.pdf
COPY --chown=${container_user}:${container_user} ./src/test/resources/QRCodes/ /home/${container_user}/QRCodes/

COPY --chown=${container_user_uid}:${container_user} ./browserstack.yml /home/${container_user}/
COPY --chown=${container_user_uid}:${container_user} ./target/*.jar ${work_dir}

# Set working directory
WORKDIR /home/${container_user}/
