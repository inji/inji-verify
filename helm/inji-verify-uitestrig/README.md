# INJI-VERIFY-UITESTRIG

Helm chart to deploy UITESTRIG for `Inji Verify`. It runs the Inji Verify UI automation suite (`ui-test/`) as a Kubernetes CronJob.

## TL;DR

```console
$ helm repo add inji https://inji.github.io/helm
$ helm install inji-verify-uitestrig inji/inji-verify-uitestrig
```

For the full install flow (config maps, secrets, BrowserStack/Google credentials) use the [deploy scripts](../../deploy/inji-verify-uitestrig).
