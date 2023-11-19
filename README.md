# mysql-native-sqldelight

A Kotlin native MySQL driver using libmysqlclient. Initial work inspired on [postgres-native-sqldelight](https://github.com/hfhbd/postgres-native-sqldelight).

You can use the driver with SQLDelight, but this is not required.

> Keep in mind, until now, this is only a single-threaded wrapper over libmysqlclient using 1 connection only. There is no connection pool nor multithread support (like JDBC or R2DBC).


## Testing
```shell
./gradlew clean nativeTest --info
```

## Licensing
The code written in this repository is released under the Apache License 2.0. Of note:
- This library statically links `libmysqlclient` from MySQL, which is released under GPLv2 [but also subject to 
    the Universal FOSS Exception](https://github.com/mysql/mysql-server/blob/87307d4ddd88405117e3f1e51323836d57ab1f57/LICENSE#L30-L36), 
    therefore not mandating release of this library under GPL too. 
- This library statically links `libssl` and `libcrypto` from OpenSSL 3, which is also released under Apache License 2.0