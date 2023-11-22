# mysql-native-sqldelight

A Kotlin native MySQL driver using libmysqlclient. Initial work inspired on [postgres-native-sqldelight](https://github.com/hfhbd/postgres-native-sqldelight).

You can use the driver with SQLDelight, but this is not required.

<details>
<summary>Contents</summary>

- [Caveats](#Caveats)
- [Testing](#Testing)
- [Documentation](#Documentation)
- [Licensing](#Licensing)

</details>

## Caveats
1. MySQL does not support nested transactions. While this driver will handle SQLDelight's post-commit and -rollback hooks 
    correctly for nested tranactions, the actual transaction is implicitly committed as soon as the new 'nested' transaction
    is started. This _may_ be solved in the future by implementing `SAVEPOINT`s.

## Testing
```shell
./gradlew clean allTests --info
```

## Documentation
[<img alt="Deployed with FTP Deploy Action" src="https://img.shields.io/badge/Deployed With-FTP DEPLOY ACTION-%3CCOLOR%3E?style=for-the-badge&color=0077b6">](https://github.com/SamKirkland/FTP-Deploy-Action)

Documentation is automatically generated for the latest commit in `main` and published to [sqldelight.joepeding.nl](https://sqldelight.joepeding.nl/index.html).
To manually generate the documentation, run the following command to generate HTML documentation from KDoc:
```shell
./gradlew clean dokkaHtmlMultiModule
```
Then open `build/dokka/htmlMultiModule/index.html` to browse the generated documentation 

## Licensing
The code written in this repository is released under the Apache License 2.0. Of note:
- This library statically links `libmysqlclient` from MySQL, which is released under GPLv2 [but also subject to 
    the Universal FOSS Exception](https://github.com/mysql/mysql-server/blob/87307d4ddd88405117e3f1e51323836d57ab1f57/LICENSE#L30-L36), 
    therefore not mandating release of this library under GPL too. 
- This library statically links `libssl` and `libcrypto` from OpenSSL 3, which is also released under Apache License 2.0