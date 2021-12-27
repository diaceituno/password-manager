package drive.model;

import java.util.List;

public class ListFilesResponse {

    private List<DriveFile> files;

    public List<DriveFile> getFiles() {
        return files;
    }

    public void setFiles(List<DriveFile> files) {
        this.files = files;
    }
}
