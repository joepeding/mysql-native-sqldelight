# CentOS 7 has glibc 2.17, like Kotlin/Native
FROM centos:centos7.9.2009 as build
# Remove old OpenSSL
RUN yum -y remove openssl openssl-devel
# Install GCC 8
RUN yum -y install centos-release-scl
RUN yum -y install devtoolset-8
RUN scl enable devtoolset-8 -- bash
### Install tools
RUN yum -y install epel-release
RUN yum -y groupinstall "Development Tools"
RUN yum -y install perl-IPC-Cmd
RUN yum -y install wget patchelf ncurses-devel
# Install Cmake 3
RUN wget --no-check-certificate https://cmake.org/files/v3.12/cmake-3.12.3.tar.gz
RUN tar -xf cmake-3.12.3.tar.gz
RUN cd cmake-3.12.3 && ./bootstrap --prefix=/usr/local && make -j$(nproc) && make install && cmake --version
# Get OpenSSL source (--no-check-certificate because of clock problems breaking SSL?)
RUN wget --no-check-certificate https://www.openssl.org/source/openssl-3.1.3.tar.gz
RUN tar -xf openssl-3.1.3.tar.gz
# Compile OpenSSL
RUN cd openssl-3.1.3 && ./config --prefix=/usr/local/openssl --openssldir=/usr/local/openssl
RUN cd openssl-3.1.3 && make -j $(nproc)
RUN cd openssl-3.1.3 && make install
RUN ldconfig
# Download MySQL 8 from source
RUN wget --no-check-certificate https://dev.mysql.com/get/Downloads/MySQL-8.1/mysql-8.1.0.tar.gz
RUN tar -xf mysql-8.1.0.tar.gz
# Download Boost
RUN wget https://boostorg.jfrog.io/artifactory/main/release/1.77.0/source/boost_1_77_0.tar.bz2
# Install MySQL
RUN cd mysql-8.1.0 && mkdir "bld"
RUN cd mysql-8.1.0/bld && cmake -D CMAKE_C_COMPILER="/opt/rh/devtoolset-8/root/usr/bin/gcc" \
                                -D CMAKE_CXX_COMPILER="/opt/rh/devtoolset-8/root/usr/bin/c++" \
                                -D CMAKE_LINKER="/opt/rh/devtoolset-8/root/usr/bin/ld" \
                                -D WITH_BOOST="../../" \
                                -D WITH_SSL="/usr/local/openssl" \
                                -D WITHOUT_SERVER=ON \
                                ..
RUN cd mysql-8.1.0/bld && make
RUN cd mysql-8.1.0/bld && make install
