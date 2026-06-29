package com.example.byodsystem.byod.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmailService {

    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());

    private static final String SMTP_HOST     = "smtp.gmail.com";
    private static final String SMTP_PORT     = "587";
    private static final String FROM_EMAIL    = "devgress.pupsrc@gmail.com";
    private static String       FROM_PASSWORD = "";
    private static final String ADMIN_NAME    = "Doc. LAGUERTA, MARION Claveria";
    private static final String SUBJECT_PREFIX = "PUP STA ROSA DEVGRESS";

    private static final Color PDF_MAROON = new Color(0x7A, 0x00, 0x00);
    private static final Color PDF_GOLD   = new Color(0xFF, 0xD7, 0x00);
    private static final Color PDF_DARK   = new Color(0x11, 0x18, 0x27);
    private static final Color PDF_GRAY   = new Color(0x6B, 0x72, 0x80);
    private static final Color PDF_WHITE  = Color.WHITE;
    private static final Color PDF_LIGHT_BG = new Color(0xFF, 0xF8, 0xF8);
    private static final Color PDF_BORDER  = new Color(0xE8, 0xC8, 0xC8);

    static {
        try (InputStream input = EmailService.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            Properties props = new Properties();
            if (input != null) {
                props.load(input);
                FROM_PASSWORD = props.getProperty("mail.password");
            } else {
                LOGGER.severe("config.properties not found for EmailService initialization!");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load mail configurations from config.properties", e);
        }
    }

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("MMMM d, yyyy hh:mm a");
    private static final DateTimeFormatter DATE_FMT    = DateTimeFormatter.ofPattern("MMMM d, yyyy");
    private static final DateTimeFormatter FILE_FMT    = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");

    public record DeviceEntry(String brand, String model, String serial, String deviceType) {}

    public static void sendCombinedGatePassEmail(
            String toEmail, String studentName, String studentCode,
            String course, String yearLevel,
            LocalDateTime ingressTime, LocalDateTime egressTime,
            List<DeviceEntry> devices) {

        if (toEmail == null || toEmail.isBlank()) return;

        Session session = buildMailSession();
        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(FROM_EMAIL, "PUP Sta. Rosa DEVGRESS"));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));

            String dateLabel = ingressTime != null ? ingressTime.format(DATE_FMT) : "";
            msg.setSubject(SUBJECT_PREFIX + " — Gate Request (Ingress & Egress) | " + dateLabel);

            byte[] pdfBytes = buildCombinedGatePassPdf(studentName, studentCode, course, yearLevel,
                    ingressTime, egressTime, devices);

            MimeMultipart multipart = new MimeMultipart();

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(
                    buildCombinedEmailHtml(studentName, studentCode, course, yearLevel,
                            ingressTime, egressTime, devices),
                    "text/html; charset=utf-8");
            multipart.addBodyPart(htmlPart);

            if (pdfBytes != null) {
                MimeBodyPart pdfPart = new MimeBodyPart();
                pdfPart.setContent(pdfBytes, "application/pdf");
                String filename = "GatePass_" + studentCode + "_"
                        + (ingressTime != null ? ingressTime.format(FILE_FMT) : "NA") + ".pdf";
                pdfPart.setFileName(filename);
                pdfPart.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                multipart.addBodyPart(pdfPart);
            }

            msg.setContent(multipart);
            Transport.send(msg);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send combined gate pass email to " + toEmail, e);
        }
    }

    public static void sendGatePassEmail(
            String toEmail, String studentName, String studentCode,
            String course, String yearLevel, String direction,
            LocalDateTime scheduledTime, LocalDateTime officialTime,
            List<DeviceEntry> devices) {

        if (toEmail == null || toEmail.isBlank()) return;

        Session session = buildMailSession();
        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(FROM_EMAIL, "PUP Sta. Rosa DEVGRESS"));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));

            String dirLabel  = "IN".equals(direction) ? "INGRESS" : "EGRESS";
            LocalDateTime ref = officialTime != null ? officialTime : scheduledTime;
            String dateLabel = ref != null ? ref.format(DATE_FMT) : "";
            msg.setSubject(SUBJECT_PREFIX + " — " + dirLabel + " Gate Pass Approved | " + dateLabel);

            byte[] pdfBytes = buildApprovalGatePassPdf(studentName, studentCode, course, yearLevel,
                    direction, scheduledTime, officialTime, devices);

            MimeMultipart multipart = new MimeMultipart();

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(
                    buildApprovalEmailHtml(studentName, studentCode, course, yearLevel,
                            direction, scheduledTime, officialTime, devices),
                    "text/html; charset=utf-8");
            multipart.addBodyPart(htmlPart);

            if (pdfBytes != null) {
                MimeBodyPart pdfPart = new MimeBodyPart();
                pdfPart.setContent(pdfBytes, "application/pdf");
                String filename = "GatePass_" + dirLabel + "_" + studentCode + "_"
                        + (ref != null ? ref.format(FILE_FMT) : "NA") + ".pdf";
                pdfPart.setFileName(filename);
                pdfPart.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                multipart.addBodyPart(pdfPart);
            }

            msg.setContent(multipart);
            Transport.send(msg);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send gate pass email to " + toEmail, e);
        }
    }

    /**
     * Notifies a student that their PENDING gate request was voided by an
     * administrator before it could be acted on at the gate. No PDF pass
     * slip is attached — voiding cancels the request outright, so there is
     * no approved pass to issue. The email simply states the student and
     * device details plus the reason, so the student knows what happened
     * and why.
     */
    public static void sendVoidNoticeEmail(
            String toEmail, String studentName, String studentCode,
            String course, String direction, LocalDateTime scheduledTime,
            String reason, List<DeviceEntry> devices) {

        if (toEmail == null || toEmail.isBlank()) return;

        Session session = buildMailSession();
        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(FROM_EMAIL, "PUP Sta. Rosa DEVGRESS"));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));

            String dirLabel  = "IN".equals(direction) ? "INGRESS" : "EGRESS";
            String dateLabel = scheduledTime != null ? scheduledTime.format(DATE_FMT) : "";
            msg.setSubject(SUBJECT_PREFIX + " — " + dirLabel + " Gate Request Voided | " + dateLabel);

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(
                    buildVoidNoticeEmailHtml(studentName, studentCode, course,
                            direction, scheduledTime, reason, devices),
                    "text/html; charset=utf-8");

            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(htmlPart);

            msg.setContent(multipart);
            Transport.send(msg);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send void notice email to " + toEmail, e);
        }
    }

    /**
     * Notifies a student that the schedule of their gate request was amended
     * by an officer. The request remains PENDING — amending only changes the
     * scheduled date/time, it does NOT approve the request. This is a plain
     * HTML notice with no PDF attachment (there is nothing official to issue
     * yet, since the device has not been confirmed at the gate).
     */
    public static void sendAmendmentNoticeEmail(
            String toEmail, String studentName, String studentCode,
            String course, String direction,
            LocalDateTime previousScheduledTime, LocalDateTime newScheduledTime,
            String reason, List<DeviceEntry> devices) {

        if (toEmail == null || toEmail.isBlank()) return;

        Session session = buildMailSession();
        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(FROM_EMAIL, "PUP Sta. Rosa DEVGRESS"));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));

            String dirLabel  = "IN".equals(direction) ? "INGRESS" : "EGRESS";
            String dateLabel = newScheduledTime != null ? newScheduledTime.format(DATE_FMT) : "";
            msg.setSubject(SUBJECT_PREFIX + " — " + dirLabel + " Gate Request Amended | " + dateLabel);

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(
                    buildAmendmentNoticeEmailHtml(studentName, studentCode, course,
                            direction, previousScheduledTime, newScheduledTime, reason, devices),
                    "text/html; charset=utf-8");

            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(htmlPart);

            msg.setContent(multipart);
            Transport.send(msg);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send amendment notice email to " + toEmail, e);
        }
    }

    private static String buildAmendmentNoticeEmailHtml(
            String studentName, String studentCode, String course,
            String direction, LocalDateTime previousScheduledTime, LocalDateTime newScheduledTime,
            String reason, List<DeviceEntry> devices) {

        String dirLabel          = "IN".equals(direction) ? "INGRESS" : "EGRESS";
        String previousDisplay   = previousScheduledTime != null ? previousScheduledTime.format(DISPLAY_FMT) : "—";
        String newDisplay        = newScheduledTime      != null ? newScheduledTime.format(DISPLAY_FMT)      : "—";
        String issuedDate        = LocalDateTime.now().format(DATE_FMT);

        StringBuilder deviceRows = new StringBuilder();
        for (int i = 0; i < devices.size(); i++) {
            DeviceEntry d = devices.get(i);
            String bg = i % 2 == 0 ? "#FFF8F8" : "#FFFFFF";
            deviceRows.append("<tr style='background:").append(bg).append(";'>")
                    .append("<td style='padding:9px 14px;border-bottom:1px solid #F0E0E0;font-size:13px;color:#374151;font-family:Arial,sans-serif;'>")
                    .append(esc(d.brand())).append(" ").append(esc(d.model())).append("</td>")
                    .append("<td style='padding:9px 14px;border-bottom:1px solid #F0E0E0;font-size:13px;color:#6B7280;font-family:Arial,sans-serif;'>")
                    .append(esc(d.serial())).append("</td>")
                    .append("<td style='padding:9px 14px;border-bottom:1px solid #F0E0E0;font-size:13px;color:#374151;font-family:Arial,sans-serif;'>")
                    .append(esc(d.deviceType())).append("</td>")
                    .append("</tr>");
        }

        String body = """
            <p style="font-family:Arial,sans-serif;font-size:13.5px;color:#374151;line-height:1.7;margin:0 0 20px 0;">
              This is to inform you that the schedule of your <strong>%s</strong> gate request has
              been <strong>amended</strong> by the Gate Officer. This request is <strong>still
              pending</strong> — it has not yet been approved and will only be confirmed once the
              device(s) are physically presented and verified at the gate.
            </p>
            <div style="font-size:10px;letter-spacing:1.5px;text-transform:uppercase;color:#9CA3AF;
                        margin-bottom:10px;font-weight:700;font-family:Arial,sans-serif;">Student Information</div>
            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="background:#FFF8F8;border-radius:6px;border:1px solid #E8C8C8;margin-bottom:20px;">
              <tr>
                <td style="padding:12px 18px;border-bottom:1px solid #F0E0E0;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Student Name</div>
                  <div style="font-size:15px;font-weight:700;color:#111827;font-family:Arial,sans-serif;">%s</div>
                </td>
                <td style="padding:12px 18px;border-bottom:1px solid #F0E0E0;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Student Code</div>
                  <div style="font-size:15px;font-weight:700;color:#111827;font-family:Arial,sans-serif;">%s</div>
                </td>
              </tr>
              <tr>
                <td colspan="2" style="padding:12px 18px;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Section</div>
                  <div style="font-size:13px;font-weight:600;color:#374151;font-family:Arial,sans-serif;">%s</div>
                </td>
              </tr>
            </table>
            <div style="font-size:10px;letter-spacing:1.5px;text-transform:uppercase;color:#9CA3AF;
                        margin-bottom:10px;font-weight:700;font-family:Arial,sans-serif;">Amended Request Details</div>
            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="background:#FFF8F8;border-radius:6px;border:1px solid #E8C8C8;margin-bottom:20px;">
              <tr>
                <td style="padding:12px 18px;border-bottom:1px solid #F0E0E0;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Direction</div>
                  <div style="font-size:14px;font-weight:700;color:#92400E;font-family:Arial,sans-serif;">%s</div>
                </td>
                <td style="padding:12px 18px;border-bottom:1px solid #F0E0E0;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Status</div>
                  <div style="display:inline-block;background:#FEF9C3;color:#92400E;padding:3px 10px;
                              border-radius:4px;font-size:12px;font-weight:700;font-family:Arial,sans-serif;">PENDING</div>
                </td>
              </tr>
              <tr>
                <td style="padding:12px 18px;border-bottom:1px solid #F0E0E0;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Previous Schedule</div>
                  <div style="font-size:13px;color:#9CA3AF;font-family:Arial,sans-serif;text-decoration:line-through;">%s</div>
                </td>
                <td style="padding:12px 18px;border-bottom:1px solid #F0E0E0;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">New Schedule</div>
                  <div style="font-size:13px;font-weight:700;color:#7A0000;font-family:Arial,sans-serif;">%s</div>
                </td>
              </tr>
              <tr>
                <td colspan="2" style="padding:12px 18px;border-top:1px solid #F0E0E0;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Reason for Amendment</div>
                  <div style="font-size:13px;color:#374151;font-family:Arial,sans-serif;">%s</div>
                </td>
              </tr>
            </table>
            <div style="font-size:10px;letter-spacing:1.5px;text-transform:uppercase;color:#9CA3AF;
                        margin-bottom:10px;font-weight:700;font-family:Arial,sans-serif;">Device(s) Affected</div>
            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border-radius:6px;border:1px solid #E8C8C8;overflow:hidden;margin-bottom:20px;">
              <thead>
                <tr style="background:#7A0000;">
                  <th style="padding:10px 14px;text-align:left;font-size:11px;color:#FFFFFF;font-weight:700;font-family:Arial,sans-serif;">DEVICE</th>
                  <th style="padding:10px 14px;text-align:left;font-size:11px;color:#FFFFFF;font-weight:700;font-family:Arial,sans-serif;">SERIAL NO.</th>
                  <th style="padding:10px 14px;text-align:left;font-size:11px;color:#FFFFFF;font-weight:700;font-family:Arial,sans-serif;">TYPE</th>
                </tr>
              </thead>
              <tbody>%s</tbody>
            </table>
            <p style="font-family:Arial,sans-serif;font-size:12px;color:#6B7280;line-height:1.7;
                      border-left:3px solid #7A0000;padding-left:12px;margin:0 0 20px 0;">
              No pass slip has been issued yet. An official gate pass will be emailed to you
              once this request is approved at the gate. For questions, contact your department
              officer or the system administrator.
            </p>
            """.formatted(
                dirLabel,
                esc(studentName), esc(studentCode), esc(course),
                dirLabel, esc(previousDisplay), esc(newDisplay), esc(reason),
                deviceRows.toString());

        return pupEmailWrapper(dirLabel + " GATE REQUEST — AMENDED",
                "This request is still pending officer approval at the gate", body, issuedDate);
    }

    private static String buildVoidNoticeEmailHtml(
            String studentName, String studentCode, String course,
            String direction, LocalDateTime scheduledTime,
            String reason, List<DeviceEntry> devices) {

        String dirLabel         = "IN".equals(direction) ? "INGRESS" : "EGRESS";
        String scheduledDisplay = scheduledTime != null ? scheduledTime.format(DISPLAY_FMT) : "—";
        String issuedDate       = LocalDateTime.now().format(DATE_FMT);

        StringBuilder deviceRows = new StringBuilder();
        for (int i = 0; i < devices.size(); i++) {
            DeviceEntry d = devices.get(i);
            String bg = i % 2 == 0 ? "#FFF8F8" : "#FFFFFF";
            deviceRows.append("<tr style='background:").append(bg).append(";'>")
                    .append("<td style='padding:9px 14px;border-bottom:1px solid #F0E0E0;font-size:13px;color:#374151;font-family:Arial,sans-serif;'>")
                    .append(esc(d.brand())).append(" ").append(esc(d.model())).append("</td>")
                    .append("<td style='padding:9px 14px;border-bottom:1px solid #F0E0E0;font-size:13px;color:#6B7280;font-family:Arial,sans-serif;'>")
                    .append(esc(d.serial())).append("</td>")
                    .append("<td style='padding:9px 14px;border-bottom:1px solid #F0E0E0;font-size:13px;color:#374151;font-family:Arial,sans-serif;'>")
                    .append(esc(d.deviceType())).append("</td>")
                    .append("</tr>");
        }

        String body = """
            <p style="font-family:Arial,sans-serif;font-size:13.5px;color:#374151;line-height:1.7;margin:0 0 20px 0;">
              This is to inform you that your pending <strong>%s</strong> gate request, originally
              scheduled for the date and time below, has been <strong>voided</strong> and will
              <strong>not</strong> be processed at the gate.
            </p>
            <div style="font-size:10px;letter-spacing:1.5px;text-transform:uppercase;color:#9CA3AF;
                        margin-bottom:10px;font-weight:700;font-family:Arial,sans-serif;">Student Information</div>
            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="background:#FFF8F8;border-radius:6px;border:1px solid #E8C8C8;margin-bottom:20px;">
              <tr>
                <td style="padding:12px 18px;border-bottom:1px solid #F0E0E0;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Student Name</div>
                  <div style="font-size:15px;font-weight:700;color:#111827;font-family:Arial,sans-serif;">%s</div>
                </td>
                <td style="padding:12px 18px;border-bottom:1px solid #F0E0E0;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Student Code</div>
                  <div style="font-size:15px;font-weight:700;color:#111827;font-family:Arial,sans-serif;">%s</div>
                </td>
              </tr>
              <tr>
                <td colspan="2" style="padding:12px 18px;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Section</div>
                  <div style="font-size:13px;font-weight:600;color:#374151;font-family:Arial,sans-serif;">%s</div>
                </td>
              </tr>
            </table>
            <div style="font-size:10px;letter-spacing:1.5px;text-transform:uppercase;color:#9CA3AF;
                        margin-bottom:10px;font-weight:700;font-family:Arial,sans-serif;">Voided Request Details</div>
            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="background:#FFF8F8;border-radius:6px;border:1px solid #E8C8C8;margin-bottom:20px;">
              <tr>
                <td style="padding:12px 18px;border-bottom:1px solid #F0E0E0;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Direction</div>
                  <div style="font-size:14px;font-weight:700;color:#92400E;font-family:Arial,sans-serif;">%s</div>
                </td>
                <td style="padding:12px 18px;border-bottom:1px solid #F0E0E0;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Status</div>
                  <div style="display:inline-block;background:#DC2626;color:#FFFFFF;padding:3px 10px;
                              border-radius:4px;font-size:12px;font-weight:700;font-family:Arial,sans-serif;">VOIDED</div>
                </td>
              </tr>
              <tr>
                <td colspan="2" style="padding:12px 18px;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Originally Scheduled For</div>
                  <div style="font-size:13px;color:#374151;font-family:Arial,sans-serif;">%s</div>
                </td>
              </tr>
              <tr>
                <td colspan="2" style="padding:12px 18px;border-top:1px solid #F0E0E0;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Reason</div>
                  <div style="font-size:13px;color:#374151;font-family:Arial,sans-serif;">%s</div>
                </td>
              </tr>
            </table>
            <div style="font-size:10px;letter-spacing:1.5px;text-transform:uppercase;color:#9CA3AF;
                        margin-bottom:10px;font-weight:700;font-family:Arial,sans-serif;">Device(s) Affected</div>
            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border-radius:6px;border:1px solid #E8C8C8;overflow:hidden;margin-bottom:20px;">
              <thead>
                <tr style="background:#7A0000;">
                  <th style="padding:10px 14px;text-align:left;font-size:11px;color:#FFFFFF;font-weight:700;font-family:Arial,sans-serif;">DEVICE</th>
                  <th style="padding:10px 14px;text-align:left;font-size:11px;color:#FFFFFF;font-weight:700;font-family:Arial,sans-serif;">SERIAL NO.</th>
                  <th style="padding:10px 14px;text-align:left;font-size:11px;color:#FFFFFF;font-weight:700;font-family:Arial,sans-serif;">TYPE</th>
                </tr>
              </thead>
              <tbody>%s</tbody>
            </table>
            <p style="font-family:Arial,sans-serif;font-size:12px;color:#6B7280;line-height:1.7;
                      border-left:3px solid #7A0000;padding-left:12px;margin:0 0 20px 0;">
              If you still need to bring this device on or off campus, please submit a new gate
              request through the system. For questions, contact your department officer or the
              system administrator.
            </p>
            """.formatted(
                dirLabel,
                esc(studentName), esc(studentCode), esc(course),
                dirLabel, esc(scheduledDisplay), esc(reason),
                deviceRows.toString());

        return pupEmailWrapper(dirLabel + " GATE REQUEST — VOIDED",
                "This pending request will not be processed at the gate", body, issuedDate);
    }

    private static Session buildMailSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
            }
        });
    }

    private static byte[] buildCombinedGatePassPdf(
            String studentName, String studentCode, String course, String yearLevel,
            LocalDateTime ingressTime, LocalDateTime egressTime,
            List<DeviceEntry> devices) {
        try {
            PDDocument doc = new PDDocument();
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float pageWidth = page.getMediaBox().getWidth();
            float margin = 50;
            float y = page.getMediaBox().getHeight() - 40;

            PDType1Font bold   = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font italic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                pdfFillRect(cs, 0, y - 10, pageWidth, 80, PDF_MAROON);
                pdfText(cs, bold,   12, PDF_WHITE, margin, y + 30, "POLYTECHNIC UNIVERSITY OF THE PHILIPPINES");
                pdfText(cs, bold,   9,  PDF_GOLD,  margin, y + 14, "OFFICE OF THE VICE PRESIDENT FOR CAMPUSES — SANTA ROSA CAMPUS");
                pdfText(cs, normal, 8,  new Color(220, 220, 220), margin, y,    "City of Santa Rosa, Laguna");
                pdfFillRect(cs, margin, y - 12, pageWidth - margin * 2, 2, PDF_GOLD);
                pdfText(cs, bold, 10, PDF_GOLD, margin, y - 26, "GATE REQUEST NOTIFICATION — Device Ingress & Egress");
                y -= 100;

                pdfFillRect(cs, margin, y - 2, pageWidth - margin * 2, 2, PDF_MAROON);
                y -= 20;
                pdfText(cs, normal, 8, PDF_GRAY,  margin, y, "Date Issued:");
                pdfText(cs, normal, 10, PDF_DARK, margin + 80, y, LocalDateTime.now().format(DATE_FMT));
                y -= 24;

                pdfSectionTitle(cs, bold, normal, margin, pageWidth, y, "STUDENT INFORMATION");
                y -= 32;
                pdfTwoCol(cs, bold, normal, margin, pageWidth, y, "Student Name:", studentName, "Student Code:", studentCode);
                y -= 24;
                pdfText(cs, bold,   8,  PDF_GRAY, margin, y, "Program / Year Level:");
                pdfText(cs, normal, 10, PDF_DARK, margin, y - 14,
                        (course != null ? course : "") + (yearLevel != null && !yearLevel.isBlank() ? " " + yearLevel : ""));
                y -= 40;

                pdfSectionTitle(cs, bold, normal, margin, pageWidth, y, "SCHEDULE");
                y -= 32;
                pdfTwoCol(cs, bold, normal, margin, pageWidth, y,
                        "Ingress (Scheduled):", ingressTime != null ? ingressTime.format(DISPLAY_FMT) : "—",
                        "Egress (Scheduled):",  egressTime  != null ? egressTime.format(DISPLAY_FMT)  : "—");
                y -= 40;

                pdfSectionTitle(cs, bold, normal, margin, pageWidth, y, "DEVICE(S) INCLUDED");
                y -= 28;
                y = pdfDeviceTable(cs, bold, normal, margin, pageWidth, y, devices);
                y -= 16;

                pdfText(cs, italic, 8, PDF_GRAY, margin, y,
                        "Note: The device(s) are scheduled for both ingress and egress. The Gate Officer will confirm upon physical verification.");
                y -= 40;

                pdfSignatory(cs, bold, normal, italic, margin, pageWidth, y);
                y -= 80;

                pdfFillRect(cs, margin, y, pageWidth - margin * 2, 1, PDF_GRAY);
                y -= 14;
                pdfText(cs, normal, 7, PDF_GRAY, margin, y,
                        "LCA Boulevard, Brgy. Tagapo, City of Santa Rosa, Laguna  |  www.pup.edu.ph  |  starosa@pup.edu.ph");
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to generate combined gate pass PDF", e);
            return null;
        }
    }

    private static byte[] buildApprovalGatePassPdf(
            String studentName, String studentCode, String course, String yearLevel,
            String direction, LocalDateTime scheduledTime, LocalDateTime officialTime,
            List<DeviceEntry> devices) {
        try {
            PDDocument doc = new PDDocument();
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float pageWidth = page.getMediaBox().getWidth();
            float margin = 50;
            float y = page.getMediaBox().getHeight() - 40;

            PDType1Font bold   = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font italic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            String dirLabel = "IN".equals(direction) ? "INGRESS" : "EGRESS";

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                pdfFillRect(cs, 0, y - 10, pageWidth, 80, PDF_MAROON);
                pdfText(cs, bold,   12, PDF_WHITE, margin, y + 30, "POLYTECHNIC UNIVERSITY OF THE PHILIPPINES");
                pdfText(cs, bold,   9,  PDF_GOLD,  margin, y + 14, "OFFICE OF THE VICE PRESIDENT FOR CAMPUSES — SANTA ROSA CAMPUS");
                pdfText(cs, normal, 8,  new Color(220, 220, 220), margin, y, "City of Santa Rosa, Laguna");
                pdfFillRect(cs, margin, y - 12, pageWidth - margin * 2, 2, PDF_GOLD);
                pdfText(cs, bold, 10, PDF_GOLD, margin, y - 26, dirLabel + " GATE PASS — APPROVED");
                y -= 100;

                pdfFillRect(cs, margin, y - 2, pageWidth - margin * 2, 2, PDF_MAROON);
                y -= 20;
                pdfText(cs, normal, 8, PDF_GRAY,  margin, y, "Date Issued:");
                pdfText(cs, normal, 10, PDF_DARK, margin + 80, y, LocalDateTime.now().format(DATE_FMT));
                y -= 24;

                pdfSectionTitle(cs, bold, normal, margin, pageWidth, y, "STUDENT INFORMATION");
                y -= 32;
                pdfTwoCol(cs, bold, normal, margin, pageWidth, y, "Student Name:", studentName, "Student Code:", studentCode);
                y -= 24;
                pdfText(cs, bold,   8,  PDF_GRAY, margin, y, "Program / Year Level:");
                pdfText(cs, normal, 10, PDF_DARK, margin, y - 14,
                        (course != null ? course : "") + (yearLevel != null && !yearLevel.isBlank() ? " " + yearLevel : ""));
                y -= 40;

                pdfSectionTitle(cs, bold, normal, margin, pageWidth, y, "GATE LOG DETAILS");
                y -= 32;
                pdfTwoCol(cs, bold, normal, margin, pageWidth, y, "Direction:", dirLabel, "Status:", "APPROVED");
                y -= 32;
                pdfTwoCol(cs, bold, normal, margin, pageWidth, y,
                        "Scheduled Time:", scheduledTime != null ? scheduledTime.format(DISPLAY_FMT) : "—",
                        "Official Time:",  officialTime  != null ? officialTime.format(DISPLAY_FMT)  : "Pending");
                y -= 40;

                pdfSectionTitle(cs, bold, normal, margin, pageWidth, y, "DEVICE(S) RECORDED");
                y -= 28;
                y = pdfDeviceTable(cs, bold, normal, margin, pageWidth, y, devices);
                y -= 16;

                pdfText(cs, italic, 8, PDF_GRAY, margin, y,
                        "Note: The device(s) above have been officially recorded in the DEVGRESS system. For inquiries, contact your department officer.");
                y -= 40;

                pdfSignatory(cs, bold, normal, italic, margin, pageWidth, y);
                y -= 80;

                pdfFillRect(cs, margin, y, pageWidth - margin * 2, 1, PDF_GRAY);
                y -= 14;
                pdfText(cs, normal, 7, PDF_GRAY, margin, y,
                        "LCA Boulevard, Brgy. Tagapo, City of Santa Rosa, Laguna  |  www.pup.edu.ph  |  starosa@pup.edu.ph");
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to generate approval gate pass PDF", e);
            return null;
        }
    }

    private static void pdfFillRect(PDPageContentStream cs, float x, float y, float w, float h, Color color) throws Exception {
        cs.setNonStrokingColor(color);
        cs.addRect(x, y, w, h);
        cs.fill();
    }

    private static void pdfText(PDPageContentStream cs, PDType1Font font, float size, Color color,
                                float x, float y, String text) throws Exception {
        if (text == null) text = "";
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private static void pdfSectionTitle(PDPageContentStream cs, PDType1Font bold, PDType1Font normal,
                                        float margin, float pageWidth, float y, String title) throws Exception {
        pdfText(cs, bold, 8, PDF_MAROON, margin, y, title);
        pdfFillRect(cs, margin, y - 4, pageWidth - margin * 2, 1, PDF_MAROON);
    }

    private static void pdfTwoCol(PDPageContentStream cs, PDType1Font bold, PDType1Font normal,
                                  float margin, float pageWidth, float y,
                                  String lbl1, String val1, String lbl2, String val2) throws Exception {
        float colW = (pageWidth - margin * 2) / 2 - 6;
        float col2x = margin + colW + 12;

        pdfFillRect(cs, margin, y - 6, colW, 30, PDF_LIGHT_BG);
        pdfFillRect(cs, col2x,  y - 6, colW, 30, PDF_LIGHT_BG);

        pdfText(cs, bold,   8,  PDF_GRAY, margin + 6, y + 16, lbl1);
        pdfText(cs, normal, 10, PDF_DARK, margin + 6, y + 2,  val1 != null ? val1 : "—");
        pdfText(cs, bold,   8,  PDF_GRAY, col2x + 6,  y + 16, lbl2);
        pdfText(cs, normal, 10, PDF_DARK, col2x + 6,  y + 2,  val2 != null ? val2 : "—");
    }

    private static float pdfDeviceTable(PDPageContentStream cs, PDType1Font bold, PDType1Font normal,
                                        float margin, float pageWidth, float y,
                                        List<DeviceEntry> devices) throws Exception {
        float tableW = pageWidth - margin * 2;
        float rowH   = 22;

        pdfFillRect(cs, margin, y, tableW, rowH, PDF_MAROON);
        float col1 = margin + 6, col2 = margin + tableW * 0.45f + 6, col3 = margin + tableW * 0.75f + 6;
        pdfText(cs, bold, 9, PDF_WHITE, col1, y + 7, "DEVICE");
        pdfText(cs, bold, 9, PDF_WHITE, col2, y + 7, "SERIAL NO.");
        pdfText(cs, bold, 9, PDF_WHITE, col3, y + 7, "TYPE");
        y -= rowH;

        for (int i = 0; i < devices.size(); i++) {
            DeviceEntry d = devices.get(i);
            Color bg = i % 2 == 0 ? PDF_LIGHT_BG : PDF_WHITE;
            pdfFillRect(cs, margin, y, tableW, rowH, bg);
            pdfText(cs, normal, 9, PDF_DARK, col1, y + 7, d.brand() + " " + d.model());
            pdfText(cs, normal, 9, PDF_GRAY, col2, y + 7, d.serial() != null ? d.serial() : "—");
            pdfText(cs, normal, 9, PDF_DARK, col3, y + 7, d.deviceType() != null ? d.deviceType() : "—");
            y -= rowH;
        }
        return y;
    }

    private static void pdfSignatory(PDPageContentStream cs, PDType1Font bold, PDType1Font normal,
                                     PDType1Font italic, float margin, float pageWidth, float y) throws Exception {
        float halfW = (pageWidth - margin * 2) / 2;
        pdfText(cs, normal, 8, PDF_GRAY, margin,            y,      "Noted:");
        pdfText(cs, normal, 8, PDF_GRAY, margin + halfW,    y,      "Approved by:");
        pdfFillRect(cs, margin,         y - 28, 140, 1, PDF_DARK);
        pdfFillRect(cs, margin + halfW, y - 28, 140, 1, PDF_DARK);
        pdfText(cs, bold,   9, PDF_DARK, margin,            y - 42, "Dr. Leny V. Salmingo");
        pdfText(cs, normal, 8, PDF_GRAY, margin,            y - 54, "Director");
        pdfText(cs, bold,   9, PDF_DARK, margin + halfW,    y - 42, ADMIN_NAME);
        pdfText(cs, normal, 8, PDF_GRAY, margin + halfW,    y - 54, "System Administrator — DEVGRESS");
    }

    private static String buildCombinedEmailHtml(
            String studentName, String studentCode, String course, String yearLevel,
            LocalDateTime ingressTime, LocalDateTime egressTime,
            List<DeviceEntry> devices) {

        String ingressDisplay = ingressTime != null ? ingressTime.format(DISPLAY_FMT) : "—";
        String egressDisplay  = egressTime  != null ? egressTime.format(DISPLAY_FMT)  : "—";
        String issuedDate     = LocalDateTime.now().format(DATE_FMT);

        String placeholder = """
            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="background:#FDF6E3;border-radius:8px;border:1px solid #C9A84C;margin-bottom:24px;">
              <tr>
                <td style="padding:16px 20px;">
                  <div style="font-size:11px;font-weight:700;color:#92400E;font-family:Arial,sans-serif;
                              letter-spacing:1px;text-transform:uppercase;margin-bottom:6px;">
                    Official Gate Pass Attached
                  </div>
                  <div style="font-size:13px;color:#374151;font-family:Arial,sans-serif;line-height:1.6;">
                    Your official <strong>PUP DEVGRESS Gate Pass</strong> is attached to this email as a PDF file
                    (<code style="background:#FEF3C7;padding:1px 4px;border-radius:3px;">GatePass_%s_%s.pdf</code>).
                    Please <strong>download and keep</strong> a copy for your records.
                    Present it to the Gate Officer at the time of entry/exit.
                  </div>
                </td>
              </tr>
            </table>
            """.formatted(esc(studentCode), ingressTime != null ? ingressTime.format(FILE_FMT) : "NA");

        StringBuilder deviceRows = new StringBuilder();
        for (int i = 0; i < devices.size(); i++) {
            DeviceEntry d = devices.get(i);
            String bg = i % 2 == 0 ? "#FFF8F8" : "#FFFFFF";
            deviceRows.append("<tr style='background:").append(bg).append(";'>")
                    .append("<td style='padding:9px 14px;border-bottom:1px solid #F0E0E0;font-size:13px;color:#374151;font-family:Arial,sans-serif;'>")
                    .append(esc(d.brand())).append(" ").append(esc(d.model())).append("</td>")
                    .append("<td style='padding:9px 14px;border-bottom:1px solid #F0E0E0;font-size:13px;color:#6B7280;font-family:Arial,sans-serif;'>")
                    .append(esc(d.serial())).append("</td>")
                    .append("<td style='padding:9px 14px;border-bottom:1px solid #F0E0E0;font-size:13px;color:#374151;font-family:Arial,sans-serif;'>")
                    .append(esc(d.deviceType())).append("</td>")
                    .append("</tr>");
        }

        String body = placeholder + """
            <p style="font-family:Arial,sans-serif;font-size:13.5px;color:#374151;line-height:1.7;margin:0 0 20px 0;">
              This is to notify that a <strong>Device Gate Request</strong> has been officially submitted
              through the <strong>DEVGRESS</strong> system for the student identified below.
              The officer at the gate will confirm the actual ingress and egress times upon approval.
            </p>
            <div style="font-size:10px;letter-spacing:1.5px;text-transform:uppercase;color:#9CA3AF;
                        margin-bottom:10px;font-weight:700;font-family:Arial,sans-serif;">Student Information</div>
            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="background:#FFF8F8;border-radius:6px;border:1px solid #E8C8C8;margin-bottom:20px;">
              <tr>
                <td style="padding:12px 18px;border-bottom:1px solid #F0E0E0;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Student Name</div>
                  <div style="font-size:15px;font-weight:700;color:#111827;font-family:Arial,sans-serif;">%s</div>
                </td>
                <td style="padding:12px 18px;border-bottom:1px solid #F0E0E0;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Student Code</div>
                  <div style="font-size:15px;font-weight:700;color:#111827;font-family:Arial,sans-serif;">%s</div>
                </td>
              </tr>
              <tr>
                <td colspan="2" style="padding:12px 18px;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Program / Year Level</div>
                  <div style="font-size:13px;font-weight:600;color:#374151;font-family:Arial,sans-serif;">%s %s</div>
                </td>
              </tr>
            </table>
            <div style="font-size:10px;letter-spacing:1.5px;text-transform:uppercase;color:#9CA3AF;
                        margin-bottom:10px;font-weight:700;font-family:Arial,sans-serif;">Schedule</div>
            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="background:#FFF8F8;border-radius:6px;border:1px solid #E8C8C8;margin-bottom:20px;">
              <tr>
                <td style="padding:12px 18px;border-bottom:1px solid #F0E0E0;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Ingress (Scheduled)</div>
                  <div style="font-size:13px;font-weight:700;color:#15803D;font-family:Arial,sans-serif;">%s</div>
                </td>
              </tr>
              <tr>
                <td style="padding:12px 18px;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Egress (Scheduled)</div>
                  <div style="font-size:13px;font-weight:700;color:#92400E;font-family:Arial,sans-serif;">%s</div>
                </td>
              </tr>
            </table>
            <div style="font-size:10px;letter-spacing:1.5px;text-transform:uppercase;color:#9CA3AF;
                        margin-bottom:10px;font-weight:700;font-family:Arial,sans-serif;">Device(s) Included</div>
            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border-radius:6px;border:1px solid #E8C8C8;overflow:hidden;margin-bottom:20px;">
              <thead>
                <tr style="background:#7A0000;">
                  <th style="padding:10px 14px;text-align:left;font-size:11px;color:#FFFFFF;font-weight:700;letter-spacing:0.5px;font-family:Arial,sans-serif;">DEVICE</th>
                  <th style="padding:10px 14px;text-align:left;font-size:11px;color:#FFFFFF;font-weight:700;letter-spacing:0.5px;font-family:Arial,sans-serif;">SERIAL NO.</th>
                  <th style="padding:10px 14px;text-align:left;font-size:11px;color:#FFFFFF;font-weight:700;letter-spacing:0.5px;font-family:Arial,sans-serif;">TYPE</th>
                </tr>
              </thead>
              <tbody>%s</tbody>
            </table>
            <p style="font-family:Arial,sans-serif;font-size:12px;color:#6B7280;line-height:1.7;
                      border-left:3px solid #7A0000;padding-left:12px;margin:0 0 20px 0;">
              <strong>Note:</strong> The device(s) listed above are scheduled for both ingress and egress.
              The actual confirmation will be done by the Gate Officer upon physical verification at the campus gate.
            </p>
            """.formatted(
                esc(studentName), esc(studentCode), esc(course), esc(yearLevel),
                esc(ingressDisplay), esc(egressDisplay), deviceRows.toString());

        return pupEmailWrapper("GATE REQUEST NOTIFICATION",
                "Device Ingress &amp; Egress — Both Scheduled", body, issuedDate);
    }

    private static String buildApprovalEmailHtml(
            String studentName, String studentCode, String course, String yearLevel,
            String direction, LocalDateTime scheduledTime, LocalDateTime officialTime,
            List<DeviceEntry> devices) {

        String dirLabel         = "IN".equals(direction) ? "INGRESS" : "EGRESS";
        String officialDisplay  = officialTime  != null ? officialTime.format(DISPLAY_FMT)  : "Pending";
        String scheduledDisplay = scheduledTime != null ? scheduledTime.format(DISPLAY_FMT) : "—";
        String issuedDate       = LocalDateTime.now().format(DATE_FMT);
        String statusColor      = "IN".equals(direction) ? "#15803D" : "#92400E";
        LocalDateTime ref       = officialTime != null ? officialTime : scheduledTime;
        String pdfFilename      = "GatePass_" + dirLabel + "_" + esc(studentCode) + "_"
                + (ref != null ? ref.format(FILE_FMT) : "NA") + ".pdf";

        String placeholder = """
            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="background:#FDF6E3;border-radius:8px;border:1px solid #C9A84C;margin-bottom:24px;">
              <tr>
                <td style="padding:16px 20px;">
                  <div style="font-size:11px;font-weight:700;color:#92400E;font-family:Arial,sans-serif;
                              letter-spacing:1px;text-transform:uppercase;margin-bottom:6px;">
                    Official Gate Pass Attached
                  </div>
                  <div style="font-size:13px;color:#374151;font-family:Arial,sans-serif;line-height:1.6;">
                    Your official <strong>PUP DEVGRESS Gate Pass</strong> is attached as
                    <code style="background:#FEF3C7;padding:1px 4px;border-radius:3px;">%s</code>.
                    Please <strong>download and keep</strong> a copy. Present it to the Gate Officer.
                  </div>
                </td>
              </tr>
            </table>
            """.formatted(pdfFilename);

        StringBuilder deviceRows = new StringBuilder();
        for (int i = 0; i < devices.size(); i++) {
            DeviceEntry d = devices.get(i);
            String bg = i % 2 == 0 ? "#FFF8F8" : "#FFFFFF";
            deviceRows.append("<tr style='background:").append(bg).append(";'>")
                    .append("<td style='padding:9px 14px;border-bottom:1px solid #F0E0E0;font-size:13px;color:#374151;font-family:Arial,sans-serif;'>")
                    .append(esc(d.brand())).append(" ").append(esc(d.model())).append("</td>")
                    .append("<td style='padding:9px 14px;border-bottom:1px solid #F0E0E0;font-size:13px;color:#6B7280;font-family:Arial,sans-serif;'>")
                    .append(esc(d.serial())).append("</td>")
                    .append("<td style='padding:9px 14px;border-bottom:1px solid #F0E0E0;font-size:13px;color:#374151;font-family:Arial,sans-serif;'>")
                    .append(esc(d.deviceType())).append("</td>")
                    .append("</tr>");
        }

        String body = placeholder + """
            <p style="font-family:Arial,sans-serif;font-size:13.5px;color:#374151;line-height:1.7;margin:0 0 20px 0;">
              This is to confirm that the device(s) listed below have been officially recorded
              as <strong>%s</strong> through the campus gate.
            </p>
            <div style="font-size:10px;letter-spacing:1.5px;text-transform:uppercase;color:#9CA3AF;
                        margin-bottom:10px;font-weight:700;font-family:Arial,sans-serif;">Student Information</div>
            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="background:#FFF8F8;border-radius:6px;border:1px solid #E8C8C8;margin-bottom:20px;">
              <tr>
                <td style="padding:12px 18px;border-bottom:1px solid #F0E0E0;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Student Name</div>
                  <div style="font-size:15px;font-weight:700;color:#111827;font-family:Arial,sans-serif;">%s</div>
                </td>
                <td style="padding:12px 18px;border-bottom:1px solid #F0E0E0;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Student Code</div>
                  <div style="font-size:15px;font-weight:700;color:#111827;font-family:Arial,sans-serif;">%s</div>
                </td>
              </tr>
              <tr>
                <td colspan="2" style="padding:12px 18px;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Program / Year Level</div>
                  <div style="font-size:13px;font-weight:600;color:#374151;font-family:Arial,sans-serif;">%s %s</div>
                </td>
              </tr>
            </table>
            <div style="font-size:10px;letter-spacing:1.5px;text-transform:uppercase;color:#9CA3AF;
                        margin-bottom:10px;font-weight:700;font-family:Arial,sans-serif;">Gate Log Details</div>
            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="background:#FFF8F8;border-radius:6px;border:1px solid #E8C8C8;margin-bottom:20px;">
              <tr>
                <td style="padding:12px 18px;border-bottom:1px solid #F0E0E0;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Direction</div>
                  <div style="font-size:14px;font-weight:700;color:%s;font-family:Arial,sans-serif;">%s</div>
                </td>
                <td style="padding:12px 18px;border-bottom:1px solid #F0E0E0;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Status</div>
                  <div style="display:inline-block;background:#15803D;color:#FFFFFF;padding:3px 10px;
                              border-radius:4px;font-size:12px;font-weight:700;font-family:Arial,sans-serif;">APPROVED</div>
                </td>
              </tr>
              <tr>
                <td style="padding:12px 18px;border-bottom:1px solid #F0E0E0;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Scheduled Time</div>
                  <div style="font-size:13px;color:#374151;font-family:Arial,sans-serif;">%s</div>
                </td>
                <td style="padding:12px 18px;border-bottom:1px solid #F0E0E0;">
                  <div style="font-size:10px;color:#9CA3AF;margin-bottom:2px;font-family:Arial,sans-serif;">Official Time</div>
                  <div style="font-size:13px;font-weight:700;color:#7A0000;font-family:Arial,sans-serif;">%s</div>
                </td>
              </tr>
            </table>
            <div style="font-size:10px;letter-spacing:1.5px;text-transform:uppercase;color:#9CA3AF;
                        margin-bottom:10px;font-weight:700;font-family:Arial,sans-serif;">Device(s) Recorded</div>
            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border-radius:6px;border:1px solid #E8C8C8;overflow:hidden;margin-bottom:20px;">
              <thead>
                <tr style="background:#7A0000;">
                  <th style="padding:10px 14px;text-align:left;font-size:11px;color:#FFFFFF;font-weight:700;font-family:Arial,sans-serif;">DEVICE</th>
                  <th style="padding:10px 14px;text-align:left;font-size:11px;color:#FFFFFF;font-weight:700;font-family:Arial,sans-serif;">SERIAL NO.</th>
                  <th style="padding:10px 14px;text-align:left;font-size:11px;color:#FFFFFF;font-weight:700;font-family:Arial,sans-serif;">TYPE</th>
                </tr>
              </thead>
              <tbody>%s</tbody>
            </table>
            <p style="font-family:Arial,sans-serif;font-size:12px;color:#6B7280;line-height:1.7;
                      border-left:3px solid #7A0000;padding-left:12px;margin:0 0 20px 0;">
              The device(s) listed above have been officially recorded in the DEVGRESS system.
              For inquiries, contact your department officer or the system administrator.
            </p>
            """.formatted(
                dirLabel,
                esc(studentName), esc(studentCode), esc(course), esc(yearLevel),
                statusColor, dirLabel,
                esc(scheduledDisplay), esc(officialDisplay),
                deviceRows.toString());

        return pupEmailWrapper(dirLabel + " GATE PASS — APPROVED",
                "Device " + dirLabel + " has been confirmed at the campus gate", body, issuedDate);
    }

    private static String pupEmailWrapper(String headerBadge, String subtitle, String bodyHtml, String issuedDate) {
        String pupLogoBase64 = "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";
        String bpLogoBase64  = "iVBORw0KGgoAAAANSUhEUgAAAHgAAAB4CAYAAAA5ZDbSAAAxKUlEQVR42u2dd5hlRZn/P+9bdc4NHSf1BCYQRxkQUHAVUQdQdMWwpmFldRUTZsC067rqMOZVFxBWRVnFyPoDUVDUXVFmMKAiYYYhB4EJDBM7d997T1W9vz/O7aEBZQ0zCG6/z3Oevn3POXVO1bfe/FZdmKIpmqIpmqIpmqIpmqIpmqIpmqIpmqIpmqIpmqIpmqIpmqIpmqIpmiLADJkahb9SYCeDOwX0XymFa/iHsatZ9H+pz/rX3sE7V1K1W5llV1DD9MM+6fNtI3VbTd8UwH8FYnlahXoacx9JXe5mM1mong+nfv0lwR1hhtjyv/5J/ldvVNlyNFzrb7M7vdl6b+FqfSPAypX4KQ5+tIMMwouYiUlHGrAPpUH7pSH7myFHTvHAXwkHr6nNt9XZ4wHsB1TsWnfUAy3rKfor8FVtN4vkqQnzF/JV223rAz/vKhXwwPeeAvohqHkVr9+0mo5H23uPXc2icA3HP9Le6xEx0+x6OunAD68nq3fqrdZKL/aLuJI7qchT2LELxaeKEHfpu6+mj1mMxE36aoN3+i1pf/akE8+ILKb5f9qKnhBnjYKZadh/uWOaW20inVLV89N2/VGRsf+u8FXNEBFMhLirxKctR82QYO6I1O9+hZOPgCxMC/Tm0JCP0aJjSly3BwrAfkFfWO3H7XZvdoe34lfuaAA7H/dngqsA41fP2jfc2Pf+yeDsKqOtdZV/i230Znd6C9e4Wybe+ZEA7l/eDz61PRA1vwiTkTjI+9Mod1om+5khzPrTB2ligO9cuaiaZXzd9bkPFtf1/YesIHHqruGu5ctREdk/7ZCfp0H7qCHdLKLXmDK07gdC41f5ksav88UAw7+gr3WtP+LP5YIJ7i1WzzrP7ppjxepZ47ZujhVr+r40YU3/We23LWi72j99Ejc/eezK6oIpt2k3+6orV+LNkOLaWSvs7jlWrO4rwpo+K9b0tWz9HCtWzzpnQgXsKrfJHoGhT/+I4uRTETmK0J75IkL6M4yqYOfj5DFyAuOWdqojwzFmSZAjAOS40qo2Q/+M5ylgIoRJn+3/GsBSSrXfc7IcEHvg5z/VYrarpvXIYf2DYY2tIZOFNNrukWA4HGKbAZprZx1MUw4Q2XLenwry5Hv+1EnyaDey5OHwu+18nAjWurrvDSn3y0uGlVtwUgLbVpzlVJNxADXbI5uh3yiunflcwP4YcT3hwrV1ubO2WnhIowx0+cNo3PqHEdz25+UCpxrILhVhthxlGcnWztk/VdIZNsZqADG7jmST38BQMGEYICXm+CqI6Oe4bd/9ZPHtzQkpcD8RvAplFUlWlBxqy3dyu7HiDzfKhIeXwx92HWy2wkRWGJj8sSBP5o4H6bgjURFCWB2PdJ2uGkbj3nb9kpy4bbWNG2K4++SIoJaGSg6WPXFiiN0mi29vTgJusshPtIGxK+bXOHxDQ4Rk58+vsf/oTNTNDIXM9h0yvzEqP6kdsvnOB00SUIF0/nT/N8Fk9B/6ixuWg67YzYA/jH7wUoeZ7Vn57mMWsOqTJbimDxJ55+PudxhuAth2NMpEsAeJwlXlQLVcuCRui2O+Q/uK1o6Db9vYfWssGHIZYjZpUgiDbbAX4hExuWpiokzmXBGstXrWU+Otsz9vt8y+InY1XyqCta6Z88R0UOvmiL8Z02t8TX9ItzvHW1zywLE1kFOBi2bQRZSLQ+I/AQ54GHxlfZjEs8BjBBULre53evZ410J+egRIgvPdZK6U44j3O4Q4AWjjmpn72Z2z5gz8rGeaCDY5hCkrSGZo/XH961PiCqapIempi4+9vSnYXWTCZMPNkIHymbaAAkzT1b9LWtjKRVUxvqJz3Imxkbpc019oN83o0ix9XXNZSJJ6jBAaVjCYAk4fFH9eBW4FpLHCn+tgTrfw5K/3Zv94HMTz+fMidY8EDhZYrvD5sDD7wRKh8o+RkQj+7CVcnwOsXL7UAzTX9L3O7pz932F13yVhdd/37aa+7xfX9X12wr9UkRelpqzvmVO5Naye9S5ZQbqf77mq7I8K30ERVZ5qhphwIxlwPwvX+ttAL2Qg4bDr2hyc2m05EVIxffwNfprbO64PI870eDls01ho6udcly4OQ1YYO403h+EI9+/85yE7CsLXu/N/qQkvGTNrBcN8so/+5wy6bgDbnVGvh0VEH8omATFrdb3L0VFJjASl+8BRtv8rHBfvumtP3zaIZtKlz3Z1ea7rkmPxcqyfqW8KM/q+JoLlj9/6idSw8+nQmZLLR5prZj5Bjir93cngONMfxs0xWeJwEczgWvSBYyg7zBDB5oextJkN0+7cybyGsIpk18+frvAeMkit9HE5ZPP14dq+1/sefXkYTEGETO4DJ+ER1HZOuJXg3wDF13rzF+TYR0cTQSFrJWKv8/N7UvXNKyCt2o1c/DCI6KV6NV8Ie3LJooT/e2PUBM0SI1Ho/JdFrHr8a7/+lQYImcrNDKQYxqwVhi2GUYthS2r5Hn1Zcd2szwP4nvpr49Z0hfZq7ky/ZLfuW5lsDNlyVA7ZfCdN+7Xv1bl248y5YnoV0SZsaCEZZraDK+dNd91aFZVr5djbmxM6F1BZQQrN1rvdXDcnbo1XZhdt+1jzqr7HSZVPpzGL2H2gmBF8XXxqWn9Uf5cZcvUb0KMgnNebHeRj+nphlhI4ykmlTcOcccqnp9N9JMTdxcW6+/XvkQpYi+5XKV11I0VAjYjgMiU/9yUvPj+HpOOF35yaJoAXwbWPPA6mwve4E+Oavk/KXnc3nHPHx+1pg5utB4fhwY/IcURW7eTisk8qFzBNLDZ5ms/dmjhiptK2pBOYaX+rFvagUzHsygkR3/aD09gNcxZph7wlDcZWdJzIsiXe5XaeeqmlAkTaOtoIvkt8inZ7aNrR1YM23bzqSNxhX6A4fxZzCHaxGF3RwLXtahHRwkg9onMWuO6XC9ju4uKHQ0SHJSzPDfcPRrPtLgiCuMRYUHoPvu6iuR8DSdu29m5VFXUONZuUmDeyMJCC9ui7wuq+D8iBm9a5aMenoTSuPfr24qrZR+8U1W0xHWK6hEETcfJM9r9nB8m2qG/PuQgpFcNq7FUGRfXK9uQwLijdm6yRTtXZ2hUH+XDlcVvXhGLrZ1yPOzCMWhDZmQ4Mvld9bNgvdCwurTx+6+qNn59bX3U56auz6Ujj7ns57Nk0olPVzHlUFTWhkucEEZNWeOMycEeyawsRHiaAT3SwIg2y9PFQfYzRNBC9j8HFix8J3vW+Y//6z56z9/NuvL0x6j5BJviaOLNJJovh4lAKrltWtK7te7scsvXnYcTeojVRqdkX7Nbp3SzDJtKPlcdvuz3sSLenxLOlDG6sJZ+Yckala/pvLaY9GU54K9bYSjy34VlGal4z62A/Q14Z18fV+WFbPtS8pu9436OviwMpiODNMITgp6lPI+n89VfWnimHbr+nee2sE+Y8NXxwBaT6mP9O1eSwcbPgMEcqzbHM+TJQXhSuaVhNOOi4Ln+YgO0Oi3o3i+i5pRjT2jFCHUNiG1gE8JninGmIwcJ4/cvzwuVza4fd88+x4Z+Rkt3oe9WbkcxIIoglXBixmHXJaeGama+tHLr13LA5/Zvbw+2TRt3pIiSej2MVFRFMlM/4Ll1oa+ctsCQbJwytGCGODx7vMn12GErbuGXHJjmKIItpimBO+SRO1AV9+dhVcxe6KufEcUtmuJQwUcx1ig9D8d/dgVv+fq9X392I1836UL6HP1cr2vMV5PNdwjGjZsEZXlVRERRwoqgolgwRTTVRall1GcCs3aCH/e7Vv22r1ulRopGQRCyV0YZKRREgRtEUWlHp7pve0fj27Vfs8fbs4I2X2fVLHh9Gtp3u6/JmEoSGldyT0NiwqB3uP4trZg1mT9jynnDdrP3dAv+axtWzfiCHbr0QKOyquTPJQ28qxFIM1yN0xOGElIYOrlPPpQWpoBmX9K2Jt7BKW/qVkMJMN9cfE9aHFdmhW28Ma/t+43LpCCMWMfC5KA6JI+mk7KCtZ9naaQuSZP+pPfosBlP89duGXtap0jmQLDnDJwE1kPbkspRQFSwaMQQtnAPT5y1fuvQ9R15+edxdQOyGdpcpXBAX9nxjWjGyz221vDJDXWEpIIqhCEXLsCQogtOUWjJD3/rKr6R3nfbxswd/8aT39T7v5/3FmpnP0Uw/o3XdKw6m2A7soxkmnpTGeI6/eMtl8fi+m8VkdhhPT/eOF5HLmzSTvjBi5j2SUulwTrJ8I6CqiOYCNSEOJFzOeEzc4w/Ysm9c3fcJna7vDjtSAMTXxCVjJI3ay7NDt3zXrpt5bFQ9x83UebE/ja5+93Bt3SVNpUNSSKgBScA7h5V2B947LBlOlVQUZOrMidhALA48fqR100RI81EgopcIwPjgvANCrM5oFS2LUcR7QVRoNEpwZSIDkLzmaSCddu4r5Sdfefmbe5545XWDv1r0muzgbT/Uu7r3T2PpXNchzudSukMBiOK0zsW8aObBY5vcUwzU53K1duhyTfSFEUsiSAiY2YNSfE4EMYPQsBT6UzAwi9QINi+snrUywbviQJmp8F3iYrQNYdSOyA7d8t3WdX3Lqbvvu16dV2xP/3PVywbu3HpJg6wuSSJaUUfuHI5yAufel0mslHYWaKsoMaVYR6Qjqz69HfXSR4kOnteWDvnBSpUiSAyt0jqJgZ16eEIng6CodumovPaU94Srf3Tk/O59t32xtWaPH7Hn9v3cki2vCaP2wiS2yfeoQ7DYsqQq9Sh6Scf0+H6J5jBcGLUQCqyt9na6NA+Rz1URvIDEgKlKzXXokSKIRcx3iU8tu3pkoHFwbsXd8fq+H2Tz3ak41o3dE9/ymwO2NlprigNcXZBo6spoGk4Er4qlhMWEd44YIk5LLlZVYkzmYhJFH/tosqINFlv5we9/XxC4bWCJ7gRXJoWrkynOjEySP/HNH7PbfvHYVrZo+JhktbXxhr6PZQdvuVj7833SWPqm71aXVcUVI2bOMU+75SRJ1FLEpPSj/yT1I1KK8zBm0SD6DpHUsMvc/lsOq+eVJakju0373HNoprNv/VrzBTf+zdYX9VTl72JVzEXRTBQvgqcUEaRSHcUYwAynjhCKskA7WerwWTYIG1NVTi89tV3rLu1GDt7aDh2xd1uliFHGDVPp7O/USzKJi5N5KrRsaLiHV7zySyM3/nTvc7Q6skHn5e+JN/atCx2tY9wBW46Pw/b3CVvnciwEUhi0WLa7a+yKtjGmlhArbG5c0/exbI77GQljmjzz53M3/0daPvjt6VV95mAk5iaatcVApgpmWDIqziHt7HcMsdTHyUjJzIMhEkczfdkLNvWvO38JubBrS312I8DHtQ0Fmd2elKJOqFQV5xRVxdqRQybxM0A0L7mM2/DgtOnPecGq/S/93glH0z/2/hh1lp/vLo439v23G2xcak17v9ZFKVOIu9yHFEFSw3C57q/T9T0Mp8/okv33uDW7p2dRRa6tePYeiBarIs61B7MiurNIS8yIMeG01AGYYTGhqqQQYoeqG4K3Htc/8vOLemufivd27jNR9fEIt6InkvlXZTNJtznyRUaRMq/a1dEWzwliq0wYWhRSklILT+LoPLfUWZmug8PFVTew4Ck2LNNad8x7R16L74otKyzYkObSl0p9u/tWJUaCn6XSHE7/tG3Jls5Z3bpiSwNGsNRKqDrHuBmFCFEhqTIeExFw3pMEWiFiUr6ic77oMsu2qXz6uKGxU77d6d/UKfrZAW8HHddfrN2VlvRu5GATqIohTfBIKahoNqHVgGYDUgJ1SlZR8oqi7j6OLjk90+GxoZBJftih/varpMtalUPuec+6m3oXIlzje7QvFcTdBq6VE1F61LduLFx28uC/z+mUFcNYyitida8qVlrE+YRJbhBjoqJKpkorhFJU+6wst0xWdJplA95feNzQ2Cnnd7hjFDmrYWZFYT0AF+xCxttNAIvBBQoHtozw1jK0oSZtV1REEBGKQmg2jNAqhUlHt1LvVKodikh5LkX1wYZCDD0HPcHd9Yu/2WPN/H2W3XJPbPg5NMzYXeBGkFywuhC+P0Z6Sz/FVU1r9krMMtFYmKRgVJwQYkLM8AIkI2tXKKSYqLRdpdJP09Alkg1il605+R3HXdSZL/HI/4ttXZWZqz2KQpXHRTDdweGXwtgHhG4PBLPS0NKJlANKjEJoQmscnFfUKY1xMJvQzc4HhoLSsWTzxj1+uOrMF38gnz+4d2PUI7u6DxOCsVsJWyLhwwPoaUNl3VBNxBkuNIxYlHo2V6GqillZ9OVEiLGMctS8J8ZETAkRCb2qfljlyl90NF7whE+tmBUsXSIwLWBRrW1wP7qSDasUVvotPOlDkYHvOunJwEJMgnOTDazyaLXFdgrstK6tXe8qqG+lodQt4cAPrPjIiqv+5ylW7dsmMfidE2GXiOO6YALhh6PI+3eQXd2Eejuom8CahvNQyYTcldmBXNmZO3SU3wnQCoGsdLJDpdXy/SFcc6uv/u3zEtLE/cDDXuNGFMOVUa9QPKoAnktXvojqP4Npi/ETEiM3O+nwMVpU1Z0A3hfoaH9ui/AHllObOVVG08Z7e9JrX/cl+e43Xk4+cztgpKR/HrAVwWpCuL6F/Xs//qvDuJigq4y80TIkAwuQZUKlAopgyQjpvu0DSmtaiKkUzyISqkXhR51etWH6zGftMzg4unXUXVIxecK4ERw4kklMCZKMPooANt3EYWNgL9qTn39okKf153njpVjsxypqWLoPxPsOS5CitOti2989AOS6NJRgvOu9n+L0FR/A15pk1QYx+D8OWANywWpKuLvAzhnAf6Yfv6FAOsAS0JpUa2EgHiwYFiAGw1LJsaUPLKR2KNKJEEIsOmL0rVrlF3fMqh3z1nvuGZAO/W4Vlo4aQcBjmMtySZVqGvM6AHDDLvSFdxvAS1nVbttdlTHvvXvzi6PvGH3SDepGXwYujo24svppEojOC/VOJa8JzsvOChubBLIByRxiiZ5siC986a2c+Jovs/ne2eTT+4nB/X6RbZN0bEWwipA2FNj/G8CdvYNsbROpgfn2ta329dGQmiB1QT0QSmPKK2SuBLaMOZcDKoJ5I3SbZeOV7JKhZx98zCF3D45cWNPvVU2ePZLa4CbDDLJqFSq1odg5YzuUK2ofNRUdQroRMEf1vL1Ztd/tY4f9KNnwa8xqCj61+QJQUhQao2X5f6WuVOpCXhGc536cbIB6Rb2jp7KNlb94On+/7EJW/uBY8hk7UBeJ0T0YWAfUFVMh3dGCbw3izt1BtqaBdoLVgaK0oMuHlLpXarJTyEguZcmCgdPybcyMkBKWDBCTZNar6nfk7j+fMTz+fC74FQM1/UENec5IIqiVhXmiDpdn1q4juvfHT31q/64OTuw2gC9vhyoTcn1iSIR8dsX3fOvgnpW9Gzn0azB8stDtwMVymUMZjkpBaI5BcwxazbK7PhOympDlZdezCvgMihY0mxm9bpDt26bx5reczUff+zHGmxXynkFiLNN0VARqShoz0m/G4Js78Bf0429qoB1ADRjbGTBH2hwsFSnBnZDSjdLPVS9YLEV0ShATExuAxBpIVUS35Pq+Z4+1Xv/tTmZUa3ppBTlmOFlwqt5Xq/hKFV+r4ap1c4alEG+/4IILJuqkHw0cvCwB5LTWJsJwoply7TgoNqadt3TpSr+Og86M9L/X0eMnQC5ZpLSg1ZeVF61mGRQBqHYIHT2KaNvibteHxOioSItOHeGL572G4196IT//8THkvQO4VFDcEkgXD+L+azvZz4bwWwrEtS3jMaDZ5tq8zeWR8nzTSs4v2seEQtGSe60tGZyAQ0KXmTOV/h2d2UuPHmp85Ns5+9QKXVkxjhiOFrw6n1WqqM9Q5xBRUhFSlqKY92t3R1XHbgRYDJbr7Tx9q6KrlQ6NNtzMtfc5O34+7QsAd3PgxyL973f0eMPFiRrwFGWnnwwCJjTHoTFWiu+ieV/y3tox7GRKSspMt53f3rWIN77hi3zw9R9hx+eF6g+2k90yDqNAVUogizawrTaolUl8M6F7BRidxEyudJNUSy6Wtg5SI/ZifjR3v9nSW3na0TvGL/x+1R/RaXq5Nx43kixkzvm8UinviSXbxxBSbpYNI5uD6RcNZNUuXqu0W3Xw0rJkFsMuFXJEcIkdwdPz6gO58jQB7mT/D4tuel9Gl1dUDDNLJZAPDJlPVGXIQ9hQIXrqvkGHG+ZbPzmBV33tf7jgppcTK4pWEjYCsekm1P7OoqWyOmHS4wpK1gyTygRj+54EmgNIqCbTuuK21fxnL33toqc9Y/PoDSur+oqOlC5V2GPciJk6n+d5+fJWdiTFmLwhCUYHSC84fuvWOwDZ1YvRdvPiJ1OQtB+/PsTouDrLnXiX0Wz56OJ038A+fCd974cmVexls3X87GnVbd2hWZCSk2T3JRMNyHOoVIViHGJx3+ozbZteBngPta5SR9YrkR3bKzSo8YR5v+SEg87gyNk/Kjm2EOK4oiSkaK8+abQt5AiMtBl6QoRngg2V4BYNUmoJ9QLdiqzf3pG/ff/+8Qsx+GVF/s0l+acRoK1FVPIMnJswyInJkqqKcy70K887YcfQj5aDX8EDF7484gEun7Gc5XJv9adrcq0cOE5nCrFTY6pHtZlunl72pXl6w62zrX/4p3Lisp+5U49oNsS7OCYJvzOJaECWQaWqhKYRWyXPTsS7dGeKzxAp0zF5RdAYSZYYanVQIDxx1uW8av+zOXrBZWWj4xCDQ2NbPLTaDY2CjbdzJsMGTkgtzBrELOKtJQzm/qvXdXS/Z+m6bZt+VmVhR5RzKsaztkFsCRoRiQiaZ0RVkkHAkpZ7EKSB3C07YcfId1aCP2o3gPuwADwxMz/Y2f2vFdyHm6kZMgveSyBPKVXp1DxBjlJhiDuyp4x8ozjXj7VmVqsMEMh25hOyDLKKYIURmvfvgGsLaWuHR7IM8lxwGI0xI1nESAzRRSJx2LRf8rJ9v8KzZv8PVd+EBsSoWEvQZkKSQaOMXFlLzMaJXvBEaJq7YSDL/nXO+vGLAVbn7iVZSmd5mLsdQgE+AAUCTpEsI4hQJEuuzLSkgap/2T9uHfzW+ZAfd9+02uXkdjfARwKXg/2N7nN3IemN6mp5kXKMmjjfJajEVoqpIKQmNZkX7qgcpJe0bpfDi21p36zK6M6Ilndl+amIlLLPgXOURo+WXDvhoyLlOYvtox0wqco4ubRYN74337/n+fxowzEMNzqZ7TczzQ+W+yoVEM1hLcWaJCeoa5gWov2Nzuyjv+3sfd3edwyuva6Hae9Icnrd7BOIdg2bxSSlc48I4hRRh6lQpJQyEUWkGMz1Zf+4ffjCb3Xmjx2ruyde1Ii3Lge9nF2/ccvDso/T+eCOg/jejtlf7nH+VUliyEV8TkJCATGgKdDtlGaRmMY43bV66+t2evjJyPHVThtSpcDnfqeLUowbrg2wtFNKCogaRaOMEDkp3RwHJDESZRV9xFAiSGTEqoxRZZpu5im9P+N5sy/hqdUr6LGRsmhnBClartVM2ZdH88q/zblt8LcAd+buhVlMn8iN/bZKmR+JItK0MgaJ90RRokDTiF7EFSoj/VV/3Cu2Df/wv/J8cdL41aGKf9mbBpt3tW1He9RxMMASkMuBpZWuW8zSiZXMO7NECEEwI5tIFFuiJ1N6Kx10uuSeohdqTbc2rucZ0kodrubGSVYG/y0K6tocm+B+qQkprZsJv9VJaXz5DLT9V7wQguCkRV1GKSxn7fgBXLztOXx367O5MeyTtCmSEv89a7/+YyvXhK99akezf/1M9ns/nFUr7CPJZMYgBANngqT29gPBIJlhKrSShTriCyf37qj5F7xi28hl3+jMH1tN8eejXr954nDrouWlDk6PSh38QC5+f8esc3qce10ghRzzmRguRSwUSAz0esc07yhCgGhMZ9zu8geNn9M6U9bzxGq39ItzkVC4MsCQlcF/neDissSC0KIsTaVkxEoVAobJfUfRSLRiIk1swSGBSGTMMkbILKMpo+imITrXHKhX/3RV7yvzvNV8e4fSszmQCoRWw3QiDhIEoghBhGYyWhC7nHPDFX/jvR21l77q3h03fbPmn9wZ7aJhJ82VSw7e9/NXXx2FP33bqEcEB09w8Srgiqz+m6bZ66reVUvfNkiIsdy+SiClRLRItIRKokkuc+LG7Oj8PJJq44bwFG21qprZeDsYUnL/xPpunTRtbRJn51lpEDMpQKJqhJB2fpnaZlouDXJaMkpOjwx1vbr+7X0/2XnmMxbErUcWjuqwEE1whkhs6/ckpXeVBFCHiaZuohvLuWRkn/rzXnbHwIZv5vkLK5a+5YUZW2vu+BW/3XDLAaAX7MaNWB7WvRQnuPgDtRlv6fH6H00LwVnyZYQw4WIkx6hIufNKbkaO4UXpsMh0F+wGPazx5eLj3Nx8crXXjUjFj5dL69sguzYziEBolrpYMeqV+/SwSWr/NVqNSJEiQmllj5ExRM403Zye3/EDe239PB7DOlILG2oCOS6qSKuAIkBMQkQoIoynRAMl15RUTa6u7nvxiXPe/XpuPXTwksphbyRxRhV0Q+6/dsJoeOXEeOzeZM/DTBOdWlGfvrLLyZEti9GbObVIbgmPkVECW7EyYVrFyBEcSi8N1GvxvXhi8V17px8s5uZdMoDTYCaOUi2XhcgplIcA1bwU37ENcCmmI8TAcFMYpUbAbEF2e/q7zottWcd3ZKHe62jBcMthRSLFUvQHJ1gORQFJhCJAYcKYKdVUsN1X0ocqr7Kf+KXDPgyaaDG+r9s0z4lRjdvvfknPjx7fWPfqwVNZYcLu3fLwYQe4vTeUfajauzBzstpJ6gYhQ1RTRCyRYWRmZEAVqDCJo1WpS6IzNdni54x/x94VL0//UImxM6vLIJCiF9GJ7SJDsxy/ag6aEuIjiUTAMUqVgJIX2+NB1V+lv+u6SI6qrdQuxpUWjAdHigkrjBggFlBEsIpgXgihDCs3o9BK0Enk527P+AF/Inf5vVyNIVrJwEH0eYGIozrtKDa88KewzMEFcXeP919ku9sJLv5gZ88L61L5TiZDUVNDXUIyBW/gUpkTyICqlTkCb2VOoEIpjusB6h5uruzPN8O7B1YXf9tdsQ41hlAXo5AcoczXOlVSzGhqhSDQodvT4soae2rHj9LTqz+WvXrWKcNoakIjONQnUtNIzdIiT6FMTwYB7RSKAmIUGqb4IhAd9sX82HSGLdNWdJLpKEnFkGTiLYir5pr631oMvOszT2O5v5wV4eEY67/YfsZL2518cfbhE4p0yLleNoWK9LsOHRVvw2Q2Rk6TigRyDDVB8STLLdIFUhl6THbpaU/VK1fOVRpZL9uWDlwzk+a0k3H5Sy10VsZCkyJ5DKgymvbIN9j+tdV2SP5TnlC9QvbK7lbnEQooMsW8EHckLBquo0woNLZBHC1Tl0UsY8mWQXCOFBIdZlzNnPTh7AR+2TpU1YZxviA5wUiIS4X6jqwI/Z9k9JR/guWehwncvyjAk0FeQuMdFSr/XjAcwNxEyd3O+PLOWg5p27mY4EzQO1vUTl8LnyvXSJS0sGJ7PbZ13T/36NZXLPRr/WMq1+r++WpZpLfpTD8umSBWgVYSYtRSdJtR7YM4Ds0t4Orgu8tQ5djGsgAhAlHLndnqlhhRsS+559pZ6cUyTJfUGSIgmCZMEtGsEFfP1A2dE4bfcmIb3AgP31bDf/EdyZez3K9gRXgiN73PMfdDgYGoklRMRSijVmA7gxkO27nzmFIho4vA4LpZes2XXi9/n8+muR+JOVXHLDL2qTt8vQvRKowPgWlZIFObZmS1RGvLxPIZyDrAVaC1HawFlTltHbsDGiNC9EotRdTgJ/K49CleIVfGx0otDSPSomVleZFJNM2IrVDzzo18oRh9wxvKHf2OSw8nuI8IgEtONn85Ep7AXe+u5r2fcJUxa40XloJqrdZOzzZoL+qSdmrQEDVTZ6lC1WWSM5ff8KTwRR7P9+m1MRoChcupVKBzWkQaZWrQKfgqVKZDGIQ03B6JCFlXCXhzO0gO0q2EqPixQAW4tlhg/2HH8UN7OgZS02GalmhFI1giy0khJtGsJjGNfKIYP/GfS3CXpV29w+6jBuDJIB/R9dtXxfHOc7JcsvGx8ZBn4idEdSzKyIyqoY6dzCDlfhipkXUy3HTMlevlcPkvPZyLZCH34BXoAskcMm5kaqgaeQe4HIodJcdKO1nhalgYJ6UWdHQgCnKrzeXL4YVcWDyTQeuQHhnGJDIejSIV7Z2/Ukh4b1hMMn5SaL7hs38pzn3EATwZ5Kf33vE0r11fGx+oLUIHQuZLvVw07otKgVG0k/6K4QHnE60QGafGGFV65B4OkB/zdPcdHud/zcxaC4vQKCDh8A5q0xNpxEgNUE9KDZLP8T1VCE3P9ezL+eFv+R93FDusk2roBytoJKEIiSSJoCkVsTDTTpekeZda89XN+PpVD7dB9YgHuAR5pb+co8LS+q/ntBoLPqt0vUh0FHGtYIV4myhyc0YME1UdbYAdtJKRLGASaVhGqHbgKi1mNW/moPRjDq/8mP3CdfQUoVzt1wUpF5wq+ZiRN2BAxVxVLvv8+KuGv9x6xV4b07Rps/zWufjkx5JISGUmyrtIEYvoLDhRwaR1Hs0dp4xw8tZHAriPSIABlmHugrICiifrxtdmLvuoS7W+KAOEYNEjzjsjxfsqpRXwYrg6RIysE8bHIpVqYDwYI80qA80MxyiLardxsPsVjwtXcGB9LfN0m1VbjPjEGjV+MOC5eN4OuRHgcmpzQ9bzzqHU+fp73ezKBpvFJpth2/yMNKr1fDTU/G1h+h0N5987WpxwftvTd+Xiu788PYJ/18dkOcgKJD25dsse2uj+QKUmr7XxmmvZEBWXAmYuJRFtlwQ4DJ9DYYlqLxTJCEWiIKFZWUAzPGKMWIb05IRWkx7dZAsbN+Fc89pL3dFnDjQev5L5bF67ITvMQjxBsBd1wIxQhriTCiYJlwEDjsHhWvaZ1t7FJ49aw8AylrkLOP8vYkw9CgF+MDc/d+7dh45srfxTXrWXdNU7XbM5zNhoK1o5+ppRgk0VxCekAuOjEckSUSJFkQgpUMSCSpcRvGdYqmxv5YSRUfZJt3GE/wnPkYu3L2LzjLqDoZbRTNISxFcR9QbDKv0h81+7d1bXmS9ev/WORxrXPqoAnuDmZaATQC9btOGQgc3uRCW91FnHLJdFYhqlaMao0Uwyk7w7aoiGVESGB43gleQcIw1hrBWp1ceYUd/C7OImFrd+zZLWlWmx3Jq6HFooGjzBZSCD+EoqFzW0VK4vnPv6tr7s68/fML5xUtj1L2Yl/5UAPBEUKX/jYUU7anU4t/d1VarPq3XyUuftKdKq9oQhxVUTdCVGRloEWsnriLnYb3XbSG+6kzl2E4vkRha439I9PkgFFEVNoT5LyaMRhozhpiBwkymXNnP9zpnHvvhnF1xQJggM3KlgK3hk/U7SoxrgyUDfCDLB0QAvqt841ywePj1cdlSPv+GQPNyzTyVu6+tmwPXKAF3WT6c1qQNdWRkhiwLNhpIQohjibEfmuDsVrE0mv5Kqu2LTc194/XEX3Jf1WQn+yPJWezSM1aP8xxNNlrLKXc6RaXIsGmAzdF4JcwdgjwSzUWaA7xa0UhERJ9bMCaOVxA4vskWrek/TFff87dCDf5B6JfitYI9kUfx/gEyWYW4pK31ZnDOxc5Vy/5K8P6AlcCvL7KQ+2plAdtG99hDX2O8DpH2J/RHnHtje72jf2r+fI5wKcgDIDSBHts/eOum9F7fv3dr+e0OpUycK4H7fu0/+/qHG4IHX/S/v/VDP29nMX1J6nO9+N0gP0qBaHpOvm/j/oc5Nvv8PaRf5/ddObv/8XVR4+KDn84e990ON1R86prtJJP7v5x54zaROzv5qB4vO7f3Dzj2w3YlrJybApHvnfr7+u+99yIGWB08o+z0T5IET8qHaMnnwd7/rOQ81niYsOreXRcurD4OILuOruvDT/2zUX0Ma7yxT59kGLH7fqumD3L4jwoog808/DTftWNLQWlt/0rKyQysS8z5xuLjO90E8GnBIdp1Z80OsP/li5n3qKeLq/wrp6DInqL824rtZ/7ardMGn/9XczFdK3PbVtP7kj7DgjFeJm/Ve4raLbP3J/1y22/VeJD4DMw/5WrPxD7PhlItYsjzjxhUtFpx5gkjtX7DxHKQC/i6z1mfYcPI3ZMGZ38DPONSaO16NpgMkn/NuKe4tn1WCoTJ/+g9wHXtabBwrpBPQ+jJsrF7us+pvslS8n9mVa2Vz41eI7zS1pRLjqfhpR1mx471sPOVCmX/mx/EzXmRp24dZd/LXAHT+6e8z1/sK4tB6697xXG48tYALVOff8z7TymshLQA3jjXPtfUnv7X8gc8Vf5B79keuD76x3CI4yTNwtcXApRhfRdzB+O730giLYUVg/unPxvW8HeJjwF7CwjP3ghWJPc46UnzX5Wh+LJZ+DOkXmByASZ09/uNJ4rtXodmxWLoUi7fjuo4U0rfY99MVQ54BxWKT/EMsOO0AQR6H61hsJuuZf9bjxPdejmbPw+JlkH6G2P6g00u9dkBbg9lSXH0xMA52M+oPF5Gvs/DTSzA7Eiseg8RtAs8CtzgZA2W/VyTmdc1F3DGk8f3IwjDwHDRbDHY32Fa061kCZ7Np7ABc5xMgzKSwAng+xMWi/mzmfHoWcASiiyd+eY35ZzzRtP4hSI9B82cyPPMgEGP+Pe+0rO9UrNhuqbUc9HJDftPuzx/MmPrHcfsFkUM/nwF7k8YAqyLaDRaJg+fihm5j9ic7RCpnk8ZvI418G9crJJ4MIJL+Ha1nlkbfbBtOfr6F6nPNpSez4aT/Ek1noZXM0uibbMNJLzCJzyb2j4JbRCsegNmc8pkqgjsPYylxhyGsEbGP4+qZheGTbf3JzzPtfq6pHc6Gk74ICDcua28wJvtiMZrZm6xiL8SKcYwm0e2J6Ezi4DZcvg04kDgApLWl1FquSLYvWjcsXUPTBWAf4khhFo4x0nvAAmIF6vZHKgnjCiTVEJlNHAbNZ4rnmwiLCNsLCq4rx8Sfg8V7SaPfQOsG6emUS9eWgoLRLZJ3mo1+jvUnfbnE4Q8Pif4RAC8vZ822sT0QFpIa44gsAHsqqGB47l7REO8/gO/ZEyvWmMlmxJuIPbWtQw4kDicS3wMQ3zhTkr+UhWc+F9iLOJKQdDEASWcgFQ9pK8l7xO2Nhe2kxhakchDiDyMONzCawBOJwwnPdwEkDp4mVruUPU5/Ujnjxdj3092IPZY06kT4nrR0ENddQ+wTuNTC9eYga3GtHNF9SWPDzK7/skz5rUggj0NrILKazGahWQ/ETPCbRGs/guDN5GOoLURzReQanF+I63LA3aTxETQ/GskWQLyXTaesY4/TT8L3HowVaw3ZCIKYHAWYpeKNhG1fRBXEv1vcjItZcNbJZX+W+90AcFssJNkP7cgw7rFk7zTsbLSiiB3J/DOeiKu+g9AP4l8q6t9EHBLgGGK3ILIV7VBRPsHCM18B7gXQmkXKVgNjuLpKch9n/hmvFnHfwvVWDPsoThy+t4Jxham8vL0bXsJkE1W7GWwMrapE928sPPMViHsh1phF1N/ufP1x2xPxM7DWCKaXgX7Fiv6X2/qTP4DZk5DcwK4huj3QeobZKJubr2fhOW9l/qeXitp+iBMzribZfiW3sQ50FTF9zor+Z7DhpIsk8TQsYsa1xHgA2mkGXzHRdyI55W+w8Bv2OmO2uOrHiYMgcoyI/yfSsCDpcGZ/skM1f5XBz410Eql5Vbn1T/qjreg/AuAbJlb/HAoKIvuIr/9StHY6qXGXib5T0NPQmjfSq0xa+1gMh5Ba/Wh9P1K+wIhvJjUHcL3Hi/Z+DXG9WDiXDW/aaGLvJDV24HteKX7Gl8DtSXHvx1l/8hmkdCSSAWxg3Uk/JjXOxvUocDu3nzxkwrvKe7uPa7fbR2p+mXtP2rZztosegOsVkKttw9teaOvecgIb3nZeWSDPYYiImVxNsscjHlTniKt/RnzPWYg9rxTvCYS1IIehHYLZebbhbS+0DW96MxvfcRmLzq0iciBpHJy7RZQnQxKMdax72xdIoz/FdYkh10uQM3DdNbPiFJOwj4XwGCyuQ2ozqORvNeED4qefK9p5CZodRrHtfEL1C6W1vSLuDiu6dMT3+PR8fG0moSiXwbu8IN58NxtOH2f+5/4GRsbY8O7rd94176zF5D01QnM9G16/g4WnzSVVD0KJaLqNu956904Lu++M2VRqByMx4VprufOUzYAw7zPzyXpm4Ibu4bebt7H0SOWuuw4kDG1j48kbAWOvM2YTagdDy1B/G3e/6a773A8x9v5sH7F7Hq1tO9g0byP0K9xjsCIw76zFZNPqNDbdRsV1I9NmE4YN1YBVBW1tJNBH5mtk4zcxarPJZ06nNbqBTdP6ObRfuXpaghuEPaYtwUy5Z+B65nfviZveSTb0W24/aZh9z+yimLE3NroZacwhWGTjKdftHKuFp+2NzutC7r2X4BziHgtWA72TdW+68REUFfsd/unv9Rsf6Ns+lKP/x/i29mgJMcr/HiTZ2Uf5M0D5YwIcpz7gvhXt8N7ES0720XZ+177GpNwsHOAGu/+1k89NLjM1uS+UJ+0wok3+n4dud3Ibp/Lgc8u1/F7a+xw9sH+n2n3frUi/431+x2Rbke7f7kTfJ+6b3N4D32Pi/IQ79Lv6M0VTNEVTNEVTNEVTNEVTNEVTNEVTNEVTNEVTNEVTNEVTNEVTNEVTNEVTNEX/l+n/A8nxAQInB9q7AAAAAElFTkSuQmCC";
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
            <body style="margin:0;padding:0;background:#F3F4F6;font-family:Arial,sans-serif;">
            <table width="100%%" cellpadding="0" cellspacing="0" style="background:#F3F4F6;padding:28px 0;">
              <tr><td align="center">
                <table width="650" cellpadding="0" cellspacing="0"
                       style="background:#FFFFFF;border-radius:2px;overflow:hidden;
                              border:1px solid #cccccc;box-shadow:0 2px 8px rgba(0,0,0,0.08);">

                  <!-- ===== HEADER ===== -->
                  <tr>
                    <td style="background:#FFFFFF;padding:16px 30px 10px 30px;">
                      <table width="100%%" cellpadding="0" cellspacing="0">
                        <tr>
                          <td valign="middle" width="70">
                            <img src="data:image/png;base64,%s"
                                 width="60" height="60" alt="PUP Seal"
                                 style="display:block;border:0;" />
                          </td>
                          <td valign="middle" style="padding-left:12px;">
                            <div style="font-size:8px;color:#444444;font-family:Arial,sans-serif;line-height:1.4;">
                              Republic of the Philippines
                            </div>
                            <div style="font-size:13px;font-weight:700;color:#7A0000;font-family:Arial,sans-serif;line-height:1.3;margin-top:1px;">
                              POLYTECHNIC UNIVERSITY OF THE PHILIPPINES
                            </div>
                            <div style="font-size:8.5px;color:#333333;font-family:Arial,sans-serif;margin-top:1px;">
                              OFFICE OF THE VICE PRESIDENT FOR CAMPUSES
                            </div>
                            <div style="font-size:11px;font-weight:700;color:#7A0000;font-family:Arial,sans-serif;margin-top:1px;letter-spacing:0.3px;">
                              SANTA ROSA CAMPUS
                            </div>
                            <div style="font-size:8px;color:#555555;font-family:Arial,sans-serif;margin-top:1px;">
                              City of Santa Rosa, Laguna
                            </div>
                          </td>
                          <td valign="middle" align="right" width="80">
                            <img src="data:image/png;base64,%s"
                                 width="68" height="68" alt="Bagong Pilipinas"
                                 style="display:block;border:0;margin-left:auto;" />
                          </td>
                        </tr>
                      </table>
                      <!-- Gold/maroon divider line matching the certificate -->
                      <div style="border-bottom:2.5px solid #7A0000;margin-top:10px;"></div>
                    </td>
                  </tr>

                  <!-- ===== DATE + TITLE ===== -->
                  <tr>
                    <td style="padding:20px 40px 6px 40px;text-align:right;">
                      <span style="font-size:12px;color:#374151;font-family:Arial,sans-serif;">%s</span>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:6px 40px 4px 40px;text-align:center;">
                      <div style="font-size:17px;font-weight:700;color:#111827;font-family:Arial,sans-serif;
                                  letter-spacing:6px;text-transform:uppercase;">
                        NOTIFICATION
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:4px 40px 18px 40px;text-align:center;">
                      <div style="font-size:11px;color:#6B7280;font-family:Arial,sans-serif;font-style:italic;">
                        %s
                      </div>
                    </td>
                  </tr>

                  <!-- ===== BODY ===== -->
                  <tr>
                    <td style="padding:0 40px 20px 40px;">
                      %s
                    </td>
                  </tr>

                  <!-- ===== SIGNATURE BLOCK (matches certificate) ===== -->
                  <tr>
                    <td style="padding:10px 40px 28px 40px;">
                      <table width="100%%" cellpadding="0" cellspacing="0">
                        <tr>
                          <td width="55%%" valign="top">
                            <div style="font-size:11px;color:#374151;font-family:Arial,sans-serif;margin-bottom:32px;">Noted:</div>
                            <div style="font-size:11px;font-style:italic;color:#374151;font-family:Arial,sans-serif;margin-bottom:2px;">
                              &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;/s/
                            </div>
                            <div style="border-bottom:1px solid #111827;width:170px;margin-bottom:4px;"></div>
                            <div style="font-size:12px;font-weight:700;color:#111827;font-family:Arial,sans-serif;">
                              Dr. Leny V. Salmingo
                            </div>
                            <div style="font-size:10px;color:#374151;font-family:Arial,sans-serif;">Director</div>
                          </td>
                          <td width="45%%" valign="top" align="right">
                            <div style="font-size:11px;color:#374151;font-family:Arial,sans-serif;margin-bottom:32px;">Approved by:</div>
                            <div style="font-size:11px;font-style:italic;color:#374151;font-family:Arial,sans-serif;margin-bottom:2px;text-align:right;">
                              /s/&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                            </div>
                            <div style="border-bottom:1px solid #111827;width:170px;margin-left:auto;margin-bottom:4px;"></div>
                            <div style="font-size:12px;font-weight:700;color:#111827;font-family:Arial,sans-serif;text-align:right;">
                              %s
                            </div>
                            <div style="font-size:10px;color:#374151;font-family:Arial,sans-serif;text-align:right;">
                              Head, Student Services
                            </div>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>

                  <!-- ===== FOOTER ===== -->
                  <tr>
                    <td style="padding:0 40px 10px 40px;">
                      <div style="border-top:1px solid #cccccc;padding-top:10px;">
                        <table width="100%%" cellpadding="0" cellspacing="0">
                          <tr>
                            <td valign="top">
                              <div style="font-size:8.5px;color:#555555;font-family:Arial,sans-serif;line-height:1.8;">
                                LCA Boulevard, Brgy. Tagapo, City of Santa Rosa, Laguna<br>
                                Direct Line: 0961-8023780<br>
                                Website: <span style="color:#7A0000;">www.pup.edu.ph</span> | inquiries: starosa@pup.edu.ph
                              </div>
                            </td>
                            <td valign="top" align="right">
                              <div style="font-size:8px;font-weight:700;color:#7A0000;font-family:Arial,sans-serif;
                                          letter-spacing:0.3px;text-align:right;line-height:1.7;text-transform:uppercase;">
                                A Leading Comprehensive<br>
                                Polytechnic University in Asia
                              </div>
                            </td>
                          </tr>
                        </table>
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:0 40px 14px 40px;">
                      <div style="font-size:8px;color:#888888;font-family:Arial,sans-serif;">
                        File Name: DEVGRESS System Notification
                      </div>
                    </td>
                  </tr>

                </table>
              </td></tr>
            </table>
            </body></html>
            """.formatted(pupLogoBase64, bpLogoBase64, issuedDate, subtitle, bodyHtml, ADMIN_NAME);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}