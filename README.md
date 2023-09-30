# mysql-native-sqldelight

A Kotlin native MySQL driver using libmysqlclient. Initial work inspired on [postgres-native-sqldelight](https://github.com/hfhbd/postgres-native-sqldelight).

You can use the driver with SQLDelight, but this is not required.

> Keep in mind, until now, this is only a single-threaded wrapper over libmysqlclient using 1 connection only. There is no connection pool nor multithread support (like JDBC or R2DBC).


## Testing
```shell
./gradlew clean nativeTest --info
```

## Caveats
1. MySQL does not support nested transactions. While this driver will handle SQLDelight's post-commit and -rollback hooks correctly for nested tranactions, the actual transaction is implicitly committed as soon as the new 'nested' transaction is started. <br /> This _may_ be solved in the future by implementing `SAVEPOINT`s.