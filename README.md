# Terraform to Ansible (for AWS)

## Overview

This is a very small utility for generating Ansible inventory files from Terraform tfstate.tf files. When Terraform creates servers in the cloud then it creates a config file which contains details of all the server instances and other resources it creates and saves them in the terraform.tfstate file. This utility reads in this file and creates a list of the servers created in a format understood by Ansible. You can then use Ansible to ensure that the servers have the correct settings and correct software on them.

## How to use it

1. git clone <repo> or just download the jar file from <link to jar>.

2. Assuming you have a terraform.tfstate file in your current directory, run: <code>java -jar terraform_to_ansible.jar</code>

You should now have the Ansible inventory file printed on your screen. It is up to you whether you redirect this to a file or pass in the -a parameter to get the utility to write it to a file for you.

## How I use it in my workflow

<code>terraform apply && java -jar terraform_to_ansible.jar -o ~/ansible/hosts && cd ~/ansible && ansible-playbook -i hosts playbook.yml</code>

I prefer this approach since I find it easier than having one all-encompassing utility which handles both Terraform and Ansible for you.