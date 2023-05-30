# 一个简单的热更java class的方法  
## 使用步骤
1. 编译出 reloader.jar
2. java项目本体启动时添加启动参数</br>
`-javaagent: reloader.jar` </br>
会额外启动一个rmi服务，端口暂定为9999
3. 热更时，将新编译出的本体程序用解压软件打开（如7z）删除掉没有更新的class文件，只保留有更新的class文件
4. 将删减后的jar包命名为`hotdeploy.jar`（或使用-deploy参数传入自定义名称）
5. 执行`java -jar reload.jar -rmiHost 127.0.0.1 -rmiPort 9999 [-deploy hotdeploy.jar]`

## 原理 

javaagent会将本体的`Instrumentation`对象传入agent

调用`Instrumentation`的`redefineClasses`方法即可更新class  

反射调用`ClassLoader`的`defineClass`方法即可新增class

使用时需要用rmi找到本体的jvm实例

## 缺点

更新的class不能新增方法和变量，不能修改方法签名