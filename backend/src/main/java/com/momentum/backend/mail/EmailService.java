package com.momentum.backend.mail;

import com.momentum.backend.config.Config;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

// Sends emails, through the mail account in config.properties (for example a
// Gmail account with an app password). Used for two-step login codes and
// password reset codes.
//
// If no mail account is set up, isConfigured() is false and nothing is sent.
public class EmailService {

    private final String from;
    private final Session session;

    public EmailService(Config config) {
        String host = config.getOptional("mail.host");
        String username = config.getOptional("mail.username");
        String password = config.getOptional("mail.password");
        this.from = config.getOptional("mail.from") != null ? config.getOptional("mail.from") : username;

        if (host == null || username == null || password == null || password.startsWith("CHANGE_ME")) {
            this.session = null;
            return;
        }

        // STARTTLS: the connection to the mail server is encrypted.
        // The timeouts stop a slow mail server from hanging a login.
        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", config.getOptional("mail.port") != null ? config.getOptional("mail.port") : "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.starttls.required", "true");
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");

        this.session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    public boolean isConfigured() {
        return session != null;
    }

    // Sends a plain text email. Gives back false if it couldn't be sent.
    public boolean send(String to, String subject, String body) {
        if (session == null) {
            return false;
        }
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from, "LearnTech Momentum"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
            return true;
        } catch (Exception e) {
            System.out.println("Could not send email: " + e.getMessage());
            return false;
        }
    }
}
