#!/bin/bash
# Installs all inji-verify-service helm charts
## Usage: ./install.sh [kubeconfig]

if [ $# -ge 1 ] ; then
  export KUBECONFIG=$1
fi

NS=injiverify
CHART_VERSION=0.16.0

echo Create $NS namespace
kubectl create ns $NS

function installing_inji-verify-service() {

  echo Istio label
  kubectl label ns $NS istio-injection=enabled --overwrite
  helm repo add mosip https://mosip.github.io/mosip-helm
  helm repo update

  echo Copy configmaps
  COPY_UTIL=../copy_cm_func.sh
  $COPY_UTIL configmap inji-stack-config default $NS

  echo "Do you have public domain & valid SSL? (Y/n) "
  echo "Y: if you have public domain & valid ssl certificate"
  echo "n: If you don't have a public domain and a valid SSL certificate. Note: It is recommended to use this option only in development environments."
  read -p "" flag

  if [ -z "$flag" ]; then
    echo "'flag' was provided; EXITING;"
    exit 1;
  fi
  ENABLE_INSECURE=''
  if [ "$flag" = "n" ]; then
    ENABLE_INSECURE='--set enable_insecure=true';
  fi

  DEFAULT_INJIVERIFY_HOST=$(kubectl get cm inji-stack-config -n config-server -o jsonpath={.data.injiverify-host})

  # Check if INJIVERIFY_HOST is present under configmap/inji-stack-config of configserver
  if echo "$DEFAULT_INJIVERIFY_HOST" | grep -q "INJIVERIFY_HOST"; then
    echo "INJIVERIFY_HOST is already present in configmap/inji-stack-config of configserver"
    MOSIP_INJIVERIFY_HOST=$DEFAULT_INJIVERIFY_HOST
  else
    read -p "Please provide injiverifyhost (eg: injiverify.sandbox.xyz.net): " INJIVERIFY_HOST

    if [ -z "$INJIVERIFY_HOST" ]; then
      echo "INJIVERIFY Host not provided; EXITING;"
      exit 0
    fi    
  fi   

  CHK_INJIVERIFY_HOST=$(nslookup "$INJIVERIFY_HOST")
  if [ $? -gt 0 ]; then
    echo "InjiVERIFY Host does not exist; EXITING;"
    exit 0
  fi

  INJIVERIFY_HOST=$(kubectl get cm inji-stack-config -o jsonpath={.data.injiverify-host})

  echo Installing inji-verify-service
  helm -n $NS install inji-verify-service mosip/inji-verify-service \
    --version $CHART_VERSION $ENABLE_INSECURE \
    --set extraEnv[0].name=INJI_VP_SUBMISSION_BASE_URL \
    --set extraEnv[0].value="https://${INJIVERIFY_HOST}/v1/verify" \
    --set extraEnv[1].name=INJI_DID_VERIFY_URI \
    --set extraEnv[1].value="did:web:${INJIVERIFY_HOST//:/\\:}:v1:verify" \
    --set extraEnv[2].name=INJI_DID_VERIFY_PUBLIC_KEY_URI \
    --set extraEnv[2].value="did:web:${INJIVERIFY_HOST//:/\\:}:v1:verify#key-0" \
    --set extraEnv[3].name=INJI_VERIFY_REDIRECT_URI \
    --set extraEnv[3].value=" " # Space character is intentional; empty string causes Helm deployment errors
    
  kubectl -n $NS  get deploy -o name |  xargs -n1 -t  kubectl -n $NS rollout status

  echo Installed inji-verify-service service
  return 0
}

# set commands for error handling.
set -e
set -o errexit   ## set -e : exit the script if any statement returns a non-true return value
set -o nounset   ## set -u : exit the script if you try to use an uninitialised variable
set -o errtrace  # trace ERR through 'time command' and other functions
set -o pipefail  # trace ERR through pipes
installing_inji-verify-service   # calling function
