package com.concessionaria.gomez.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.util.Set;

public class ImagemValidator implements ConstraintValidator<ImagemValida, MultipartFile> {
    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null) return true; // @NotNull fornece a mensagem de obrigatoriedade.
        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024 ||
                !Set.of("image/jpeg", "image/png", "image/webp").contains(file.getContentType() == null ? "" : file.getContentType())) return false;
        try (var stream = file.getInputStream(); ImageInputStream image = ImageIO.createImageInputStream(stream)) {
            if (image == null) return false;
            var readers = ImageIO.getImageReaders(image);
            if (!readers.hasNext()) return false;
            var reader = readers.next();
            try {
                reader.setInput(image);
                String format = reader.getFormatName();
                boolean matches = ("JPEG".equalsIgnoreCase(format) && "image/jpeg".equals(file.getContentType())) ||
                        ("PNG".equalsIgnoreCase(format) && "image/png".equals(file.getContentType())) ||
                        ("WEBP".equalsIgnoreCase(format) && "image/webp".equals(file.getContentType()));
                int width = reader.getWidth(0), height = reader.getHeight(0);
                return matches && width > 0 && height > 0 && width <= 12000 && height <= 12000 && (long) width * height <= 40000000;
            } finally { reader.dispose(); }
        } catch (Exception ex) { return false; }
    }
}
