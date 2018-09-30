package com.monetto.router;

import com.monetto.utils.Utils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class DispatcherServlet extends HttpServlet {
    private Properties properties = new Properties();
    private List<String> classNames = new ArrayList<String>();
    private Map<String, Object> beanFactory  = new HashMap<String,Object>();
    private Map<String, Method> handlerMapping = new  HashMap<String,Method>();
    private Map<String, Object> controllers  =new HashMap<String,Object>();
    private String prefix;
    private String suffix;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response){
        doPost(request,response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response){
        try {
            doDispatcher(request,response);
        } catch (Exception e) {
            System.out.println(500);
        }
    }

    private void doDispatcher(HttpServletRequest request, HttpServletResponse response){
        String url =request.getRequestURI();
        String contextPath = request.getContextPath();
        if(this.handlerMapping.containsKey(url+contextPath)){
            //匹配handlerMapping。
            url=url.replace(contextPath, "").replaceAll("/+", "/");
            Method method = this.handlerMapping.get(url);
            //找到该url对应的方法。
            Class<?>[] parameterTypes = method.getParameterTypes();
            Map<String, String[]> parameterMap = request.getParameterMap();
            //请求参数集合。
            Object [] paramValues= new Object[parameterTypes.length];
            for (int index = 0; index<parameterTypes.length; ++index){
                String requestParam = parameterTypes[index].getSimpleName();
                if (requestParam.equals("HttpServletRequest")){
                    paramValues[index]=request;
                    continue;
                }
                if (requestParam.equals("HttpServletResponse")){
                    paramValues[index]=response;
                    continue;
                }
                if(requestParam.equals("String")){
                    for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
                        String value =Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
                        paramValues[index]=value;
                    }
                }
            }
            try {
                String pageName = (String)method.invoke(this.controllers.get(url), paramValues);
                //由反射原理将请求参数映射到方法里执行。
                if(method.isAnnotationPresent(ResponseBody.class)){
                    response.getWriter().write(pageName);
                }
                else {
                    resourceViewResolver(pageName,request,response);
                }
                return ;
            } catch (Exception e) {e.printStackTrace();}
        }else{
            System.out.println(404);
        }
    }

    @Override
    public void init(ServletConfig servletConfig){
        try {
            loadConfig(servletConfig.getInitParameter("applicationConfiguration"));
            scanPackage(this.properties.getProperty("scanPackage"));
            initialInstance();
            initialHandlerMapping();
            this.prefix = this.properties.getProperty("prefix");
            this.suffix = this.properties.getProperty("suffix");
        } catch (Exception e) {
            e.printStackTrace();
        }
        //web.xml下进行配置,对应的配置文件为application.properties

    }
    private void loadConfig(String configLocation) throws IOException {
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(configLocation);
        this.properties.load(resourceAsStream);
        resourceAsStream.close();
    }
    private void scanPackage(String packageName){
        URL url = this.getClass().getClassLoader().getResource(packageName.replaceAll("\\.", "/"));
        //将包路径由a.b.c变为文件URL绝对路径file:/...a/b/c的形式。
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            if(file.isDirectory()){
                scanPackage(packageName+"."+file.getName());
                //file是目录的情况下,递归扫包,扫描子包下其他类。
            }else{
                String className =packageName +"." +file.getName().replace(".class", "");
                //扫描出来的形式为 包.类.class的形式。
                this.classNames.add(className);
                //把包下的所有的类都添加到classNames里面。
            }
        }
    }
    private void initialInstance() throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        for(String className : this.classNames){
            Class<?> clazz = Class.forName(className);
            if (clazz.isAnnotationPresent(Controller.class)){
                this.beanFactory.put(Utils.lowerFirstWord(clazz.getSimpleName()),clazz.newInstance());
                //获取含有Controller注解的class的名字,将第一个字母变成小写的"单例名",作为key放入HashMap。
                //而value则是通过反射生成的实例。
            }
        }
    }
    private void initialHandlerMapping() throws IllegalAccessException, InstantiationException {
        for(Map.Entry<String,Object> entry : this.beanFactory.entrySet()){
            Class<? extends Object> clazz = entry.getValue().getClass();
            String baseUrl = "";
            if(clazz.isAnnotationPresent(Controller.class)){
                if(clazz.isAnnotationPresent(RequestMapping.class)){
                    //如果类含有RequestMapping注解,则其值作为父URL提取。
                    RequestMapping annotation = clazz.getAnnotation(RequestMapping.class);
                    baseUrl=annotation.value();
                }
            }
            Method[] methods = clazz.getMethods();
            //其次扫描这个类中的方法,如果方法标有RequestMapping,则结合父url作为mapping。
            for (Method method : methods) {
                if(method.isAnnotationPresent(RequestMapping.class)){
                    RequestMapping annotation = method.getAnnotation(RequestMapping.class);
                    String url = annotation.value();
                    url =(baseUrl+"/"+url).replaceAll("/+", "/");
                    this.handlerMapping.put(url,method);
                    //mappingURL和其对应的方法。
                    this.controllers.put(url,clazz.newInstance());
                    //记录哪个Controller能导向那个URL。
                }
            }
        }
    }

    public void resourceViewResolver(String pageName,HttpServletRequest request,HttpServletResponse response){
        try {
            request.getServletContext().getRequestDispatcher(prefix+pageName+suffix).forward(request,response);
        } catch (ServletException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
