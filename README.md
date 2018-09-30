# SummerMVC

仿Spring MVC 实现了简易路由功能。仅供学习参考。

更完善的MVC框架请参考 https://github.com/monettoCoffee/EELinker

目前实现了@Controller,@RequestMapping,@ResponseBody。
具有DispacherServlet与简单的视图转发功能。

1.将DispatcherServlet与配置文件注册进web.xml中。

2.定义@Controller等注解。

3.在DsipatcherServlet生命周期初始化中加入读取配置文件,扫包的功能。

4.如果scanPackage扫到了包，则递归扫包。

5.将扫到的包加入到IOC容器中。并反射初始化实例。

6.如果Class附有Controller.class注解,则加入HandlerMapping与Controllers。

7.maven运行 tomcat7:run

8.浏览器输入localhost:8080/index.do测试视图解析方法。从webapp/WEB_INF/view返回v.html。

9.浏览器输入localhost:8080/index2.do?param=test测试ResponseBody注解。返回get参数param。
