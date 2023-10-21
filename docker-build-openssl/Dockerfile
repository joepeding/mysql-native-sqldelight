# CentOS 7 has glibc 2.17, like Kotlin/Native
FROM centos:centos7.9.2009 as build
# Remove old OpenSSL
RUN yum -y remove openssl openssl-devel
# Install tools
RUN yum -y groupinstall "Development Tools"
RUN yum -y install perl-IPC-Cmd
RUN yum -y install wget
# Get OpenSSL source (--no-check-certificate because of clock problems breaking SSL?)
RUN wget --no-check-certificate https://www.openssl.org/source/openssl-3.1.3.tar.gz
RUN tar -xf openssl-3.1.3.tar.gz
# Compile OpenSSL
RUN cd openssl-3.1.3 && ./config --prefix=/usr/local/openssl --openssldir=/usr/local/openssl
RUN cd openssl-3.1.3 && make -j $(nproc)
RUN cd openssl-3.1.3 && make install
RUN ldconfig
# Install MySQL 8
RUN rpm -Uvh https://repo.mysql.com/mysql80-community-release-el7-3.noarch.rpm
RUN rpm --import http://repo.mysql.com/RPM-GPG-KEY-mysql-2022
RUN sed -i 's/enabled=1/enabled=0/' /etc/yum.repos.d/mysql-community.repo
RUN yum -y --enablerepo=mysql80-community install mysql-community-server
