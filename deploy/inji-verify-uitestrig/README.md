# UITESTRIG

## Introduction
UITESTRIG will test end-to-end functional flows involving multiple INJIVERIFY-UI module.

It is deployed using the [inji-verify-uitestrig](../../helm/inji-verify-uitestrig) helm chart maintained in this repository (published as `inji/inji-verify-uitestrig`).

## Update the values.yaml

1. Create the required BrowserStack credentials by referring to this [documentation](https://www.browserstack.com/docs/references/service-accounts) and  create google credentials by referring to this [documentation](https://mosip.atlassian.net/wiki/spaces/QT/pages/1671168131/Generate+ID+Token+for+Inji+Web+Login+API+using+Google+Sign+In).
2. Add the BrowserStack and Google credentials in the values.yaml file.
3. Replace sandbox with the appropriate environment name.
``` example : api-internal.sandbox to api-internal.dev ```
4. Replace sandbox.xyz.net with the required environment endpoint.
``` example : api-internal.sandbox.xyz.net to api-internal.dev.mosip.net ```
5. Update other fields as per your requirements, if needed.

## Install
* Install
```sh
./install.sh
```

## Uninstall
* To uninstall UITESTRIG, run `delete.sh` script.
```sh
./delete.sh 
```

## Run UITESTRIG manually

#### CLI
* Download Kubernetes cluster `kubeconfig` file from `rancher dashboard` to your local.
* Install `kubectl` package to your local machine.
* Run UITESTRIG manually via CLI by creating a new job from an existing k8s cronjob.
  ```
  kubectl --kubeconfig=<k8s-config-file> -n uitestrig-injiverify create job --from=cronjob/<cronjob-name> <job-name>
  ```
  example:
  ```
  kubectl --kubeconfig=/home/xxx/Downloads/qa4.config -n uitestrig-injiverify create job --from=cronjob/cronjob-inji-verify-uitestrig-verifyui cronjob-inji-verify-uitestrig-verifyui
  ```


