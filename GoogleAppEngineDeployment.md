Describes procedure for deploying js4ms servlet projects to Google App Engine.

# Eclipse Setup #


# Project Deployment #

  * Checkout Project

## For projects built using Ant ##
  * Open Ant view and add project build.xml as build file.
  * Answer prompts as they appear.
  * Run "deploy-gae" build target. This should result in the generation of all files that will be included in the WAR file structure that will be uploaded to the Google App Engine.

## For projects built using Eclipse builders ##
  * Select project action 'Google->Deploy to App Engine".