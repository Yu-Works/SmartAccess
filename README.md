# SmartAccess - 简单易用的数据库访问工具
## 介绍
SmartAccess，一个简单易用的数据库访问工具。  
对于使用 [Rain](https://github.com/IceCream-QAQ/Rain) 作为开发框架的应用，SmartAccess 可以非常方便的集成进去。  
同时，为了满足不使用 [Rain](https://github.com/IceCream-QAQ/Rain) 的应用，也提供了一个精简的库模式。

SmartAccess 不是 ORM，他是一个 ORM 的二次封装。  
提供了多种多样的数据库访问方式，同时也提供了多种多样的数据库连接池支持。  
同时为了适应现代应用开发，SmartAccess 提供了完整的纯异步应用模式。（依赖于 Kotlin 及 Kotlin 协程）

## 设计思路
TODO

## 支持内容
### ORM

* Hibernate5
* Hibernate6
* Hibernate-Reactive


### 连接池

* [HikariCP](https://github.com/brettwooldridge/HikariCP)

## 文档

## 依赖引用表

<details>
  <summary>引用依赖表</summary>

* [Kotlin](https://kotlinlang.org/) ([Apache-2.0 license](https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt))
* [Kotlin-stdlib](https://kotlinlang.org/) ([Apache-2.0 license](https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt))
* [Kotlin-reflect](https://kotlinlang.org/) ([Apache-2.0 license](https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt))
* [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) ([Apache-2.0 license](https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt))
* [Rain](https://github.com/IceCream-QAQ/Rain) ([Apache-2.0 license](https://github.com/IceCream-QAQ/Rain/blob/master/LICENSE)) 及从 [Rain](https://github.com/IceCream-QAQ/Rain) 传递而来的[所有依赖](https://github.com/IceCream-QAQ/Rain#%E4%BE%9D%E8%B5%96%E5%86%85%E5%AE%B9)。
* [HikariCP](https://github.com/brettwooldridge/HikariCP) ([Apache-2.0 license](https://github.com/brettwooldridge/HikariCP/blob/dev/LICENSE))
* [Jakarta Persistence API](https://github.com/jakartaee/persistence)([Eclipse Public License (EPL)](https://github.com/jakartaee/persistence/blob/master/LICENSE.md))
* [Hibernate ORM](https://hibernate.org/orm/) ([LGPL 2.1](https://hibernate.org/community/license/))
* [Hibernate Reactive](https://github.com/hibernate/hibernate-reactive) ([Apache-2.0 license](https://github.com/hibernate/hibernate-reactive/blob/main/LICENSE))
</details>
