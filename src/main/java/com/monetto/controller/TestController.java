package com.monetto.controller;

import com.monetto.router.Controller;
import com.monetto.router.RequestMapping;
import com.monetto.router.ResponseBody;

/**
 * @author monetto
 */
@Controller
public class TestController {

    @RequestMapping("/index.do")
    public String index() {
        // Test get method return view page
        System.out.println("This is Common HandlerMapping");
        return "v";
    }

    @ResponseBody
    @RequestMapping("/index2.do")
    public String index2(String param) {
        // Test get method return param
        System.out.println("This is ResponseBody HandlerMapping");
        return "ResponseBody:" + param;
    }

}
