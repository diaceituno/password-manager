package drive.definition;

public interface BlobUploader {

    public FileRequestResult<Void> upload(byte[] blob);
}
