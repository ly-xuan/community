package com.liu.community.dao;

import com.liu.community.entity.LoginTicket;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Deprecated
public interface LoginTicketMapper {

    @Insert({
            "insert into login_ticket (user_id , ticket , status , expired) ",
            "value(#{userId} , #{ticket} , #{status} , #{expired})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertLoginTicket(LoginTicket loginTicket);

    @Select({
            "<script>",
            "select id , user_id , ticket , status, expired from login_ticket ",
            "where ticket = #{ticket} ",
            "<if test='ticket!=null'>",
                "and id > 0",
            "</if>",
            "</script>"
    })
    LoginTicket selectByTicket(String ticket);

    @Update({
            "update login_ticket set status = #{status} ",
            "where ticket = #{ticket}",
    })
    int updateStatus(String ticket, int status);
}
