# CentOS 7 has glibc 2.17, like Kotlin/Native
FROM centos:centos7.9.2009 as build
# Remove old OpenSSL
RUN yum -y remove openssl openssl-devel
# Install GCC 8 + development tools
RUN yum -y install centos-release-scl && \
    yum -y install devtoolset-8 && \
    scl enable devtoolset-8 -- bash && \
    yum -y install epel-release && \
    yum -y groupinstall "Development Tools" && \
    yum -y install perl-IPC-Cmd && \
    yum -y install wget patchelf ncurses-devel && \
    yum clean all
# Install Cmake 3
RUN wget --no-check-certificate https://cmake.org/files/v3.12/cmake-3.12.3.tar.gz && \
    tar -xf cmake-3.12.3.tar.gz && \
    rm cmake-3.12.3.tar.gz && \
    cd cmake-3.12.3 &&  \
    ./bootstrap --prefix=/usr/local &&  \
    make -j$(nproc) &&  \
    make install &&  \
    cmake --version && \
    cd .. && \
    rm -r cmake-3.12.3
# Get OpenSSL source (--no-check-certificate because of clock problems breaking SSL?) & install it
RUN wget --no-check-certificate https://www.openssl.org/source/openssl-3.2.1.tar.gz && \
    tar -xf openssl-3.2.1.tar.gz && \
    rm openssl-3.2.1.tar.gz && \
    cd openssl-3.2.1 && \
    ./config --prefix=/usr/local/openssl --openssldir=/usr/local/openssl && \
    make -j $(nproc) && \
    make install && \
    ldconfig && \
    cd .. && \
    rm -r openssl-3.2.1
# Download MySQL 8 (--no-check-certificate because of clock problems breaking SSL?) from source and install
RUN wget --no-check-certificate https://dev.mysql.com/get/Downloads/MySQL-8.3/mysql-8.3.0.tar.gz && \
    tar -xf mysql-8.3.0.tar.gz && \
    rm mysql-8.3.0.tar.gz && \
    wget https://boostorg.jfrog.io/artifactory/main/release/1.77.0/source/boost_1_77_0.tar.bz2 && \
    cd mysql-8.3.0 && \
    mkdir bld && \
    cd bld && \
    cmake -D CMAKE_C_COMPILER="/opt/rh/devtoolset-8/root/usr/bin/gcc" \
                                -D CMAKE_CXX_COMPILER="/opt/rh/devtoolset-8/root/usr/bin/c++" \
                                -D CMAKE_LINKER="/opt/rh/devtoolset-8/root/usr/bin/ld" \
                                -D WITH_BOOST="../../" \
                                -D WITH_SSL="/usr/local/openssl" \
                                -D WITHOUT_SERVER=ON \
                                .. && \
    make && \
    make install && \
    cd ../.. && \
    rm -r mysql-8.3.0 && \
    rm -r boost_1_77_0 && \
    rm boost_1_77_0.tar.bz2
