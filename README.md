# Terraform to Ansible (for AWS)

## Overview

This is a very small utility for generating Ansible inventory files from Terraform tfstate.tf files.

When Terraform creates servers in the cloud then it saves details of all the server instances and other resources it creates in the terraform.tfstate file. This utility reads in this file and creates a list of the servers in a format understood by Ansible. You can then use Ansible to ensure that the servers have the correct settings and correct software on them.

## How to use it

1. git clone <repo> or just download the [jar file](https://github.com/poveyd/terraform-to-ansible/blob/master/build/libs/terraform_to_ansible-1.0.jar).

2. Tag your EC2 instances in Terraform using the `Ansible_host` tag (this is case-insensitive). This tag will be used as the name of the host group in the Ansible inventory file.

3. Create your EC2 instances using Terraform.

4. Assuming you have a `terraform.tfstate` file in your current directory, run: <code>java -jar terraform_to_ansible.jar</code>

You should now have the Ansible inventory file printed on your screen. It is up to you whether you redirect this to a file or pass in the -a parameter to get the utility to write it to a file for you.

## How I use it in my workflow

I have my Ansible playbook in the `~/ansible` directory.

<code>terraform apply && \  
java -jar terraform_to_ansible.jar -a ~/ansible/hosts && \  
cd ~/ansible && \  
ansible-playbook -i hosts playbook.yml</code>

I like this approach since I find it easier than using one all-encompassing utility which handles both Terraform and Ansible for you.
