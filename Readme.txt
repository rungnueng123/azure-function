mvn clean package
mvn azure-functions:run
mvn azure-functions:deploy

// config local.setting.json file
func azure functionapp fetch-app-settings smc-email-queue
func azure storage fetch-connection-string merchantportalblob