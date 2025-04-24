@Library('Jenkins-Shared-Lib') _

pipeline {
    agent {
        kubernetes {
            yaml jenkinsAgent(["agent-java-21": "registry.runicrealms.com/jenkins/agent-java-21:latest"])
        }
    }

    environment {
        ARTIFACT_NAME = 'game'
        PROJECT_NAME = 'Game'
        REGISTRY = 'registry.runicrealms.com'
        REGISTRY_PROJECT = 'library'
    }

    stages {
        stage('Send Discord Notification (Build Start)') {
            steps {
                discordNotifyStart(env.PROJECT_NAME, env.GIT_URL, env.GIT_BRANCH, env.GIT_COMMIT.take(7))
            }
        }
        stage('Determine Environment') {
            steps {
                script {
                    def branchName = env.GIT_BRANCH.replaceAll(/^origin\//, '').replaceAll(/^refs\/heads\//, '')

                    echo "Using normalized branch name: ${branchName}"

                    if (branchName == 'dev') {
                        env.RUN_MAIN_DEPLOY = 'false'
                    } else if (branchName == 'main') {
                        env.RUN_MAIN_DEPLOY = 'true'
                    } else {
                        error "Unsupported branch: ${branchName}"
                    }
                }
            }
        }
        stage('Build and Push Artifacts') {
            steps {
                container('agent-java-21') {
                    script {
                        sh """
                        ./gradlew :plugin:shadowJar --no-daemon
                        """
                        orasPush(env.ARTIFACT_NAME, env.GIT_COMMIT.take(7), "plugin/build/libs/game-plugin-all.jar", env.REGISTRY, env.REGISTRY_PROJECT)
                    }
                }
            }
        }
        stage('Update Realm-Velocity and Realm-Paper Manifests') {
            steps {
                container('agent-java-21') {
                    updateManifest('dev', 'Realm-Paper', 'artifact-manifest.yaml', env.ARTIFACT_NAME, env.GIT_COMMIT.take(7), 'artifacts.velagones.tag')
                }
            }
        }
        stage('Create PRs to Promote Realm-Paper Dev to Main (Prod Only)') {
            when {
                expression { return env.RUN_MAIN_DEPLOY == 'true' }
            }
            steps {
                container('agent-java-21') {
                    createPR('Velagones', 'Realm-Paper', 'dev', 'main')
                }
            }
        }
    }

    post {
        success {
            discordNotifySuccess(env.PROJECT_NAME, env.GIT_URL, env.GIT_BRANCH, env.GIT_COMMIT.take(7))
        }
        failure {
            discordNotifyFail(env.PROJECT_NAME, env.GIT_URL, env.GIT_BRANCH, env.GIT_COMMIT.take(7))
        }
    }
}