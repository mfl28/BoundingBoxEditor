package BoundingboxEditor.exceptions;

public class AnnotationToNonExistentImageException extends RuntimeException {
    public AnnotationToNonExistentImageException(String message) {
        super(message);
    }
}
