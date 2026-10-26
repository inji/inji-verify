# INJIVERIFY-APITESTRIG

Helm chart to deploy APITESTRIG for `Inji Verify`. It runs the Inji Verify API automation suite (`api-test/`) as a Kubernetes CronJob.

## TL;DR

```console
$ helm repo add inji https://inji.github.io/helm
$ helm install injiverify-apitestrig inji/injiverify-apitestrig
```

For the full install flow (config maps, secrets, cron time, SSL, reports) use the [deploy scripts](../../deploy/injiverify-apitestrig).
