# mysql-native-sqldelight

A Kotlin native Mysql driver using libmysqlclient. Initial work inspired on [postgres-native-sqldelight](https://github.com/hfhbd/postgres-native-sqldelight).

You can use the driver with SQLDelight, but this is not required.

> Keep in mind, until now, this is only a single-threaded wrapper over libmysqlclient using 1 connection only. There is no connection pool nor multithread support (like JDBC or R2DBC).
