package drive.definition;

public interface BlobUploader {

    public DriveRequestResult<Void> upload(byte[] blob);
}
