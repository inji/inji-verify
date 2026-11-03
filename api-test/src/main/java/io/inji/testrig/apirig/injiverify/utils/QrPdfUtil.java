package io.inji.testrig.apirig.injiverify.utils;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

/**
 * Reads the QR code printed in an Inji VC PDF and, for a data-share QR
 * (INJI_OVP://...), extracts the dcql_query exactly like the Verify UI does.
 */
public class QrPdfUtil {
	public static final String QR_PDF_FOLDER = "/qrpdfs/";
	public static final String OVP_HEADER = "INJI_OVP://";
	private static final int RENDER_DPI = 300;

	private QrPdfUtil() {
	}

	public static boolean isPdfFileName(String value) {
		return value != null && value.toLowerCase().endsWith(".pdf");
	}

	/** Renders every page of the PDF and returns the text of the first QR code found. */
	public static String readQrTextFromPdf(String pdfFileName) throws Exception {
		try (InputStream in = QrPdfUtil.class.getResourceAsStream(QR_PDF_FOLDER + pdfFileName)) {
			if (in == null)
				throw new IllegalArgumentException("PDF not found in resources" + QR_PDF_FOLDER + ": " + pdfFileName);
			try (PDDocument document = PDDocument.load(in)) {
				PDFRenderer renderer = new PDFRenderer(document);
				Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
				hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
				hints.put(DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE));
				for (int page = 0; page < document.getNumberOfPages(); page++) {
					BufferedImage image = renderer.renderImageWithDPI(page, RENDER_DPI);
					BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
					try {
						return new MultiFormatReader().decode(bitmap, hints).getText();
					} catch (NotFoundException e) {
						// no QR on this page, try the next one
					}
				}
				throw new IllegalArgumentException("No QR code found in PDF: " + pdfFileName);
			}
		}
	}

	/** Returns the dcql_query JSON from an INJI_OVP:// data-share link. */
	public static String extractDcqlQuery(String qrText) {
		if (qrText == null || !qrText.startsWith(OVP_HEADER))
			throw new IllegalArgumentException("QR is not a data-share (INJI_OVP://) QR code");
		String url = qrText.substring(OVP_HEADER.length());
		int queryStart = url.indexOf('?');
		if (queryStart < 0)
			throw new IllegalArgumentException("Data-share link has no query string");
		for (String pair : url.substring(queryStart + 1).split("&")) {
			int eq = pair.indexOf('=');
			if (eq > 0 && "dcql_query".equals(pair.substring(0, eq)))
				return URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
		}
		throw new IllegalArgumentException("Missing dcql_query in data-share link");
	}

	public static String readDcqlQueryFromPdf(String pdfFileName) throws Exception {
		return extractDcqlQuery(readQrTextFromPdf(pdfFileName));
	}
}