# Terraform to Ansible (for AWS)

## Overview

This is a very small utility for generating Ansible inventory files from Terraform tfstate.tf files.

When Terraform creates servers in the cloud then it saves details of all the server instances and other resources it creates in the terraform.tfstate file. This utility reads in this file and creates a list of the servers in a format understood by Ansible. You can then use Ansible to ensure that the servers have the correct settings and correct software on them.

## How to use it

1. git clone <repo> or just download the [jar file](https://github.com/poveyd/terraform-to-ansible/blob/master/build/libs/terraform_to_ansible-1.1.jar).

2. Tag your EC2 instances in Terraform using the `Ansible_host` tag (this is case-insensitive). This tag will be used as the name of the host group in the Ansible inventory file.

3. Create your EC2 instances using Terraform.

4. Assuming you have a `terraform.tfstate` file in your current directory, run: <code>java -jar terraform_to_ansible.jar</code>

You should now have the Ansible inventory file printed on your screen. It is up to you whether you redirect this to a file or pass in the -a parameter to get the utility to write it to a file for you.

## How I use it in my workflow

I have my Ansible playbook in the `~/ansible` directory.

<code>terraform apply && java -jar terraform_to_ansible.jar -a ~/ansible/hosts && cd ~/ansible && ansible-playbook -i hosts playbook.yml</code>

I like this approach since I find it easier than using one all-encompassing utility which handles both Terraform and Ansible for you.

## What is is used for?

For setting up a cluster of servers where you need to know the details, such as ip addresses, of all the servers in the cluster before you start confuring each server. If you use Terraform provisioners then you only know the ip address of the current server, not the other servers you need to connect to because they may not have been created yet.

I use it for setting up clusters of database servers (e.g. Mongo replica-sets). Each Mongo server needs to know the ip addresses of the other servers in the replica-set. It can only do this once the servers have been created by Terraform. Once Terraform has created all the servers which make up the replica-set then this utility can be run to create the Ansible inventory file and then, finally, the mongodb code can be configured on each server using Ansible.

## Extras

There is an additional command-line parameter which allows you to add content to your Ansible inventory file. It is:

`-p or --prepend "prepend this text to the start of the Ansible inventory file"`  

`-t or --ansible-tags "Pick off this set of tags from Terraform EC2 instance and place them at the end of each server entry in the Ansible inventory file (e.g. specify \"-t ansible_user\" in parameters then with a tag of Ansible_user=root in the Terraform EC2 config, you will get 1.2.3.4 ansible_user=root in the Ansible inventory file. If not present then any tags starting with \"Ansible-\" will be appended to the end of the server line in the Ansible inventory file (minus the \"Ansible-\" bit). Please do not use spaces in your tags."`
