package drive.definition;

public interface BlobDownloader {

    DriveRequestResult<byte[]> download();
}
