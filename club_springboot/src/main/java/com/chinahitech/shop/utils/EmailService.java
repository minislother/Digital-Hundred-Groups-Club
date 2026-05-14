package com.chinahitech.shop.utils;

import com.chinahitech.shop.exception.EmailException;
import com.chinahitech.shop.exception.RedisAddException;
import org.springframework.scheduling.annotation.Async;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.Random;

@Async
public class EmailService {

    private static final String REQUIRED_EMAIL_DOMAIN = "mail2.sysu.edu.cn";

    public String emailHost = getConfig("MAIL_SMTP_HOST", "smtp.qq.com");
    public String transportType = getConfig("MAIL_SMTP_PROTOCOL", "smtp");
    public String fromUser = "Michael";
    public String fromEmail = getConfig("MAIL_FROM_EMAIL", null);
    public String authCode = getConfig("MAIL_AUTH_CODE", null);
    public String subject = "\u9a8c\u8bc1\u7801\u53d1\u9001\u4fe1\u606f";

    private final String toEmail;
    private String valicode;

    public EmailService(String email) throws EmailException {
        this.toEmail = email;
        if (!checkEmail()) {
            throw new EmailException(toEmail + "\u4e0d\u5c5e\u4e8e\u6821\u56ed\u90ae\u7bb1\uff0c\u8bf7\u4f7f\u7528\u4e2d\u5927\u90ae\u7bb1\u6ce8\u518c");
        }
    }

    private boolean checkEmail() {
        if (toEmail == null) {
            return false;
        }
        int atPos = toEmail.indexOf('@');
        if (atPos < 0 || atPos == toEmail.length() - 1) {
            return false;
        }
        return REQUIRED_EMAIL_DOMAIN.equalsIgnoreCase(toEmail.substring(atPos + 1));
    }

    private String getValicode() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }

    public void sendEmail() throws Exception {
        valicode = getValicode();

        Properties prop = new Properties();
        prop.setProperty("mail.debug", "false");
        prop.setProperty("mail.host", emailHost);
        prop.setProperty("mail.smtp.auth", "true");
        prop.setProperty("mail.transport.protocol", transportType);

        Session session = Session.getInstance(prop);
        Transport transport = session.getTransport();
        try {
            transport.connect(emailHost, fromEmail, authCode);
            Message message = createSimpleMail(session);
            transport.sendMessage(message, message.getAllRecipients());
        } finally {
            transport.close();
        }

        boolean check = RedisUtils.set(toEmail, valicode, 300);
        if (!check) {
            throw new RedisAddException("Redis added error!");
        }
    }

    public MimeMessage createSimpleMail(Session session) throws Exception {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromEmail));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
        message.setSubject("\u9a8c\u8bc1\u7801\u53d1\u9001\u90ae\u4ef6");
        message.setContent("\u60a8\u7684\u9a8c\u8bc1\u7801\u662f\uff1a" + valicode, "text/html;charset=UTF-8");
        return message;
    }

    private static String getConfig(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(key);
        }
        if ((value == null || value.trim().isEmpty()) && defaultValue == null) {
            throw new IllegalStateException(key + " is not configured");
        }
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }
}
