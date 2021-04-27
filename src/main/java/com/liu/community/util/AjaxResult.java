package com.liu.community.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AjaxResult {

    private int code; //200正常，非200表示异常
    private String msg;
    private Object data;

    public static AjaxResult succ(Object data){
        return succ(200,"操作成功",data);
    }
    public static AjaxResult succ(int code, String msg, Object data){
        AjaxResult r=new AjaxResult();
        r.setCode(code);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }

    public static AjaxResult fail(String msg){
        return fail(400,msg,null);
    }
    public static AjaxResult fail(String msg, Object data){
        return fail(400,msg,data);
    }
    public static AjaxResult fail(int code, String msg, Object data){
        AjaxResult r=new AjaxResult();
        r.setCode(code);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }
}
