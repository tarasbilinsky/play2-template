package base.models.exceptions;

public class UploadException extends RuntimeException {
    public UploadException(String message){
        super(message);
    }
    public UploadException(String message, Exception e){
        super(message, e);
    }
}
