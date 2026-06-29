package com.ddd.webbb.auth.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetEmailSender {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    public PasswordResetEmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("[오구오구] 비밀번호 재설정 인증 코드");
        message.setText(
                """
                안녕하세요.

                비밀번호 재설정 인증 코드: %s

                이 코드는 5분간 유효합니다.
                본인이 요청하지 않은 경우 이 메일을 무시해 주세요.
                """
                        .formatted(code));
        mailSender.send(message);
    }
}
