package com.lutetiae.utils;

import com.google.zxing.WriterException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class QRCodeTest {
    @Test
    public void testGenerateQRCode() throws WriterException {
        QRCode.generateQRCode("http://localhost:8080", 1, 1);
    }
}
