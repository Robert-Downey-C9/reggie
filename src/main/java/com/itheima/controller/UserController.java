package com.itheima.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.common.R;
import com.itheima.entity.User;
import com.itheima.service.UserService;
import com.itheima.utils.SMSUtils;
import com.itheima.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 给客户端发送短信验证码
     * @param user
     * @param session
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session){
        // 获取手机号
        String phone = user.getPhone();
        if (StringUtils.isNotEmpty(phone)){
            // 生成随机4位验证码
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("code={}", code);
            // 调用阿里云提供的短信服务API完成发送
            //SMSUtils.sendMessage("瑞吉外卖", "", phone, code);

            // 需要将生成的验证码保存到Session中
            session.setAttribute(phone, code);
            return R.success("手机验证码短信发送成功");
        }

        return R.error("短信发送失败");

    }

    /**
     * 客户端用户登录
     * @param map
     * @param session
     * @return
     */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpSession session){
        log.info("前端用户的信息为{}", map.toString());
        // 获取手机号
        String phone = map.get("phone").toString();

        // 获取验证码
        String code = map.get("code").toString();

        // 从Session中获取保存下来的验证码
        Object codeInSession = session.getAttribute(phone);

        // 进行验证码的对比（页面提交的验证码和Session中保存的验证码比对）
        if (codeInSession != null && codeInSession.equals(code)){
            // 如果比对成功就显示登录成功
            LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
            userLambdaQueryWrapper.eq(User::getPhone, phone);

            User user = userService.getOne(userLambdaQueryWrapper);
            if (user == null){
                // 与数据库的手机号进行比较，如果未注册就直接注册加入数据库

                user = new User();
                user.setPhone(phone);
                user.setStatus(1);
                userService.save(user);
            }
            session.setAttribute("user", user.getId());
            return R.success(user);
        }



        return R.error("登录失败");
    }
}
