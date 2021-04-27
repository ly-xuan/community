package com.liu.community.config;

import com.liu.community.util.CommunityUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/css/**","/js/**","/img/**");
    }

//    @Override
//    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
//        auth.userDetailsService(InMemoryUserDetailsManager).passwordEncoder(passwordEncoder());
//    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //授权
        http.authorizeRequests()
                .antMatchers(
                        "/user/setting",
                        "/user/upload",
                        "/comment/add",
                        "/letter/**",
                        "/notice/**",
                        "/like",
                        "/follow",
                        "/follow"
                )
                .hasAnyAuthority(
                        "user",
                        "admin",
                        "moderator"
                )

                .antMatchers(
                        "/discuss/top",
                        "/discuss/wonderful"
                ).hasAnyAuthority("moderator")

                .antMatchers(
                        "/discuss/delete"
                ).hasAnyAuthority("admin")

                .anyRequest().permitAll()
                .and().csrf().disable();

        //权限不够的处理
        http.exceptionHandling()
                //没有登录时的处理
                .authenticationEntryPoint((httpServletRequest, httpServletResponse, e) -> {
                    String xRequestedWith = httpServletRequest.getHeader("x-requested-with");
                    if ("XMLHttpRequest".equals(xRequestedWith)){
                        httpServletResponse.setContentType("application/plain;charset=utf-8");
                        httpServletResponse.getWriter().write(CommunityUtil.getJson(403,"你还没有登录哦！请先登录"));
                    } else {
                        httpServletResponse.sendRedirect(httpServletRequest.getContextPath() + "/login");
                    }
                })
                //没有权限的处理
                .accessDeniedHandler((httpServletRequest, httpServletResponse, e) -> {
                    String xRequestedWith = httpServletRequest.getHeader("x-requested-with");
                    if ("XMLHttpRequest".equals(xRequestedWith)){
                        httpServletResponse.setContentType("application/plain;charset=utf-8");
                        httpServletResponse.getWriter().write(CommunityUtil.getJson(403,"你没有访问此功能的权限！"));
                    } else {
                        httpServletResponse.sendRedirect(httpServletRequest.getContextPath() + "/denied");
                    }
                });
        http.logout()
                .logoutUrl("/securitylogout");

    }
}
