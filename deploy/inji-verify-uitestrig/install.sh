#!/bin/bash
# Installs uitestrig automation
## Usage: ./install.sh [kubeconfig]

if [ $# -ge 1 ] ; then
  export KUBECONFIG=$1
fi

NS=uitestrig-injiverify
CHART_VERSION=1.3.5
COPY_UTIL=../copy_cm_func.sh

echo Create $NS namespace
kubectl create ns $NS

function installing_uitestrig() {
  ENV_NAME=$( kubectl -n default get cm global -o json |jq -r '.data."installation-domain"')

  read -p "Is values.yaml under inji-verify-uitestrig set correctly as part of pre-requisites? (Y/n) : " yn;
  if [[ $yn = "Y" ]] || [[ $yn = "y" ]] ; then

  echo "Do you have public domain & valid SSL? (Y/n) "
  echo "Y: if you have public domain & valid ssl certificate"
  echo "n: if you don't have public domain & valid ssl certificate"
  read -p "" flag

  if [ -z "$flag" ]; then
    echo "'flag' was provided; EXITING;"
    exit 1;
  fi

  ENABLE_INSECURE=''
  if [ "$flag" = "n" ]; then
    ENABLE_INSECURE='--set uitestrig.configmaps.uitestrig.ENABLE_INSECURE=true';
  fi

  echo Istio label
  kubectl label ns $NS istio-injection=disabled --overwrite
  helm repo update

  echo Copy configmaps
  $COPY_UTIL configmap global default $NS
  $COPY_UTIL configmap keycloak-host keycloak $NS
  $COPY_UTIL configmap config-server-share config-server $NS

  echo Copy secrets
  $COPY_UTIL secret keycloak-client-secrets keycloak $NS
  $COPY_UTIL secret s3 s3 $NS
  $COPY_UTIL secret postgres-postgresql postgres $NS

  DB_HOST=$( kubectl -n default get cm global -o json  |jq -r '.data."mosip-api-internal-host"' )
  INJI_VERIFY_HOST=$(kubectl -n default get cm global -o json  |jq -r '.data."mosip-injiverify-host"')
  INJI_WEB_HOST=$(kubectl -n default get cm global -o json  |jq -r '.data."mosip-injiweb-host"')
  ESIGNET_HOST=$(kubectl -n default get cm global -o json  |jq -r '.data."mosip-esignet-host"')
  API_INTERNAL_HOST=$( kubectl -n default get cm global -o json  |jq -r '.data."mosip-api-internal-host"' )

  echo Installing verify uitestrig
  helm -n $NS install uitestrig-injiverify mosip/uitestrig \
  -f values.yaml  \
  --version $CHART_VERSION \
  $ENABLE_INSECURE

  echo Installed verify uitestrig
  fi
  return 0
}

# set commands for error handling.
set -e
set -o errexit   ## set -e : exit the script if any statement returns a non-true return value
set -o nounset   ## set -u : exit the script if you try to use an uninitialised variable
set -o errtrace  # trace ERR through 'time command' and other functions
set -o pipefail  # trace ERR through pipes
installing_uitestrig   # calling function
