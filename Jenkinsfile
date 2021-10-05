pipeline {
    agent { docker { image 'maven:3.6.3-jdk-11-slim' } }
    stages {
        stage('Test') {
            steps {
                retry(3) {
                    echo 'Step 1 retry'
                    sh 'java -version'
                    sh 'javac -version'

                }
                timout(time:10, units:minutes) {
                    echo 'Step 2 timout'
                    sh 'mvn --version'
                    sh 'sleep 9m'
                }
            }
        }
    }
    post {
        always {
            echo 'This will always run'
        }
        success {
            echo 'This will run only if successful'
        }
        failure {
            echo 'This will run only if failed'
        }
        unstable {
            echo 'This will run only if the run was marked as unstable'
        }
        changed {
            echo 'This will run only if the state of the Pipeline has changed'
            echo 'For example, if the Pipeline was previously failing but is now successful'
        }
    }
}