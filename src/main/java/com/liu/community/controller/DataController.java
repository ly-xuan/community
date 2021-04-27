package com.liu.community.controller;

import com.liu.community.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
public class DataController {

    @Autowired
    private DataService dataService;

    //统计页面
    @RequestMapping(path = "/data", method = {RequestMethod.GET,RequestMethod.POST})
    public String getDataPage(){
        return "site/admin/data";
    }
    //统计网站uv
    @PostMapping("/data/uv")
    public String getUv(
            @DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
            @DateTimeFormat(pattern = "yyyy-MM-dd") Date end, Model model){
        long uv = dataService.calculateUv(start, end);
        model.addAttribute("uv",uv);
        model.addAttribute("uvStart",start);
        model.addAttribute("uvEnd",end);
//        return "site/admin/data";
//        转发表示当前请求未处理完，转发给其他方法处理
//        这里是为了复用post和get请求的getDataPage
        return "forward:/data";
    }

    //统计网站dau
    @PostMapping("/data/dau")
    public String getDau(
            @DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
            @DateTimeFormat(pattern = "yyyy-MM-dd") Date end, Model model){
        long dau = dataService.calculateDau(start, end);
        model.addAttribute("dau",dau);
        model.addAttribute("dauStart",start);
        model.addAttribute("dauEnd",end);

        return "forward:/data";
    }


}
