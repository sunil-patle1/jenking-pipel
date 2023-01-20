pipeline {
    agent any
    stages {
        stage('pull repo') {
            steps {
                git branch: 'J2EE', url: 'https://github.com/pradip-selukar/onlinebookstore.git'
            }
        }
        stage('Build artifacts') {
            steps {
                sh 'sudo yum update -y'
                sh 'sudo yum install maven -y'
                sh 'mvn clean package'
            }
        }
        stage('push artifacts to S3') {
            steps {
               withAWS(credentials: 'abc', region: 'us-west-2') {
                   sh 'sudo yum update -y'
                   sh 'sudo yum install awscli -y'
                   sh 'aws --version'
                   sh 'aws s3 ls'
                   //sh 'aws s3 mb s3://mynew-jenkins-bucket138 --region us-west-2'
                   sh 'sudo mv /var/lib/jenkins/workspace/aaa/target/onlinebookstore-0.0.1-SNAPSHOT.war /mnt/onlinebookstore-0.0.1-SNAPSHOT.war'
                   sh 'aws s3 cp /mnt/onlinebookstore-0.0.1-SNAPSHOT.war s3://mynew-jenkins-bucket138/'
                   sh 'aws s3api put-object-acl --bucket mynew-jenkins-bucket138 --key onlinebookstore-0.0.1-SNAPSHOT.war --acl public-read'
                }
            }
        }
        stage("Dev-Deployment"){
            steps{
                withCredentials([sshUserPrivateKey(credentialsId: 'new-jen', keyFileVariable: 'abc', usernameVariable: 'ec2-user')]) {
                  
                 sh'''
                   ssh -i ${abc} -o StrictHostKeyChecking=no ec2-user@54.201.176.27<<EOF
                  sudo yum update -y
                  sudo yum install awscli -y 
                  aws --version
                  aws configure set aws_access_key_id AKIASGOCXLABWE
                  aws configure set aws_secret_access_key /TM0dNG/rLmSPY8CtWH3E5xPS
                  aws s3 ls
                  aws s3 cp s3://mynew-jenkins-bucket138/onlinebookstore-0.0.1-SNAPSHOT.war .
                  sudo curl -O https://dlcdn.apache.org/tomcat/tomcat-9/v9.0.71/bin/apache-tomcat-9.0.71.tar.gz
                  sudo tar -xvf apache-tomcat-9.0.71.tar.gz -C /opt/
                  sudo sh /opt/apache-tomcat-9.0.71/bin/shutdown.sh
                  sudo cp -rv onlinebookstore-0.0.1-SNAPSHOT.war bookstore.war
                  sudo cp -rv bookstore.war /opt/apache-tomcat-9.0.71/webapps/
                  sudo sh /opt/apache-tomcat-9.0.71/bin/startup.sh
                  '''
                 
                }
            } 
        }
    }
}