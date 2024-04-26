
https://www.liaoxuefeng.com/wiki/1252599548343744/1282383699509281
我们在前面已经介绍了Java程序访问数据库的标准接口JDBC，它的实现方式非常简洁，即：Java标准库定义接口，各数据库厂商以“驱动”的形式实现接口。
应用程序要使用哪个数据库，就把该数据库厂商的驱动以jar包形式引入进来，同时自身仅使用JDBC接口，编译期并不需要特定厂商的驱动。

使用JDBC虽然简单，但代码比较繁琐。Spring为了简化数据库访问，主要做了以下几点工作：
1 提供了简化的访问JDBC的模板类，不必手动释放资源；
2 提供了一个统一的DAO类以实现Data Access Object模式；
3 把SQLException封装为DataAccessException，这个异常是一个RuntimeException，并且让我们能区分SQL异常的原因，例如，
   DuplicateKeyException表示违反了一个唯一约束；
4 能方便地集成Hibernate、JPA和MyBatis这些数据库访问框架


JPA：Java Persistence API
JPA就是JavaEE的一个ORM标准，它的实现其实和Hibernate没啥本质区别，但是用户如果使用JPA，那么引用的就是jakarta.persistence这个“标准”包，
而不是org.hibernate这样的第三方包。因为JPA只是接口，所以，还需要选择一个实现产品，跟JDBC接口和MySQL驱动一个道理。

我们使用JPA时也完全可以选择Hibernate作为底层实现，但也可以选择其它的JPA提供方，比如EclipseLink。Spring内置了JPA的集成，
并支持选择Hibernate或EclipseLink作为实现