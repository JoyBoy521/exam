package com.exam.system.controller;

import com.exam.system.dto.ChangePasswordRequest;
import com.exam.system.service.AuthService;
import com.exam.system.util.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/profile")
public class StudentProfileController {

    private final AuthService authService;

    public StudentProfileController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                 HttpServletRequest httpRequest) {
        Long studentId = CurrentUser.userId(httpRequest);
        authService.changeStudentPassword(studentId, request.oldPassword(), request.newPassword());
        return "密码修改成功";
    }
}
