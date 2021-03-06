# Jenkins will do internal stuff, such as updating download links and deploying
# on our servers.
# The API URL below is provided by this plug-in: https://plugins.jenkins.io/build-token-root/
# The token in configured in the job, in the Build Triggers section.
#
echo -e "\n\n\tTriggering RRes deployment\n"
  
job='ondex-knet-builder_Update_Downloads_Links'
curl --user "$KNET_JENKINS_USER:$KNET_JENKINS_TOKEN" -X POST -o - --fail \
     "https://knetminer.org/build/job/$job/build"
