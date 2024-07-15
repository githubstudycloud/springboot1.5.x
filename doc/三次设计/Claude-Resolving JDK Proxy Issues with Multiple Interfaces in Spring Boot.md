## Prompt:
undefined

## Prompt:
运行提示cacherefreshservice在被注入时遇到问题，是jdk代理，原因可能是它有多个可用接口，spring boot不知道用哪一个

## Prompt:
我把之前的cacheservice改成cacheserviceimpl并新建了一个接口cacheservice，就好了，就你分析，其他业务构建自动刷新缓存如何做，目前是共用一个队列吗

## Prompt:
先以当前设计，给一个从数据库查询需要构造的数据发送到rabbitmq然后触发刷新的过程实际例子，要修改哪几个类的方法。key的构造会是什么样

## Prompt:
这个里面的测试类的mock.open在springboot1.5没有，我改成init，但报无法解析org.junit.platform，请修改这3个测试类让它在springboot1.5下可以运行

## Prompt:
对于生成key和元数据哪几个类，要怎么使用起来，请结合例子说明

## Prompt:
我还不太能理解，请更详细些说明使用方法

## Prompt:
我理解一点了，假设，我有一个业务数据，可以通过接口分页刷新，我通过项目id加类型+需求number确定唯一性，我打算查询出所有key然后轮询获取数据后缓存，请以这个场景为例子谈怎么修改业务代码

## Prompt:
我怎么没看到rabbitmq的自动刷新部分，在自动刷新里怎么加入，和原来的设计怎么没有一个继承关系，还有手动触发能覆盖自动触发的部分

## Prompt:
队列都是一个吧，搞个队列分离和队列优先，结合刚才的最终设计，在原来的代码上进行修改，给出修改的部分保证一致和易于扩展

## Prompt:
业务代码怎么加还没说，就接口分页那个

## Prompt:
这是在我们刚才改的基础上加的吗

