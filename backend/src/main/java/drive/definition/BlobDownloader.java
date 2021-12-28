package drive.definition;

public interface BlobDownloader {

    FileRequestResult<byte[]> download();
}
