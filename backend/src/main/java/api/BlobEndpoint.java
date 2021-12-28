package api;

import drive.definition.BlobDownloader;
import drive.definition.BlobUploader;
import drive.definition.FileRequestResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
public class BlobEndpoint {

    @Autowired
    private BlobDownloader blobDownloader;

    @Autowired
    private BlobUploader blobUploader;

    @GetMapping("/")
    public ResponseEntity<String> getBlob() {
        FileRequestResult<byte[]> downloadResult = blobDownloader.download();
        String message = downloadResult.getMessage();
        if (downloadResult.getStatusCode() == 200) {
            message = new String(downloadResult.getData(), StandardCharsets.UTF_8);
        }
        return new ResponseEntity<String>(message, null, downloadResult.getStatusCode());
    }

    @PostMapping("/")
    public ResponseEntity<String> postBlob(@RequestBody byte[] blob) {
        FileRequestResult<Void> uploadResult = blobUploader.upload(blob);
        return new ResponseEntity<>(uploadResult.getMessage(),null, uploadResult.getStatusCode());
    }
}
