package com.event.registration.service;

import com.event.registration.dto.ReceiptResponse;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

/**
 * Generates a styled PDF receipt using the OpenPDF library.
 * The PDF is sent as an email attachment after successful payment.
 */
@Service
public class PdfGenerationService {

    public byte[] generateReceiptPdf(ReceiptResponse r) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document();
            PdfWriter.getInstance(doc, out);
            doc.open();

            // ── Title ──
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(55, 0, 140));
            Paragraph title = new Paragraph("Event Registration Receipt", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(6);
            doc.add(title);

            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(100, 100, 100));
            Paragraph sub = new Paragraph("Event Management System", subFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(20);
            doc.add(sub);

            // ── Divider ──
            doc.add(new Paragraph("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                    FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(200, 200, 200))));

            // ── EVENT DETAILS ──
            doc.add(section("EVENT DETAILS"));
            doc.add(row("Event Name", r.getEventName()));
            doc.add(row("Date",       r.getEventDate()));
            doc.add(row("Venue",      r.getVenueName() + ", " + r.getVenueCity()));
            doc.add(spacer());

            // ── RECEIPT DETAILS ──
            doc.add(section("RECEIPT DETAILS"));
            doc.add(row("Receipt No",  r.getReceiptNo()));
            doc.add(row("Ticket No",   r.getTicketNo()));
            doc.add(row("Amount Paid", "₹ " + r.getAmount()));
            doc.add(row("Payment ID",  r.getPaymentId()));
            doc.add(row("Payment Mode", r.getPaymentMode() != null ? r.getPaymentMode() : "RAZORPAY"));
            doc.add(row("Status",      r.getStatus()));
            doc.add(row("Issued At",   r.getIssuedAt() != null ? r.getIssuedAt().toString() : "—"));
            doc.add(spacer());

            // ── REGISTRANT ──
            doc.add(section("REGISTRANT"));
            doc.add(row("Registrant ID", String.valueOf(r.getRegistrantId())));
            doc.add(row("Email",         r.getRegistrantEmail()));
            doc.add(spacer());

            // ── Footer ──
            Font footer = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, new Color(120, 120, 120));
            Paragraph footerPara = new Paragraph(
                    "Please present this receipt at the event entrance. Thank you!", footer);
            footerPara.setAlignment(Element.ALIGN_CENTER);
            doc.add(footerPara);

            doc.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    private Paragraph section(String heading) {
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(55, 0, 140));
        Paragraph p = new Paragraph(heading, f);
        p.setSpacingBefore(10);
        p.setSpacingAfter(4);
        return p;
    }

    private Paragraph row(String label, String value) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ": ", labelFont));
        p.add(new Chunk(value != null ? value : "—", valueFont));
        p.setSpacingAfter(3);
        return p;
    }

    private Paragraph spacer() {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(5);
        return p;
    }
}
