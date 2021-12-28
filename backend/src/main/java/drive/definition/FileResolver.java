package drive.definition;

import drive.model.DriveFile;

import java.util.List;

public interface FileResolver {

    FileRequestResult<DriveFile> resolve(List<String> keys);

}
