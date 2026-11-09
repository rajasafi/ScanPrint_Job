package com.printdoc.api.job;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class DocumentStorageService {
    private final Path root;
    public DocumentStorageService(@Value("${printdoc.storage.directory:./data/uploads}") String directory) throws IOException {root=Path.of(directory).toAbsolutePath().normalize();Files.createDirectories(root);}
    public String store(MultipartFile file) throws IOException {String storage=UUID.randomUUID()+".upload";Path target=root.resolve(storage).normalize();if(!target.getParent().equals(root))throw new IOException("Invalid storage key.");Files.copy(file.getInputStream(),target,StandardCopyOption.REPLACE_EXISTING);return storage;}
    public void delete(String storageName) throws IOException {if(storageName==null||!storageName.matches("[0-9a-fA-F-]{36}\\.upload"))return;Files.deleteIfExists(root.resolve(storageName).normalize());}
    public Path getRoot(){return root;}
}
