#!/bin/bash
## Installs inji-verify-ui helm charts
## Usage: ./install.sh [kubeconfig]

if [ $# -ge 1 ] ; then
  export KUBECONFIG=$1
fi

NS=injiverify
CHART_VERSION=0.18.0-develop

DEFAULT_INJIVERIFY_HOST=$( kubectl get cm inji-stack-config -n config-server -o jsonpath={.data.injiverify-host} )
# Check if INJIVERIFY_HOST is present under configmap/inji-stack-config of configserver
if echo "$DEFAULT_INJIVERIFY_HOST" | grep -q "INJIVERIFY_HOST"; then
    echo "INJIVERIFY_HOST is already present in configmap/inji-stack-config of configserver"
else
    read -p "Please provide injiverifyhost (eg: injiverify.sandbox.xyz.net ) : " INJIVERIFY_HOST

    if [ -z "INJIVERIFY_HOST" ]; then
    echo "INJIVERIFY Host not provided; EXITING;"
    exit 0;
    fi
fi

CHK_INJIVERIFY_HOST=$( nslookup "$INJIVERIFY_HOST" )
if [ $? -gt 0 ]; then
    echo "InjiVERIFY Host does not exists; EXITING;"
    exit 0;
fi

read -p "Please provide Inji Web host (optional (eg:injiweb.sandbox.xyz.net), press Enter to skip): " INJIWEB_HOST

WALLET_BASE_URL=""

if [ -n "$INJIWEB_HOST" ]; then
  nslookup "$INJIWEB_HOST" >/dev/null 2>&1
  if [ $? -gt 0 ]; then
    echo "Inji Web host does not exist; EXITING;"
    exit 1
  fi

  WALLET_BASE_URL="https://$INJIWEB_HOST"
  echo "Using walletBaseUrl: $WALLET_BASE_URL"
else
  echo "Skipping Inji Web host configuration"
fi

echo "INJIVERIFY_HOST is not present in configmap/inji-stack-config of configserver"
    # Add injiverify host to inji-stack-config
    kubectl patch configmap inji-stack-config -n config-server --type merge -p "{\"data\": {\"injiverify-host\": \"$INJIVERIFY_HOST\"}}"
    kubectl patch configmap inji-stack-config -n default --type merge -p "{\"data\": {\"injiverify-host\": \"$INJIVERIFY_HOST\"}}"
    # Add the host
    kubectl -n config-server set env --keys=injiverify-host --from configmap/inji-stack-config deployment/config-server --prefix=SPRING_CLOUD_CONFIG_SERVER_OVERRIDES_
    # Restart the configserver deployment
    kubectl -n config-server get deploy -o name | xargs -n1 -t kubectl -n config-server rollout status

echo Create $NS namespace
kubectl create ns $NS

function installing_inji-verify-ui() {
  echo Istio label
  kubectl label ns $NS istio-injection=enabled --overwrite

  helm repo add inji https://inji.github.io/helm
  helm repo update

  echo Copy configmaps
  COPY_UTIL=../copy_cm_func.sh
  $COPY_UTIL configmap inji-stack-config default $NS

  while true; do
    read -p "Enable VP_SUBMISSION_SUPPORTED? (true/false) [default: true]: " VP_SUBMISSION_SUPPORTED
    VP_SUBMISSION_SUPPORTED=${VP_SUBMISSION_SUPPORTED:-true}
    if [[ "$VP_SUBMISSION_SUPPORTED" == "true" || "$VP_SUBMISSION_SUPPORTED" == "false" ]]; then
      break
    else
      echo "Invalid input. Please enter 'true' or 'false'."
    fi
  done

  INJIVERIFY_HOST=$(kubectl get cm inji-stack-config -o jsonpath={.data.injiverify-host})
  echo Installing INJIVERIFY
  helm -n $NS install inji-verify-ui inji/inji-verify-ui \
  --set istio.hosts\[0\]=$INJIVERIFY_HOST \
  --set inji_verify_service.host="inji-verify-service.$NS" \
  --set extraEnvVars[0].name=VP_SUBMISSION_SUPPORTED \
  --set-string extraEnvVars[0].value="${VP_SUBMISSION_SUPPORTED}" \
  --set-string walletBaseUrl="$WALLET_BASE_URL" \
  --version $CHART_VERSION

  kubectl -n $NS  get deploy -o name |  xargs -n1 -t  kubectl -n $NS rollout status

  echo Installed inji-verify-ui
  return 0
}

# set commands for error handling.
set -e
set -o errexit   ## set -e : exit the script if any statement returns a non-true return value
set -o nounset   ## set -u : exit the script if you try to use an uninitialised variable
set -o errtrace  # trace ERR through 'time command' and other functions
set -o pipefail  # trace ERR through pipes
installing_inji-verify-ui   # calling function
