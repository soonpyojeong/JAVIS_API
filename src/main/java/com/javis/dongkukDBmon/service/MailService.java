package com.javis.dongkukDBmon.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;


@Service
public class MailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail; // application.properties에서 메일 계정 읽음

    public void sendResetPasswordMail(String toEmail, String resetLink) throws MessagingException {
        // 디버깅: 파라미터 값 출력
        System.out.println("[MailService] sendResetPasswordMail 호출됨");
        System.out.println("[MailService] 수신자: " + toEmail);
        System.out.println("[MailService] resetLink: " + resetLink);
        System.out.println("[MailService] 발신자(from): " + fromEmail);

        String subject = "[자비스] 비밀번호 재설정 안내";
        String body = String.format("""
            <html><body>
            <p>비밀번호 재설정 요청이 접수되었습니다.</p>
            <p>아래 링크를 클릭하여 새 비밀번호를 설정해주세요.</p>
            <p><a href="%s">비밀번호 재설정 바로가기</a></p>
            <p>본인이 요청하지 않은 경우 이 메일을 무시하셔도 됩니다.</p>
            </body></html>
        """, resetLink);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, true);
            helper.setFrom(fromEmail);  // properties에서 읽어오기

            mailSender.send(message);
            System.out.println("[MailService] 메일 발송 성공!");
        } catch (Exception e) {
            System.err.println("[MailService] 메일 발송 실패: " + e.getMessage());
            e.printStackTrace();
            throw e; // 다시 throw해서 컨트롤러에서 캐치
        }
    }
}
